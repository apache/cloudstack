package com.cloud.async;

import org.apache.log4j.Logger;

public class CleanupDelegate implements com.cloud.utils.CleanupDelegate<String, Object> {
    private static final Logger s_logger = Logger.getLogger(CleanupDelegate.class);

	@Override
	public boolean cleanup(String param, Object managerContext) {
		s_logger.info("Action called with param: " + param);
		return true;
	}
}
