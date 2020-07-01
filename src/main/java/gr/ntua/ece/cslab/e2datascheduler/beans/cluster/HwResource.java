package gr.ntua.ece.cslab.e2datascheduler.beans.cluster;

/**
 * Generic description of a hardware resource.
 */
public class HwResource {

    private long maximumAllocation;
    private int minimumAllocation;
    private String name;
    private String units; // has meaning only in memory and indicates whether the reported amount is counted in MB/GB etc.
    private int value;
    private String host; // indicates the machine where this resource exists.

    /**
     * Nullary constructor.
     */
    public HwResource() {}

    /**
     * Clone from an existing instance, but setting the {@code value} field to 1 and appending <code>-{id}</code>
     * to its {@code name} field.
     *
     * @param hwResource is the existing instance.
     * @param id is the ID to append to existing instance's {@code name} field.
     */
    public HwResource(final HwResource hwResource, final int id) {
        this.maximumAllocation = hwResource.maximumAllocation;
        this.minimumAllocation = hwResource.minimumAllocation;
        this.name = hwResource.name + "-" + id;
        this.units = hwResource.units;
        this.value = 1;
        this.host = hwResource.host;
    }

    public long getMaximumAllocation() {
        return maximumAllocation;
    }

    public int getMinimumAllocation() {
        return minimumAllocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnits() {
        return units;
    }

    public int getValue() {
        return value;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public String toString() {
        return "<HwResource name=" + this.name + " host=" + this.host + " value=" + this.value + ">";
    }
}
