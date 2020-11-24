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
                        "    \"blur\": {\n" +
                        "        \"TOTAL_TASK_SCHEDULE_TIME\": \"571761753\",\n" +
                        "        \"COPY_IN_TIME\": \"103040\",\n" +
                        "        \"TOTAL_GRAAL_COMPILE_TIME\": \"388707122\",\n" +
                        "        \"COPY_OUT_TIME\": \"62848\",\n" +
                        "        \"DISPATCH_TIME\": \"602368\",\n" +
                        "        \"TOTAL_DRIVER_COMPILE_TIME\": \"40661231\",\n" +
                        "        \"TOTAL_BYTE_CODE_GENERATION\": \"12440172\",\n" +
                        "        \"TOTAL_KERNEL_TIME\": \"345088\",\n" +
                        "        \"blur.red\": {\n" +
                        "            \"METHOD\": \"ComputeKernels.channelConvolution\",\n" +
                        "            \"DEVICE_ID\": \"0:0\",\n" +
                        "            \"DEVICE\": \"Tesla V100-SXM2-32GB\",\n" +
                        "            \"TASK_COPY_OUT_SIZE_BYTES\": \"786504\",\n" +
                        "            \"TASK_COPY_IN_SIZE_BYTES\": \"2097344\",\n" +
                        "            \"TASK_COMPILE_DRIVER_TIME\": \"20522312\",\n" +
                        "            \"TASK_COMPILE_GRAAL_TIME\": \"223208325\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"117760\"\n" +
                        "        }, \n" +
                        "        \"blur.green\": {\n" +
                        "            \"METHOD\": \"ComputeKernels.channelConvolution\",\n" +
                        "            \"DEVICE_ID\": \"0:0\",\n" +
                        "            \"DEVICE\": \"Tesla V100-SXM2-32GB\",\n" +
                        "            \"TASK_COMPILE_DRIVER_TIME\": \"7864277\",\n" +
                        "            \"TASK_COMPILE_GRAAL_TIME\": \"88263099\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"110592\"\n" +
                        "        }, \n" +
                        "        \"blur.blue\": {\n" +
                        "            \"METHOD\": \"ComputeKernels.channelConvolution\",\n" +
                        "            \"DEVICE_ID\": \"0:0\",\n" +
                        "            \"DEVICE\": \"Tesla V100-SXM2-32GB\",\n" +
                        "            \"TASK_COMPILE_DRIVER_TIME\": \"12274642\",\n" +
                        "            \"TASK_COMPILE_GRAAL_TIME\": \"77235698\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"116736\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                "{\n" +
                        "    \"benchmark\": {\n" +
                        "        \"COPY_IN_TIME\": \"2048\",\n" +
                        "        \"COPY_OUT_TIME\": \"2016\",\n" +
                        "        \"TOTAL_TASK_SCHEDULE_TIME\": \"5125380\",\n" +
                        "        \"DISPATCH_TIME\": \"37568\",\n" +
                        "        \"TOTAL_KERNEL_TIME\": \"34816\",\n" +
                        "        \"benchmark.t0\": {\n" +
                        "            \"METHOD\": \"ComputeKernels.blackscholes\",\n" +
                        "            \"DEVICE_ID\": \"0:2\",\n" +
                        "            \"DEVICE\": \"GeForce GTX 1060 6GB\",\n" +
                        "            \"TASK_COPY_IN_SIZE_BYTES\": \"4144\",\n" +
                        "            \"TASK_COPY_OUT_SIZE_BYTES\": \"4144\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"34816\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                "{\n" +
                        "    \"blur\": {\n" +
                        "        \"TOTAL_TASK_SCHEDULE_TIME\": \"1031014625\",\n" +
                        "        \"COPY_IN_TIME\": \"13044149\",\n" +
                        "        \"COPY_OUT_TIME\": \"10494533\",\n" +
                        "        \"DISPATCH_TIME\": \"88251\",\n" +
                        "        \"TOTAL_KERNEL_TIME\": \"1006810445\",\n" +
                        "        \"blur.red\": {\n" +
                        "            \"METHOD\": \"ComputeKernels.channelConvolution\",\n" +
                        "            \"DEVICE_ID\": \"0:3\",\n" +
                        "            \"DEVICE\": \"Intel(R) Xeon(R) Silver 4114 CPU @ 2.20GHz\",\n" +
                        "            \"TASK_COPY_OUT_SIZE_BYTES\": \"50331720\",\n" +
                        "            \"TASK_COPY_IN_SIZE_BYTES\": \"100663440\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"336838882\"\n" +
                        "        }, \n" +
                        "        \"blur.green\": {\n" +
                        "            \"METHOD\": \"ComputeKernels.channelConvolution\",\n" +
                        "            \"DEVICE_ID\": \"0:3\",\n" +
                        "            \"DEVICE\": \"Intel(R) Xeon(R) Silver 4114 CPU @ 2.20GHz\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"335296092\"\n" +
                        "        }, \n" +
                        "        \"blur.blue\": {\n" +
                        "            \"METHOD\": \"ComputeKernels.channelConvolution\",\n" +
                        "            \"DEVICE_ID\": \"0:3\",\n" +
                        "            \"DEVICE\": \"Intel(R) Xeon(R) Silver 4114 CPU @ 2.20GHz\",\n" +
                        "            \"TASK_KERNEL_TIME\": \"334675471\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}"
        };

        for (String ser : sz) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final TornadoProfilingInfoRoot des = objectMapper.readValue(ser, TornadoProfilingInfoRoot.class);

            System.out.println("Deserialized:\n" + des);
            for (Map.Entry entryInRoot : des.getTaskSchedule().entrySet()) {
                System.out.println("Key: " + entryInRoot.getKey() + " --> Value: " + entryInRoot.getValue());
                final TornadoProfilingInfoTaskSchedule tornadoProfilingInfoTaskSchedule = (TornadoProfilingInfoTaskSchedule) entryInRoot.getValue();
                System.out.println("\ttotalBytecodeGeneration = " + tornadoProfilingInfoTaskSchedule.getTotalBytecodeGeneration());
                System.out.println("\ttotalKernelTime = " + tornadoProfilingInfoTaskSchedule.getTotalKernelTime());
                System.out.println("\ttotalGraalCompileTime = " + tornadoProfilingInfoTaskSchedule.getTotalGraalCompileTime());
                System.out.println("\ttotalTaskScheduleTime = " + tornadoProfilingInfoTaskSchedule.getTotalTaskScheduleTime());
                System.out.println("\tcopyInTime = " + tornadoProfilingInfoTaskSchedule.getCopyInTime());
                System.out.println("\tcopyOutTime = " + tornadoProfilingInfoTaskSchedule.getCopyOutTime());
                System.out.println("\tdispatchTime = " + tornadoProfilingInfoTaskSchedule.getDispatchTime());
                System.out.println("\ttotalDriverCompileTime = " + tornadoProfilingInfoTaskSchedule.getTotalDriverCompileTime());
                for (Map.Entry entryInTaskScheduleInfo : tornadoProfilingInfoTaskSchedule.getTask().entrySet()) {
                    System.out.println("\tKey: " + entryInTaskScheduleInfo.getKey() + " --> Value: " + entryInTaskScheduleInfo.getValue());
                    final TornadoProfilingInfoTask tornadoProfilingInfoTask = (TornadoProfilingInfoTask) entryInTaskScheduleInfo.getValue();
                    System.out.println("\t\tmethod = " + tornadoProfilingInfoTask.getMethod());
                    System.out.println("\t\tdeviceID = " + tornadoProfilingInfoTask.getDeviceID());
                    System.out.println("\t\tdevice = " + tornadoProfilingInfoTask.getDevice());
                    System.out.println("\t\ttaskCopyOutSizeBytes = " + tornadoProfilingInfoTask.getTaskCopyOutSizeBytes());
                    System.out.println("\t\ttaskCopyInSizeBytes = " + tornadoProfilingInfoTask.getTaskCopyInSizeBytes());
                    System.out.println("\t\ttaskKernelTime = " + tornadoProfilingInfoTask.getTaskKernelTime());
                    System.out.println("\t\ttaskCompileGraalTime = " + tornadoProfilingInfoTask.getTaskCompileGraalTime());
                    System.out.println("\t\ttaskCompileDriverTime = " + tornadoProfilingInfoTask.getTaskCompileDriverTime());
                }
            }
            System.out.println("\n");
        }
    }

}
