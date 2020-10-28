package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.cache;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.FeatureCache;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.TornadoFeatureExtractor;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.TornadoFeatureVector;
import gr.ntua.ece.cslab.e2datascheduler.util.HaierCacheException;

import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * FIXME(ckatsak): Devices retrieved by Tornado's fake compilation should be mapped to {@link HwResource}s
 *                 reported by YARN for feature extraction (including this cache) to work!
 */
public class TornadoFeatureCache implements FeatureCache {

    private static final Logger logger = Logger.getLogger(TornadoFeatureCache.class.getCanonicalName());

    private final Map<JobVertex, Map<HwResource, List<TornadoFeatureVector>>> cacheEntryMap;


    // --------------------------------------------------------------------------------------------


    /**
     * FIXME(ckatsak): Implementation & Documentation
     *
     * @param jobGraph
     * @param devices
     */
    public TornadoFeatureCache(final JobGraph jobGraph, final List<HwResource> devices) {
        /*
         * FIXME(ckatsak):
         *  - Initialize a new TornadoFeatureExtractor
         *   * Make sure the provided HwResources are correctly mapped to TornadoVM virtual devices
         *   * Use it to "fake compile" every operator in every JobVertex in the provided JobGraph
         *   * Update the cache entries so that all subsequent queries for this JobGraph are satisfied
         */
        Map<JobVertex, Map<HwResource, List<TornadoFeatureVector>>> cacheEntryMapTemp;
        try {
            cacheEntryMapTemp = new TornadoFeatureExtractor(devices)
                    .extractFrom(jobGraph, this.getClass().getClassLoader());
        } catch (final IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            cacheEntryMapTemp = new HashMap<>();
        }
        this.cacheEntryMap = cacheEntryMapTemp;
    }


    // --------------------------------------------------------------------------------------------


    /**
     * FIXME(ckatsak): Documentation
     *
     * @param jobVertex
     * @param device
     * @param tornadoFeatureVectors
     */
    private void updateFeatureVectors(
            final JobVertex jobVertex,
            final HwResource device,
            final List<TornadoFeatureVector> tornadoFeatureVectors) {
        if (null == this.cacheEntryMap.get(jobVertex)) {
            this.cacheEntryMap.put(jobVertex, new HashMap<>());
        }
        final Map<HwResource, List<TornadoFeatureVector>> cacheEntry = this.cacheEntryMap.get(jobVertex);
        if (null == cacheEntry.get(device)) {
            cacheEntry.put(device, new ArrayList<>());
        }
        cacheEntry.get(device).addAll(tornadoFeatureVectors);
    }

    /**
     * FIXME(ckatsak): Documentation
     *
     * @param jobVertex
     * @param device
     * @return
     */
    @Override
    public List<TornadoFeatureVector> getFeatureVectors(final JobVertex jobVertex, final HwResource device) {
        final Map<HwResource, List<TornadoFeatureVector>> cacheEntry = this.cacheEntryMap.get(jobVertex);
        if (null == cacheEntry) {
            throw new HaierCacheException("Unknown JobVertex '" + jobVertex.toString() +
                    "' (with JobVertexID '" + jobVertex.getID() + "')");
        }
        if (null == cacheEntry.get(device)) {
            throw new HaierCacheException("Unknown device '" + device.getName() + "@" + device.getHost() +
                    "' for JobVertex '" + jobVertex.toString() + "' (with JobVertexID '" + jobVertex.getID() + "')");
        }
        return cacheEntry.get(device);
    }

}
