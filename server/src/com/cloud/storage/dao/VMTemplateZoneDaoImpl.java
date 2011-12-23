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

package com.cloud.storage.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={VMTemplateZoneDao.class})
public class VMTemplateZoneDaoImpl extends GenericDaoBase<VMTemplateZoneVO, Long> implements VMTemplateZoneDao {
	public static final Logger s_logger = Logger.getLogger(VMTemplateZoneDaoImpl.class.getName());
	
	protected final SearchBuilder<VMTemplateZoneVO> ZoneSearch;
	protected final SearchBuilder<VMTemplateZoneVO> TemplateSearch;
	protected final SearchBuilder<VMTemplateZoneVO> ZoneTemplateSearch;
	
	
	public VMTemplateZoneDaoImpl () {
		ZoneSearch = createSearchBuilder();
		ZoneSearch.and("zone_id", ZoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
		ZoneSearch.done();
		
		TemplateSearch = createSearchBuilder();
		TemplateSearch.and("template_id", TemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		TemplateSearch.done();
		
		ZoneTemplateSearch = createSearchBuilder();
		ZoneTemplateSearch.and("zone_id", ZoneTemplateSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
		ZoneTemplateSearch.and("template_id", ZoneTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		ZoneTemplateSearch.done();
	}
	

	@Override
	public List<VMTemplateZoneVO> listByZoneId(long id) {
	    SearchCriteria<VMTemplateZoneVO> sc = ZoneSearch.create();
	    sc.setParameters("zone_id", id);
	    return listIncludingRemovedBy(sc);
	}

	@Override
	public List<VMTemplateZoneVO> listByTemplateId(long templateId) {
	    SearchCriteria<VMTemplateZoneVO> sc = TemplateSearch.create();
	    sc.setParameters("template_id", templateId);
	    return listIncludingRemovedBy(sc);
	}

	@Override
	public VMTemplateZoneVO findByZoneTemplate(long zoneId, long templateId) {
		SearchCriteria<VMTemplateZoneVO> sc = ZoneTemplateSearch.create();
	    sc.setParameters("zone_id", zoneId);
	    sc.setParameters("template_id", templateId);
	    return findOneIncludingRemovedBy(sc);
	}

	@Override
    public List<VMTemplateZoneVO> listByZoneTemplate(Long zoneId, long templateId) {
		SearchCriteria<VMTemplateZoneVO> sc = ZoneTemplateSearch.create();
        if (zoneId != null) {
            sc.setParameters("zone_id", zoneId);
        }
	    sc.setParameters("template_id", templateId);
	    return listBy(sc);
	}	

}
