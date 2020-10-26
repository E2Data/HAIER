package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.TimeEvaluationAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ExhaustiveEvaluation is the "newer" time evaluation algorithm; it is more accurate but slower than the "old"
 * algorithm, {@link gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation.LayeredEvaluation}.
 */
public class ExhaustiveEvaluation extends TimeEvaluationAlgorithm {

    /**
     * The currently tracked {@link Graph}, deduced by the {@link ComputationalGraph} that has been extracted by the
     * {@link HaierExecutionGraph}.
     * It is first initialized every time method {@code initialization()} is called, and later used to evaluate the
     * execution time every time {@code calculateExecutionTime} is called.
     */
    private Graph graph;

    // --------------------------------------------------------------------------------------------

    /**
     * ExhaustiveEvaluation is the "newer" time evaluation algorithm; it is more accurate but slower than the "old"
     * algorithm, {@link gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation.LayeredEvaluation}.
     *
     * @param mlModel The Machine Learning {@link Model} that should be consulted for predicting the time of each task.
     */
    public ExhaustiveEvaluation(final Model mlModel) {
        super(mlModel);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Initialization of the required structures for the time evaluation algorithm to run correctly.
     * Calling this method before calling the {@code calculateExecutionTime()} method is a requirement
     * for this subclass of {@link TimeEvaluationAlgorithm}.
     *
     * @param haierExecutionGraph The related {@link HaierExecutionGraph} at hand.
     */
    @Override
    public void initialization(final HaierExecutionGraph haierExecutionGraph) {
        final ComputationalGraph computationalGraph = this.deduceComputationalGraph(haierExecutionGraph);

        final List<Task> tasks = new ArrayList<>(computationalGraph.getComputationalJobVertices().size());
        for (ComputationalJobVertex computationalJobVertex : computationalGraph.getComputationalJobVertices()) {
            tasks.add(new Task(
                    computationalJobVertex.getIndex(),
                    computationalJobVertex.getAssignedResource(),
                    this.mlModel.predict(
                            "execTime",
                            computationalJobVertex.getAssignedResource(),
                            computationalJobVertex.getScheduledJobVertex()
                    ),
                    computationalJobVertex.getChildren(),
                    computationalJobVertex.getParents(),
                    computationalJobVertex.isRoot()
            ));
        }

        this.graph = new Graph(tasks);
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Calculate the estimated execution time of the given {@link HaierExecutionGraph}.
     * Method {@code initialization()} must have been called before calling this method.
     *
     * @param haierExecutionGraph The related {@link HaierExecutionGraph} at hand.
     * @return A double-precision floating-point number that represents the evaluation
     */
    @Override
    public double calculateExecutionTime(final HaierExecutionGraph haierExecutionGraph) {
        return median(this.graph.haierEvaluation());
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Returns the median among the values in the given list.
     *
     * @param list The given values.
     * @return The median.
     */
    private static double median(final List<Double> list) {
        Collections.sort(list);
        return list.size() % 2 == 0 ?
                (list.get(list.size() / 2 - 1) + list.get(list.size() / 2) ) / 2.0 :
                list.get(list.size() / 2);
    }

    /**
     * Returns the average value of those in the given list.
     *
     * @param list The given values.
     * @return The average.
     */
    private static double average(final List<Double> list) {
        return list.stream().reduce(0.0d, (sum, d) -> sum + d) / list.size();
    }

}
