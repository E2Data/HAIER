package gr.ntua.ece.cslab.e2datascheduler.beans.features;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;


public class TornadoFeatureVectorRootBean {

    private final Map<String, TornadoFeatureVectorBean> tornadoFeatureVectorBeanMap = new LinkedHashMap<>();

    public TornadoFeatureVectorRootBean(){}

    public Map<String, TornadoFeatureVectorBean> getTornadoFeatureVectorBeanMap() {
        return this.tornadoFeatureVectorBeanMap;
    }

    @JsonAnySetter
    public void setTornadoFeatureVectorBean(
            final String tornadoFeatureVectorBeanName,
            final TornadoFeatureVectorBean tornadoFeatureVectorBean) {
        this.tornadoFeatureVectorBeanMap.put(tornadoFeatureVectorBeanName, tornadoFeatureVectorBean);
    }


    // --------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        return "TornadoFeatureVectorRootBean{" +
                "tornadoFeatureVectorBeanMap=" + tornadoFeatureVectorBeanMap +
                '}';
    }


    // --------------------------------------------------------------------------------------------


    // NOTE(ckatsak): Quick & dirty deserialization test
    public static void main(String[] args) throws IOException {
        final String sz[] = new String[]{
                "{\n" +
                        "    \"channelConvolution\": {\n" +
                        "        \"Device ID\":  \"0:0\",\n" +
                        "        \"Device\":  \"Tesla V100-SXM2-32GB\",\n" +
                        "        \"Global Memory Loads\":  \"62\",\n" +
                        "        \"Global Memory Stores\":  \"1\",\n" +
                        "        \"Constant Memory Loads\":  \"0\",\n" +
                        "        \"Constant Memory Stores\":  \"0\",\n" +
                        "        \"Local Memory Loads\":  \"0\",\n" +
                        "        \"Local Memory Stores\":  \"0\",\n" +
                        "        \"Private Memory Loads\":  \"118\",\n" +
                        "        \"Private Memory Stores\":  \"118\",\n" +
                        "        \"Total Loops\":  \"3\",\n" +
                        "        \"Parallel Loops\":  \"2\",\n" +
                        "        \"If Statements\":  \"4\",\n" +
                        "        \"Integer Comparison\":  \"3\",\n" +
                        "        \"Float Comparison\":  \"1\",\n" +
                        "        \"Switch Statements\":  \"0\",\n" +
                        "        \"Switch Cases\":  \"0\",\n" +
                        "        \"Vector Operations\":  \"0\",\n" +
                        "        \"Total Integer Operations\":  \"227\",\n" +
                        "        \"Total Float Operations\":  \"0\",\n" +
                        "        \"Single Precision Float Operations\":  \"0\",\n" +
                        "        \"Double Precision Float Operations\":  \"0\",\n" +
                        "        \"Cast Operations\":  \"32\",\n" +
                        "        \"Float Math Functions\":  \"31\",\n" +
                        "        \"Integer Math Functions\":  \"64\"\n" +
                        "    }\n" +
                        "}",
                "{\n" +
                        "    \"computeDFT\": {\n" +
                        "        \"Device ID\":  \"0:0\",\n" +
                        "        \"Device\":  \"Tesla V100-SXM2-32GB\",\n" +
                        "        \"Global Memory Loads\":  \"2\",\n" +
                        "        \"Global Memory Stores\":  \"2\",\n" +
                        "        \"Constant Memory Loads\":  \"0\",\n" +
                        "        \"Constant Memory Stores\":  \"0\",\n" +
                        "        \"Local Memory Loads\":  \"0\",\n" +
                        "        \"Local Memory Stores\":  \"0\",\n" +
                        "        \"Private Memory Loads\":  \"14\",\n" +
                        "        \"Private Memory Stores\":  \"14\",\n" +
                        "        \"Total Loops\":  \"2\",\n" +
                        "        \"Parallel Loops\":  \"1\",\n" +
                        "        \"If Statements\":  \"2\",\n" +
                        "        \"Integer Comparison\":  \"2\",\n" +
                        "        \"Float Comparison\":  \"0\",\n" +
                        "        \"Switch Statements\":  \"0\",\n" +
                        "        \"Switch Cases\":  \"0\",\n" +
                        "        \"Vector Operations\":  \"0\",\n" +
                        "        \"Total Integer Operations\":  \"6\",\n" +
                        "        \"Total Float Operations\":  \"6\",\n" +
                        "        \"Single Precision Float Operations\":  \"0\",\n" +
                        "        \"Double Precision Float Operations\":  \"6\",\n" +
                        "        \"Cast Operations\":  \"2\",\n" +
                        "        \"Float Math Functions\":  \"5\",\n" +
                        "        \"Integer Math Functions\":  \"0\"\n" +
                        "    }\n" +
                        "}\n"
        };

        for (String ser : sz) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final TornadoFeatureVectorRootBean des = objectMapper.readValue(ser, TornadoFeatureVectorRootBean.class);
            System.out.println("deserialized: " + des + "\n\n");
        }
    }

}
