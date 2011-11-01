/**
 *  Copyright (C) 2011 Citrix.com, Inc.  All rights reserved.
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

import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local (value={GuestOSHypervisorDao.class})
public class GuestOSHypervisorDaoImpl extends GenericDaoBase<GuestOSHypervisorVO, Long> implements GuestOSHypervisorDao {
    
    
    protected final SearchBuilder<GuestOSHypervisorVO> hypervisor_search;
    
    protected GuestOSHypervisorDaoImpl() {
        hypervisor_search = createSearchBuilder();
        hypervisor_search.and("hypervisor_type", hypervisor_search.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        hypervisor_search.done();
    }
    

    @Override
    public List<GuestOSHypervisorVO>  findByHypervisorType(HypervisorType hypervisorType) {
        SearchCriteria<GuestOSHypervisorVO> sc = hypervisor_search.create();
        sc.setParameters("hypervisor_type", hypervisorType);
        return listBy(sc);
    }
    

}
