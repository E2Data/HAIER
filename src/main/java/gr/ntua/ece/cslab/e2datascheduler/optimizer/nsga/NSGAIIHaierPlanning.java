package gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.Objective;
import gr.ntua.ece.cslab.e2datascheduler.beans.optpolicy.OptimizationPolicy;
import gr.ntua.ece.cslab.e2datascheduler.graph.HaierExecutionGraph;
import gr.ntua.ece.cslab.e2datascheduler.graph.ScheduledJobVertex;
import gr.ntua.ece.cslab.e2datascheduler.ml.Model;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.FeatureCache;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.cache.DummyCSLabFeatureCache;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.cache.TornadoFeatureCache;
import gr.ntua.ece.cslab.e2datascheduler.ml.impl.TornadoModel;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.exhaustivetimeevaluation.ExhaustiveEvaluation;
import gr.ntua.ece.cslab.e2datascheduler.optimizer.nsga.layeredtimeevaluation.LayeredEvaluation;

import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.jobgraph.JobVertex;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class that implements the core evaluation functionality for the NSGAII genetic algorithm
 * through the MOEA Framework, i.e. the methods {@code newSolution()} and {@code evaluate()}.
 */
public class NSGAIIHaierPlanning extends AbstractProblem {

    private static final Logger logger = Logger.getLogger(NSGAIIHaierPlanning.class.getCanonicalName());

    public static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
    private static final String timeEvalAlgorithm = resourceBundle.getString("optimizer.evalAlgorithm").toLowerCase();

    /**
     * The Flink JobGraph that represents the tasks, and must be scheduled on
     * the available devices in the cluster.
     */
    private final JobGraph jobGraph;

    /**
     * A list (with stable order) of the JobVertex objects included in the
     * Flink JobGraph of this problem.
     */
    private final JobVertex[] jobVertices;

    /**
     * A mapping between objective names and their corresponding integer
     * identifier.
     *
     * E.g.: { "execTime" : 0,  "powerCons" : 1,  ... } .
     *
     * This mapping is required because MOEA handles objectives as integers.
     */
    final Map<String, Integer> objectives;

    /**
     * A list of the devices in the cluster that are available for this
     * problem.
     */
    private final List<HwResource> devices;

    /**
     * The Model that is being consulted for the cost of a task on a particular
     * device.
     */
    private final Model mlModel;

    /**
     * It stores all found solutions.
     */
    final Map<Solution, HaierExecutionGraph> solutionGraphs;

    /**
     * timeEvaluator is a GOF Strategy handle for the implementation of
     * the execution time evaluation algorithms on a Flink JobGraph.
     */
    final TimeEvaluationAlgorithm timeEvaluator;

    /**
     *
     */
    private final FeatureCache featureCache;

    /**
     * Execution plans that have been generated and aborted due to co-location constraints.
     */
    int abortedPlans;

    /**
     * The total number of generated execution plans.
     */
    int totalGeneratedPlans;


    // -------------------------------------------------------------------------------------------


    /**
     * A class that implements the core evaluation functionality for the NSGAII genetic algorithm
     * through the MOEA Framework, i.e. the methods {@code newSolution()} and {@code evaluate()}.
     *
     * @param devices The available {@link HwResource}s in the cluster.
     * @param mlModel The Machine Learning {@link Model} to be consulted for the prediction of execution time for each
     *                task.
     * @param jobGraph The initial Flink {@link JobGraph}.
     * @param policy The {@link OptimizationPolicy} to base upon the solution to the multi-objective optimization
     *               problem at hand.
     * @throws IOException During {@link TornadoFeatureCache} initialization.
     * @throws ClassNotFoundException During {@link TornadoFeatureCache} initialization.
     */
    public NSGAIIHaierPlanning(
            final List<HwResource> devices,
            final Model mlModel,
            final JobGraph jobGraph,
            final OptimizationPolicy policy)
            throws IOException, ClassNotFoundException {
        super(jobGraph.getNumberOfVertices(), policy.getNumberOfObjectives());
        this.devices = new ArrayList<>();
        this.devices.addAll(devices);
        this.mlModel = mlModel;
        this.jobGraph = jobGraph;
        this.jobVertices = initializeJobVertices(jobGraph);
        this.solutionGraphs = new HashMap<>();

        this.objectives = new HashMap<>(policy.getNumberOfObjectives(), 1.0f);
        int objectiveIndex = 0;
        for (Objective obj : policy.getObjectives()) {
            this.objectives.put(obj.getName(), objectiveIndex);
            objectiveIndex++;
        }

        switch (timeEvalAlgorithm) {
            case "layered":
                this.timeEvaluator = new LayeredEvaluation(this.mlModel);
                break;
            case "exhaustive":
            default:
                this.timeEvaluator = new ExhaustiveEvaluation(this.mlModel);
                break;
        }

        if (this.mlModel instanceof TornadoModel) {
            try {
                this.featureCache = new TornadoFeatureCache(this.jobGraph, this.devices);
            } catch (final IOException | ClassNotFoundException e) {
                logger.log(Level.SEVERE, "Failed to initialize TornadoFeatureCache: " + e.getMessage(), e);
                throw e;
            }
        } else {
            this.featureCache = new DummyCSLabFeatureCache(this.jobGraph, this.devices);
        }

        this.abortedPlans = 0;
        this.totalGeneratedPlans = 0;
    }

