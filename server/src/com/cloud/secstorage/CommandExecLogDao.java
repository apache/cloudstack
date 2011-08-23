/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.secstorage;

import java.util.Date;

import com.cloud.utils.db.GenericDao;

public interface CommandExecLogDao extends GenericDao<CommandExecLogVO, Long> {
	public void expungeExpiredRecords(Date cutTime);
}
