package gr.ntua.ece.cslab.e2datascheduler.beans.graph;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


/**
 *  A graph that represents the output of the scheduling process and is ready
 *  for execution
 */
public class ExecutionGraph extends JobGraph {

    /**
     * An ArrayList representation of the graph.
     */
    private ArrayList<ScheduledGraphNode> executionGraph;

    /**
     * An ArrayList composed of ExecutionGraph's Layers.
     */
    private ArrayList<Layer> layers;

    /**
     * A map to store the calculated cost for each objective examined.
     * This should be useful for an OptimizationPolicy to decide among multiple
     * Pareto solution graphs.
     */
    private Map<String, Double> objectiveCosts;


    public ArrayList<ScheduledGraphNode> getExecutionGraph() {
        return executionGraph;
    }

    public void setExecutionGraph(ArrayList<ScheduledGraphNode> executionGraph) {
        this.executionGraph = executionGraph;
    }

    public ArrayList<Layer> getLayers() {
        return layers;
    }

    /**
     * XXX(ckatsak): This was never really meant to be used; it's here for the
     * bean sh!t
     */
    public void setLayers(ArrayList<Layer> layers) {
        this.layers = layers;
    }

    public Layer getLayer(int index) {
        return layers.get(index);
    }

    public Map<String, Double> getObjectiveCosts() {
        return objectiveCosts;
    }

    public void setObjectiveCosts(Map<String, Double> objectiveCosts) {
        this.objectiveCosts = objectiveCosts;
    }

    /**
     * Promoted internal {@code ArrayList<ScheduledGraphNode>.get(int)}.
     */
    public ScheduledGraphNode get(int index) {
        return executionGraph.get(index);
    }

    /**
     * Promoted internal {@code ArrayList<ScheduledGraphNode>.add(int, ScheduledGraphNode)}.
     */
    public void add(int index, ScheduledGraphNode scheduledGraphNode) {
        executionGraph.add(index, scheduledGraphNode);
    }

    /**
     * Promoted internal {@code ArrayList<ScheduledGraphNode>.add(ScheduledGraphNode)}.
     */
    public boolean add(ScheduledGraphNode scheduledGraphNode) {
        return executionGraph.add(scheduledGraphNode);
    }


    /**
     * Construct and populate this ExecutionGraph's Layer objects.
     */
    public void constructLayers() {
        //constructLayersFromParents();
        constructLayersBFS();
    }

    /**
     * Makes sure that field {@code ArrayList<Layer> layers} is properly
     * initialized and already accommodates the given capacity of (possibly
     * empty) Layer objects.
     */
    private void ensureLayersCapacity(int cap) {
        if (layers.size() >= cap) {
            return;
        }

        layers.ensureCapacity(cap);
        for (int i = layers.size(); i < cap; ++i) {
            Layer layer = new Layer();
            layer.setNodes(new ArrayList<ScheduledGraphNode>());
            layers.add(i, layer);
        }
    }

    /**
     * Given a list of ScheduledGraphNodes, it returns the maximum layer found
     * among them.
     */
    private static int maxLayer(ArrayList<ScheduledGraphNode> list) {
        int max = 0;
        for (ScheduledGraphNode node : list) {
            if (node.getLayer() > max) {
                max = node.getLayer();
            }
        }
        return max;
    }

