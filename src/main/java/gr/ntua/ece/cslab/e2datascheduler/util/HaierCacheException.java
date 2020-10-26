package gr.ntua.ece.cslab.e2datascheduler.util;


/**
 * {@link RuntimeException} thrown by Haier's Caches if an entry is not present as it should.
 */
public class HaierCacheException extends RuntimeException {

    /**
     * {@link RuntimeException} thrown by Haier's Caches if an entry is not present as it should.
     *
     * @param message {@link RuntimeException}'s message
     */
    public HaierCacheException(final String message) {
        super(message);
    }

}
