package com.oskopek.transporteditor.persistence;

import com.oskopek.transporteditor.model.domain.PddlLabel;
import com.oskopek.transporteditor.model.domain.VariableDomain;
import com.oskopek.transporteditor.model.domain.action.ActionCost;
import com.oskopek.transporteditor.model.domain.action.TemporalQuantifier;
import com.oskopek.transporteditor.model.domain.action.functions.*;
import com.oskopek.transporteditor.model.domain.action.predicates.*;
import com.oskopek.transporteditor.model.domain.actionbuilder.DriveBuilder;
import com.oskopek.transporteditor.model.domain.actionbuilder.DropBuilder;
import com.oskopek.transporteditor.model.domain.actionbuilder.PickUpBuilder;
import com.oskopek.transporteditor.model.domain.actionbuilder.RefuelBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VariableDomainIO implements DataReader<VariableDomain>, DataWriter<VariableDomain> {

    private static final Map<String, Class<? extends Predicate>> predicateNameMap = new HashMap<>();
    private static final Map<String, Class<? extends Function>> functionNameMap = new HashMap<>();

    private static final Configuration configuration = new Configuration(Configuration.VERSION_2_3_25);

    private static String parseName(String contents) {
        return contents.split("\n")[0].replaceAll(";", "").trim();
    }

    private static Stream<String> normalizeInput(String contents) {
        String[] lines = contents.split("\n");
        return Arrays.stream(lines).map(String::trim).map(s -> s.replaceFirst(";.*", "")).filter(s -> !s.isEmpty());
    }

    private static Map<String, Class<? extends Predicate>> parsePredicates(String contents) {
        List<String> predicates = new ArrayList<>();
        String normalized = normalizeInput(contents).map(s -> s.replaceAll("\\s+", "")).collect(Collectors.joining(""));

        String individualPredicateRegex = "\\(([-a-zA-Z]+)[^)]*\\)";
        Pattern predicatePattern = Pattern.compile("\\(:predicates(" + individualPredicateRegex + ")*\\)");
        Pattern individualPredicatePattern = Pattern.compile(individualPredicateRegex);

        Matcher predGrpMatcher = predicatePattern.matcher(normalized);
        if (predGrpMatcher.find()) {
            String predicateGrp = predGrpMatcher.group(0);
            Matcher predMatcher = individualPredicatePattern.matcher(predicateGrp);
            while (predMatcher.find()) {
                predicates.add(predMatcher.group(1));
            }
        }

        return predicates.stream().filter(p -> !"capacity-predecessor".equals(p)).collect(
                Collectors.toMap(p -> p, predicateNameMap::get));
    }

    private static Map<String, Class<? extends Function>> parseFunctions(String contents) {
        List<String> functions = new ArrayList<>();

        String normalized = normalizeInput(contents).map(s -> s.replaceAll("\\s+", "")).collect(Collectors.joining(""));

        String individualFunctionRegex = "\\(([-a-zA-Z]+)[^)]*\\)(-[-a-zA-Z]+)?";
        Pattern functionPattern = Pattern.compile("\\(:functions(" + individualFunctionRegex + ")*\\)");
        Pattern individualFunctionPattern = Pattern.compile(individualFunctionRegex);

        Matcher funcGrpMatcher = functionPattern.matcher(normalized);
        if (funcGrpMatcher.find()) {
            String funcGrp = funcGrpMatcher.group(0);
            Matcher funcMatcher = individualFunctionPattern.matcher(funcGrp);
            while (funcMatcher.find()) {
                functions.add(funcMatcher.group(1));
            }
        }

        return functions.stream().collect(Collectors.toMap(f -> f, functionNameMap::get));
    }

    private static Set<PddlLabel> parsePddlLabels(String contents) {
        Set<PddlLabel> pddlLabels = new HashSet<>();
        if (contents.contains(":action-costs")) {
            pddlLabels.add(PddlLabel.ActionCost);
        }
        if (contents.contains(":goal-utilities")) {
            pddlLabels.add(PddlLabel.Numeric);
        }
        if (contents.contains(":durative-actions")) {
            pddlLabels.add(PddlLabel.Temporal);
        }
        return pddlLabels;
    }

    private static DriveBuilder parseDriveBuilder(PddlParser.StructureDefContext context) {
        PartialBuilder builder = parseGenericBuilder(context);
        return new DriveBuilder(builder.getPredicates(), builder.getEffects());
    }

    private static ActionCost parseEffects(List<PddlParser.CEffectContext> cEffectContextList,
            List<Predicate> effects) {
        cEffectContextList.stream().map(e -> parseAtomicTermFormula(e.pEffect().atomicTermFormula())).forEach(
                effects::add);
        Optional<Integer> optionalCost = cEffectContextList.stream().map(e -> e.pEffect().atomicTermFormula()).filter(
                f -> "increase".equals(f.predicate().NAME().getText())).map(f -> Integer.parseInt(f.term(1).getText()))
                .findFirst();
        return ActionCost.valueOf(optionalCost.isPresent() ? optionalCost.get() : 0);
    }

    private static Predicate parsePredicate(PddlParser.GoalDescContext goalDescContext) {
        if (goalDescContext.getText().startsWith("(not")) {
            return new Not(parsePredicate(goalDescContext.goalDesc(0)));
        }
        return parseAtomicTermFormula(goalDescContext.atomicTermFormula());
    }

    private static Predicate parseAtomicTermFormula(PddlParser.AtomicTermFormulaContext atomicTermFormulaContext) {
        String goalDescName = atomicTermFormulaContext.predicate().NAME().getText();
        Predicate parsed = null;
        switch (goalDescName) {
            case "at": {
                String secondArg = atomicTermFormulaContext.term(1).NAME().getText();
                if (secondArg.startsWith("?p")) {
                    parsed = new WhatAtWhere();
                } else if (secondArg.startsWith("?l2")) {
                    parsed = new WhoAtWhat();
                } else {
                    parsed = new WhoAtWhere();
                }
                break;
            }
            case "in": {
                parsed = new In();
                break;
            }
            case "capacity-predecessor": {
                // intentionally ignore
                break;
            }
            case "capacity": {
                parsed = new HasCapacity();
                break;
            }
            case "ready-loading": {
                parsed = new ReadyLoading();
                break;
            }
            case "has-petrol-station": {
                parsed = new HasPetrolStation();
                break;
            }
            case ">=": {
                // intentionally ignored, embedded in model
                break;
            }
            case "decrease": {
                // intentionally ignored, embedded in model
                break;
            }
            case "increase": {
                // intentionally ignored, embedded in model
                break;
            }

            case "assign": { // TODO: test these assumptions
                // intentionally ignored, embedded in model
                break;
            }
            default: {
                throw new IllegalStateException("Unknown precondition predicate " + goalDescName);
            }
        }
        return parsed;
    }

    private static List<Predicate> parsePreconditions(List<PddlParser.GoalDescContext> goalDescContextList) {
        return goalDescContextList.stream().map(VariableDomainIO::parsePredicate).filter(predicate -> predicate != null)
                .collect(Collectors.toList());
    }

    private static List<Predicate> parseConditions(List<PddlParser.DaGDContext> daGDContextList) {
        List<Predicate> predicates = new ArrayList<>();
        for (PddlParser.TimedGDContext timedGDContext : daGDContextList.stream().map(m -> m.prefTimedGD().timedGD())
                .collect(Collectors.toList())) {
            Predicate parsedPredicate = parsePredicate(timedGDContext.goalDesc());
            if (timedGDContext.getText().startsWith("(at")) { // at $timeSpecifier
                String timeSpecifier = timedGDContext.timeSpecifier().getText();
                TemporalQuantifier quantifier;
                switch (timeSpecifier) {
                    case "start": {
                        quantifier = TemporalQuantifier.AT_START;
                        break;
                    }
                    case "end": {
                        quantifier = TemporalQuantifier.AT_END;
                        break;
                    }
                    default: {
                        throw new IllegalStateException("Unexpected time specifier " + timeSpecifier);
                    }
                }
                predicates.add(new TemporalPredicate(parsedPredicate, quantifier));
            } else {
                predicates.add(parsedPredicate);
            }
        }
        return predicates;
    }

    private static void parseTemporalEffects(List<PddlParser.DaEffectContext> daEffectContextList,
            List<Predicate> effects) {
        for (PddlParser.TimedEffectContext timedEffectContext : daEffectContextList.stream().map(
                PddlParser.DaEffectContext::timedEffect).collect(Collectors.toList())) {
            String timeSpecifier = timedEffectContext.timeSpecifier().getText();
            TemporalQuantifier quantifier;
            switch (timeSpecifier) {
                case "start": {
                    quantifier = TemporalQuantifier.AT_START;
                    break;
                }
                case "end": {
                    quantifier = TemporalQuantifier.AT_END;
                    break;
                }
                default: {
                    throw new IllegalStateException("Unexpected time specifier " + timeSpecifier);
                }
            }
            System.out.println();

            //            timedEffectContext.
            //            Predicate parsedPredicate = parsePredicate(timedGDContext.goalDesc());

        }
    }

    private static PartialBuilder parseGenericBuilder(PddlParser.StructureDefContext context) {
        final List<Predicate> predicates;
        final List<Predicate> effects = new ArrayList<>();
        final ActionCost cost;
        final ActionCost duration;
        if (context.durativeActionDef() != null) { // temporal
            PddlParser.DaDefBodyContext daDefBodyContext = context.durativeActionDef().daDefBody();
            predicates = parseConditions(daDefBodyContext.daGD().daGD());
            parseTemporalEffects(daDefBodyContext.daEffect().daEffect(), effects);

            PddlParser.DurValueContext durValueContext = daDefBodyContext.durationConstraint().simpleDurationConstraint(
                    0).durValue();
            if (durValueContext.fExp() != null) { // durValue is function, i.e. not a constant
                duration = null;
            } else {
                duration = ActionCost.valueOf(Integer.parseInt(durValueContext.NUMBER().getText()));
            }
            cost = duration;
        } else { // sequential
            PddlParser.ActionDefBodyContext actionContext = context.actionDef().actionDefBody();
            predicates = parsePreconditions(actionContext.goalDesc().goalDesc());
            cost = parseEffects(actionContext.effect().cEffect(), effects);
            duration = ActionCost.valueOf(1);
        }
        return new PartialBuilder(predicates, effects, cost, duration);

    }

    private static DropBuilder parseDropBuilder(PddlParser.StructureDefContext context) {
        PartialBuilder builder = parseGenericBuilder(context);
        return new DropBuilder(builder.getPredicates(), builder.getEffects(), builder.getCost(), builder.getDuration());
    }

    private static PickUpBuilder parsePickUpBuilder(PddlParser.StructureDefContext context) {
        PartialBuilder builder = parseGenericBuilder(context);
        return new PickUpBuilder(builder.getPredicates(), builder.getEffects(), builder.getCost(),
                builder.getDuration());
    }

    private static RefuelBuilder parseRefuelBuilder(PddlParser.StructureDefContext context) {
        PartialBuilder builder = parseGenericBuilder(context);
        return new RefuelBuilder(builder.getPredicates(), builder.getEffects(), builder.getCost(),
                builder.getDuration());
    }

    @Override
    public VariableDomain parse(String contents) throws IllegalArgumentException {
        String name = parseName(contents);
        Map<String, Class<? extends Predicate>> predicates = parsePredicates(contents);
        Map<String, Class<? extends Function>> functions = parseFunctions(contents);
        String normalized = normalizeInput(contents).collect(Collectors.joining(""));

        Set<PddlLabel> labels = parsePddlLabels(contents);
        if (functions.containsKey("capacity")) { // only numerical
            labels.add(PddlLabel.Capacity);
            if (normalized.replaceAll(" ", "").matches("^.*\\(>=\\(capacity\\?v\\)\\(package-size\\?p\\)\\).*$")) {
                labels.add(PddlLabel.MaxCapacity);
            }
        } else if (predicates.containsKey("capacity")) {
            labels.add(PddlLabel.MaxCapacity);
            labels.add(PddlLabel.Capacity);
        }

        PddlParser parser = new PddlParser(new CommonTokenStream(new PddlLexer(new ANTLRInputStream(contents))));
        ErrorDetectionListener listener = new ErrorDetectionListener();
        parser.addErrorListener(listener);
        PddlParser.DomainContext context = parser.domain();
        if (!context.domainName().NAME().getText().equals("transport")) {
            throw new IllegalArgumentException("Domain is not a transport domain!");
        }

        Map<String, PddlParser.StructureDefContext> structureDefContextMap = new HashMap<>();
        for (PddlParser.StructureDefContext structureDefContext : context.structureDef()) {
            if (structureDefContext.getText().isEmpty()) {
                continue;
            }
            String actionName;
            if (structureDefContext.durativeActionDef() != null) {
                actionName = structureDefContext.durativeActionDef().actionSymbol().NAME().getText();
            } else if (structureDefContext.actionDef() != null) {
                actionName = structureDefContext.actionDef().actionSymbol().NAME().getText();
            } else {
                throw new IllegalArgumentException("Couldn't parse action name from PDDL.");
            }
            structureDefContextMap.put(actionName, structureDefContext);
        }

        // TODO OOO Add detailed tests

        DriveBuilder driveBuilder = parseDriveBuilder(structureDefContextMap.get("drive"));
        DropBuilder dropBuilder = parseDropBuilder(structureDefContextMap.get("drop"));
        PickUpBuilder pickUpBuilder = parsePickUpBuilder(structureDefContextMap.get("pick-up"));
        RefuelBuilder refuelBuilder = parseRefuelBuilder(structureDefContextMap.get("refuel"));

        return new VariableDomain(name, driveBuilder, dropBuilder, pickUpBuilder, refuelBuilder, labels, predicates,
                functions);
    }

    @Override
    public String serialize(VariableDomain object) throws IllegalArgumentException {
        Map<String, Object> input = new HashMap<>();
        input.put("date", new Date());
        input.put("domain", object);
        input.put("actionCost", PddlLabel.ActionCost);
        input.put("numeric", PddlLabel.Numeric);
        input.put("temporal", PddlLabel.Temporal);

        Template template;
        try {
            template = configuration.getTemplate("domain.pddl.ftl");
        } catch (IOException e) {
            throw new IllegalStateException("Error occurred during reading template file.", e);
        }

        StringWriter writer = new StringWriter();
        try {
            template.process(input, writer);
        } catch (IOException | TemplateException e) {
            throw new IllegalStateException("Error occurred during processing template.", e);
        }
        return writer.toString().replaceAll("\\r\\n", "\n");
    }

    private static final class PartialBuilder {
        final List<Predicate> predicates;
        final List<Predicate> effects;
        final ActionCost cost;
        final ActionCost duration;

        PartialBuilder(List<Predicate> predicates, List<Predicate> effects, ActionCost cost) {
            this(predicates, effects, cost, ActionCost.valueOf(1));
        }

        PartialBuilder(List<Predicate> predicates, List<Predicate> effects, ActionCost cost,
                ActionCost duration) {
            this.predicates = predicates;
            this.effects = effects;
            this.cost = cost;
            this.duration = duration;
        }

        public List<Predicate> getPredicates() {
            return predicates;
        }

        public List<Predicate> getEffects() {
            return effects;
        }

        public ActionCost getCost() {
            return cost;
        }

        public ActionCost getDuration() {
            return duration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof PartialBuilder)) {
                return false;
            }

            PartialBuilder that = (PartialBuilder) o;

            return new EqualsBuilder().append(getPredicates(), that.getPredicates()).append(getEffects(),
                    that.getEffects()).append(getCost(), that.getCost()).append(getDuration(), that.getDuration())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(getPredicates()).append(getEffects()).append(getCost()).append(
                    getDuration()).toHashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this).append("predicates", predicates).append("effects", effects).append("cost",
                    cost).append("duration", duration).toString();
        }
    }

    static {
        predicateNameMap.put("at", WhoAtWhere.class);
        predicateNameMap.put("capacity", HasCapacity.class);
        predicateNameMap.put("has-petrol-station", HasPetrolStation.class);
        predicateNameMap.put("in", In.class);
        predicateNameMap.put("road", IsRoad.class);
        predicateNameMap.put("ready-loading", ReadyLoading.class);
    }

    static {
        functionNameMap.put("capacity", Capacity.class);
        functionNameMap.put("fuel-demand", FuelDemand.class);
        functionNameMap.put("fuel-left", FuelLeft.class);
        functionNameMap.put("fuel-max", FuelMax.class);
        functionNameMap.put("package-size", PackageSize.class);
        functionNameMap.put("road-length", RoadLength.class);
        functionNameMap.put("total-cost", TotalCost.class);
    }

    static {
        configuration.setClassForTemplateLoading(VariableDomainIO.class, "");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setLocale(Locale.US);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }
}
