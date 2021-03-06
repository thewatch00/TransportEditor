package com.oskopek.transport.planners.sequential;

import com.oskopek.transport.model.domain.SequentialDomain;
import com.oskopek.transport.model.domain.action.Action;
import com.oskopek.transport.model.domain.action.TemporalPlanAction;
import com.oskopek.transport.model.plan.Plan;
import com.oskopek.transport.model.plan.SequentialPlan;
import com.oskopek.transport.model.problem.Problem;
import com.oskopek.transport.persistence.DefaultProblemIO;
import com.oskopek.transport.persistence.SequentialPlanIO;
import com.oskopek.transport.tools.test.TestUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

public class ForwardBFSPlannerIT {

    private ForwardBFSPlanner planner;
    private static final SequentialDomain domain = new SequentialDomain("");
    private static Problem problem;
    private static Problem p02Problem;
    private static Problem p03Problem;
    private static Plan plan;
    private static Plan planEquivalent;

    @BeforeClass
    public static void setUpClass() throws Exception {
        problem = new DefaultProblemIO(domain).parse(TestUtils.getPersistenceTestFile("p01SeqProblem.pddl"));
        p02Problem = new DefaultProblemIO(domain).parse(TestUtils.getPersistenceTestFile("p02SeqProblem.pddl"));
        p03Problem = new DefaultProblemIO(domain).parse(TestUtils.getPersistenceTestFile("p03SeqProblem.pddl"));
        plan = new SequentialPlanIO(domain, problem).parse(TestUtils.getPersistenceTestFile("p01SeqPlan.val"));

        List<Action> actions = new ArrayList<>(plan.getActions());
        Action action0 = actions.remove(0);
        actions.add(1, action0);
        planEquivalent = new SequentialPlan(actions);
    }

    @Before
    public void setUp() throws Exception {
        planner = new ForwardBFSPlanner();
    }

    @Test
    public void isAvailable() throws Exception {
        assertThat(planner.isAvailable()).isTrue();
    }

    @Test
    public void plansP01Sequential() throws Exception {
        Plan plan = planner.startAndWait(domain, problem);
        assertThatPlanIsEqualToAny(plan, ForwardBFSPlannerIT.plan, planEquivalent);
    }

    @Test
    @Ignore
    public void plansP02Sequential() throws Exception {
        Plan plan = planner.startAndWait(domain, p02Problem);
        System.out.println(new SequentialPlanIO(domain, p02Problem).serialize(plan));
//        assertThatPlanIsEqualToAny(plan, ForwardBFSPlannerIT.plan, planEquivalent);
    }

    @Test
    @Ignore
    public void plansP03Sequential() throws Exception {
        Plan plan = planner.startAndWait(domain, p03Problem);
        System.out.println(new SequentialPlanIO(domain, p03Problem).serialize(plan));
//        assertThatPlanIsEqualToAny(plan, ForwardBFSPlannerIT.plan, planEquivalent);
    }

    private static void assertThatActionsAreEqual(Action left, Action right) {
        assertThat(left.getName()).isEqualTo(right.getName());
        assertThat(left.getWho().getName()).isEqualTo(right.getWho().getName());
        assertThat(left.getWhere().getName()).isEqualTo(right.getWhere().getName());
        if (left.getWhat() == null) {
            assertThat(right.getWhat()).isNull();
        } else {
            assertThat(right.getWhat()).isNotNull();
            assertThat(left.getWhat().getName()).isEqualTo(right.getWhat().getName());
        }
    }

    public static void assertThatPlansAreEqual(Plan left, Plan right) {
        assertThat(left).isNotNull();
        assertThat(right).isNotNull();
        List<TemporalPlanAction> leftActions = left.getTemporalPlanActions().stream()
                .sorted().collect(Collectors.toList());
        List<TemporalPlanAction> rightActions = right.getTemporalPlanActions().stream()
                .sorted().collect(Collectors.toList());
        assertThat(leftActions.size()).isEqualTo(rightActions.size());
        for (int index = 0; index < leftActions.size(); index++) {
            TemporalPlanAction leftAction = leftActions.get(index);
            TemporalPlanAction rightAction = rightActions.get(index);
            assertThat(leftAction.getStartTimestamp()).isEqualTo(rightAction.getStartTimestamp());
            assertThat(leftAction.getEndTimestamp()).isEqualTo(rightAction.getEndTimestamp());
            assertThatActionsAreEqual(leftAction.getAction(), rightAction.getAction());
        }
    }

    public static void assertThatPlanIsEqualToAny(Plan actual, Plan... expected) {
        for (Plan expectedPlan : expected) {
            try {
                assertThatPlansAreEqual(actual, expectedPlan);
            } catch (AssertionError assertionError) {
                continue;
            }
            return;
        }
        fail("Not equal to any plan.");
    }

}
