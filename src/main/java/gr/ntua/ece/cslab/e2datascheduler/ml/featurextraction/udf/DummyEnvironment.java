package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.udf;

import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.JobID;
import org.apache.flink.api.common.TaskInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.accumulators.AccumulatorRegistry;
import org.apache.flink.runtime.broadcast.BroadcastVariableManager;
import org.apache.flink.runtime.checkpoint.CheckpointMetrics;
import org.apache.flink.runtime.checkpoint.TaskStateSnapshot;
import org.apache.flink.runtime.execution.Environment;
import org.apache.flink.runtime.executiongraph.ExecutionAttemptID;
import org.apache.flink.runtime.externalresource.ExternalResourceInfoProvider;
import org.apache.flink.runtime.io.disk.iomanager.IOManager;
import org.apache.flink.runtime.io.network.TaskEventDispatcher;
import org.apache.flink.runtime.io.network.api.writer.ResultPartitionWriter;
import org.apache.flink.runtime.io.network.partition.consumer.IndexedInputGate;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.jobgraph.tasks.InputSplitProvider;
import org.apache.flink.runtime.jobgraph.tasks.TaskOperatorEventGateway;
import org.apache.flink.runtime.memory.MemoryManager;
import org.apache.flink.runtime.metrics.groups.TaskMetricGroup;
import org.apache.flink.runtime.query.TaskKvStateRegistry;
import org.apache.flink.runtime.state.TaskStateManager;
import org.apache.flink.runtime.taskexecutor.GlobalAggregateManager;
import org.apache.flink.runtime.taskmanager.TaskManagerRuntimeInfo;

import java.util.Map;
import java.util.concurrent.Future;


/**
 * TODO(ckatsak): Documentation
 */
public class DummyEnvironment implements Environment {

    Configuration taskConfig;
    ClassLoader userClassLoader;

    public DummyEnvironment(Configuration taskConfig, ClassLoader userClassLoader) {
        this.taskConfig = taskConfig;
        this.userClassLoader = userClassLoader;
    }

    /**
     * Returns the job specific {@link ExecutionConfig}.
     *
     * @return The execution configuration associated with the current job.
     */
    @Override
    public ExecutionConfig getExecutionConfig() {
        return null;
    }

    /**
     * Returns the ID of the job that the task belongs to.
     *
     * @return the ID of the job from the original job graph
     */
    @Override
    public JobID getJobID() {
        return null;
    }

    /**
     * Gets the ID of the JobVertex for which this task executes a parallel subtask.
     *
     * @return The JobVertexID of this task.
     */
    @Override
    public JobVertexID getJobVertexId() {
        return null;
    }

    /**
     * Gets the ID of the task execution attempt.
     *
     * @return The ID of the task execution attempt.
     */
    @Override
    public ExecutionAttemptID getExecutionId() {
        return null;
    }

    /**
     * Returns the task-wide configuration object, originally attached to the job vertex.
     *
     * @return The task-wide configuration
     */
    @Override
    public Configuration getTaskConfiguration() {
        return this.taskConfig;
    }

    /**
     * Gets the task manager info, with configuration and hostname.
     *
     * @return The task manager info, with configuration and hostname.
     */
    @Override
    public TaskManagerRuntimeInfo getTaskManagerInfo() {
        return null;
    }

    /**
     * Returns the task specific metric group.
     *
     * @return The MetricGroup of this task.
     */
    @Override
    public TaskMetricGroup getMetricGroup() {
        return null;
    }

    /**
     * Returns the job-wide configuration object that was attached to the JobGraph.
     *
     * @return The job-wide configuration
     */
    @Override
    public Configuration getJobConfiguration() {
        return null;
    }

    /**
     * Returns the {@link TaskInfo} object associated with this subtask
     *
     * @return TaskInfo for this subtask
     */
    @Override
    public TaskInfo getTaskInfo() {
        return new TaskInfo("DummyTask", 1, 0, 1, 0);
    }

    /**
     * Returns the input split provider assigned to this environment.
     *
     * @return The input split provider or {@code null} if no such
     * provider has been assigned to this environment.
     */
    @Override
    public InputSplitProvider getInputSplitProvider() {
        return null;
    }

