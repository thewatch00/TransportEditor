package com.oskopek.transport.planners.sequential;

import com.google.common.collect.ArrayTable;
import com.oskopek.transport.model.domain.Domain;
import com.oskopek.transport.model.domain.action.Action;
import com.oskopek.transport.model.domain.action.Drive;
import com.oskopek.transport.model.plan.Plan;
import com.oskopek.transport.model.plan.SequentialPlan;
import com.oskopek.transport.model.problem.Location;
import com.oskopek.transport.model.problem.Package;
import com.oskopek.transport.model.problem.Problem;
import com.oskopek.transport.model.problem.Vehicle;
import com.oskopek.transport.model.problem.graph.RoadEdge;
import com.oskopek.transport.planners.AbstractPlanner;
import com.oskopek.transport.planners.sequential.state.ImmutablePlanState;
import com.oskopek.transport.planners.sequential.state.ShortestPath;
import javaslang.Tuple;
import javaslang.Tuple2;
import javaslang.Tuple3;
import javaslang.Value;
import javaslang.collection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// choose a random vehicle and package,
// find the path, find all packages on it and around it
// choose the ones whose target is on the path
// if still not fully capacitated, choose the others in the
// order of distance from the shortest path
public class RandomizedRestartWithAroundPathPickupPlanner extends AbstractPlanner {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ArrayTable<String, String, ShortestPath> shortestPathMatrix;
    private Random random;
    private Plan bestPlan;
    private int bestPlanScore;

    public RandomizedRestartWithAroundPathPickupPlanner() {
        setName(RandomizedRestartWithAroundPathPickupPlanner.class.getSimpleName());
    }

    public ArrayTable<String, String, ShortestPath> getShortestPathMatrix() {
        return shortestPathMatrix;
    }

    void resetState() {
        random = null;
        bestPlan = null;
        bestPlanScore = Integer.MAX_VALUE;
    }

    void initialize(Problem problem) {
        shortestPathMatrix = PlannerUtils.computeAPSP(problem.getRoadGraph());
        random = new Random(2017L);
    }

    @Override
    public Optional<Plan> plan(Domain domain, Problem problem) {
        logger.debug("Initializing planning...");
        resetState();
        initialize(problem);
        logger.debug("Starting planning...");

        List<Vehicle> vehicles = new ArrayList<>(problem.getAllVehicles());
        List<Location> locations = problem.getRoadGraph().getAllLocations().collect(Collectors.toList());
        int i = 1;
        float exploration = 0.2f; // best so far: 0.2 or 0.1
        float multiplier = 0.00f; // best so far: 0
        int everySteps = 50_000;
        while (true) {
            if (i % everySteps == 0) {
                float delta = exploration;
                exploration -= delta * multiplier;
                logger.debug("Exploration increased to: {}", exploration);
            }
            i++;

            ImmutablePlanState current = new ImmutablePlanState(problem);
            while (!current.isGoalState() && current.getTotalTime() < bestPlanScore) {
                Problem curProblem = current.getProblem();
                List<Package> unfinished = new ArrayList<>(PlannerUtils.getUnfinishedPackages(curProblem.getAllPackages()));
                if (unfinished.isEmpty()) {
                    throw new IllegalStateException("Zero packages left but not in goal state.");
                }

                Package chosenPackage = unfinished.get(random.nextInt(unfinished.size()));
                Vehicle chosenVehicle;
                while (true) {
                    if (random.nextFloat() < exploration) {
                        chosenVehicle = vehicles.get(random.nextInt(vehicles.size()));
                    } else {
                        Optional<Vehicle> maybeVehicle = nearestVehicle(curProblem.getAllVehicles(),
                                chosenPackage.getLocation(), chosenPackage.getSize().getCost());
                        if (maybeVehicle.isPresent()) {
                            chosenVehicle = maybeVehicle.get();
                        } else {
                            continue;
                        }
                    }
                    if (chosenVehicle.getCurCapacity().getCost() >= chosenPackage.getSize().getCost()) {
                        break;
                    }
                }

                List<Action> newActions = findPartialPlan(domain, current, chosenVehicle.getName(), chosenPackage,
                        unfinished);
                current = Stream.ofAll(newActions).foldLeft(Optional.of(current),
                        (state, action) -> state.flatMap(state2 -> state2.apply(action)))
                        .orElseThrow(() -> new IllegalStateException("Could not apply all new actions to current state."));

                if (shouldCancel()) {
                    logger.debug("Cancelling, returning best found plan so far with score: {}.", bestPlanScore);
                    return Optional.ofNullable(bestPlan);
                }
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Finished one iteration. Length: {}", current.getTotalTime());
            }

            // TODO: collapse plan?
            if (bestPlanScore > current.getTotalTime()) {
                logger.debug("Found new best plan {} -> {}", bestPlanScore, current.getTotalTime());
                bestPlanScore = current.getTotalTime();
                bestPlan = new SequentialPlan(current.getAllActionsInList());
            }
        }
    }

