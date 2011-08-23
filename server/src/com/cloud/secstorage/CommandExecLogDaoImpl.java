/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.secstorage;

import java.util.Date;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value={CommandExecLogDao.class})
public class CommandExecLogDaoImpl extends GenericDaoBase<CommandExecLogVO, Long> implements CommandExecLogDao {

    protected final SearchBuilder<CommandExecLogVO> ExpungeSearch;
	
	public CommandExecLogDaoImpl() {
		ExpungeSearch = createSearchBuilder();
		ExpungeSearch.and("created", ExpungeSearch.entity().getCreated(), Op.LT);
		ExpungeSearch.done();
	}
	
	@Override
	public void expungeExpiredRecords(Date cutTime) {
		SearchCriteria<CommandExecLogVO> sc = ExpungeSearch.create();
		sc.setParameters("created", cutTime);
		expunge(sc);
	}
}

