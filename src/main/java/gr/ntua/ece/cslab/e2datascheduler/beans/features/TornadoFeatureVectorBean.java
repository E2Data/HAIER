package gr.ntua.ece.cslab.e2datascheduler.beans.features;

import com.fasterxml.jackson.annotation.JsonProperty;


public class TornadoFeatureVectorBean {
    private String deviceID;
    private String device;
    private int globalMemoryLoads;
    private int globalMemoryStores;
    private int localMemoryLoads;
    private int localMemoryStores;
    private int constantMemoryLoads;
    private int constantMemoryStores;
    private int privateMemoryLoads;
    private int privateMemoryStores;
    private int totalLoops;
    private int parallelLoops;
    private int ifStatements;
    private int switchStatements;
    private int switchCases;
    private int castOperations;
    private int vectorOperations;
    private int totalIntegerOperations;
    private int totalFloatOperations;
    private int singlePrecisionFloatOperations;
    private int doublePrecisionFloatOperations;
    private int binaryOperations;
    private int booleanOperations;
    private int floatMathFunctions;
    private int integerMathFunctions;
    private int integerComparison;
    private int floatComparison;

    @JsonProperty("Device ID")
    public String getDeviceID() {
        return this.deviceID;
    }

    @JsonProperty("Device ID")
    public void setDeviceID(final String deviceID) {
        this.deviceID = deviceID;
    }

    @JsonProperty("Device")
    public String getDevice() {
        return this.device;
    }

    @JsonProperty("Device")
    public void setDevice(final String device) {
        this.device = device;
    }

    @JsonProperty("Global Memory Loads")
    public int getGlobalMemoryLoads() {
        return this.globalMemoryLoads;
    }

    @JsonProperty("Global Memory Loads")
    public void setGlobalMemoryLoads(final int globalMemoryLoads) {
        this.globalMemoryLoads = globalMemoryLoads;
    }

    @JsonProperty("Global Memory Stores")
    public int getGlobalMemoryStores() {
        return this.globalMemoryStores;
    }

    @JsonProperty("Global Memory Stores")
    public void setGlobalMemoryStores(final int globalMemoryStores) {
        this.globalMemoryStores = globalMemoryStores;
    }

    @JsonProperty("Local Memory Loads")
    public int getLocalMemoryLoads() {
        return this.localMemoryLoads;
    }

    @JsonProperty("Local Memory Loads")
    public void setLocalMemoryLoads(final int localMemoryLoads) {
        this.localMemoryLoads = localMemoryLoads;
    }

    @JsonProperty("Local Memory Stores")
    public int getLocalMemoryStores() {
        return this.localMemoryStores;
    }

    @JsonProperty("Local Memory Stores")
    public void setLocalMemoryStores(final int localMemoryStores) {
        this.localMemoryStores = localMemoryStores;
    }

    @JsonProperty("Constant Memory Loads")
    public int getConstantMemoryLoads() {
        return this.constantMemoryLoads;
    }

    @JsonProperty("Constant Memory Loads")
    public void setConstantMemoryLoads(final int constantMemoryLoads) {
        this.constantMemoryLoads = constantMemoryLoads;
    }

    @JsonProperty("Constant Memory Stores")
    public int getConstantMemoryStores() {
        return this.constantMemoryStores;
    }

    @JsonProperty("Constant Memory Stores")
    public void setConstantMemoryStores(final int constantMemoryStores) {
        this.constantMemoryStores = constantMemoryStores;
    }

    @JsonProperty("Private Memory Loads")
    public int getPrivateMemoryLoads() {
        return this.privateMemoryLoads;
    }

    @JsonProperty("Private Memory Loads")
    public void setPrivateMemoryLoads(final int privateMemoryLoads) {
        this.privateMemoryLoads = privateMemoryLoads;
    }

    @JsonProperty("Private Memory Stores")
    public int getPrivateMemoryStores() {
        return this.privateMemoryStores;
    }

    @JsonProperty("Private Memory Stores")
    public void setPrivateMemoryStores(final int privateMemoryStores) {
        this.privateMemoryStores = privateMemoryStores;
    }

    @JsonProperty("Total Loops")
    public int getTotalLoops() {
        return this.totalLoops;
    }

    @JsonProperty("Total Loops")
    public void setTotalLoops(final int totalLoops) {
        this.totalLoops = totalLoops;
    }

    @JsonProperty("Parallel Loops")
    public int getParallelLoops() {
        return this.parallelLoops;
    }

    @JsonProperty("Parallel Loops")
    public void setParallelLoops(final int parallelLoops) {
        this.parallelLoops = parallelLoops;
    }

    @JsonProperty("If Statements")
    public int getIfStatements() {
        return this.ifStatements;
    }

    @JsonProperty("If Statements")
    public void setIfStatements(final int ifStatements) {
        this.ifStatements = ifStatements;
    }

    @JsonProperty("Switch Statements")
    public int getSwitchStatements() {
        return this.switchStatements;
    }

    @JsonProperty("Switch Statements")
    public void setSwitchStatements(final int switchStatements) {
        this.switchStatements = switchStatements;
    }

    @JsonProperty("Switch Cases")
    public int getSwitchCases() {
        return this.switchCases;
    }

