package gr.ntua.ece.cslab.e2datascheduler.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * HAIER's Singleton log Handler that safely exposes logs to a {@link BlockingQueue<String>}.
 */
public class HaierLogHandler extends Handler {

    private static HaierLogHandler handler;
    private static BlockingQueue<String> queue;

    public HaierLogHandler(final BlockingQueue<String> queue) {
        HaierLogHandler.queue = queue;
    }

    public static synchronized HaierLogHandler getHandler() {
        if (null == handler) {
            handler = new HaierLogHandler(new LinkedBlockingQueue<>());
        }
        return handler;
    }

    public static synchronized BlockingQueue<String> getLogsQueue() {
        getHandler();
        return queue;
    }


    // --------------------------------------------------------------------------------------------


    @Override
    public void publish(final LogRecord logRecord) {
        if (!isLoggable(logRecord)) {
            return;
        }
        if (!queue.offer(logRecord.getMessage())) {
            getErrorManager().error("Queue is full", null, ErrorManager.WRITE_FAILURE);
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}
}