    /**
     * Construct the Layer objects through a BFS-like traversal of the graph.
     * It should be ~ O(V+E), since it visits each node once every time that
     * the latter is found anywhere in the adjacency list.
     * This implementation does not require the ScheduledGraphNode.parents.
     */
    private void constructLayersBFS() {
        // Allocate memory for the ArrayList and a Queue.
        layers = new ArrayList<Layer>();
        Queue<Integer> q = new LinkedList<Integer>();

        // Construct the first Layer, i.e. the graph's roots.
        Layer layer = new Layer();
        layer.setNodes(new ArrayList<ScheduledGraphNode>());
        layers.add(layer);
        for (Integer nodeId : getRoots()) {
            layer.addNode(executionGraph.get(nodeId));
            //executionGraph.get(nodeId).setLayer(0);  // initialized to 0 by default anyway
            q.add(nodeId);
        }

        // Construct the next Layers via a BFS-like traversal using a Queue.
        while (!q.isEmpty()) {
            // Pop the next node and figure out its corresponding ScheduledGraphNode and layer.
            Integer parentGnId = q.remove();
            ScheduledGraphNode parentSgn = executionGraph.get(parentGnId);
            int parentLayer = parentSgn.getLayer();

            // Then, loop through its children:
            GraphNode parentGn = indexedGraph.get(parentGnId);
            for (Integer childGnId: parentGn.getChildren()) {

                GraphNode childGn = indexedGraph.get(childGnId);
                // Figure out its corresponding ScheduledGraphNode and layer.
                ScheduledGraphNode childSgn = executionGraph.get(childGn.getId());
                int childLayer = childSgn.getLayer();
                // If this parent imposes a higher layer than the one already set, then change it
                if (parentLayer + 1 > childLayer) {
                    ensureLayersCapacity(parentLayer + 2);         // make sure layers is big enough
                    layers.get(childLayer).removeNode(childSgn);   // modify old Layer object
                    layers.get(parentLayer + 1).addNode(childSgn); // modify new Layer object
                    childSgn.setLayer(parentLayer + 1);            // modify child's layer index
                }
            }

            // Last, append all parent's children to the queue to be (possibly re-)examined.
            q.addAll(parentGn.getChildren());
        }
    }

//    /**
//     * Calculates and stores the parent nodes for each node in the graph.
//     * It performs a graph traversal through the adjacency list, in O(V+E).
//     * For now, it is only needed by constructLayersFromParents() method.
//     */
//    private void calculateParents() {
//        /* Loop through every node in the graph. */
//        for (ScheduledGraphNode node : executionGraph) {
//            /* If it is a root node, it has no parents, thus we may skip it. */
//            if (isRoot(node)) {
//                node.setParents(null);
//                continue;
//            }
//            /* If it has no children, it's parent to no one, thus we may skip it. */
//            if (node.children == null || node.children.isEmpty()) {
//                continue;
//            }
//
//            /* Reaching here, we are sure that the node is parent to some other nodes. */
//            if (node.getParents() == null) {
//                node.setParents(new ArrayList<ScheduledGraphNode>());
//            }
//            for (GraphNode child : node.getChildren()) {
//                //XXX(ckatsak): We assume that GraphNode.ID is the same as the
//                // node's index in JobGraph, hence in the ExecutionGraph too.
//                node.addParent(executionGraph.get(child.getId()));
//            }
//        }
//    }
//
//    /**
//     * Construct the Layer objects through repeated graph traversals.
//     * FIXME(ckatsak): Is this even sound? Furthermore, ~ O(V*(V+E)) (?) sucks.
//     */
//    @Deprecated
//    private void constructLayersFromParents() {
//        // First, calculate each graph node's parent nodes.
//        calculateParents();
//
//        // Allocate memory for the ArrayList.
//        layers = new ArrayList<Layer>();
//
//        // Construct the first Layer, i.e. the graph's roots.
//        Layer layer = new Layer();
//        layer.setNodes(new ArrayList<ScheduledGraphNode>());
//        layers.add(layer);
//        for (GraphNode node : getRoots()) {
//            int nodeId = node.getId();
//            layer.addNode(executionGraph.get(nodeId));
//            //executionGraph.get(nodeId).setLayer(0);  // initialized to 0 by default anyway
//        }
//
//        // Construct the next Layers via repeated graph traversals and recalculations.
//        for (int times = 0; times < executionGraph.size(); ++times) {
//            for (ScheduledGraphNode child : executionGraph) {
//                int childLayer = child.getLayer();
//                int parentsMaxLayer = maxLayer(child.getParents());
//                if (childLayer < parentsMaxLayer + 1) {
//                    ensureLayersCapacity(parentsMaxLayer + 2);
//                    layers.get(childLayer).removeNode(child);        // modify old Layer object
//                    layers.get(parentsMaxLayer + 1).addNode(child);  // modify new Layer object
//                    child.setLayer(parentsMaxLayer + 1);             // modify child's layer index
//                }
//            }
//        }
//    }

    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

}
