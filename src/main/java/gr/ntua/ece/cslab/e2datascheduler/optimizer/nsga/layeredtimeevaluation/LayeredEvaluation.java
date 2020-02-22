package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.TimeEvaluationAlgorithm;

import org.apache.flink.runtime.jobgraph.JobVertex;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class LayeredEvaluation implements TimeEvaluationAlgorithm {

    /**
     * The Model that is being consulted for the cost of a task on a particular
     * device.
     */
    private final Model mlModel;

    /**
     * An ArrayList that includes all Layers in this FlinkExecutionGraph, in
     * order.
     */
    private final ArrayList<Layer> layers;

    // --------------------------------------------------------------------------------------------

    public LayeredEvaluation(final Model mlModel) {
        this.mlModel = mlModel;
        this.layers = new ArrayList<>();
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void initialization(final FlinkExecutionGraph flinkExecutionGraph) {
        this.constructLayersBFS(flinkExecutionGraph.getJobVertices(), flinkExecutionGraph.getScheduledJobVertices());
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Calculate the total execution time for the given FlinkExecutionGraph.
     */
    @Override
    public double calculateExecutionTime(final FlinkExecutionGraph flinkExecutionGraph) {
        double totalDuration = 0.0d;

        for (Layer layer : layers) {
            // Group the JobVertex objects based on the device they have been assigned to.
            Map<HwResource, ArrayList<ScheduledJobVertex>> layerVerticesPerDevice = layer.getColocations();

            // Initialize an auxiliary array to store the execution time for each device.
            double[] deviceExecTime = new double[layerVerticesPerDevice.size()];
            // Loop through the tasks assigned on each device to calculate the
            // total execution time per device. Then, store this in the array.
            int i = 0;
            for (ArrayList<ScheduledJobVertex> deviceVertices : layerVerticesPerDevice.values()) {
                deviceExecTime[i++] = sumDurations(deviceVertices);
            }

            // Once the execution time on each device is calculated for all
            // devices, current Layer's total execution time is defined as the
            // maximum of these values, since execution on different devices
            // takes place in parallel.
            double layerDuration = 0.0d;
            for (i = 0; i < deviceExecTime.length; ++i) {
                if (deviceExecTime[i] > layerDuration) {
                    layerDuration = deviceExecTime[i];
                }
            }

            // Store it in Layer too. NOTE(ckatsak): not really needed for now
            layer.setDuration(layerDuration);
            // Sum it to the FlinkExecutionGraph's total execution time.
            totalDuration += layerDuration;
        }

        return totalDuration;
    }

    /**
     * Given an List of ScheduledJobVertex objects, this function returns the
     * sum of the estimated execution durations, using the Model's predictions
     * for each task.
     */
    private double sumDurations(final List<ScheduledJobVertex> scheduledJobVertices) {
        double sum = 0.0d;

        for (ScheduledJobVertex scheduledJobVertex : scheduledJobVertices) {
            // XXX(ckatsak): Two versions: one using the FeatureExtractor and another
            // one passing the source code to the Model, as per @kbitsak 's preference.
            sum += this.mlModel.predict("execTime",
                    scheduledJobVertex.getAssignedResource(),
                    scheduledJobVertex.getSourceCode());
            //sum += this.mlModel.predict("execTime", scheduledJobVertex.getAssignedResource(),
            //        FeatureExtractor.extract(scheduledJobVertex.getSourceCode()));
        }

        return sum;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Construct the Layer objects through a BFS-like traversal of the graph.
     *
     * It should be ~ O(V+E), since it visits each node once every time that
     * the latter is found anywhere in the adjacency list.
     *
     * TODO(ckatsak): Special handling for case (this.jobVertices.length == 0)?
     *                Not necessary for now.
     */
    private void constructLayersBFS(final JobVertex[] jobVertices, final ArrayList<ScheduledJobVertex> scheduledJobVertices) {
        // Construct a queue for the BFS-like traversal.
        final Queue<Integer> q = new LinkedList<Integer>();

        // Construct the first Layer, i.e. JobGraph's "root" JobVertex objects.
        final Layer rootLayer = new Layer();
        for (int rootJobVertexIndex : this.findRootJobVertices(jobVertices)) {
            rootLayer.addScheduledJobVertex(scheduledJobVertices.get(rootJobVertexIndex));
            //scheduledJobVertices.get(rootJobVertexIndex).setLayer(0);  // initialized to 0 by default anyway
            q.add(rootJobVertexIndex);
        }
        this.layers.add(rootLayer);

        // Construct the next Layers via a BFS-like traversal using a Queue.
        while (!q.isEmpty()) {
            // Pop the next JobVertex index and retrieve the corresponding ScheduledJobVertex and its layer.
            int parentIndex = q.remove();
            ScheduledJobVertex parentScheduledJobVertex = scheduledJobVertices.get(parentIndex);
            int parentLayer = parentScheduledJobVertex.getLayer();

            // Then, loop through its children:
            for (int childIndex : parentScheduledJobVertex.getChildren()) {
                // Retrieve child's corresponding ScheduledJobVertex and its layer.
                ScheduledJobVertex childScheduledJobVertex = scheduledJobVertices.get(childIndex);
                int childLayer = childScheduledJobVertex.getLayer();
                // If this parent imposes a higher layer than the one already set, then change it.
                if (parentLayer + 1 > childLayer) {
                    ensureLayersCapacity(parentLayer + 2);                                            // make sure layers is big enough
                    this.layers.get(childLayer).removeScheduledJobVertex(childScheduledJobVertex);    // modify old Layer object
                    this.layers.get(parentLayer + 1).addScheduledJobVertex(childScheduledJobVertex);  // modify new Layer object
                    childScheduledJobVertex.setLayer(parentLayer + 1);                                // modify child's layer index
                }
            }

            // Last, append all parent's children to the queue to be (possibly re-)examined.
            q.addAll(parentScheduledJobVertex.getChildren());
        }
    }

    /**
     * Find and return the root vertices for the JobGraph related to this
     * FlinkExecutionGraph.
     *
     * ~ O(V)
     */
    private List<Integer> findRootJobVertices(final JobVertex[] jobVertices) {
        if (jobVertices.length == 0) {
            return java.util.Collections.emptyList();
        }

        final List<Integer> roots = new ArrayList<>(1);
        for (int i = 0; i < jobVertices.length; i++) {
            if (jobVertices[i].hasNoConnectedInputs()) {
                roots.add(i);
            }
        }
        return roots;
    }

    /**
     * Makes sure that field {@code ArrayList<Layer> layers} is properly
     * initialized and already accommodates the given capacity of (possibly
     * empty) Layer objects.
     */
    private void ensureLayersCapacity(int cap) {
        if (this.layers.size() >= cap) {
            return;
        }

        this.layers.ensureCapacity(cap);
        for (int i = this.layers.size(); i < cap; ++i) {
            this.layers.add(i, new Layer());
        }
    }

}
