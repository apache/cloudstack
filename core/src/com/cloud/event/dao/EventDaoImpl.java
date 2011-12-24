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

import com.cloud.event.Event.State;
import com.cloud.event.EventVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={EventDao.class})
public class EventDaoImpl extends GenericDaoBase<EventVO, Long> implements EventDao {
	public static final Logger s_logger = Logger.getLogger(EventDaoImpl.class.getName());
	protected final SearchBuilder<EventVO> CompletedEventSearch;
	
	public EventDaoImpl () {
	    CompletedEventSearch = createSearchBuilder();
	    CompletedEventSearch.and("state",CompletedEventSearch.entity().getState(),SearchCriteria.Op.EQ);
	    CompletedEventSearch.and("startId", CompletedEventSearch.entity().getStartId(), SearchCriteria.Op.EQ);
	    CompletedEventSearch.done();
	}

	@Override
	public List<EventVO> searchAllEvents(SearchCriteria<EventVO> sc, Filter filter) {
	    return listIncludingRemovedBy(sc, filter);
	}

    @Override
    public List<EventVO> listOlderEvents(Date oldTime) {
        if (oldTime == null) return null;
        SearchCriteria<EventVO> sc = createSearchCriteria();
        sc.addAnd("createDate", SearchCriteria.Op.LT, oldTime);
        return listIncludingRemovedBy(sc, null);
        
    }
    
    @Override
    public EventVO findCompletedEvent(long startId) {
        SearchCriteria<EventVO> sc = CompletedEventSearch.create();
        sc.setParameters("state", State.Completed);
        sc.setParameters("startId", startId);
        return findOneIncludingRemovedBy(sc);
    }
}
