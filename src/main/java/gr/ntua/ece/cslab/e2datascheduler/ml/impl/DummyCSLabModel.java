package gr.ntua.ece.cslab.e2datascheduler.ml.impl;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.util.CSLabFeatureExtractor;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

import java.util.logging.Logger;

/**
 * Dummy class that mocks a model and serves integration purposes
 */
public class DummyCSLabModel extends Model {

    private static final Logger logger = Logger.getLogger(DummyCSLabModel.class.getCanonicalName());

    /**
     * Predictions for all kernels can be cached here for future use.
     */
    private final Map<String, Double> predictionCache = new HashMap<>();

    // --------------------------------------------------------------------------------------------

    @Override
    public void load(String path) {}

    @Override
    public double predict(
            final String objective,
            final HwResource device,
            final String sourceCode) {
        if (!this.predictionCache.containsKey(sourceCode)) {
            // FIXME(ckatsak): CSLabFeatureExtractor must be run as a separate microservice, so we
            //                 may keep it commented it out for now to make HAIER easier to deploy.
            logger.info("Feature extraction is happening here...");
            //final List<Double> inputFeatures = CSLabFeatureExtractor.getInstance().extract(sourceCode);
            //logger.info("Features retrieved: " + inputFeatures);

            // XXX(ckatsak): Random prediction gets cached for future use until
            //               the integration with an existing ML model. The
            //               "actual" code should calculate (or somehow obtain)
            //               the real prediction, and cache that instead.
            final Random randomPrediction = new Random(System.currentTimeMillis());
            this.predictionCache.put(sourceCode, (double) randomPrediction.nextInt(10));
        }

        return this.predictionCache.get(sourceCode);
    }

/*
    @Override
    public double predict(String objective, HwResource device, String sourceCode) {
        final CSLabFeatureExtractor extractor = CSLabFeatureExtractor.getInstance();
        final List<Double> inputFeatures = extractor.extract(sourceCode);

        logger.info("Features retrieved: " + inputFeatures);

        final Random r = new Random(System.currentTimeMillis());
        return r.nextInt(10);
    }
*/

}