    private Optional<Vehicle> nearestVehicle(Collection<Vehicle> vehicles, Location curLocation, int minFreeCapacity) {
        return Stream.ofAll(vehicles).filter(v -> v.getCurCapacity().getCost() >= minFreeCapacity)
                .minBy(v -> shortestPathMatrix.get(v.getLocation().getName(), curLocation.getName()).getDistance())
                .toJavaOptional();
    }

    private List<Action> findPartialPlan(Domain domain, ImmutablePlanState current, String chosenVehicleName,
            final Package chosenPackage, List<Package> unfinished) {
        Map<Location, Set<Package>> packageLocMap = new HashMap<>();
        for (Package pkg : unfinished) {
            packageLocMap.computeIfAbsent(pkg.getLocation(), l -> new HashSet<>()).add(pkg);
        }

        final Vehicle chosenVehicle = current.getProblem().getVehicle(chosenVehicleName);
        Location packageLoc = chosenPackage.getLocation();
        List<RoadEdge> basicPath = new ArrayList<>();
        basicPath.addAll(shortestPathMatrix.get(chosenVehicle.getLocation().getName(), packageLoc.getName()).getRoads());
        basicPath.addAll(shortestPathMatrix.get(packageLoc.getName(), chosenPackage.getTarget().getName()).getRoads());
        if (basicPath.isEmpty()) {
            return Collections.emptyList();
        }
        List<Location> locationsOnPath = basicPath.stream().map(RoadEdge::getFrom).collect(Collectors.toList());
        locationsOnPath.add(basicPath.get(basicPath.size() - 1).getTo());

        Set<Package> packagesOnPath = new HashSet<>();
        for (RoadEdge edge : basicPath) {
            Set<Package> atFrom = packageLocMap.get(edge.getFrom());
            if (atFrom != null) {
                packagesOnPath.addAll(atFrom);
            }
        }

        javaslang.collection.List<Tuple3<Integer, Integer, Package>> completelyOnPath = Stream.ofAll(packagesOnPath)
                .map(p -> Tuple.of(locationsOnPath.indexOf(p.getLocation()), locationsOnPath.lastIndexOf(p.getTarget()), p))
                .filter(t -> t._1 >= 0 && t._2 >= 0 && t._1 <= t._2).toList();
        // only packages with pick up and drop on path and with pick up before drop
        // list should have at least one entry now
        final int curCapacity = chosenVehicle.getCurCapacity().getCost();
        List<Package> chosenPackages;
        int capacityLeft;
        if ((long) completelyOnPath.map(t -> t._3.getSize().getCost()).sum() > curCapacity) { // if not take everything
            Tuple3<Integer, Integer, javaslang.collection.List<Package>> pkgTuples = Stream.ofAll(completelyOnPath).combinations().map(Value::toList)
                    .map(sTuple -> Tuple.of(curCapacity - calculateMaxCapacity(sTuple), sTuple.map(t -> t._3)))
                    .filter(t -> t._1 >= 0).map(t -> Tuple.of(t._1, t._2.toStream().map(p -> locationsOnPath.lastIndexOf(p.getTarget())).max().getOrElse(Integer.MAX_VALUE), t._2))
                    .minBy(t -> Tuple.of(t._1, t._2)).get(); // TODO: T2 not last index, but length of indexes + maximize?
            capacityLeft = pkgTuples._1;
            chosenPackages = pkgTuples._3.toJavaList();
        } else {
            Tuple3<Integer, Integer, javaslang.collection.List<Package>> pkgTuples
                    = Tuple.of(curCapacity - calculateMaxCapacity(completelyOnPath),
            completelyOnPath.toStream().map(t -> locationsOnPath.lastIndexOf(t._3.getTarget())).max().getOrElse(Integer.MAX_VALUE),
            completelyOnPath.map(t -> t._3).toList());
            capacityLeft = pkgTuples._1;
            chosenPackages = pkgTuples._3.toJavaList();
        }

        if (chosenPackages == null) {
            throw new IllegalStateException("Should not occur.");
        }

        Map<Package, Location> targetMap = new HashMap<>(chosenPackages.size() + capacityLeft);
        for (Package pkg : chosenPackages) {
            targetMap.put(pkg, pkg.getTarget());
        }

        if (capacityLeft > 0) { // fill in with packages that get closer to goal
            List<Tuple3<Integer, Location, Package>> distancesFromPath = new ArrayList<>(capacityLeft * 1);
            for (Package pkg : packagesOnPath) {
                if (chosenPackages.contains(pkg)) {
                    continue;
                }
                List<Tuple2<Integer, Location>> distancesFromPathLocation = new ArrayList<>();
                boolean pickedUp = false;
                boolean first = true;
                for (RoadEdge edge : basicPath) {
                    if (first) {
                        first = false;
                        continue;
                    }
                    if (edge.getFrom().equals(pkg.getLocation())) {
                        pickedUp = true;
                        continue;
                    }
                    if (pickedUp) {
                        ShortestPath pkgToTarget = shortestPathMatrix.get(edge.getFrom().getName(), pkg.getTarget().getName());
                        distancesFromPathLocation.add(Tuple.of(pkgToTarget.getDistance(), edge.getFrom()));
                    }
                }
                if (!distancesFromPathLocation.isEmpty()) {
                    Tuple2<Integer, Location> minLoc = Collections.min(distancesFromPathLocation, Comparator.comparing(t -> t._1));
                    distancesFromPath.add(Tuple.of(minLoc._1, minLoc._2, pkg));
                }
            }
            distancesFromPath.sort(Comparator.comparing(t -> t._1));
            for (Tuple3<Integer, Location, Package> tuple : distancesFromPath) {
                int pkgSize = tuple._3.getSize().getCost();
                if (capacityLeft - pkgSize < 0) {
                    continue;
                }
                capacityLeft -= pkgSize;

                chosenPackages.add(tuple._3);
                targetMap.put(tuple._3, tuple._2);
            }
        }

        List<RoadEdge> path = basicPath;
        return buildPlan(domain, path, chosenVehicle, chosenPackages, targetMap);
    }

