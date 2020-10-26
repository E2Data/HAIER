package gr.ntua.ece.cslab.e2datascheduler.ml.impl;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.TornadoFeatureVector;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.util.HaierCacheException;

import org.apache.flink.runtime.jobgraph.JobVertex;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * TODO(ckatsak): Documentation
 */
public class TornadoModel extends Model {

    private static final Logger logger = Logger.getLogger(TornadoModel.class.getCanonicalName());

    private final PredictionCache execTimePredictionCache = new PredictionCache();
    private final PredictionCache powerConsPredictionCache = new PredictionCache();


    // --------------------------------------------------------------------------------------------


    /**
     * Empty all prediction caches, getting ready to process a new {@link org.apache.flink.runtime.jobgraph.JobGraph}.
     */
    public void emptyPredictionCaches() {
        this.execTimePredictionCache.clear();
        this.powerConsPredictionCache.clear();
        logger.finer("Prediction caches emptied!");
    }


    // --------------------------------------------------------------------------------------------


    /**
     * Loads a model from disk.
     *
     * @param path
     */
    @Override
    public void load(final String path) {
        // FIXME(ckatsak): Maybe here initialize HTTP client to @kbitsak 's inference endpoint?
    }

    /**
     * Make a prediction of the value of the given {@code objective} if the provided
     * {@link ScheduledJobVertex} is executed on the given {@code device} (i.e., {@link HwResource}).
     *
     * @param objective          The objective to make the prediction for
     * @param device             The {@link HwResource} allocated for the underlying {@link JobVertex}
     * @param scheduledJobVertex The {@link ScheduledJobVertex} that represents the {@link JobVertex} to be executed
     * @return The predicted value for the objective
     */
    @Override
    public double predict(
            final String objective,
            final HwResource device,
            final ScheduledJobVertex scheduledJobVertex) {
        if (null == device) {
            throw new IllegalArgumentException("Parameter 'device' cannot be null");
        }
        if (null == scheduledJobVertex) {
            throw new IllegalArgumentException("Parameter 'scheduledJobVertex' cannot be null");
        }
        switch (objective) {
            case "execTime":
                return this.predictExecTime(device, scheduledJobVertex);
            case "powerCons":
                return this.predictPowerCons(device, scheduledJobVertex);
            default:
                logger.warning("Unknown objective '" + objective + "'; panicking...");
                throw new RuntimeException("Unknown objective '" + objective + "'");
        }
    }


    // --------------------------------------------------------------------------------------------


    /**
     * <pre>FIXME(ckatsak): queryModel() implementation pending</pre>
     *
     * Make a prediction for the execution time of the provided {@link ScheduledJobVertex} (actually,
     * its underlying {@link JobVertex}) on the given {@code device} (i.e., {@link HwResource}).
     *
     * First, there is an attempt to retrieve the prediction from the local cache. If that fails, the
     * ML inference microservice is queried for all operators in the {@link JobVertex} at hand, the
     * partial results are combined into a single predicted value, which is also cached for future use.
     *
     * @param device             The {@link HwResource} that the {@link ScheduledJobVertex} is assigned on
     * @param scheduledJobVertex The {@link ScheduledJobVertex} at hand
     * @return The predicted execution time
     */
    private double predictExecTime(final HwResource device, final ScheduledJobVertex scheduledJobVertex) {
        double ret;

        try {
            ret = this.execTimePredictionCache.getPrediction(scheduledJobVertex.getJobVertex(), device);
        } catch (final HaierCacheException e) {
            final double upstreamResponse = inferenceFromNetwork(
                    "execTime",
                    device,
                    scheduledJobVertex.getTornadoFeatures()
            );
            this.execTimePredictionCache.update(
                    scheduledJobVertex.getJobVertex(),
                    device,
                    upstreamResponse
            );

            // NOTE(ckatsak): If it fails again, there must be some kind of bug around the PredictionCache logic.
            try {
                ret = this.execTimePredictionCache.getPrediction(scheduledJobVertex.getJobVertex(), device);
                if (ret != upstreamResponse) {
                    throw new HaierCacheException("ret != modelResponse");
                }
            } catch (final HaierCacheException nested) {
                logger.log(Level.SEVERE, "PredictionCache appears to be bugged: " + nested.getMessage(), nested);
                throw nested;
            }
        }

        return ret;
    }

    /**
     * FIXME(ckatsak): Implementation & Documentation
     *
     * @param device
     * @param scheduledJobVertex
     * @return
     */
    private double predictPowerCons(final HwResource device, final ScheduledJobVertex scheduledJobVertex) {
        /*
         * FIXME(ckatsak): Implementation
         */
        return -0.0d;
    }


    // --------------------------------------------------------------------------------------------


    /**
     * <pre>
     * FIXME(ckatsak): Implementation?
     *  - queryModel() for each TornadoFeatureVector
     *  - return the synthesis of the responses; probably just a sum, for both objectives (?)
     * </pre>
     *
     * Query the ML inference microservice over the network to retrieve one prediction per operator present in
     * the examined {@link JobVertex} and combine the responses (i.e., multiple predicted values) into a single
     * predicted value for the given objective.
     *
     * @param objective       The objective to query for
     * @param device          The device (i.e., {@link HwResource}) to query for
     * @param tornadoFeatures A {@link List} of {@link TornadoFeatureVector} (one per operator in the examined
     *                        {@link JobVertex}), which serve as input for the ML inference microservice
     * @return The combined, final predicted value for the given objective
     */
    private double inferenceFromNetwork(
            final String objective,
            final HwResource device,
            final List<TornadoFeatureVector> tornadoFeatures) {
        final double[] upstreamResponses = new double[tornadoFeatures.size()];
        for (int i = 0; i < tornadoFeatures.size(); i++) {
            upstreamResponses[i] = queryModel(objective, device, tornadoFeatures.get(i));
        }
        return this.combinePredictedValues(upstreamResponses);
    }

    /**
     * Combine multiple responses of the inference microservice (due to the presence of multiple operators within a
     * single {@link JobVertex}) into a single value.
     * This is just a summation for now.
     *
     * @param upstreamResponses The responses of the inference microservice
     * @return The final value produced by the synthesis
     */
    private double combinePredictedValues(final double[] upstreamResponses) {
        return Arrays.stream(upstreamResponses).sum();
    }

    /**
     * <pre>
     * FIXME(ckatsak): Implementation:
     *  - HTTP POST @kbitsak 's ML inference microservice
     * </pre>
     *
     * Query the ML inference microservice for one specific prediction over the network.
     *
     * @param objective       The objective to query for
     * @param device          The device (i.e., {@link HwResource}) to query for
     * @param tornadoFeatures The input code features to query for
     * @return ML inference microservice's response to the query
     */
    private double queryModel(
            final String objective,
            final HwResource device,
            final TornadoFeatureVector tornadoFeatures) {
        /*
         * FIXME(ckatsak): HTTP POST @kbitsak 's ML inference microservice
         */
        return -0.0d;
    }

}
