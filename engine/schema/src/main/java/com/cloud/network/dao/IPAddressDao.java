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
package com.cloud.network.dao;

import java.util.List;

import com.cloud.dc.Vlan.VlanType;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.Ip;

public interface IPAddressDao extends GenericDao<IPAddressVO, Long> {

    IPAddressVO markAsUnavailable(long ipAddressId);

    void unassignIpAddress(long ipAddressId);

    List<IPAddressVO> listByAccount(long accountId);

    List<IPAddressVO> listByVlanId(long vlanId);

    List<IPAddressVO> listByDcIdIpAddress(long dcId, String ipAddress);

    List<IPAddressVO> listByDcId(long dcId);

    List<IPAddressVO> listByAssociatedNetwork(long networkId, Boolean isSourceNat);

    List<IPAddressVO> listStaticNatPublicIps(long networkId);

    int countIPs(long dcId, boolean onlyCountAllocated);

    int countIPs(long dcId, long vlanDbId, boolean onlyCountAllocated);

    int countIPs(long dcId, Long accountId, String vlanId, String vlanGateway, String vlanNetmask);

    long countAllocatedIPsForAccount(long accountId);

    boolean mark(long dcId, Ip ip);

    int countIPsForNetwork(long dcId, boolean onlyCountAllocated, VlanType vlanType);

    IPAddressVO findByAssociatedVmId(long vmId);

    // for vm secondary ips case mapping is  IP1--> vmIp1, IP2-->vmIp2, etc
    // This method is used when one vm is mapped to muliple to public ips
    List<IPAddressVO> findAllByAssociatedVmId(long vmId);

    IPAddressVO findByIpAndSourceNetworkId(long networkId, String ipAddress);

    public IPAddressVO findByIpAndDcId(long dcId, String ipAddress);

    List<IPAddressVO> listByPhysicalNetworkId(long physicalNetworkId);

    List<IPAddressVO> listByAssociatedVpc(long vpcId, Boolean isSourceNat);

    long countFreePublicIPs();

    long countFreeIPsInNetwork(long networkId);

    IPAddressVO findByVmIp(String vmIp);

    IPAddressVO findByAssociatedVmIdAndVmIp(long vmId, String vmIp);

    IPAddressVO findByIpAndNetworkId(long networkId, String ipAddress);

    IPAddressVO findByIpAndVlanId(String ipAddress, long vlanid);

    long countFreeIpsInVlan(long vlanDbId);

    void deletePublicIPRangeExceptAliasIP(long vlanDbId, String aliasIp);

    boolean deletePublicIPRange(long vlanDbId);

    void lockRange(long vlandbId);

    List<IPAddressVO> listByAssociatedVmId(long vmId);

    IPAddressVO findByVmIdAndNetworkId(long networkId, long vmId);
}
