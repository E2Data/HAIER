package gr.ntua.ece.cslab.e2datascheduler.optimizer;

import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;

import org.apache.flink.runtime.jobgraph.JobGraph;


/**
 *  Generic optimization algorithm
 */
public interface Optimizer {

    /**
     *
     * @param jobGraph An Apache Flink {@link JobGraph} that describes the application to be optimized.
     * @return A {@link HaierExecutionGraph} with the optimal task placement.
     */
    HaierExecutionGraph optimize(JobGraph jobGraph, OptimizationPolicy policy, Model mlModel);
}
