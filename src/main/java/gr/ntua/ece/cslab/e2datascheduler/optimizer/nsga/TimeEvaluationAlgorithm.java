package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;

public interface TimeEvaluationAlgorithm {

    /**
     * TODO(ckatsak): Documentation
     *
     * @param flinkExecutionGraph
     */
    void initialization(final FlinkExecutionGraph flinkExecutionGraph);

    /**
     * TODO(ckatsak): Documentation
     *
     * @param flinkExecutionGraph
     * @return
     */
    double calculateExecutionTime(final FlinkExecutionGraph flinkExecutionGraph);

}
