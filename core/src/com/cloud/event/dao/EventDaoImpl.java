/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.event.dao;

import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.event.EventState;
import com.cloud.event.EventVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={EventDao.class})
public class EventDaoImpl extends GenericDaoBase<EventVO, Long> implements EventDao {
	public static final Logger s_logger = Logger.getLogger(EventDaoImpl.class.getName());
	protected final SearchBuilder<EventVO> StartedEventsSearch;
	protected final SearchBuilder<EventVO> CompletedEventSearch;
	
	public EventDaoImpl () {
	    StartedEventsSearch = createSearchBuilder();
	    StartedEventsSearch.and("state",StartedEventsSearch.entity().getState(),SearchCriteria.Op.NEQ);
	    StartedEventsSearch.and("startId", StartedEventsSearch.entity().getStartId(), SearchCriteria.Op.EQ);
	    StartedEventsSearch.and("createDate", StartedEventsSearch.entity().getCreateDate(), SearchCriteria.Op.BETWEEN);
	    StartedEventsSearch.done();
	    
	    CompletedEventSearch = createSearchBuilder();
	    CompletedEventSearch.and("state",CompletedEventSearch.entity().getState(),SearchCriteria.Op.EQ);
	    CompletedEventSearch.and("startId", CompletedEventSearch.entity().getStartId(), SearchCriteria.Op.EQ);
	    CompletedEventSearch.done();
	}

	@Override
	@DB
	public List<EventVO> searchAllEvents(SearchCriteria sc, Filter filter) {
	    return listBy(sc, filter);
	}

	@Override
    public List<EventVO> search(final SearchCriteria sc, final Filter filter) {
	    return super.search(sc, filter);
	}

    @Override
    public List<EventVO> listOlderEvents(Date oldTime) {
        if (oldTime == null) return null;
        SearchCriteria sc = createSearchCriteria();
        sc.addAnd("createDate", SearchCriteria.Op.LT, oldTime);
        return listBy(sc, null);
        
    }
    
    @Override
    public List<EventVO> listStartedEvents(Date minTime, Date maxTime) {
        if (minTime == null || maxTime == null) return null;
        SearchCriteria sc = StartedEventsSearch.create();
        sc.setParameters("state", EventState.Completed);
        sc.setParameters("startId", 0);
        sc.setParameters("createDate", minTime, maxTime);
        return listBy(sc, null);
    }
    
    @Override
    public EventVO findCompletedEvent(long startId) {
        SearchCriteria sc = CompletedEventSearch.create();
        sc.setParameters("state", EventState.Completed);
        sc.setParameters("startId", startId);
        return findOneBy(sc);
    }
}
