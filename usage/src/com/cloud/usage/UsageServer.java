/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage;

import org.apache.log4j.Logger;

import com.cloud.utils.component.ComponentLocator;

public class UsageServer {
    private static final Logger s_logger = Logger.getLogger(UsageServer.class.getName());
    public static final String Name = "usage-server";

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO: do we need to communicate with mgmt server?
        final ComponentLocator _locator = ComponentLocator.getLocator(UsageServer.Name, "usage-components.xml", "log4j-cloud_usage");
        UsageManager mgr = _locator.getManager(UsageManager.class);
        if (mgr != null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("UsageServer ready...");
            }
        }
    }
}
