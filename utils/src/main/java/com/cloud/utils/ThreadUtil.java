package com.cloud.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadUtil {

    protected static Logger LOGGER = LogManager.getLogger(AutoCloseableUtil.class);

    public static void wait(Object object, long timeoutInMillis, long id, String uuid, String name) {
        synchronized (object) {
            try {
                object.wait(timeoutInMillis);
            } catch (InterruptedException e) {
                LOGGER.warn("PingTask interrupted while waiting to retry ping [id: {}, uuid: {}, name: {}]", id, uuid, name, e);
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }

    }
}
