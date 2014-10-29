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

import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.utils.db.GenericDao;

public interface CiscoNexusVSMDeviceDao extends GenericDao<CiscoNexusVSMDeviceVO, Long> {

    /**
     * Return a Cisco Nexus VSM record given its switch domain Id.
     * @param Cisco Nexus VSM Switch Domain Id
     * @return CiscoNexusVSMDeviceVO for the VSM having the specified switch domain Id.
     */
    CiscoNexusVSMDeviceVO getVSMbyDomainId(long domId);

    /**
     * Return a Cisco Nexus VSM VO (db record) given its name.
     * @param vsmName
     */
    CiscoNexusVSMDeviceVO getVSMbyName(String vsmName);

    /**
     * Return a Cisco Nexus VSM VO (db record) given its ipaddress.
     * @param vsmIpaddr
     */
    CiscoNexusVSMDeviceVO getVSMbyIpaddress(String ipaddress);

    /**
     * Return a list of VSM devices that use the same VLAN for no matter what interface. Unlikely, but oh well.
     * @param vlanId
     *     - Needs to filter results by the invoker's account Id. So we may end up adding another param
     *    or may query it in the function.
     * @return
     */
    List<CiscoNexusVSMDeviceVO> listByVlanId(int vlanId);

    /**
     * Return a list of VSM devices that use the same VLAN for their mgmt interface. Again, unlikely, but we'll just keep it around.
     * @param vlanId
     * @return
     */
    List<CiscoNexusVSMDeviceVO> listByMgmtVlan(int vlanId);

    /**
     * Lists all configured VSMs on the management server.
     * @return
     */
    List<CiscoNexusVSMDeviceVO> listAllVSMs();

    /**
     * Below is a big list of other functions that we may need, but will declare/define/implement once we implement
     * the functions above. Pasting those below to not lose track of them.
     *
     *      ListbyZoneId()
        - Lists all VSMs in the specified zone.

    ListbyAccountId()
        - Lists all VSMs owned by the specified Account.

    ListbyStorageVLAN(vlanId)
        - Lists all VSMs whose storage VLAN matches the specified VLAN.
            - Filters results by the invoker's account Id.

    ListbyControlVLAN(vlanId)
        - Lists all VSMs whose control VLAN matches the specified VLAN.
            - Filters results by the invoker's account Id.

    ListbyPacketVLAN(vlanId)
        - Lists all VSMs whose Packet VLAN matches the specified VLAN.
            - Filters results by the invoker's account Id.

    ListbyConfigMode(mode)
        - Lists all VSMs which are currently configured in the specified mode (standalone/HA).
            - Filters results by the invoker's account Id.

    ListbyConfigState(configState)
        - Lists all VSMs which are currently configured in the specified state (primary/standby).
            - Filters results by the invoker's account Id.

    ListbyDeviceState(deviceState)
        - Lists all VSMs which are currently in the specified device state (enabled/disabled).
            - Filters results by the invoker's account Id.


    getBySwitchDomainId(domId)
        - Retrieves the VSM with the specified switch domain Id. Each VSM has a unique switch domain Id, just like a real physical switch would.
            - Filters results by invoker's account id.


    getbySwitchName(vsmName)
        - Retrieves the VSM's VO object by the specified vsmName.

     */
}
