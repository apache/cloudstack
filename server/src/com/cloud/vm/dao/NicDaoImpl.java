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

/**
 * 
 */
package com.cloud.vm.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;

@Local(value=NicDao.class)
public class NicDaoImpl extends GenericDaoBase<NicVO, Long> implements NicDao {
    private final SearchBuilder<NicVO> AllFieldsSearch;
    private final GenericSearchBuilder<NicVO, String> IpSearch;
    
    protected NicDaoImpl() {
        super();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("vmType", AllFieldsSearch.entity().getVmType(), Op.EQ);
        AllFieldsSearch.and("address", AllFieldsSearch.entity().getIp4Address(), Op.EQ);
        AllFieldsSearch.and("isDefault", AllFieldsSearch.entity().isDefaultNic(), Op.EQ);
        AllFieldsSearch.done();
        
        IpSearch = createSearchBuilder(String.class);
        IpSearch.select(null, Func.DISTINCT, IpSearch.entity().getIp4Address());
        IpSearch.and("network", IpSearch.entity().getNetworkId(), Op.EQ);
        IpSearch.and("address", IpSearch.entity().getIp4Address(), Op.NNULL);
        IpSearch.done();
    }
    
    @Override
    public void removeNicsForInstance(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        remove(sc);
    }
    
    @Override
    public List<NicVO> listByVmId(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        return listBy(sc);
    }
    
    @Override
    public List<NicVO> listByVmIdIncludingRemoved(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        return listIncludingRemovedBy(sc);
    }
    
    
    @Override
    public List<String> listIpAddressInNetwork(long networkId) {
        SearchCriteria<String> sc = IpSearch.create();
        sc.setParameters("network", networkId);
        return customSearch(sc, null);
    }
    
    @Override
    public List<NicVO> listByNetworkId(long networkId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        return listBy(sc);
    }
    
    @Override
    public NicVO findByInstanceIdAndNetworkId(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("instance", instanceId);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findByInstanceIdAndNetworkIdIncludingRemoved(long networkId, long instanceId) {
        SearchCriteria<NicVO> sc = createSearchCriteria();
        sc.addAnd("networkId", SearchCriteria.Op.EQ, networkId);
        sc.addAnd("instanceId", SearchCriteria.Op.EQ, instanceId);
        return findOneIncludingRemovedBy(sc);
    }
    
    @Override
    public NicVO findByNetworkIdAndType(long networkId, VirtualMachine.Type vmType) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        sc.setParameters("vmType", vmType);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findByIp4Address(String ip4Address) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("address", ip4Address);
        return findOneBy(sc);
    }
    
    @Override
    public NicVO findDefaultNicForVM(long instanceId) {
        SearchCriteria<NicVO> sc = AllFieldsSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("isDefault", 1);
        return findOneBy(sc);
    }
}
