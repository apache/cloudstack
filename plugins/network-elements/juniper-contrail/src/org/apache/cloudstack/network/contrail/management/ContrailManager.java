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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;
import java.util.List;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.FloatingIp;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.apache.cloudstack.network.contrail.model.ModelController;
import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;

import com.cloud.domain.DomainVO;
import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.projects.ProjectVO;

public interface ContrailManager {
    public static final String offeringName = "Juniper Contrail offering";
    public static final String offeringDisplayText = "Juniper Contrail network offering";
    public static final int DB_SYNC_INTERVAL_DEFAULT = 600000;
    public static final String VNC_ROOT_DOMAIN = "default-domain";
    public static final String VNC_DEFAULT_PROJECT = "default-project";
    public static final String managementNetworkName = "ip-fabric";

    public NetworkOffering getOffering();

    public void syncNetworkDB(short syncMode) throws IOException;

    public boolean isManagedPhysicalNetwork(Network network);

    /**
     * Lookup the virtual network that implements the CloudStack network object.
     * @param net_id internal identifier of the NetworkVO object.
     * @return the uuid of the virtual network that corresponds to the
     * specified CloudStack network.
     */
    public String findVirtualNetworkId(Network net) throws IOException;

    public void findInfrastructureNetworks(PhysicalNetworkVO phys, List<NetworkVO> dbList);

    public String getPhysicalNetworkName(PhysicalNetworkVO phys_net);

    public String getCanonicalName(Network net);

    public String getDomainCanonicalName(DomainVO domain);

    public String getProjectCanonicalName(ProjectVO project);

    public String getFQN(Network net);

    public String getDomainName(long domainId);

    public String getProjectName(long accountId);

    public String getDefaultPublicNetworkFQN();

    public String getProjectId(long domainId, long accountId) throws IOException;

    public net.juniper.contrail.api.types.Project getVncProject(long domainId, long accountId) throws IOException;

    public boolean isSystemRootDomain(net.juniper.contrail.api.types.Domain vnc);

    public boolean isSystemRootDomain(DomainVO domain);

    public boolean isSystemDefaultProject(net.juniper.contrail.api.types.Project project);

    public boolean isSystemDefaultProject(ProjectVO project);

    public boolean isSystemDefaultNetwork(VirtualNetwork vnet);

    public boolean isSystemDefaultNetwork(NetworkVO dbNet);

    public String getVifNameByVmName(String vm_name, Integer device_id);

    public String getVifNameByVmUuid(String vm_uuid, Integer device_id);

    public ApiConnector getApiConnector();

    public ModelDatabase getDatabase();

    public ModelController getModelController();

    public List<NetworkVO> findJuniperManagedNetworks(List<TrafficType> types);

    public List<IPAddressVO> findJuniperManagedPublicIps();

    public VirtualNetwork findDefaultVirtualNetwork(TrafficType trafficType) throws IOException;

    public List<FloatingIp> getFloatingIps();

    public VirtualNetworkModel lookupPublicNetworkModel();

    public void createPublicNetworks();

    public boolean createFloatingIp(PublicIpAddress ip);

    public boolean deleteFloatingIp(PublicIpAddress ip);
}
