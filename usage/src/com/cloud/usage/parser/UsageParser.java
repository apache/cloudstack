/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 *
 */

package com.cloud.usage.parser;

import java.util.Date;

import org.apache.log4j.Logger;

public abstract class UsageParser implements Runnable {
	public static final Logger s_logger = Logger.getLogger(UsageParser.class.getName());
	
	public void run() {
		try {
			parse(null);
		} catch (Exception e) {
			s_logger.warn("Error while parsing usage events", e);
		}
	}
	
	public abstract void parse(Date endDate);
}
