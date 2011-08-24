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
 * @author-aj
 */

package com.cloud.netapp.dao;

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.netapp.LunVO;
import com.cloud.netapp.NetappVolumeVO;
import com.cloud.netapp.PoolVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={LunDao.class})
public class LunDaoImpl extends GenericDaoBase<LunVO, Long> implements LunDao {
    private static final Logger s_logger = Logger.getLogger(PoolDaoImpl.class);
		
    protected final SearchBuilder<LunVO> LunSearch;    
    protected final SearchBuilder<LunVO> LunNameSearch;    
    	    
	protected LunDaoImpl() {
        
        LunSearch = createSearchBuilder();
        LunSearch.and("volumeId", LunSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        LunSearch.done();

        LunNameSearch = createSearchBuilder();
        LunNameSearch.and("name", LunNameSearch.entity().getLunName(), SearchCriteria.Op.EQ);
        LunNameSearch.done();
        
	}

	@Override
    public List<LunVO> listLunsByVolId(Long volId) {
		Filter searchFilter = new Filter(LunVO.class, "id", Boolean.TRUE, Long.valueOf(0), Long.valueOf(10000));
		
        SearchCriteria sc = LunSearch.create();
        sc.setParameters("volumeId", volId);
        List<LunVO> lunList = listBy(sc,searchFilter);
        
        return lunList;
    }


	@Override
    public LunVO findByName(String name) {
        SearchCriteria sc = LunNameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }
}
