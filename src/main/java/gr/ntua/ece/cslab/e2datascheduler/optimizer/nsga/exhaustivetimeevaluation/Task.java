package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalJobVertex;

import java.util.ArrayList;
import java.util.List;

class Task {

    /**
     * FAKE_DURATION_FACTOR is the integer (power of 10) number by which we have to multiply the estimated duration of
     * the Task in order to make it an integer without losing accuracy.
     */
    static final int FAKE_DURATION_FACTOR = 1000;

    /**
     * A unique ID for each Task in a Graph.
     */
    private final int index;
    /**
     * A representation of the the device that the Task has been assigned on.
     */
    private final HwResource device;
    /**
     * The estimated duration of execution for the Task on the device it has been assigned on.
     */
    private final double duration;
    /**
     * A list of the unique IDs of Task's children-Tasks on a particular Graph.
     */
    private final List<Integer> children;
    /**
     * Indicates whether the Task is a "source" in a Graph (i.e. it has no parent Tasks).
     */
    private final boolean isSource;

    /**
     * Parent Tasks are not provided, but they can be calculated.
     */
    private List<Integer> parents;
    /**
     * Same as this.duration, multiplied by {@code Task.FAKE_DURATION_FACTOR} and cast to int.
     */
    private int fakeDuration;
    /**
     * The total time (fakeDurations) that is required for the execution of this Task to be completed, having taken
     * into account both the data and the device dependencies. (NOTE: this.fakeDuration is included)
     */
    private int pathCost;

    // --------------------------------------------------------------------------------------------

    Task(final int index, final HwResource device, final double duration, final List<Integer> children,
         final boolean isSource) {
        this.index = index;
        this.device = device;
        this.duration = duration;
        this.children = new ArrayList<>(children);
        this.isSource = isSource;

        this.parents = new ArrayList<>();
        this.fakeDuration = (int) this.duration * Task.FAKE_DURATION_FACTOR;
        this.pathCost = -1;
    }

    Task(final int index, final HwResource device, final double duration, final List<Integer> children,
         final List<Integer> parents, final boolean isSource) {
        this(index, device, duration, children, isSource);
        this.parents = new ArrayList<>(parents);
    }

    /* Copy constructor */
    Task(final Task task) {
        this.index = task.index;
        this.device = task.device;
        this.duration = task.duration;
        this.children = new ArrayList<>(task.children.size());
        for (int i = 0; i < task.children.size(); i++) {
            this.children.add(i, task.children.get(i));
        }
        this.isSource = task.isSource;

        this.parents = new ArrayList<>(task.parents);
        this.fakeDuration = task.fakeDuration;
        this.pathCost = task.pathCost;
    }

    // --------------------------------------------------------------------------------------------

    int getIndex() {
        return index;
    }

    HwResource getDevice() {
        return device;
    }

    double getDuration() {
        return duration;
    }

    int getFakeDuration() {
        return fakeDuration;
    }

    List<Integer> getChildren() {
        return children;
    }

    boolean isSource() {
        return isSource;
    }

    List<Integer> getParents() {
        return parents;
    }

    void setParents(List<Integer> parents) {
        this.parents = parents;
    }

    int getPathCost() {
        return pathCost;
    }

    void setPathCost(int pathCost) {
        this.pathCost = pathCost;
    }

    @Override
    public String toString() {
        String ret = "<Task-" + index;
        ret += isSource ? " (source) " : "";
        ret += children.isEmpty() ? " (sink)" : "";
        ret += " ('" + device + '\'' + ", " + duration + ", " + pathCost + ")>";
        return ret;
    }

}
