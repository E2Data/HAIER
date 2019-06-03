package gr.ntua.ece.cslab.e2datascheduler.beans.cluster;

public abstract class YarnResource {

    private long maximumAllocation;
    private int minimumAllocation;
    private String name;
    private String units; // has meaning only in memory and indicates whether the reported amount is counted in MB/GB etc.
    private int value;


}
