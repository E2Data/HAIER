package gr.ntua.ece.cslab.e2datascheduler.ml.impl;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.util.HaierCacheException;

import org.apache.flink.runtime.jobgraph.JobVertex;

import java.util.HashMap;
import java.util.Map;


/**
 * TODO(ckatsak): Implementation & Documentation
 */
public class PredictionCache {

//    private static final Logger logger = Logger.getLogger(PredictionCache.class.getCanonicalName());

    private final Map<JobVertex, Map<HwResource, Double>> cacheEntryMap = new HashMap<>();


    // --------------------------------------------------------------------------------------------


    public void clear() {
        this.cacheEntryMap.clear();
    }


    // --------------------------------------------------------------------------------------------


    /**
     * Retrieve the cached predicted value for the provided {@link JobVertex} on the given {@link HwResource}.
     *
     * @param jobVertex The {@link JobVertex} referred by the query
     * @param device The {@link HwResource} referred by the query
     * @return The cached predicted value
     * @throws HaierCacheException If either one of the keys ({@link JobVertex} or {@link HwResource}) are absent
     */
    public double getPrediction(final JobVertex jobVertex, final HwResource device) {
        if (null == this.cacheEntryMap.get(jobVertex)) {
            throw new HaierCacheException("Absent JobVertex '" + jobVertex.toString() +
                    "' (with JobVertexID '" + jobVertex.getID().toString() + "')");
        }
        final Map<HwResource, Double> cacheEntry = this.cacheEntryMap.get(jobVertex);
        if (null == cacheEntry.get(device)) {
            throw new HaierCacheException("Absent device '" + device.getName() + "@" + device.getHost() +
                    "' for JobVertex '" + jobVertex.toString() +
                    "' (with JobVertexID '" + jobVertex.getID().toString() + "')");
        }
        return cacheEntry.get(device);
    }

    /**
     * Cache the predicted value {@code modelResponse} that refers to the execution of the provided
     * {@link JobVertex} on {@link HwResource} {@code device} to satisfy subsequent queries.
     *
     * @param jobVertex The {@link JobVertex} key of the new cache entry
     * @param device The {@link HwResource} key of the new cache entry
     * @param modelResponse The predicted value to cache
     */
    public void update(final JobVertex jobVertex, final HwResource device, final double modelResponse) {
        if (null == this.cacheEntryMap.get(jobVertex)) {
            this.cacheEntryMap.put(jobVertex, new HashMap<>());
        }
        final Map<HwResource, Double> cacheEntry = this.cacheEntryMap.get(jobVertex);
        cacheEntry.put(device, modelResponse);
    }

}
