package gr.ntua.ece.cslab.e2datascheduler.graph;

import java.util.ArrayList;

/**
 * An auxiliary class that for the DAG extracted by a {@link FlinkExecutionGraph} but comprised only of those
 * vertices that represent computational workloads that can be offloaded to some heterogeneous architecture
 * supported by E2Data.
 */
public class ComputationalGraph {

    /**
     * The initial {@link FlinkExecutionGraph} off which this {@code ComputationalGraph} has been created.
     */
    private final FlinkExecutionGraph flinkExecutionGraph;
    /**
     * indexMapping contains the mapping between the (internal to HAIER) IDs of {@link ComputationalJobVertex} to
     * {@link ScheduledJobVertex}.
     * In other words, {@code indexMapping.get(3)} contains the ID of the {@link ScheduledJobVertex} that corresponds
     * to the {@link ComputationalJobVertex} with {@code ID == 3}.
     */
    private final ArrayList<Integer> indexMapping;
    /**
     * Enables an adjacency list-like representation of the {@code ComputationalGraph}.
     */
    private final ArrayList<ComputationalJobVertex> computationalJobVertices;
    /**
     * A list of the root {@link ComputationalJobVertex} objects of this graph (i.e. the vertices that have no parents).
     */
    private final ArrayList<ComputationalJobVertex> roots;
    /**
     * A list of the sink {@link ComputationalJobVertex} objects of this graph (i.e. the vertices that have no
     * children.)
     */
    private final ArrayList<ComputationalJobVertex> sinks;


    // --------------------------------------------------------------------------------------------

    /**
     * An auxiliary class that for the DAG extracted by a {@link FlinkExecutionGraph} but comprised only of those
     * vertices that represent computational workloads that can be offloaded to some heterogeneous architecture
     * supported by E2Data.
     *
     * @param flinkExecutionGraph The initial {@link FlinkExecutionGraph} off which the new {@code ComputationalGraph}
     *                            will be created.
     * @param computationalJobVertices Enables an adjacency list-like representation of the {@code ComputationalGraph}.
     * @param indexMapping indexMapping contains the mapping between the (internal to HAIER) IDs of
     *                     {@link ComputationalJobVertex} to {@link ScheduledJobVertex}. In other words,
     *                     {@code indexMapping.get(3)} contains the ID of the {@link ScheduledJobVertex} that
     *                     corresponds to the {@link ComputationalJobVertex} with {@code ID == 3}.
     */
    public ComputationalGraph(
            final FlinkExecutionGraph flinkExecutionGraph,
            final ArrayList<ComputationalJobVertex> computationalJobVertices,
            final ArrayList<Integer> indexMapping) {
        this.flinkExecutionGraph = flinkExecutionGraph;
        this.indexMapping = indexMapping;
        this.computationalJobVertices = computationalJobVertices;
        this.roots = new ArrayList<>();
        this.sinks = new ArrayList<>();
    }

    public ArrayList<ComputationalJobVertex> getComputationalJobVertices() {
        return this.computationalJobVertices;
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Given all parents, update all vertices' children, as well as the roots and the sinks of the graph.
     */
    public void updateChildren() {
        for (ComputationalJobVertex computationalJobVertex : this.computationalJobVertices) {
            assert computationalJobVertex.getIndex() == this.computationalJobVertices.indexOf(computationalJobVertex);
            if (computationalJobVertex.getParents().isEmpty()) {
                this.roots.add(computationalJobVertex);
                computationalJobVertex.setRoot(true);
            } else {
                for (int parentIndex : computationalJobVertex.getParents()) {
                    this.computationalJobVertices.get(parentIndex).addChild(computationalJobVertex.getIndex());
                }
            }
        }

        for (ComputationalJobVertex computationalJobVertex : this.computationalJobVertices) {
            if (computationalJobVertex.getChildren().isEmpty()) {
                this.sinks.add(computationalJobVertex);
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ComputationalGraph{" +
                "indexMapping=" + indexMapping +
                ", computationalJobVertices=" + computationalJobVertices +
                '}';
    }
}
