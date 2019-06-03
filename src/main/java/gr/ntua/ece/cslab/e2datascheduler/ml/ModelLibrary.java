package gr.ntua.ece.cslab.e2datascheduler.ml;

import java.util.HashMap;

/**
 * A library of ML models that is maintained in the server's memory.
 *
 * The library is practically a map which associates the name of the model with the corresponding memory object.
 * {@link Model}.name ==> {@link Model}
 */
public class ModelLibrary extends HashMap<String, Model> { }
