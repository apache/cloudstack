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

package org.apache.cloudstack.api.command.admin.cluster;

import java.util.ArrayList;
import java.util.List;

import com.cloud.cpu.CPU;
import org.apache.cloudstack.api.ApiCommandResourceType;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.DiscoveryException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.org.Cluster;
import com.cloud.user.Account;

@APICommand(name = "addCluster", description = "Adds a new cluster", responseObject = ClusterResponse.class,
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class AddClusterCmd extends BaseCmd {


    @Parameter(name = ApiConstants.CLUSTER_NAME, type = CommandType.STRING, required = true, description = "the cluster name")
    private String clusterName;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = false, description = "the password for the host")
    private String password;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, required = true, description = "the Pod ID for the host")
    private Long podId;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = false, description = "the URL")
    private String url;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = false, description = "the username for the cluster")
    private String username;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the Zone ID for the cluster")
    private Long zoneId;

    @Parameter(name = ApiConstants.HYPERVISOR,
               type = CommandType.STRING,
               required = true,
               description = "hypervisor type of the cluster: XenServer,KVM,VMware,Hyperv,BareMetal,Simulator,Ovm3")
    private String hypervisor;

    @Parameter(name = ApiConstants.ARCH, type = CommandType.STRING,
            description = "the CPU arch of the cluster. Valid options are: x86_64, aarch64",
            since = "4.20")
    private String arch;

    @Parameter(name = ApiConstants.CLUSTER_TYPE, type = CommandType.STRING, required = true, description = "type of the cluster: CloudManaged, ExternalManaged")
    private String clusterType;

    @Parameter(name = ApiConstants.ALLOCATION_STATE, type = CommandType.STRING, description = "Allocation state of this cluster for allocation of new resources")
    private String allocationState;

    @Parameter(name = ApiConstants.VSM_USERNAME, type = CommandType.STRING, required = false, description = "the username for the VSM associated with this cluster")
    private String vsmusername;

    @Parameter(name = ApiConstants.VSM_PASSWORD, type = CommandType.STRING, required = false, description = "the password for the VSM associated with this cluster")
    private String vsmpassword;

    @Parameter(name = ApiConstants.VSM_IPADDRESS, type = CommandType.STRING, required = false, description = "the ipaddress of the VSM associated with this cluster")
    private String vsmipaddress;

    @Parameter(name = ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC,
               type = CommandType.STRING,
               required = false,
               description = "Type of virtual switch used for guest traffic in the cluster. Allowed values are, vmwaresvs (for VMware standard vSwitch) and vmwaredvs (for VMware distributed vSwitch)")
    private String vSwitchTypeGuestTraffic;

    @Parameter(name = ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC,
               type = CommandType.STRING,
               required = false,
               description = "Type of virtual switch used for public traffic in the cluster. Allowed values are, vmwaresvs (for VMware standard vSwitch) and vmwaredvs (for VMware distributed vSwitch)")
    private String vSwitchTypePublicTraffic;

    @Parameter(name = ApiConstants.VSWITCH_NAME_GUEST_TRAFFIC,
               type = CommandType.STRING,
               required = false,
               description = "Name of virtual switch used for guest traffic in the cluster. This would override zone wide traffic label setting.")
    private String vSwitchNameGuestTraffic;

    @Parameter(name = ApiConstants.VSWITCH_NAME_PUBLIC_TRAFFIC,
               type = CommandType.STRING,
               required = false,
               description = "Name of virtual switch used for public traffic in the cluster.  This would override zone wide traffic label setting.")
    private String vSwitchNamePublicTraffic;

    @Parameter(name = ApiConstants.OVM3_POOL, type = CommandType.STRING, required = false, description = "Ovm3 native pooling enabled for cluster")
    private String ovm3pool;
    @Parameter(name = ApiConstants.OVM3_CLUSTER, type = CommandType.STRING, required = false, description = "Ovm3 native OCFS2 clustering enabled for cluster")
    private String ovm3cluster;
    @Parameter(name = ApiConstants.OVM3_VIP, type = CommandType.STRING, required = false,  description = "Ovm3 vip to use for pool (and cluster)")
    private String ovm3vip;
    public String getOvm3Pool() {
         return ovm3pool;
    }
    public String getOvm3Cluster() {
        return ovm3cluster;
    }
    public String getOvm3Vip() {
        return ovm3vip;
    }

    public String getVSwitchTypeGuestTraffic() {
        return vSwitchTypeGuestTraffic;
    }

    public String getVSwitchTypePublicTraffic() {
        return vSwitchTypePublicTraffic;
    }

    public String getVSwitchNameGuestTraffic() {
        return vSwitchNameGuestTraffic;
    }

    public String getVSwitchNamePublicTraffic() {
        return vSwitchNamePublicTraffic;
    }

    public String getVSMIpaddress() {
        return vsmipaddress;
    }

    public String getVSMPassword() {
        return vsmpassword;
    }

    public String getVSMUsername() {
        return vsmusername;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getPassword() {
        return password;
    }

    public Long getPodId() {
        return podId;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String type) {
        this.clusterType = type;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public String getAllocationState() {
        return allocationState;
    }

    public void setAllocationState(String allocationState) {
        this.allocationState = allocationState;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Cluster;
    }

    public CPU.CPUArch getArch() {
        return CPU.CPUArch.fromType(arch);
    }

    @Override
    public void execute() {
        try {
            List<? extends Cluster> result = _resourceService.discoverCluster(this);
            ListResponse<ClusterResponse> response = new ListResponse<ClusterResponse>();
            List<ClusterResponse> clusterResponses = new ArrayList<ClusterResponse>();
            if (result != null && result.size() > 0) {
                for (Cluster cluster : result) {
                    ClusterResponse clusterResponse = _responseGenerator.createClusterResponse(cluster, false);
                    clusterResponses.add(clusterResponse);
                }
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add cluster");
            }

            response.setResponses(clusterResponses);
            response.setResponseName(getCommandName());

            this.setResponseObject(response);
        } catch (DiscoveryException ex) {
            logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (ResourceInUseException ex) {
            logger.warn("Exception: ", ex);
            ServerApiException e = new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
            for (String proxyObj : ex.getIdProxyList()) {
                e.addProxyObject(proxyObj);
            }
            throw e;
        }
    }
}
