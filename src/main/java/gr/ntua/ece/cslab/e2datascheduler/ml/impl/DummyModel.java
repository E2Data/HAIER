package gr.ntua.ece.cslab.e2datascheduler.ml.impl;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.util.FeatureExtractor;

import java.util.List;
import java.util.Random;

/**
 * Dummy class that mocks a model and serves integration purposes
 */
public class DummyModel extends Model {

    @Override
    public void load(String path) {

    }

    @Override
    public double predict(String objective, HwResource device, String sourceCode) {
//        FeatureExtractor extractor = FeatureExtractor.getInstance();
//        List<Double> inputFeatures = extractor.extract(sourceCode);
//        double sum = 0;
//        for(Double f : inputFeatures){
//            sum += f;
//        }

        Random r = new Random(System.currentTimeMillis());
        //return r.nextInt((int) sum);
        return r.nextInt(10);
    }
}
