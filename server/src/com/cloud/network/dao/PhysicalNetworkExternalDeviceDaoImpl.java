/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import java.util.List;
import javax.ejb.Local;
import com.cloud.network.PhysicalNetworkExternalDeviceVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value=PhysicalNetworkExternalDeviceDao.class) @DB(txn=false)
public class PhysicalNetworkExternalDeviceDaoImpl extends GenericDaoBase<PhysicalNetworkExternalDeviceVO, Long> implements PhysicalNetworkExternalDeviceDao {
    final SearchBuilder<PhysicalNetworkExternalDeviceVO> physicalNetworkServiceProviderSearch;

    protected PhysicalNetworkExternalDeviceDaoImpl() {
        super();
        physicalNetworkServiceProviderSearch = createSearchBuilder();
        physicalNetworkServiceProviderSearch.and("networkServiceProviderId", physicalNetworkServiceProviderSearch.entity().getNetworkServiceProviderId(), Op.EQ);
        physicalNetworkServiceProviderSearch.done();

    }

    @Override
    public List<PhysicalNetworkExternalDeviceVO> listByNetworkServiceProviderId(long networkServiceProviderId) {
        SearchCriteria<PhysicalNetworkExternalDeviceVO> sc = physicalNetworkServiceProviderSearch.create();
        sc.setParameters("networkServiceProviderId", networkServiceProviderId);
        return search(sc, null);
    }
   
}
