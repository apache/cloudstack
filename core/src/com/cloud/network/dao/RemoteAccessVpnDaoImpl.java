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

package com.cloud.network.dao;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={RemoteAccessVpnDao.class})
public class RemoteAccessVpnDaoImpl extends GenericDaoBase<RemoteAccessVpnVO, Long> implements RemoteAccessVpnDao {
    private static final Logger s_logger = Logger.getLogger(RemoteAccessVpnDaoImpl.class);
    
    private final SearchBuilder<RemoteAccessVpnVO> ListByIp;
    private final SearchBuilder<RemoteAccessVpnVO> AccountAndZoneSearch;

    protected RemoteAccessVpnDaoImpl() {
        ListByIp  = createSearchBuilder();
        ListByIp.and("ipAddress", ListByIp.entity().getVpnServerAddress(), SearchCriteria.Op.EQ);
        ListByIp.done();

        AccountAndZoneSearch = createSearchBuilder();
        AccountAndZoneSearch.and("accountId", AccountAndZoneSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        AccountAndZoneSearch.and("zoneId", AccountAndZoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        AccountAndZoneSearch.done();
    }

    @Override
    public RemoteAccessVpnVO findByPublicIpAddress(String ipAddress) {
        SearchCriteria<RemoteAccessVpnVO> sc = ListByIp.create();
        sc.setParameters("ipAddress", ipAddress);
        return findOneBy(sc);
    }

    @Override
    public RemoteAccessVpnVO findByAccountAndZone(Long accountId, Long zoneId) {
        SearchCriteria<RemoteAccessVpnVO> sc = AccountAndZoneSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("zoneId", zoneId);
        return findOneBy(sc);
    }
}
