package gr.ntua.ece.cslab.e2datascheduler.ml.impl;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.util.FeatureExtractor;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

/**
 * Dummy class that mocks a model and serves integration purposes
 */
public class DummyModel extends Model {

    /**
     * Predictions for all kernels can be cached here for future use.
     */
    private final Map<String, Double> predictionCache = new HashMap<>();

    // --------------------------------------------------------------------------------------------

    @Override
    public void load(String path) {}

    @Override
    public double predict(String objective, HwResource device, String sourceCode) {
        if (!this.predictionCache.containsKey(sourceCode)) {
            final List<Double> inputFeatures = FeatureExtractor.getInstance().extract(sourceCode);
            System.err.println("[DummyModel] Features retrieved: " + inputFeatures);

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
        FeatureExtractor extractor = FeatureExtractor.getInstance();
        List<Double> inputFeatures = extractor.extract(sourceCode);

        System.err.println("[Model] Features retrieved: " + inputFeatures);

        Random r = new Random(System.currentTimeMillis());
        return r.nextInt(10);
    }
*/

}
