package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 *
 */
public abstract class TimeEvaluationAlgorithm {

    /**
     * The Model that is being consulted for the cost of a task on a particular device.
     */
    protected final Model mlModel;

    protected TimeEvaluationAlgorithm(final Model mlModel) {
        this.mlModel = mlModel;
    }

    /**
     * TODO(ckatsak): Documentation
     *
     * @param flinkExecutionGraph
     */
    public abstract void initialization(final FlinkExecutionGraph flinkExecutionGraph);

    /**
     * TODO(ckatsak): Documentation
     *
     * @param flinkExecutionGraph
     * @return
     */
    public abstract double calculateExecutionTime(final FlinkExecutionGraph flinkExecutionGraph);

    // --------------------------------------------------------------------------------------------

    /**
     * TODO(ckatsak): Documentation
     *
     * @param flinkExecutionGraph
     * @return
     */
    protected final Set<Integer> findComputationalRootJobVertices(final FlinkExecutionGraph flinkExecutionGraph) {
        final Set<Integer> ret = new HashSet<>();
        final ArrayList<ScheduledJobVertex> scheduledJobVertices = flinkExecutionGraph.getScheduledJobVertices();

        final Stack<Integer> stack = new Stack<>();
        stack.addAll(flinkExecutionGraph.findRootJobVertices());
        final boolean[] visited = new boolean[flinkExecutionGraph.getJobVertices().length];
        while (!stack.isEmpty()) {
            final int current = stack.pop();
            if (visited[current]) {
                continue;
            }
            if (FlinkExecutionGraph.isComputational(scheduledJobVertices.get(current).getJobVertex())) {
                ret.add(current);
                for (Integer vertex : flinkExecutionGraph.getSubDAGInclusive(scheduledJobVertices.get(current))) {
                    visited[vertex] = true;
                }
            } else {
                stack.addAll(scheduledJobVertices.get(current).getChildren());
                visited[current] = true;
            }
        }

        return ret;
    }

    /**
     * TODO(ckatsak): Documentation
     *
     * @param flinkExecutionGraph
     * @return
     */
    protected final ComputationalGraph deduceComputationalGraph(final FlinkExecutionGraph flinkExecutionGraph) {
        class SubDAGPair {
            final ScheduledJobVertex scheduledJobVertex;
            final int lastComputationalParent;

            /**
             * A simple method-local inner class to allow pushing/popping
             * 2-tuples in/off the Stack during the traversal of the graph.
             */
            SubDAGPair(final ScheduledJobVertex scheduledJobVertex, final int lastComputationalParent) {
                this.scheduledJobVertex = scheduledJobVertex;
                this.lastComputationalParent = lastComputationalParent;
            }
        }

        final ArrayList<ScheduledJobVertex> scheduledJobVertices = flinkExecutionGraph.getScheduledJobVertices();
        final Map<ScheduledJobVertex, ComputationalJobVertex> compVertices = new HashMap<>();
        final boolean[] visited = new boolean[scheduledJobVertices.size()];  // Must be all initialized to false.

        final Stack<SubDAGPair> stack = new Stack<>();
        for (int rootIndex : flinkExecutionGraph.findRootJobVertices()) {
            stack.add(new SubDAGPair(scheduledJobVertices.get(rootIndex), -1));
        }

        // Create the set of the new ComputationalJobVertex objects for the new ComputationalGraph.
        while (!stack.isEmpty()) {
            final SubDAGPair current = stack.pop();
            if (FlinkExecutionGraph.isComputational(current.scheduledJobVertex)) {
                final ComputationalJobVertex computationalJobVertex;
                if (visited[current.scheduledJobVertex.getJobVertexIndex()]) {
                    computationalJobVertex = compVertices.get(current.scheduledJobVertex);
                } else {
                    computationalJobVertex = new ComputationalJobVertex(current.scheduledJobVertex);
                    compVertices.put(current.scheduledJobVertex, computationalJobVertex);
                    visited[current.scheduledJobVertex.getJobVertexIndex()] = true;
                }
                if (-1 != current.lastComputationalParent) {
                    computationalJobVertex.addParent(current.lastComputationalParent);
                }
                for (int childIndex : current.scheduledJobVertex.getChildren()) {
                    stack.push(new SubDAGPair(scheduledJobVertices.get(childIndex),
                                              current.scheduledJobVertex.getJobVertexIndex()));
                }
            } else {
                for (int childIndex : current.scheduledJobVertex.getChildren()) {
                    stack.push(new SubDAGPair(scheduledJobVertices.get(childIndex), current.lastComputationalParent));
                }
                //visited[current.scheduledJobVertex.getJobVertexIndex()] = true;
            }
        }

        // Now create a List of those ComputationalJobVertex objects.
        final ArrayList<ComputationalJobVertex> newVertices = new ArrayList<>(compVertices.size());
        newVertices.addAll(compVertices.values());
        // Store the mapping between the new and the old (all internal to HAIER) indices.
        final ArrayList<Integer> indexMapping = new ArrayList<>(newVertices.size());
        for (int i = 0; i < newVertices.size(); i++) {
            indexMapping.add(i, newVertices.get(i).getIndex());
        }

        // Set the indices of the ComputationalJobVertex according to their (otherwise random) order
        // in the newVertices list, and fix the indices of their parents according to the mapping.
        for (int i = 0; i < newVertices.size(); i++) {
            final ComputationalJobVertex compJobVertex = newVertices.get(i);
            compJobVertex.setIndex(i);
            compJobVertex.translateParents(indexMapping);
        }

        final ComputationalGraph ret = new ComputationalGraph(flinkExecutionGraph, newVertices, indexMapping);
        ret.updateChildren();
        return ret;
    }

}
