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
 */

package com.cloud.usage.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.usage.ExternalPublicIpStatisticsVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.dao.DomainRouterDao;

@Local(value = { ExternalPublicIpStatisticsDao.class })
public class ExternalPublicIpStatisticsDaoImpl extends GenericDaoBase<ExternalPublicIpStatisticsVO, Long> implements ExternalPublicIpStatisticsDao {

	private final SearchBuilder<ExternalPublicIpStatisticsVO> AccountZoneSearch;
	private final SearchBuilder<ExternalPublicIpStatisticsVO> SingleRowSearch;    
    
    public ExternalPublicIpStatisticsDaoImpl() {
    	AccountZoneSearch = createSearchBuilder();
    	AccountZoneSearch.and("accountId", AccountZoneSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
    	AccountZoneSearch.and("zoneId", AccountZoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
    	AccountZoneSearch.done();

    	SingleRowSearch = createSearchBuilder();
    	SingleRowSearch.and("accountId", SingleRowSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
    	SingleRowSearch.and("zoneId", SingleRowSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
    	SingleRowSearch.and("publicIp", SingleRowSearch.entity().getPublicIpAddress(), SearchCriteria.Op.EQ);
    	SingleRowSearch.done();
    }
	
	public ExternalPublicIpStatisticsVO lock(long accountId, long zoneId, String publicIpAddress) {
		SearchCriteria<ExternalPublicIpStatisticsVO> sc = getSingleRowSc(accountId, zoneId, publicIpAddress);
		return lockOneRandomRow(sc, true);
	}

    public ExternalPublicIpStatisticsVO findBy(long accountId, long zoneId, String publicIpAddress) {
    	SearchCriteria<ExternalPublicIpStatisticsVO> sc = getSingleRowSc(accountId, zoneId, publicIpAddress);
        return findOneBy(sc);
    }
    
    private SearchCriteria<ExternalPublicIpStatisticsVO> getSingleRowSc(long accountId, long zoneId, String publicIpAddress) {
    	SearchCriteria<ExternalPublicIpStatisticsVO> sc = SingleRowSearch.create();
    	sc.setParameters("accountId", accountId);
		sc.setParameters("zoneId", zoneId);
		sc.setParameters("publicIp", publicIpAddress);
		return sc;
    }
	
	public List<ExternalPublicIpStatisticsVO> listBy(long accountId, long zoneId) {
		SearchCriteria<ExternalPublicIpStatisticsVO> sc = AccountZoneSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("zoneId", zoneId);
        return search(sc, null);
	}
	
}
