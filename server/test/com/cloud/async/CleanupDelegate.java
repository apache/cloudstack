package com.cloud.async;

import org.apache.log4j.Logger;

import com.cloud.utils.ActionDelegate;

public class CleanupDelegate implements ActionDelegate<String> {
    private static final Logger s_logger = Logger.getLogger(CleanupDelegate.class);

	@Override
	public void action(String param) {
		s_logger.info("Action called with param: " + param);
	}
}
