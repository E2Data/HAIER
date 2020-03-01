package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.TimeEvaluationAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
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
     * An ArrayList that includes all {@link Layer} objects in this {@link FlinkExecutionGraph}, in order.
     */
    private ArrayList<Layer> layers;

    /**
     * TODO(ckatsak): Documentation
     */
    private ComputationalGraph computationalGraph;

    /**
     * TODO(ckatsak): Documentation
     */
    private Map<ComputationalJobVertex, Integer> computationalJobVertexLayers;

    /**
     * LayeredEvaluation is the "older" evaluation algorithm; it is less accurate but faster than the "new" algorithm,
     * {@link gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation.ExhaustiveEvaluation}.
     *
     * @param mlModel The Machine Learning {@link Model} that should be consulted for predicting the execution time
     *                for each task.
     */
    public LayeredEvaluation(final Model mlModel) {
        super(mlModel);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Initialization of the required structures for the time evaluation algorithm to run correctly.
     * Calling this method can be a requirement before calling the {@code calculateExecutionTime()}
     * method, and each subclass of {@link TimeEvaluationAlgorithm} may implement it differently.
     *
     * @param flinkExecutionGraph The related {@link FlinkExecutionGraph} at hand.
     */
    @Override
    public void initialization(final FlinkExecutionGraph flinkExecutionGraph) {
        this.computationalGraph = this.deduceComputationalGraph(flinkExecutionGraph);
        this.layers = new ArrayList<>();
        this.computationalJobVertexLayers = new HashMap<>();
        for (ComputationalJobVertex computationalJobVertex : this.computationalGraph.getComputationalJobVertices()) {
            this.computationalJobVertexLayers.put(computationalJobVertex, 0);
        }

        this.constructLayersBFS();
    }

    /**
     * Construct the Layer objects through a BFS-like traversal of the graph.
     *
     * It should be ~ O(V+E), since it visits each node once every time that
     * the latter is found anywhere in the adjacency list.
     *
     * TODO(ckatsak): Special handling for case (this.jobVertices.length == 0)?
     *                Not necessary for now.
     *
     * //\//@param flinkExecutionGraph The initial {@link FlinkExecutionGraph}.
     */
    private void constructLayersBFS() {
        final ArrayList<ComputationalJobVertex> computationalJobVertices =
                this.computationalGraph.getComputationalJobVertices();
        // Construct a queue for the BFS-like traversal.
        final Queue<Integer> q = new LinkedList<>();

        // Construct the first Layer, i.e. JobGraph's "root" JobVertex objects.
        final Layer rootLayer = new Layer();
        for (ComputationalJobVertex rootComputationalJobVertex : this.computationalGraph.getRoots()) {
            rootLayer.addComputationalJobVertex(rootComputationalJobVertex);
            q.add(rootComputationalJobVertex.getIndex());
        }
        this.layers.add(rootLayer);

        // Construct the next Layers via a BFS-like traversal using a Queue.
        while (!q.isEmpty()) {
            // Pop the next JobVertex index and retrieve the corresponding ComputationalJobVertex and its layer.
            final int parentIndex = q.remove();
            final ComputationalJobVertex parentComputationalJobVertex = computationalJobVertices.get(parentIndex);
            final int parentLayer = this.computationalJobVertexLayers.get(parentComputationalJobVertex);

            // Then, loop through its children:
            for (int childIndex : parentComputationalJobVertex.getChildren()) {
                // Retrieve child's corresponding ScheduledJobVertex and its layer.
                final ComputationalJobVertex childComputationalJobVertex = computationalJobVertices.get(childIndex);
                final int childLayer = this.computationalJobVertexLayers.get(childComputationalJobVertex);
                // If this parent imposes a higher layer than the one already set, then change it.
                if (parentLayer + 1 > childLayer) {
                    // Make sure this.layers is big enough:
                    ensureLayersCapacity(parentLayer + 2);
                    // Modify the old Layer object:
                    this.layers.get(childLayer).removeComputationalJobVertex(childComputationalJobVertex);
                    // Modify the new Layer object:
                    this.layers.get(parentLayer + 1).addComputationalJobVertex(childComputationalJobVertex);
                    // Modify child's layer index:
                    this.computationalJobVertexLayers.put(childComputationalJobVertex, parentLayer + 1);
                }
            }

            // Last, append all parent's children to the queue to be (possibly re-)examined.
            q.addAll(parentComputationalJobVertex.getChildren());
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
            final Map<HwResource, ArrayList<ComputationalJobVertex>> layerVerticesPerDevice = layer.getColocations();

            // Initialize an auxiliary array to store the execution time for each device.
            final double[] deviceExecTime = new double[layerVerticesPerDevice.size()];
            // Loop through the tasks assigned on each device to calculate the
            // total execution time per device. Then, store this in the array.
            int i = 0;
            for (ArrayList<ComputationalJobVertex> deviceVertices : layerVerticesPerDevice.values()) {
                deviceExecTime[i++] = sumDurations(flinkExecutionGraph, deviceVertices);
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

            // Store it in Layer too. FIXME(ckatsak): not really needed for now
            layer.setDuration(layerDuration);
            // Sum it to the FlinkExecutionGraph's total execution time.
            totalDuration += layerDuration;
        }

        return totalDuration;
    }

    /**
     * Given an list of {@link ComputationalJobVertex} objects, this function returns the sum of the estimated
     * execution durations, using {@link Model}'s predictions for each task.
     *
     * @param computationalJobVertices The given list of {@link ComputationalJobVertex}.
     * @return The sum of the estimated execution time durations.
     */
    private double sumDurations(final FlinkExecutionGraph flinkExecutionGraph,
                                final List<ComputationalJobVertex> computationalJobVertices) {
        double sum = 0.0d;

        for (ComputationalJobVertex computationalJobVertex : computationalJobVertices) {
            // FIXME(ckatsak): Two versions: one using the FeatureExtractor and another one
            //                 passing the source code to the Model, as per @kbitsak 's preference.

            // FIXME(ckatsak): Temporarily using associated ScheduledJobVertex's sourceCode field.
            final ScheduledJobVertex scheduledJobVertex = flinkExecutionGraph.getScheduledJobVertices().get(
                    this.computationalGraph.getIndexMapping().get(computationalJobVertex.getIndex())
            );

            sum += this.mlModel.predict("execTime",
                    computationalJobVertex.getAssignedResource(),
                    scheduledJobVertex.getSourceCode());
            //sum += this.mlModel.predict("execTime",
            //        scheduledJobVertex.getAssignedResource(),
            //        FeatureExtractor.extract(scheduledJobVertex.getSourceCode()));
        }

        return sum;
    }

}
