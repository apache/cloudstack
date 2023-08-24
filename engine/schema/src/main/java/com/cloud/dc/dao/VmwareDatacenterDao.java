// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.dc.dao;

import java.util.List;

import com.cloud.dc.VmwareDatacenterVO;
import com.cloud.utils.db.GenericDao;

public interface VmwareDatacenterDao extends GenericDao<VmwareDatacenterVO, Long> {

    /**
     * Return a VMware Datacenter given guid
     * @param guid of VMware datacenter
     * @return VmwareDatacenterVO for the VMware datacenter having the specified guid.
     */
    VmwareDatacenterVO getVmwareDatacenterByGuid(String guid);

    /**
     * Return a VMware Datacenter given name and vCenter host.
     * For legacy zones multiple records will be present in the table.
     * @param name of VMware datacenter
     * @param vCenter host
     * @return VmwareDatacenterVO for the VMware datacenter with given name and
     * belonging to specified vCenter host.
     */
    List<VmwareDatacenterVO> getVmwareDatacenterByNameAndVcenter(String name, String vCenterHost);

    /**
     * Return a list of VMware Datacenter given name.
     * @param name of Vmware datacenter
     * @return list of VmwareDatacenterVO for VMware datacenters having the specified name.
     */
    List<VmwareDatacenterVO> listVmwareDatacenterByName(String name);

    /**
     * Return a list of VMware Datacenters belonging to specified vCenter
     * @param vCenter Host
     * @return list of VmwareDatacenterVO for all VMware datacenters belonging to
     * specified vCenter
     */
    List<VmwareDatacenterVO> listVmwareDatacenterByVcenter(String vCenterHost);

    /**
     * Lists all associated VMware datacenter on the management server.
     * @return list of VmwareDatacenterVO for all associated VMware datacenters
     */
    List<VmwareDatacenterVO> listAllVmwareDatacenters();

}
