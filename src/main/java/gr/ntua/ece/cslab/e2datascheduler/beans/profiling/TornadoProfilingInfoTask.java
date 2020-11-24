package gr.ntua.ece.cslab.e2datascheduler.beans.profiling;

import com.fasterxml.jackson.annotation.JsonProperty;


public class TornadoProfilingInfoTask {

    @JsonProperty("METHOD")
    private String method;

    @JsonProperty("DEVICE_ID")
    private String deviceID;

    @JsonProperty("DEVICE")
    private String device;

    @JsonProperty("TASK_COPY_OUT_SIZE_BYTES")
    private String taskCopyOutSizeBytes;

    @JsonProperty("TASK_COPY_IN_SIZE_BYTES")
    private String taskCopyInSizeBytes;

    @JsonProperty("TASK_KERNEL_TIME")
    private String taskKernelTime;

    @JsonProperty("TASK_COMPILE_GRAAL_TIME")
    private String taskCompileGraalTime;

    @JsonProperty("TASK_COMPILE_DRIVER_TIME")
    private String taskCompileDriverTime;


    public TornadoProfilingInfoTask(){}


    @JsonProperty("METHOD")
    public String getMethod() {
        return this.method;
    }

    @JsonProperty("METHOD")
    public void setMethod(final String method) {
        this.method = method;
    }

    @JsonProperty("DEVICE_ID")
    public String getDeviceID() {
        return this.deviceID;
    }

    @JsonProperty("DEVICE_ID")
    public void setDeviceID(final String DEVICE_ID) {
        this.deviceID = DEVICE_ID;
    }

    @JsonProperty("DEVICE")
    public String getDevice() {
        return this.device;
    }

    @JsonProperty("DEVICE")
    public void setDevice(final String device) {
        this.device = device;
    }

    @JsonProperty("TASK_COPY_OUT_SIZE_BYTES")
    public String getTaskCopyOutSizeBytes() {
        return this.taskCopyOutSizeBytes;
    }

    @JsonProperty("TASK_COPY_OUT_SIZE_BYTES")
    public void setTaskCopyOutSizeBytes(final String taskCopyOutSizeBytes) {
        this.taskCopyOutSizeBytes = taskCopyOutSizeBytes;
    }

    @JsonProperty("TASK_COPY_IN_SIZE_BYTES")
    public String getTaskCopyInSizeBytes() {
        return this.taskCopyInSizeBytes;
    }

    @JsonProperty("TASK_COPY_IN_SIZE_BYTES")
    public void setTaskCopyInSizeBytes(final String taskCopyInSizeBytes) {
        this.taskCopyInSizeBytes = taskCopyInSizeBytes;
    }

    @JsonProperty("TASK_KERNEL_TIME")
    public String getTaskKernelTime() {
        return this.taskKernelTime;
    }

    @JsonProperty("TASK_KERNEL_TIME")
    public void setTaskKernelTime(final String taskKernelTime) {
        this.taskKernelTime = taskKernelTime;
    }

    @JsonProperty("TASK_COMPILE_GRAAL_TIME")
    public String getTaskCompileGraalTime() {
        return this.taskCompileGraalTime;
    }

    @JsonProperty("TASK_COMPILE_GRAAL_TIME")
    public void setTaskCompileGraalTime(final String taskCompileGraalTime) {
        this.taskCompileGraalTime = taskCompileGraalTime;
    }

    @JsonProperty("TASK_COMPILE_DRIVER_TIME")
    public String getTaskCompileDriverTime() {
        return this.taskCompileDriverTime;
    }

    @JsonProperty("TASK_COMPILE_DRIVER_TIME")
    public void setTaskCompileDriverTime(final String taskCompileDriverTime) {
        this.taskCompileDriverTime = taskCompileDriverTime;
    }

    @Override
    public String toString() {
        return "TornadoProfilingInfoTask{" +
                "method='" + method + '\'' +
                ", deviceID='" + deviceID + '\'' +
                ", device='" + device + '\'' +
                ", taskCopyOutSizeBytes='" + taskCopyOutSizeBytes + '\'' +
                ", taskCopyInSizeBytes='" + taskCopyInSizeBytes + '\'' +
                ", taskKernelTime='" + taskKernelTime + '\'' +
                ", taskCompileGraalTime='" + taskCompileGraalTime + '\'' +
                ", taskCompileDriverTime='" + taskCompileDriverTime + '\'' +
                '}';
    }
}