    /**
     * Auxiliary method to initialize the array of {@link JobVertex} objects.
     *
     * First, it attempts to retrieve them topologically sorted;
     * if that does not work well, it falls back to random order.
     *
     * @param jobGraph The initial Flink's {@link JobGraph} object.
     * @return An array of {@link JobVertex} objects included in the given {@link JobGraph}
     */
    private JobVertex[] initializeJobVertices(final JobGraph jobGraph) {
        try {
            return jobGraph.getVerticesSortedTopologicallyFromSources().toArray(new JobVertex[jobGraph.getNumberOfVertices()]);
        } catch (Exception e) {
            logger.severe("OOPS!\n" + e.getMessage() + "\n");
            e.printStackTrace();
            return jobGraph.getVerticesAsArray();
        }
    }


    // -------------------------------------------------------------------------------------------


    /**
     * This method is automatically invoked by the MOEA framework at
     * each generation/iteration of the NSGAII genetic algorithm.
     *
     * @return A candidate {@link Solution} for the multi-objective optimization problem at hand.
     */
    @Override
    public Solution newSolution() {
        final Solution solution = new Solution(this.jobVertices.length, this.objectives.size());

        for (int jobVertexIndex = 0; jobVertexIndex < this.jobVertices.length; jobVertexIndex++) {
            solution.setVariable(jobVertexIndex, EncodingUtils.newInt(0, this.devices.size() - 1));
        }

        return solution;
    }

    /**
     * Method that evaluates how good or bad a candidate {@link Solution} is.
     * The result of the evaluation is stored in the {@link Solution} object itself.
     *
     * @param solution A candidate {@link Solution} for the MOEA framework.
     */
    @Override
    public void evaluate(Solution solution) {
        final HaierExecutionGraph haierExecutionGraph = constructHaierExecutionGraph(EncodingUtils.getInt(solution));
        final Map<String, Double> objectiveCosts = haierExecutionGraph.getObjectiveCosts();
        this.solutionGraphs.put(solution, haierExecutionGraph);

        ++this.totalGeneratedPlans;
        final boolean coLocationsRespected = haierExecutionGraph.checkCoLocationConstraints();
        if (!coLocationsRespected) {
            ++this.abortedPlans;
        }

        // TODO(ckatsak): For now, objectives are identified via String objects.
        //                This should probably change. Maybe Enums + Visitor ?
        for (String objective : this.objectives.keySet()) {
            double costEstimation = Double.NEGATIVE_INFINITY;
            switch (objective) {
                case "execTime":
                    costEstimation = coLocationsRespected
                            ? this.timeEvaluator.calculateExecutionTime(haierExecutionGraph)
                            : Double.POSITIVE_INFINITY;
                    break;
                case "powerCons":
                    costEstimation = coLocationsRespected
                            ? this.calculatePowerConsumption(haierExecutionGraph)
                            : Double.POSITIVE_INFINITY;
                    break;
                default:
                    // FIXME(ckatsak): This should be unreachable; yet, it depends on the input
                    //  incoming from the network. For now, just log it and skip its evaluation.
                    logger.warning("Unknown objective: '" + objective + "'\n");
                    break;
            }
            objectiveCosts.put(objective, costEstimation);
        }

        for (Map.Entry<String, Integer> objective : this.objectives.entrySet()) {
            solution.setObjective(objective.getValue(), objectiveCosts.get(objective.getKey()));
        }
    }

    /**
     * Construct the {@link HaierExecutionGraph} imposed by the the given solution's assignment plan.
     *
     * @param plan The plan ({@link JobVertex} to {@link HwResource}) to be evaluated, produced by the MOEA Framework
     *             using the NSGAII genetic algorithm.
     * @return The {@link HaierExecutionGraph} that represents the given assignment plan.
     */
    private HaierExecutionGraph constructHaierExecutionGraph(final int[] plan) {
        assert plan.length == this.jobVertices.length : "plan.length != jobVertices.length";

        // Construct the HaierExecutionGraph.
        final HaierExecutionGraph haierExecutionGraph = new HaierExecutionGraph(this.jobGraph, this.jobVertices);

        // Annotate each ScheduledJobVertex with its assigned hardware resource (and the
        // associated code features for the ML model) according to the current plan.
        for (int jobVertexIndex = 0; jobVertexIndex < plan.length; jobVertexIndex++) {
            haierExecutionGraph.assignResourceAndFeatures(
                    jobVertexIndex,
                    this.devices.get(plan[jobVertexIndex]),
                    // FIXME(ckatsak):  vv  Make sure the following is correct  vv
                    this.featureCache.getFeatureVectors(
                            this.jobVertices[jobVertexIndex],      // the JobVertex
                            this.devices.get(plan[jobVertexIndex]) // the HwResource
                    )
                    // FIXME(ckatsak):  ^^  Make sure the above is correct  ^^
            );
        }

        // Initialize the TimeEvaluationAlgorithm (e.g. construct the Layer objects for Layered time evaluation).
        timeEvaluator.initialization(haierExecutionGraph);

        return haierExecutionGraph;
    }

    /**
     * Calculate the total power consumption for the given {@link HaierExecutionGraph}.
     *
     * @param haierExecutionGraph The given {@link HaierExecutionGraph} to calculate its power consumption.
     * @return A value that represents the given {@link HaierExecutionGraph}'s total power consumption.
     */
    private double calculatePowerConsumption(final HaierExecutionGraph haierExecutionGraph) {
        double totalConsumption = 0.0d;

        for (ScheduledJobVertex scheduledJobVertex : haierExecutionGraph.getScheduledJobVertices()) {
            totalConsumption += this.mlModel.predict(
                    "powerCons",
                    scheduledJobVertex.getAssignedResource(),
                    scheduledJobVertex
            );
        }

        return totalConsumption;
    }

}
