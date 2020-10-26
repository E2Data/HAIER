package gr.ntua.ece.cslab.e2datascheduler.ml.featurextraction;

import gr.ntua.ece.cslab.e2datascheduler.beans.features.TornadoFeatureVectorBean;

import java.util.Objects;


/**
 * Feature vector, as retrieved by Tornado execution on virtual devices (aka Tornado's "fake compilation"
 * functionality).
 */
public class TornadoFeatureVector {

    private final TornadoFeatureVectorBean bean;

    public TornadoFeatureVector(final TornadoFeatureVectorBean tornadoFeatureVectorBean) {
        this.bean = tornadoFeatureVectorBean;
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
    public boolean equals(final Object otherObject) {
        if (otherObject == this) {
            return true;
        }
        if (null == otherObject || otherObject.getClass() != this.getClass()) {
            return false;
        }

        final TornadoFeatureVector otherTornadoFeatureVector = (TornadoFeatureVector) otherObject;
        // FIXME(ckatsak): Return the result of the comparison of their fields, as in:
        //  https://www.techiedelight.com/how-to-use-equal-objects-as-key-hashmap-hashset-java/
        return Objects.equals(this, otherTornadoFeatureVector); // <-- FIXME(ckatsak): This is wrong
    }

    @Override
    public int hashCode() {
        // FIXME(ckatsak): Hash all fields, as in:
        //  https://www.techiedelight.com/how-to-use-equal-objects-as-key-hashmap-hashset-java/
        return Objects.hash(this); // <-- FIXME(ckatsak): This is wrong
    }

}
