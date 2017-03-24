package com.oskopek.transporteditor.planners.benchmark.report.reporters;

import com.oskopek.transporteditor.planners.benchmark.report.Reporter;

/**
 * Generates a line chart using runtimes in seconds as the values.
 */
public class RunTimeLineChartReporter extends LineChartReporter implements Reporter {

    /**
     * Default constructor.
     */
    public RunTimeLineChartReporter() {
        super(r -> r.getResults().getDurationMs() / 1000d, DEFAULT_WIDTH, DEFAULT_HEIGHT, "Planner runtimes",
                "Runtime in seconds");
    }

    @Override
    public String getFileName() {
        return "runtime_line_chart.svg";
    }
}
