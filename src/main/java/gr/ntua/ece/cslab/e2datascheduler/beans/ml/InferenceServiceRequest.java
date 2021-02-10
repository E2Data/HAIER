package gr.ntua.ece.cslab.e2datascheduler.beans.ml;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;
import gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction.TornadoFeatureVector;

import com.fasterxml.jackson.annotation.JsonProperty;


public class InferenceServiceRequest {

    private String objective;
    private HwResource device;
    private TornadoFeatureVector tornadoFeatures;

    public InferenceServiceRequest() {}


    @JsonProperty("Objective")
    public String getObjective() {
        return this.objective;
    }

    @JsonProperty("Objective")
    public void setObjective(final String objective) {
        this.objective = objective;
    }

    @JsonProperty("Device")
    public HwResource getDevice() {
        return this.device;
    }

    @JsonProperty("Device")
    public void setDevice(final HwResource device) {
        this.device = device;
    }

    @JsonProperty("TornadoFeatures")
    public TornadoFeatureVector getTornadoFeatures() {
        return this.tornadoFeatures;
    }

    @JsonProperty("TornadoFeatures")
    public void setTornadoFeatures(final TornadoFeatureVector tornadoFeatures) {
        this.tornadoFeatures = tornadoFeatures;
    }


    @Override
    public String toString() {
        return "InferenceServiceRequest{" +
                "objective='" + objective + '\'' +
                ", device=" + device +
                ", tornadoFeatures=" + tornadoFeatures +
                '}';
    }

}
