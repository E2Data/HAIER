package gr.ntua.ece.cslab.e2datascheduler.beans.profiling;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;


public class TornadoProfilingInfoRoot {

    private final Map<String, TornadoProfilingInfoTaskSchedule> taskSchedule = new LinkedHashMap<>();

    public TornadoProfilingInfoRoot(){}

    public Map<String, TornadoProfilingInfoTaskSchedule> getTaskSchedule() {
        return this.taskSchedule;
    }

    @JsonAnySetter
    public void setTaskSchedule(
            final String taskScheduleName,
            final TornadoProfilingInfoTaskSchedule taskScheduleProfilingInfo) {
        this.taskSchedule.put(taskScheduleName, taskScheduleProfilingInfo);
    }

    @Override
    public String toString() {
        return "TornadoProfilingInfoRoot{" +
                "taskSchedule=" + taskSchedule +
                '}';
    }

    // --------------------------------------------------------------------------------------------


    // NOTE(ckatsak): Quick & dirty deserialization test
    public static void main(String[] args) throws IOException {
        final String[] sz = new String[]{
                "{\n" +
                        "    \"s0\": {\n" +
                        "        \"TOTAL_BYTE_CODE_GENERATION\": \"6840603\",\n" +
                        "        \"TOTAL_KERNEL_TIME\": \"0\",\n" +
                        "        \"TOTAL_GRAAL_COMPILE_TIME\": \"31683624\",\n" +
                        "        \"TOTAL_TASK_SCHEDULE_TIME\": \"115224482\",\n" +
                        "        \"COPY_OUT_TIME\": \"0\",\n" +
                        "        \"TOTAL_DRIVER_COMPILE_TIME\": \"64397610\",\n" +
                        "        \"s0.t0\": {\n" +
                        "            \"DEVICE_ID\": \"0:0\",\n" +
                        "            \"DEVICE\": \"GeForce GTX 1050\",\n" +
                        "            \"TASK_COPY_OUT_SIZE_BYTES\": \"152\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"0\",\n" +
                        "            \"TASK_COMPILE_GRAAL_TIME\": \"31683624\",\n" +
                        "            \"TASK_COMPILE_DRIVER_TIME\": \"64397610\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                "{\n" +
                        "    \"s3\": {\n" +
                        "        \"TOTAL_BYTE_CODE_GENERATION\": \"6840603\",\n" +
                        "        \"TOTAL_KERNEL_TIME\": \"0\",\n" +
                        "        \"TOTAL_GRAAL_COMPILE_TIME\": \"31683624\",\n" +
                        "        \"TOTAL_TASK_SCHEDULE_TIME\": \"115224482\",\n" +
                        "        \"COPY_OUT_TIME\": \"0\",\n" +
                        "        \"TOTAL_DRIVER_COMPILE_TIME\": \"64397610\",\n" +
                        "        \"s2.t1\": {\n" +
                        "            \"DEVICE_ID\": \"1:2\",\n" +
                        "            \"DEVICE\": \"GeForce GTX 1050\",\n" +
                        "            \"TASK_COPY_OUT_SIZE_BYTES\": \"152\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"0\",\n" +
                        "            \"TASK_COMPILE_GRAAL_TIME\": \"31683624\",\n" +
                        "            \"TASK_COMPILE_DRIVER_TIME\": \"64397610\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        };

        for (String ser : sz) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final TornadoProfilingInfoRoot des = objectMapper.readValue(ser, TornadoProfilingInfoRoot.class);

            System.out.println("deserialized: " + des);
            for (Map.Entry entryInRoot : des.getTaskSchedule().entrySet()) {
                System.out.println("Key: " + entryInRoot.getKey() + " --> Value: " + entryInRoot.getValue());
                final TornadoProfilingInfoTaskSchedule tornadoProfilingInfoTaskSchedule = (TornadoProfilingInfoTaskSchedule) entryInRoot.getValue();
                System.out.println("\ttotalBytecodeGeneration = " + tornadoProfilingInfoTaskSchedule.getTotalBytecodeGeneration());
                System.out.println("\ttotalKernelTime = " + tornadoProfilingInfoTaskSchedule.getTotalKernelTime());
                System.out.println("\ttotalGraalCompileTime = " + tornadoProfilingInfoTaskSchedule.getTotalGraalCompileTime());
                System.out.println("\ttotalTaskScheduleTime = " + tornadoProfilingInfoTaskSchedule.getTotalTaskScheduleTime());
                System.out.println("\tcopyOutTime = " + tornadoProfilingInfoTaskSchedule.getCopyOutTime());
                System.out.println("\ttotalDriverCompileTime = " + tornadoProfilingInfoTaskSchedule.getTotalDriverCompileTime());
                for (Map.Entry entryInTaskScheduleInfo : tornadoProfilingInfoTaskSchedule.getTask().entrySet()) {
                    System.out.println("\tKey: " + entryInTaskScheduleInfo.getKey() + " --> Value: " + entryInTaskScheduleInfo.getValue());
                    final TornadoProfilingInfoTask tornadoProfilingInfoTask = (TornadoProfilingInfoTask) entryInTaskScheduleInfo.getValue();
                    System.out.println("\t\tdeviceID = " + tornadoProfilingInfoTask.getDeviceID());
                    System.out.println("\t\tdevice = " + tornadoProfilingInfoTask.getDevice());
                    System.out.println("\t\ttaskCopyOutSizeBytes = " + tornadoProfilingInfoTask.getTaskCopyOutSizeBytes());
                    System.out.println("\t\ttaskKernelTime = " + tornadoProfilingInfoTask.getTaskKernelTime());
                    System.out.println("\t\ttaskCompileGraalTime = " + tornadoProfilingInfoTask.getTaskCompileGraalTime());
                    System.out.println("\t\ttaskCompileDriverTime = " + tornadoProfilingInfoTask.getTaskCompileDriverTime());
                }
            }
            System.out.println("\n");
        }
    }

}