    /**
     * Gets the gateway through which operators can send events to the operator coordinators.
     */
    @Override
    public TaskOperatorEventGateway getOperatorCoordinatorEventGateway() {
        return null;
    }

    /**
     * Returns the current {@link IOManager}.
     *
     * @return the current {@link IOManager}.
     */
    @Override
    public IOManager getIOManager() {
        return null;
    }

    /**
     * Returns the current {@link MemoryManager}.
     *
     * @return the current {@link MemoryManager}.
     */
    @Override
    public MemoryManager getMemoryManager() {
        return null;
    }

    /**
     * Returns the user code class loader
     */
    @Override
    public ClassLoader getUserClassLoader() {
        return userClassLoader;
    }

    @Override
    public Map<String, Future<Path>> getDistributedCacheEntries() {
        return null;
    }

    @Override
    public BroadcastVariableManager getBroadcastVariableManager() {
        return null;
    }

    @Override
    public TaskStateManager getTaskStateManager() {
        return null;
    }

    @Override
    public GlobalAggregateManager getGlobalAggregateManager() {
        return null;
    }

    /**
     * Get the {@link ExternalResourceInfoProvider} which contains infos of available external resources.
     *
     * @return {@link ExternalResourceInfoProvider} which contains infos of available external resources
     */
    @Override
    public ExternalResourceInfoProvider getExternalResourceInfoProvider() {
        return null;
    }

    /**
     * Return the registry for accumulators which are periodically sent to the job manager.
     *
     * @return the registry
     */
    @Override
    public AccumulatorRegistry getAccumulatorRegistry() {
        return null;
    }

    /**
     * Returns the registry for
     *
     * @return KvState registry
     */
    @Override
    public TaskKvStateRegistry getTaskKvStateRegistry() {
        return null;
    }

    /**
     * Confirms that the invokable has successfully completed all steps it needed to
     * to for the checkpoint with the give checkpoint-ID. This method does not include
     * any state in the checkpoint.
     *
     * @param checkpointId      ID of this checkpoint
     * @param checkpointMetrics metrics for this checkpoint
     */
    @Override
    public void acknowledgeCheckpoint(long checkpointId, CheckpointMetrics checkpointMetrics) {}

    /**
     * Confirms that the invokable has successfully completed all required steps for
     * the checkpoint with the give checkpoint-ID. This method does include
     * the given state in the checkpoint.
     *
     * @param checkpointId      ID of this checkpoint
     * @param checkpointMetrics metrics for this checkpoint
     * @param subtaskState      All state handles for the checkpointed state
     */
    @Override
    public void acknowledgeCheckpoint(
            long checkpointId,
            CheckpointMetrics checkpointMetrics,
            TaskStateSnapshot subtaskState
    ) {}

    /**
     * Declines a checkpoint. This tells the checkpoint coordinator that this task will
     * not be able to successfully complete a certain checkpoint.
     *
     * @param checkpointId The ID of the declined checkpoint.
     * @param cause        An optional reason why the checkpoint was declined.
     */
    @Override
    public void declineCheckpoint(long checkpointId, Throwable cause) {

    }

    /**
     * Marks task execution failed for an external reason (a reason other than the task code itself
     * throwing an exception). If the task is already in a terminal state
     * (such as FINISHED, CANCELED, FAILED), or if the task is already canceling this does nothing.
     * Otherwise it sets the state to FAILED, and, if the invokable code is running,
     * starts an asynchronous thread that aborts that code.
     *
     * <p>This method never blocks.
     *
     * @param cause
     */
    @Override
    public void failExternally(Throwable cause) {

    }

    @Override
    public ResultPartitionWriter getWriter(int index) {
        return null;
    }

    @Override
    public ResultPartitionWriter[] getAllWriters() {
        return new ResultPartitionWriter[0];
    }

    @Override
    public IndexedInputGate getInputGate(int index) {
        return null;
    }

    @Override
    public IndexedInputGate[] getAllInputGates() {
        return new IndexedInputGate[0];
    }

    @Override
    public TaskEventDispatcher getTaskEventDispatcher() {
        return null;
    }

}
