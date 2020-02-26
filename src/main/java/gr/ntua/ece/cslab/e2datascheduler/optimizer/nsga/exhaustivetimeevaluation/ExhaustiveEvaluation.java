package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation;

import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ComputationalJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.graph.FlinkExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.TimeEvaluationAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExhaustiveEvaluation extends TimeEvaluationAlgorithm {

    private Graph graph;

    // --------------------------------------------------------------------------------------------

    public ExhaustiveEvaluation(final Model mlModel) {
        super(mlModel);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public void initialization(final FlinkExecutionGraph flinkExecutionGraph) {
        final ComputationalGraph computationalGraph = this.deduceComputationalGraph(flinkExecutionGraph);

        final List<Task> tasks = new ArrayList<>(computationalGraph.getComputationalJobVertices().size());
        for (ComputationalJobVertex computationalJobVertex : computationalGraph.getComputationalJobVertices()) {
            tasks.add(new Task(
                    computationalJobVertex.getIndex(),
                    computationalJobVertex.getAssignedResource(),
                    this.mlModel.predict("execTime",
                            computationalJobVertex.getAssignedResource(),
                            computationalJobVertex.getSourceCode()),
                    computationalJobVertex.getChildren(),
                    computationalJobVertex.getParents(),
                    computationalJobVertex.isRoot()
            ));
        }

        this.graph = new Graph(tasks);
    }

    // --------------------------------------------------------------------------------------------

    @Override
    public double calculateExecutionTime(final FlinkExecutionGraph flinkExecutionGraph) {
        return median(this.graph.haierEvaluation());
    }

    // --------------------------------------------------------------------------------------------

    private static double median(final List<Double> list) {
        Collections.sort(list);
        return list.size() % 2 == 0 ?
                (list.get(list.size() / 2 - 1) + list.get(list.size() / 2) ) / 2.0 :
                list.get(list.size() / 2);
    }

    private static double average(final List<Double> list) {
        return list.stream().reduce(0.0d, (sum, d) -> sum + d) / list.size();
    }

}
