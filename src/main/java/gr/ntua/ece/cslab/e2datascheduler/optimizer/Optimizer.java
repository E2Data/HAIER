package gr.ntua.ece.cslab.e2datascheduler.optimizer;

import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;

import org.apache.flink.runtime.jobgraph.JobGraph;

/**
 *  Generic optimization algorithm
 */
public interface Optimizer {

    /**
     *
     * @param graph A {@link JobGraph} that describes the application to be optimized
     * @return a {@link FlinkExecutionGraph} with the optimal task placement
     */
    FlinkExecutionGraph optimize(JobGraph jobGraph, OptimizationPolicy policy, Model mlModel);
}
