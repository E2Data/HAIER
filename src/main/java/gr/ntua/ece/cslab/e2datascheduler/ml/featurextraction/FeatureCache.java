package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

import org.apache.flink.runtime.jobgraph.JobVertex;

import java.util.List;


/**
 * TODO(ckatsak): Documentation
 */
public interface FeatureCache {

    /**
     * TODO(ckatsak): Documentation
     *
     * @param jobVertex
     * @param device
     * @return
     */
    List<TornadoFeatureVector> getFeatureVectors(final JobVertex jobVertex, final HwResource device);

}