    @JsonProperty("Switch Cases")
    public void setSwitchCases(final int switchCases) {
        this.switchCases = switchCases;
    }

    @JsonProperty("Cast Operations")
    public int getCastOperations() {
        return this.castOperations;
    }

    @JsonProperty("Cast Operations")
    public void setCastOperations(final int castOperations) {
        this.castOperations = castOperations;
    }

    @JsonProperty("Vector Operations")
    public int getVectorOperations() {
        return this.vectorOperations;
    }

    @JsonProperty("Vector Operations")
    public void setVectorOperations(final int vectorOperations) {
        this.vectorOperations = vectorOperations;
    }

    @JsonProperty("Total Integer Operations")
    public int getTotalIntegerOperations() {
        return this.totalIntegerOperations;
    }

    @JsonProperty("Total Integer Operations")
    public void setTotalIntegerOperations(final int totalIntegerOperations) {
        this.totalIntegerOperations = totalIntegerOperations;
    }

    @JsonProperty("Total Float Operations")
    public int getTotalFloatOperations() {
        return this.totalFloatOperations;
    }

    @JsonProperty("Total Float Operations")
    public void setTotalFloatOperations(final int totalFloatOperations) {
        this.totalFloatOperations = totalFloatOperations;
    }

    @JsonProperty("Single Precision Float Operations")
    public int getSinglePrecisionFloatOperations() {
        return this.singlePrecisionFloatOperations;
    }

    @JsonProperty("Single Precision Float Operations")
    public void setSinglePrecisionFloatOperations(final int singlePrecisionFloatOperations) {
        this.singlePrecisionFloatOperations = singlePrecisionFloatOperations;
    }

    @JsonProperty("Double Precision Float Operations")
    public int getDoublePrecisionFloatOperations() {
        return this.doublePrecisionFloatOperations;
    }

    @JsonProperty("Double Precision Float Operations")
    public void setDoublePrecisionFloatOperations(final int doublePrecisionFloatOperations) {
        this.doublePrecisionFloatOperations = doublePrecisionFloatOperations;
    }

    @JsonProperty("Binary Operations")
    public int getBinaryOperations() {
        return this.binaryOperations;
    }

    @JsonProperty("Binary Operations")
    public void setBinaryOperations(final int binaryOperations) {
        this.binaryOperations = binaryOperations;
    }

    @JsonProperty("Boolean Operations")
    public int getBooleanOperations() {
        return this.booleanOperations;
    }

    @JsonProperty("Boolean Operations")
    public void setBooleanOperations(final int booleanOperations) {
        this.booleanOperations = booleanOperations;
    }

    @JsonProperty("Float Math Functions")
    public int getFloatMathFunctions() {
        return this.floatMathFunctions;
    }

    @JsonProperty("Float Math Functions")
    public void setFloatMathFunctions(final int floatMathFunctions) {
        this.floatMathFunctions = floatMathFunctions;
    }

    @JsonProperty("Integer Math Functions")
    public int getIntegerMathFunctions() {
        return this.integerMathFunctions;
    }

    @JsonProperty("Integer Math Functions")
    public void setIntegerMathFunctions(final int integerMathFunctions) {
        this.integerMathFunctions = integerMathFunctions;
    }

    @JsonProperty("Integer Comparison")
    public int getIntegerComparison() {
        return this.integerComparison;
    }

    @JsonProperty("Integer Comparison")
    public void setIntegerComparison(final int integerComparison) {
        this.integerComparison = integerComparison;
    }

    @JsonProperty("Float Comparison")
    public int getFloatComparison() {
        return this.floatComparison;
    }

    @JsonProperty("Float Comparison")
    public void setFloatComparison(final int floatComparison) {
        this.floatComparison = floatComparison;
    }


    // --------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        return "TornadoFeatureVectorBean{" +
                "deviceID='" + deviceID + '\'' +
                ", device='" + device + '\'' +
                ", globalMemoryLoads=" + globalMemoryLoads +
                ", globalMemoryStores=" + globalMemoryStores +
                ", localMemoryLoads=" + localMemoryLoads +
                ", localMemoryStores=" + localMemoryStores +
                ", constantMemoryLoads=" + constantMemoryLoads +
                ", constantMemoryStores=" + constantMemoryStores +
                ", privateMemoryLoads=" + privateMemoryLoads +
                ", privateMemoryStores=" + privateMemoryStores +
                ", totalLoops=" + totalLoops +
                ", parallelLoops=" + parallelLoops +
                ", ifStatements=" + ifStatements +
                ", switchStatements=" + switchStatements +
                ", switchCases=" + switchCases +
                ", castOperations=" + castOperations +
                ", vectorOperations=" + vectorOperations +
                ", totalIntegerOperations=" + totalIntegerOperations +
                ", totalFloatOperations=" + totalFloatOperations +
                ", singlePrecisionFloatOperations=" + singlePrecisionFloatOperations +
                ", doublePrecisionFloatOperations=" + doublePrecisionFloatOperations +
                ", binaryOperations=" + binaryOperations +
                ", booleanOperations=" + booleanOperations +
                ", floatMathFunctions=" + floatMathFunctions +
                ", integerMathFunctions=" + integerMathFunctions +
                ", integerComparison=" + integerComparison +
                ", floatComparison=" + floatComparison +
                '}';
    }
}
