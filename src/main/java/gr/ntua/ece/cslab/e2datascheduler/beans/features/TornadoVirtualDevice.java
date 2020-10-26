package gr.ntua.ece.cslab.e2datascheduler.beans.features;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;


/**
 * TODO(ckatsak): Documentation
 */
public class TornadoVirtualDevice {

    private String deviceName;
    private boolean doubleFPSupport;
    private int[] maxWorkItemSizes;
    private int deviceAddressBits;
    private String deviceType;
    private String deviceExtensions;
    private int availableProcessors;

    public TornadoVirtualDevice(){}

    @JsonProperty("deviceName")
    public String getDeviceName() {
        return this.deviceName;
    }

    @JsonProperty("deviceName")
    public void setDeviceName(final String deviceName) {
        this.deviceName = deviceName;
    }

    @JsonProperty("doubleFPSupport")
    public boolean getDoubleFPSupport() {
        return this.doubleFPSupport;
    }

    @JsonProperty("doubleFPSupport")
    public void setDoubleFPSupport(final boolean doubleFPSupport) {
        this.doubleFPSupport = doubleFPSupport;
    }

    @JsonProperty("maxWorkItemSizes")
    public int[] getMaxWorkItemSizes() {
        return this.maxWorkItemSizes;
    }

    @JsonProperty("maxWorkItemSizes")
    public void setMaxWorkItemSizes(final int[] maxWorkItemSizes) {
        this.maxWorkItemSizes = maxWorkItemSizes;
    }

    @JsonProperty("deviceAddressBits")
    public int getDeviceAddressBits() {
        return this.deviceAddressBits;
    }

    @JsonProperty("deviceAddressBits")
    public void setDeviceAddressBits(final int deviceAddressBits) {
        this.deviceAddressBits = deviceAddressBits;
    }

    @JsonProperty("deviceType")
    public String getDeviceType() {
        return this.deviceType;
    }

    @JsonProperty("deviceType")
    public void setDeviceType(final String deviceType) {
        this.deviceType = deviceType;
    }

    @JsonProperty("deviceExtensions")
    public String getDeviceExtensions() {
        return this.deviceExtensions;
    }

    @JsonProperty("deviceExtensions")
    public void setDeviceExtensions(final String deviceExtensions) {
        this.deviceExtensions = deviceExtensions;
    }

    @JsonProperty("availableProcessors")
    public int getAvailableProcessors() {
        return this.availableProcessors;
    }

    @JsonProperty("availableProcessors")
    public void setAvailableProcessors(final int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }


    // --------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        return "TornadoVirtualDevice{" +
                "deviceName='" + deviceName + '\'' +
                ", doubleFPSupport=" + doubleFPSupport +
                ", maxWorkItemSizes=" + Arrays.toString(maxWorkItemSizes) +
                ", deviceAddressBits=" + deviceAddressBits +
                ", deviceType='" + deviceType + '\'' +
                ", deviceExtensions='" + deviceExtensions + '\'' +
                ", availableProcessors=" + availableProcessors +
                '}';
    }


    // --------------------------------------------------------------------------------------------


    // NOTE(ckatsak): Quick & dirty deserialization test
    public static void main(String[] args) throws IOException {
        final String[] sz = new String[]{
                "{\n" +
                        "  \"deviceName\" : \"testDevice\",\n" +
                        "  \"doubleFPSupport\" : true,\n" +
                        "  \"maxWorkItemSizes\" : [8192, 8192, 8192],\n" +
                        "  \"deviceAddressBits\" : 64,\n" +
                        "  \"deviceType\" : \"CL_DEVICE_TYPE_CPU\",\n" +
                        "  \"deviceExtensions\" : \"cl_khr_int64_base_atomics\",\n" +
                        "  \"availableProcessors\" : 12\n" +
                        "}"
        };

        for (String ser : sz) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final TornadoVirtualDevice tornadoVirtualDevice = objectMapper.readValue(ser, TornadoVirtualDevice.class);
            System.out.println("\n" + tornadoVirtualDevice + "\n");
        }
    }

}
