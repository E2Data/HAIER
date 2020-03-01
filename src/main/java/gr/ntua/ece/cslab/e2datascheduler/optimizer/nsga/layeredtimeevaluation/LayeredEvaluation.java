package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.TimeEvaluationAlgorithm;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * LayeredEvaluation is the "older" time evaluation algorithm; it is less accurate but faster than the "new" algorithm,
 * {@link gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation.ExhaustiveEvaluation}.
 */
public class LayeredEvaluation extends TimeEvaluationAlgorithm {

    /**
     * An ArrayList that includes all Layers in this FlinkExecutionGraph, in order.
     */
    private final ArrayList<Layer> layers;

    /**
     * LayeredEvaluation is the "older" evaluation algorithm; it is less accurate but faster than the "new" algorithm,
     * {@link gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation.ExhaustiveEvaluation}.
     *
     * @param mlModel The Machine Learning {@link Model} that should be consulted for predicting the time of each task.
     */
    public LayeredEvaluation(final Model mlModel) {
        super(mlModel);
        this.layers = new ArrayList<>();
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Initialization of the required structures for the time evaluation algorithm to run correctly.
     * Calling this method can be a requirement before calling the {@code calculateExecutionTime()} method,
     * and each subclass of {@link TimeEvaluationAlgorithm} may implement it differently.
     *
     * @param flinkExecutionGraph The related {@link FlinkExecutionGraph} at hand.
     */
    @Override
    public void initialization(final FlinkExecutionGraph flinkExecutionGraph) {
        this.constructLayersBFS(flinkExecutionGraph);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Calculate the estimated execution time of the given {@link FlinkExecutionGraph}.
     * Method {@code initialization()} must have been called before calling this method.
     *
     * @param flinkExecutionGraph The related {@link FlinkExecutionGraph} at hand.
     * @return A double-precision floating-point number that represents the evaluation
     */
    @Override
    public double calculateExecutionTime(final FlinkExecutionGraph flinkExecutionGraph) {
        double totalDuration = 0.0d;

        for (Layer layer : layers) {
            // Group the JobVertex objects based on the device they have been assigned to.
            Map<HwResource, ArrayList<ScheduledJobVertex>> layerVerticesPerDevice = layer.getColocations();

            // Initialize an auxiliary array to store the execution time for each device.
            final double[] deviceExecTime = new double[layerVerticesPerDevice.size()];
            // Loop through the tasks assigned on each device to calculate the
            // total execution time per device. Then, store this in the array.
            int i = 0;
            for (ArrayList<ScheduledJobVertex> deviceVertices : layerVerticesPerDevice.values()) {
                deviceExecTime[i++] = sumDurations(deviceVertices);
            }

            // Once the execution time on each device is calculated for all devices, current
            // Layer's total execution time is defined as the maximum of these values, since
            // execution on different devices takes place in parallel.
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
     * Given an list of {@link ScheduledJobVertex} objects, this function returns the sum of the estimated execution
     * durations, using the Model's predictions for each task.
     *
     * @param scheduledJobVertices The given list of {@link ScheduledJobVertex}.
     * @return The sum of the estimated execution time durations.
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
     *
     * @param flinkExecutionGraph The initial {@link FlinkExecutionGraph}.
     */
    private void constructLayersBFS(final FlinkExecutionGraph flinkExecutionGraph) {
        final ArrayList<ScheduledJobVertex> scheduledJobVertices = flinkExecutionGraph.getScheduledJobVertices();
        // Construct a queue for the BFS-like traversal.
        final Queue<Integer> q = new LinkedList<>();

        // Construct the first Layer, i.e. JobGraph's "root" JobVertex objects.
        final Layer rootLayer = new Layer();
        for (int rootJobVertexIndex : flinkExecutionGraph.findRootJobVertices()) {
            rootLayer.addScheduledJobVertex(scheduledJobVertices.get(rootJobVertexIndex));
            //scheduledJobVertices.get(rootJobVertexIndex).setLayer(0);  // initialized to 0 by default anyway
            q.add(rootJobVertexIndex);
        }
        this.layers.add(rootLayer);

        // Construct the next Layers via a BFS-like traversal using a Queue.
        while (!q.isEmpty()) {
            // Pop the next JobVertex index and retrieve the corresponding ScheduledJobVertex and its layer.
            final int parentIndex = q.remove();
            final ScheduledJobVertex parentScheduledJobVertex = scheduledJobVertices.get(parentIndex);
            final int parentLayer = parentScheduledJobVertex.getLayer();

            // Then, loop through its children:
            for (int childIndex : parentScheduledJobVertex.getChildren()) {
                // Retrieve child's corresponding ScheduledJobVertex and its layer.
                final ScheduledJobVertex childScheduledJobVertex = scheduledJobVertices.get(childIndex);
                final int childLayer = childScheduledJobVertex.getLayer();
                // If this parent imposes a higher layer than the one already set, then change it.
                if (parentLayer + 1 > childLayer) {
                    // Make sure this.layers is big enough:
                    ensureLayersCapacity(parentLayer + 2);
                    // Modify the old Layer object:
                    this.layers.get(childLayer).removeScheduledJobVertex(childScheduledJobVertex);
                    // Modify the new Layer object:
                    this.layers.get(parentLayer + 1).addScheduledJobVertex(childScheduledJobVertex);
                    // Modify child's layer index:
                    childScheduledJobVertex.setLayer(parentLayer + 1);
                }
            }

            // Last, append all parent's children to the queue to be (possibly re-)examined.
            q.addAll(parentScheduledJobVertex.getChildren());
        }
    }

    /**
     * Makes sure that field {@code ArrayList<Layer> layers} is properly initialized and already accommodates the
     * given capacity of (possibly empty) Layer objects.
     *
     * @param cap The given capacity to ensure.
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
