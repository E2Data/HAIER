package gr.ntua.ece.cslab.e2datascheduler.optimizer;

import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.beans.graph.ToyJobGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;

/**
 *  Generic optimization algorithm
 */
public interface Optimizer {

    /**
     *
     * @param graph A {@link ToyJobGraph} that describes the application to be optimized
     * @return an {@link ExecutionGraph} with the optimal task placement
     */
    ExecutionGraph optimize(ToyJobGraph graph, OptimizationPolicy policy, Model mlModel);
}
