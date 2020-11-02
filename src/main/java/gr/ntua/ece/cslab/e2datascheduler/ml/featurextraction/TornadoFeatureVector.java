package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction;

import gr.ntua.ece.cslab.e2datascheduler.beans.features.TornadoFeatureVectorBean;

import java.util.List;


/**
 * Feature vector, as retrieved by Tornado execution on virtual devices (aka Tornado's "fake compilation"
 * functionality).
 */
public class TornadoFeatureVector {

    private final TornadoFeatureVectorBean bean;

    /**
     * Create a new {@link TornadoFeatureVector} to wrap the given {@link TornadoFeatureVectorBean}.
     *
     * @param tornadoFeatureVectorBean The {@link TornadoFeatureVectorBean} to be wrapped
     */
    public TornadoFeatureVector(final TornadoFeatureVectorBean tornadoFeatureVectorBean) {
        this.bean = tornadoFeatureVectorBean;
    }

    /**
     * Create a new {@link TornadoFeatureVector} to create and wrap a single {@link TornadoFeatureVectorBean}
     * that accumulates the stats of all {@link TornadoFeatureVectorBean}s underlying the
     * {@link TornadoFeatureVector}s in the provided {@link List}.
     *
     * @param tornadoFeatureVectors The provided {@link List} of {@link TornadoFeatureVector}s whose underlying
     *                              {@link TornadoFeatureVectorBean}s are to be accumulated and wrapped
     */
    public TornadoFeatureVector(final List<TornadoFeatureVector> tornadoFeatureVectors) {
        final TornadoFeatureVectorBean accBean = new TornadoFeatureVectorBean();
        for (TornadoFeatureVector tornadoFeatureVector : tornadoFeatureVectors) {
            final TornadoFeatureVectorBean currBean = tornadoFeatureVector.getBean();
            accBean.setGlobalMemoryLoads(accBean.getGlobalMemoryLoads() + currBean.getGlobalMemoryLoads());
            accBean.setGlobalMemoryStores(accBean.getGlobalMemoryStores() + currBean.getGlobalMemoryStores());
            accBean.setConstantMemoryLoads(accBean.getConstantMemoryLoads() + currBean.getConstantMemoryLoads());
            accBean.setConstantMemoryStores(accBean.getConstantMemoryStores() + currBean.getConstantMemoryStores());
            accBean.setLocalMemoryLoads(accBean.getLocalMemoryLoads() + currBean.getLocalMemoryLoads());
            accBean.setLocalMemoryStores(accBean.getLocalMemoryStores() + currBean.getLocalMemoryStores());
            accBean.setPrivateMemoryLoads(accBean.getPrivateMemoryLoads() + currBean.getPrivateMemoryLoads());
            accBean.setPrivateMemoryStores(accBean.getPrivateMemoryStores() + currBean.getPrivateMemoryStores());
            accBean.setTotalLoops(accBean.getTotalLoops() + currBean.getTotalLoops());
            accBean.setParallelLoops(accBean.getParallelLoops() + currBean.getParallelLoops());
            accBean.setIfStatements(accBean.getIfStatements() + currBean.getIfStatements());
            accBean.setIntegerComparison(accBean.getIntegerComparison() + currBean.getIntegerComparison());
            accBean.setFloatComparison(accBean.getFloatComparison() + currBean.getFloatComparison());
            accBean.setSwitchStatements(accBean.getSwitchStatements() + currBean.getSwitchStatements());
            accBean.setSwitchCases(accBean.getSwitchCases() + currBean.getSwitchCases());
            accBean.setVectorOperations(accBean.getVectorOperations() + currBean.getVectorOperations());
            accBean.setTotalIntegerOperations(accBean.getTotalIntegerOperations() + currBean.getTotalIntegerOperations());
            accBean.setTotalFloatOperations(accBean.getTotalFloatOperations() + currBean.getTotalFloatOperations());
            accBean.setSinglePrecisionFloatOperations(accBean.getSinglePrecisionFloatOperations() + currBean.getSinglePrecisionFloatOperations());
            accBean.setDoublePrecisionFloatOperations(accBean.getDoublePrecisionFloatOperations() + currBean.getDoublePrecisionFloatOperations());
            accBean.setCastOperations(accBean.getCastOperations() + currBean.getCastOperations());
            accBean.setFloatMathFunctions(accBean.getFloatMathFunctions() + currBean.getFloatMathFunctions());
            accBean.setIntegerMathFunctions(accBean.getIntegerMathFunctions() + currBean.getIntegerMathFunctions());
        }
        this.bean = accBean;
    }

    /**
     * Create a dummy/empty {@link TornadoFeatureVector} to work with
     * {@link gr.ntua.ece.cslab.e2datascheduler.ml.Model}s other than
     * {@link gr.ntua.ece.cslab.e2datascheduler.ml.impl.TornadoModel}.
     *
     * @return A new dummy/empty {@link TornadoFeatureVector}
     */
    public static TornadoFeatureVector newDummy() {
        return new TornadoFeatureVector(new TornadoFeatureVectorBean());
    }


    // --------------------------------------------------------------------------------------------


    public TornadoFeatureVectorBean getBean() {
        return this.bean;
    }


    // --------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        return "TornadoFeatureVector{" +
                "bean=" + bean +
                '}';
    }

}
