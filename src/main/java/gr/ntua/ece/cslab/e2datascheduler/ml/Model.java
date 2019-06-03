package gr.ntua.ece.cslab.e2datascheduler.ml;

import gr.ntua.ece.cslab.e2datascheduler.beans.cluster.HwResource;


//TODO: @kbitsak: Do not modify this class!!!
//TODO: @kbitsak: For each different model that you may have, create a subclass in the 'impl' package
//TODO: @kbitsak: and implement the 'load' and 'predict' methods for the subclass
/**
 * This class constitutes the description of a generic Machine Learning model
 */
public abstract class Model {

    protected String name;


    //TODO: @kbitsak: Implement the loading mechanism. You should assume that the model is stored
    //TODO: in the 'resources' folder of the project
    /**
     * Loads a model from disk.
     */
    public abstract void load(String path);

    //TODO: @kbitsak: Fill in the required code for predicting a value
    /**
     * Makes a prediction given a generic feature vector
     * @param featureVector A vector that comprise the model's input
     * @return the predicted value
     */
    //FIXME: (gmytil) A model refers to a specific objective and hardware device. Thus, it should accept only the
    // source code as input parameter. Indexing models by objectives and devices should be taking place at the
    // models library level.
    public abstract double predict(String objective, HwResource device, String sourceCode);
    //public abstract double predict(String objective, HwResource device, List<Double> featureVector);
}
