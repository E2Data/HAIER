package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.ntua.ece.cslab.e2datascheduler.optimizer.Parameters;

public class NSGAIIParameters implements Parameters {

    @JsonProperty("max_pareto_plans")
    private int maxParetoPlans;

    @JsonProperty("num_generations")
    private int numGenerations;

    private NSGAIIParameters(){}

    /**
     * Configuration parameters for the NSGA-II algorithm.
     *
     * @param maxParetoPlans is the maximum number of Pareto solutions that should be returned by a NSGA-II run.
     * @param numGenerations is the number of generations of a NSGA-II run.
     */
    public NSGAIIParameters(final int maxParetoPlans, final int numGenerations) {
        this.maxParetoPlans = maxParetoPlans;
        this.numGenerations = numGenerations;
    }

    /**
     * Return the maximum number of Pareto solutions that should be found by a NSGA-II run.
     *
     * @return the maximum number of Pareto solutions that should be found by a NSGA-II run.
     */
    public int getMaxParetoPlans() {
        return this.maxParetoPlans;
    }

    /**
     * Set the maximum number of Pareto solutions that should be found by a NSGA-II run.
     *
     * @param maxParetoPlans the maximum number of Pareto solutions that should be found by a NSGA-II run.
     */
    public void setMaxParetoPlans(int maxParetoPlans) {
        this.maxParetoPlans = maxParetoPlans;
    }

    /**
     * Retrieve the number of generations of a NSGA-II run.
     *
     * @return the number of generations of a NSGA-II run.
     */
    public int getNumGenerations() {
        return this.numGenerations;
    }

    /**
     * Set the number of generations of a NSGA-II run.
     *
     * @param numGenerations the number of generations of a NSGA-II run.
     */
    public void setNumGenerations(int numGenerations) {
        this.numGenerations = numGenerations;
    }

}
