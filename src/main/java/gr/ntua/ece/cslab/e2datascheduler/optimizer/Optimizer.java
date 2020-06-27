package gr.ntua.ece.cslab.e2datascheduler.optimizer;

import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;

import org.apache.flink.runtime.jobgraph.JobGraph;

import java.util.List;


/**
 *  Generic optimization algorithm
 */
public interface Optimizer {

    /**
     *
     * @param jobGraph An Apache Flink {@link JobGraph} that describes the application to be optimized.
     * @return A {@link HaierExecutionGraph} with the optimal task placement.
     */
    List<HaierExecutionGraph> optimize(JobGraph jobGraph, OptimizationPolicy policy, Model mlModel);

    /**
     * Configure Optimizer's parameters.
     *
     * @param parameters is an optimizer-dependent object containing configuration parameters.
     */
    void configure(Parameters parameters);

    /**
     * Retrieve the values of Optimizer's current configuration parameters.
     *
     * @return the values of Optimizer's current configuration parameters.
     */
    Parameters retrieveConfiguration();
}
