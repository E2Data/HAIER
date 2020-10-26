package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.cache;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.FeatureCache;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.TornadoFeatureVector;
import gr.ntua.ece.cslab.e2datascheduler.util.HaierCacheException;

import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class DummyCSLabFeatureCache implements FeatureCache {

    private final Map<JobVertex, Map<HwResource, List<TornadoFeatureVector>>> cacheEntryMap;

    public DummyCSLabFeatureCache(final JobGraph jobGraph, final List<HwResource> devices) {
        this.cacheEntryMap = new HashMap<>();
        for (JobVertex jobVertex : jobGraph.getVerticesSortedTopologicallyFromSources()) {
            this.cacheEntryMap.put(jobVertex, new HashMap<>());
            final Map<HwResource, List<TornadoFeatureVector>> cacheEntry = this.cacheEntryMap.get(jobVertex);
            for (HwResource device : devices) {
                final List<TornadoFeatureVector> featureVectorList = new ArrayList<>();
                featureVectorList.add(TornadoFeatureVector.newDummy());
                cacheEntry.put(device, featureVectorList);
            }
        }
    }

    /**
     * TODO(ckatsak): Documentation
     *
     * @param jobVertex
     * @param device
     * @return
     */
    @Override
    public List<TornadoFeatureVector> getFeatureVectors(final JobVertex jobVertex, final HwResource device) {
        final List<TornadoFeatureVector> ret = this.cacheEntryMap.get(jobVertex).get(device);
        if (null == ret || ret.size() != 1) {
            throw new HaierCacheException("UNREACHABLE: null or empty List<TornadoFeatureVector>");
        }
        return ret;
    }

}
