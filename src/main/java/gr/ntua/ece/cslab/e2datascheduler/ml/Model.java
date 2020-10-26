package gr.ntua.ece.cslab.e2datascheduler.ml;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;

import org.apache.flink.runtime.jobgraph.JobVertex;


//TODO: @kbitsak: Do not modify this class!!!
//TODO: @kbitsak: For each different model that you may have, create a subclass in the 'impl' package
//TODO: @kbitsak: and implement the 'load' and 'predict' methods for the subclass
/**
 * This class constitutes the description of a generic Machine Learning model
 */
public abstract class Model {

    protected String name;

    //TODO: @kbitsak: Implement the loading mechanism. You should assume that the model is stored
    //TODO: in the 'resources' folder of the project
    /**
     * Loads a model from disk.
     */
    public abstract void load(final String path);

    /**
     * Make a prediction of the value of the given {@code objective} if the provided
     * {@link ScheduledJobVertex} is executed on the given {@code device} (i.e., {@link HwResource}).
     *
     * @param objective          The objective to make the prediction for
     * @param device             The {@link HwResource} allocated for the underlying {@link JobVertex}
     * @param scheduledJobVertex The {@link ScheduledJobVertex} that represents the {@link JobVertex} to be executed
     * @return The predicted value for the provided objective
     */
    public abstract double predict(
            final String objective,
            final HwResource device,
            final ScheduledJobVertex scheduledJobVertex);
    //public abstract double predict(final String objective, final HwResource device, final String sourceCode);
    //public abstract double predict(String objective, HwResource device, List<Double> featureVector);

}
