package com.oskopek.transporteditor.planners.benchmark.report.reporters;

/**
 * Generates a {@link LatexProblemPlannerReporter} table with the
 * {@link com.oskopek.transporteditor.planners.benchmark.data.BenchmarkRun.RunExitStatus} as elements.
 */
public class ExitStatusLatexTableReporter extends LatexProblemPlannerReporter {

    /**
     * Default constructor.
     */
    public ExitStatusLatexTableReporter() {
        super(r -> r.getResults().getExitStatus().toString().toLowerCase());
    }

    @Override
    public String getFileName() {
        return "exitstatus_table.tex";
    }
}
