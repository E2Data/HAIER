package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.optimizer.Parameters;

public class NSGAIIParameters implements Parameters {

    private int maxParetoPlans;
    private int numGenerations;

    private NSGAIIParameters(){}

    /**
     * Configuration parameters for the NSGAII algorithm.
     *
     * @param maxParetoPlans is the maximum number of Pareto solutions that should be returned by a NSGAII run.
     * @param numGenerations is the number of generations of a NSGAII run.
     */
    public NSGAIIParameters(final int maxParetoPlans, final int numGenerations) {
        this.maxParetoPlans = maxParetoPlans;
        this.numGenerations = numGenerations;
    }

    /**
     * Return the maximum number of Pareto solutions that should be found by a NSGAII run.
     *
     * @return the maximum number of Pareto solutions that should be found by a NSGAII run.
     */
    public int getMaxParetoPlans() {
        return this.maxParetoPlans;
    }

    /**
     * Set the maximum number of Pareto solutions that should be found by a NSGAII run.
     *
     * @param maxParetoPlans the maximum number of Pareto solutions that should be found by a NSGAII run.
     */
    public void setMaxParetoPlans(int maxParetoPlans) {
        this.maxParetoPlans = maxParetoPlans;
    }

    /**
     * Retrieve the number of generations of a NSGAII run.
     *
     * @return the number of generations of a NSGAII run.
     */
    public int getNumGenerations() {
        return this.numGenerations;
    }

    /**
     * Set the number of generations of a NSGAII run.
     *
     * @param numGenerations the number of generations of a NSGAII run.
     */
    public void setNumGenerations(int numGenerations) {
        this.numGenerations = numGenerations;
    }

}
