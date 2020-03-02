package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


/**
 * The base (abstract) class that must be extended by any execution time evaluation algorithm.
 */
public abstract class TimeEvaluationAlgorithm {

    /**
     * The Model that is being consulted for the cost of a task on a particular device.
     */
    protected final Model mlModel;

    /**
     * The base (abstract) class that must be extended by any execution time evaluation algorithm.
     *
     * @param mlModel The Machine Learning {@link Model} to be used for predicting each task's execution time.
     */
    protected TimeEvaluationAlgorithm(final Model mlModel) {
        this.mlModel = mlModel;
    }

    /**
     * Initialization of the required structures for the time evaluation algorithm to run correctly.
     * Calling this method before calling the {@code calculateExecutionTime()} method can be a requirement,
     * although each subclass of {@link TimeEvaluationAlgorithm} may implement this differently.
     *
     * @param haierExecutionGraph The related {@link HaierExecutionGraph} at hand.
     */
    public abstract void initialization(final HaierExecutionGraph haierExecutionGraph);

    /**
     * Calculate the estimated execution time of the given {@link HaierExecutionGraph}.
     *
     * @param haierExecutionGraph The related {@link HaierExecutionGraph} at hand.
     * @return A double-precision floating-point number that represents the evaluation
     */
    public abstract double calculateExecutionTime(final HaierExecutionGraph haierExecutionGraph);

    // --------------------------------------------------------------------------------------------

    /**
     * Find the root vertices of the sub-DAG in the given {@link HaierExecutionGraph} that consists only of vertices
     * which represent computational tasks that can be offloaded to heterogeneous architectures supported by E2Data.
     *
     * FIXME(ckatsak): Probably not needed anymore.
     *
     * @param haierExecutionGraph The initial {@link HaierExecutionGraph}.
     * @return The (internal to HAIER) indices of the root vertices.
     */
    protected final Set<Integer> findComputationalRootJobVertices(final HaierExecutionGraph haierExecutionGraph) {
        final Set<Integer> ret = new HashSet<>();
        final ArrayList<ScheduledJobVertex> scheduledJobVertices = haierExecutionGraph.getScheduledJobVertices();

        final Stack<Integer> stack = new Stack<>();
        stack.addAll(haierExecutionGraph.findRootJobVertices());
        final boolean[] visited = new boolean[haierExecutionGraph.getJobVertices().length];
        while (!stack.isEmpty()) {
            final int current = stack.pop();
            if (visited[current]) {
                continue;
            }
            if (HaierExecutionGraph.isComputational(scheduledJobVertices.get(current).getJobVertex())) {
                ret.add(current);
                for (Integer vertex : haierExecutionGraph.getSubDAGInclusive(scheduledJobVertices.get(current))) {
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
     * Given a {@link HaierExecutionGraph}, deduce the associated {@link ComputationalGraph}; i.e. the DAG comprised
     * only of the vertices that represent computational tasks offloadable to heterogeneous architectures supported
     * by E2Data.
     *
     * ~ O(V+E)
     *
     * @param haierExecutionGraph The initial {@link HaierExecutionGraph}.
     * @return The produced {@link ComputationalGraph}.
     */
    protected final ComputationalGraph deduceComputationalGraph(final HaierExecutionGraph haierExecutionGraph) {
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

        final ArrayList<ScheduledJobVertex> scheduledJobVertices = haierExecutionGraph.getScheduledJobVertices();
        final Map<ScheduledJobVertex, ComputationalJobVertex> compVertices = new HashMap<>();
        final boolean[] visited = new boolean[scheduledJobVertices.size()];  // Must be all initialized to false.

        final Stack<SubDAGPair> stack = new Stack<>();
        for (int rootIndex : haierExecutionGraph.findRootJobVertices()) {
            stack.add(new SubDAGPair(scheduledJobVertices.get(rootIndex), -1));
        }

        // Create the set of the new ComputationalJobVertex objects for the new ComputationalGraph.
        while (!stack.isEmpty()) {
            final SubDAGPair current = stack.pop();
            if (HaierExecutionGraph.isComputational(current.scheduledJobVertex)) {
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

        final ComputationalGraph ret = new ComputationalGraph(haierExecutionGraph, newVertices, indexMapping);
        ret.updateChildren();
        return ret;
    }

}
