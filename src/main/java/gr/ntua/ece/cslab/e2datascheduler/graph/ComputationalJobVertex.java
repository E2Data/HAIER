package gr.ntua.ece.cslab.e2datascheduler.graph;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;

import java.util.ArrayList;

/**
 *
 */
public class ComputationalJobVertex {

    /**
     * The {@link ScheduledJobVertex} that this vertex represents.
     */
    private final ScheduledJobVertex scheduledJobVertex;
    /**
     * A list of the indices of this vertex's parent vertices.
     */
    private final ArrayList<Integer> parents;
    /**
     * A list of the indices of this vertex's children vertices.
     */
    private final ArrayList<Integer> children;

    /**
     * The (internal to HAIER) index of this vertex; probably different than the index of the {@link ScheduledJobVertex}
     * associated with this vertex.
     */
    private int index;
    /**
     *
     */
    private boolean isRoot;

    // --------------------------------------------------------------------------------------------

    /**
     *
     * @param scheduledJobVertex The {@link ScheduledJobVertex} that this vertex represents.
     */
    public ComputationalJobVertex(final ScheduledJobVertex scheduledJobVertex) {
        this.scheduledJobVertex = scheduledJobVertex;
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.index = scheduledJobVertex.getJobVertexIndex();
        this.isRoot = false;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setRoot(boolean root) {
        isRoot = root;
    }

    public ArrayList<Integer> getParents() {
        return this.parents;
    }

    public HwResource getAssignedResource() {
        return this.scheduledJobVertex.getAssignedResource();
    }

    public String getSourceCode() {
        return this.scheduledJobVertex.getSourceCode();
    }

    /**
     * Append an index to this vertex's list of parent vertices.
     *
     * @param parentIndex The new parent vertex's index.
     */
    public void addParent(final int parentIndex) {
        if (!this.parents.contains(parentIndex)) {
            this.parents.add(parentIndex);
        }
    }

    /**
     *
     * @return A list of the indices of this vertex's children vertices.
     */
    public ArrayList<Integer> getChildren() {
        return children;
    }

    /**
     * Append an index to this vertex's list of children vertices.
     *
     * @param childIndex The new child vertex's index.
     */
    public void addChild(final int childIndex) {
        if (!this.children.contains(childIndex)) {
            this.children.add(childIndex);
        }
    }

    /**
     * Translate the indices of the parent vertices of this vertex, using the given mapping.
     *
     * @param indexMapping The mapping between the (internal to HAIER) IDs of the vertices.
     */
    public void translateParents(final ArrayList<Integer> indexMapping) {
        for (int i = 0; i < this.parents.size(); i++) {
            this.parents.set(i, indexMapping.indexOf(this.parents.get(i)));
        }
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return "ComputationalJobVertex{" +
                "scheduledJobVertex=" + scheduledJobVertex.getJobVertexIndex() +
                ", parents=" + parents +
                ", children=" + children +
                ", index=" + index +
                ", isRoot=" + isRoot +
                '}';
    }
}
