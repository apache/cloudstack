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

import com.cloud.storage.DiskTemplateVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

/**
 * @author ahuang
 *
 */
@Local(value={DiskTemplateDao.class})
public class DiskTemplateDaoImpl extends GenericDaoBase<DiskTemplateVO, Long> implements DiskTemplateDao {
    
	protected SearchBuilder<DiskTemplateVO> TypeSizeSearch;
	
    protected static final String BY_TYPE_AND_SIZE_CLAUSE = "type = ? AND size = ?";
    
    protected DiskTemplateDaoImpl() {
    	super();
    	
    	TypeSizeSearch = createSearchBuilder();
    	TypeSizeSearch.and("type", TypeSizeSearch.entity().getType(), SearchCriteria.Op.EQ);
    	TypeSizeSearch.and("size", TypeSizeSearch.entity().getSize(), SearchCriteria.Op.EQ);
    }
    
    public DiskTemplateVO findByTypeAndSize(String type, long size) {
    	SearchCriteria sc = TypeSizeSearch.create();
    	sc.setParameters("type", type);
    	sc.setParameters("size", size);
    	
        List<DiskTemplateVO> vos = listActiveBy(sc);
        assert(vos.size() <= 1);   // Should only have one.  If more than one something is wrong.
        return vos.size() == 0 ? null : vos.get(0);
    }
}
