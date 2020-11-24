package gr.ntua.ece.cslab.e2datascheduler.beans.profiling;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;


public class TornadoProfilingInfoTaskSchedule {

    @JsonProperty("TOTAL_BYTE_CODE_GENERATION")
    private String totalBytecodeGeneration;

    @JsonProperty("TOTAL_KERNEL_TIME")
    private String totalKernelTime;

    @JsonProperty("TOTAL_GRAAL_COMPILE_TIME")
    private String totalGraalCompileTime;

    @JsonProperty("TOTAL_TASK_SCHEDULE_TIME")
    private String totalTaskScheduleTime;

    @JsonProperty("COPY_IN_TIME")
    private String copyInTime;

    @JsonProperty("COPY_OUT_TIME")
    private String copyOutTime;

    @JsonProperty("DISPATCH_TIME")
    private String dispatchTime;

    @JsonProperty("TOTAL_DRIVER_COMPILE_TIME")
    private String totalDriverCompileTime;

    private final Map<String, TornadoProfilingInfoTask> task = new LinkedHashMap<>();


    public TornadoProfilingInfoTaskSchedule(){}


    @JsonProperty("TOTAL_BYTE_CODE_GENERATION")
    public String getTotalBytecodeGeneration() {
        return this.totalBytecodeGeneration;
    }

    @JsonProperty("TOTAL_BYTE_CODE_GENERATION")
    public void setTotalBytecodeGeneration(final String totalByteCodeGeneration) {
        this.totalBytecodeGeneration = totalByteCodeGeneration;
    }

    @JsonProperty("TOTAL_KERNEL_TIME")
    public String getTotalKernelTime() {
        return this.totalKernelTime;
    }

    @JsonProperty("TOTAL_KERNEL_TIME")
    public void setTotalKernelTime(final String totalKernelTime) {
        this.totalKernelTime = totalKernelTime;
    }

    @JsonProperty("TOTAL_GRAAL_COMPILE_TIME")
    public String getTotalGraalCompileTime() {
        return this.totalGraalCompileTime;
    }

    @JsonProperty("TOTAL_GRAAL_COMPILE_TIME")
    public void setTotalGraalCompileTime(final String totalGraalCompileTime) {
        this.totalGraalCompileTime = totalGraalCompileTime;
    }

    @JsonProperty("TOTAL_TASK_SCHEDULE_TIME")
    public String getTotalTaskScheduleTime() {
        return this.totalTaskScheduleTime;
    }

    @JsonProperty("TOTAL_TASK_SCHEDULE_TIME")
    public void setTotalTaskScheduleTime(final String totalTaskScheduleTime) {
        this.totalTaskScheduleTime = totalTaskScheduleTime;
    }

    @JsonProperty("COPY_IN_TIME")
    public String getCopyInTime() {
        return this.copyInTime;
    }

    @JsonProperty("COPY_IN_TIME")
    public void setCopyInTime(final String copyInTime) {
        this.copyInTime = copyInTime;
    }

    @JsonProperty("COPY_OUT_TIME")
    public String getCopyOutTime() {
        return this.copyOutTime;
    }

    @JsonProperty("COPY_OUT_TIME")
    public void setCopyOutTime(final String copyOutTime) {
        this.copyOutTime = copyOutTime;
    }

    @JsonProperty("DISPATCH_TIME")
    public String getDispatchTime() {
        return this.dispatchTime;
    }

    @JsonProperty("DISPATCH_TIME")
    public void setDispatchTime(final String dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    @JsonProperty("TOTAL_DRIVER_COMPILE_TIME")
    public String getTotalDriverCompileTime() {
        return this.totalDriverCompileTime;
    }

    @JsonProperty("TOTAL_DRIVER_COMPILE_TIME")
    public void setTotalDriverCompileTime(final String totalDriverCompileTime) {
        this.totalDriverCompileTime = totalDriverCompileTime;
    }

    public Map<String, TornadoProfilingInfoTask> getTask() {
        return this.task;
    }

    @JsonAnySetter
    public void setTask(final String taskName, final TornadoProfilingInfoTask taskProfilingInfo) {
        this.task.put(taskName, taskProfilingInfo);
    }

    @Override
    public String toString() {
        return "TornadoProfilingInfoTaskSchedule{" +
                "totalBytecodeGeneration='" + totalBytecodeGeneration + '\'' +
                ", totalKernelTime='" + totalKernelTime + '\'' +
                ", totalGraalCompileTime='" + totalGraalCompileTime + '\'' +
                ", totalTaskScheduleTime='" + totalTaskScheduleTime + '\'' +
                ", copyInTime='" + copyInTime + '\'' +
                ", copyOutTime='" + copyOutTime + '\'' +
                ", dispatchTime='" + dispatchTime + '\'' +
                ", totalDriverCompileTime='" + totalDriverCompileTime + '\'' +
                ", task=" + task +
                '}';
    }
}
