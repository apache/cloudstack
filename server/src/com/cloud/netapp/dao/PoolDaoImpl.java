/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

import com.cloud.netapp.PoolVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={PoolDao.class})
public class PoolDaoImpl extends GenericDaoBase<PoolVO, Long> implements PoolDao {
    private static final Logger s_logger = Logger.getLogger(PoolDaoImpl.class);
		
    protected final SearchBuilder<PoolVO> PoolSearch;
    	    
	protected PoolDaoImpl() {
        
        PoolSearch = createSearchBuilder();
        PoolSearch.and("name", PoolSearch.entity().getName(), SearchCriteria.Op.EQ);
        PoolSearch.done();
        
	}

	@Override
    public PoolVO findPool(String poolName) {
        SearchCriteria sc = PoolSearch.create();
        sc.setParameters("name", poolName);
        List<PoolVO> poolList = listBy(sc);
        
        return(poolList.size()>0?poolList.get(0):null);
    }

	@Override
	public List<PoolVO> listPools() {
		// TODO Auto-generated method stub
		return null;
	}

    
//    @Override
//    public List<NetappStoragePoolVO> listVolumes(String poolName) {
//        SearchCriteria sc = NetappListVolumeSearch.create();
//        sc.setParameters("poolName", poolName);
//        return listBy(sc);
//    }
    
}