    private static Integer calculateMaxCapacity(javaslang.collection.List<Tuple3<Integer, Integer, Package>> combination) {
        return combination.flatMap(t -> Stream.of(Tuple.of(t._1, false), Tuple.of(t._2, true)))
                .sortBy(t -> t._1).foldLeft(Tuple.of(0, Integer.MIN_VALUE), (capTuple, elem) -> {
                    int curCapacity = capTuple._1;
                    if (elem._2) { // drop
                        curCapacity -= 1; // TODO assumes 1 sizes of packages
                    } else { // pickup
                        curCapacity += 1; // TODO assumes 1 sizes of packages
                    }
                    if (curCapacity > capTuple._2) {
                        return Tuple.of(curCapacity, curCapacity);
                    } else {
                        return Tuple.of(curCapacity, capTuple._2);
                    }
                })._2;
    }

    private List<Action> buildPlan(Domain domain, List<RoadEdge> path, Vehicle vehicle,
            List<Package> chosenPackages, Map<Package, Location> targetMap) {
        if (path.isEmpty()) {
            return Collections.emptyList();
        }
        List<Action> actions = new ArrayList<>();
        Set<Package> afterDrop = new HashSet<>(chosenPackages);
        Set<Package> inVehicle = new HashSet<>(vehicle.getPackageList());
        for (int i = path.size() - 1; i >= 0; i--) {
            RoadEdge edge = path.get(i);
            Location to = edge.getTo();
            buildPackageActions(domain, actions, afterDrop, inVehicle, to, vehicle, targetMap);

            // drive
            actions.add(domain.buildDrive(vehicle, edge.getFrom(), to, edge.getRoad()));
        }
        // last loc
        Location firstLocation = path.get(0).getFrom();
        buildPackageActions(domain, actions, afterDrop, inVehicle, firstLocation, vehicle, targetMap);

        for (int i = 0; i < actions.size(); i++) { // remove redundant drives
            Action action = actions.get(i);
            if (action instanceof Drive) {
                actions.remove(i);
                i--;
            } else {
                break;
            }
        }
        return Stream.ofAll(actions).reverse().toJavaList();
    }

    private void buildPackageActions(Domain domain, List<Action> actions, Set<Package> afterDrop, Set<Package> inVehicle, Location at, Vehicle vehicle,
            Map<Package, Location> targetMap) {
        for (Iterator<Package> iter = inVehicle.iterator(); iter.hasNext(); ) {
            Package pkg = iter.next();
            if (pkg.getLocation().equals(at)) {
                actions.add(domain.buildPickUp(vehicle, at, pkg));
                iter.remove();
            }
        }
        for (Iterator<Package> iter = afterDrop.iterator(); iter.hasNext(); ) {
            Package pkg = iter.next();
            if (targetMap.get(pkg).equals(at)) {
                actions.add(domain.buildDrop(vehicle, at, pkg));
                inVehicle.add(pkg);
                iter.remove();
            }
        }
    }


    @Override
    public RandomizedRestartWithAroundPathPickupPlanner copy() {
        return new RandomizedRestartWithAroundPathPickupPlanner();
    }

    @Override
    public int hashCode() {
        return getClass().getSimpleName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RandomizedRestartWithAroundPathPickupPlanner;
    }
}
