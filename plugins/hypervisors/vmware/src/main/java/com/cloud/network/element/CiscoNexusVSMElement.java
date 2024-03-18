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

package com.cloud.network.element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;


import com.cloud.api.commands.DeleteCiscoNexusVSMCmd;
import com.cloud.api.commands.DisableCiscoNexusVSMCmd;
import com.cloud.api.commands.EnableCiscoNexusVSMCmd;
import com.cloud.api.commands.ListCiscoNexusVSMsCmd;
import com.cloud.api.response.CiscoNexusVSMResponse;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.ClusterVSMMapVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterVSMMapDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.CiscoNexusVSMDevice;
import com.cloud.network.CiscoNexusVSMDeviceManagerImpl;
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.org.Cluster;
import com.cloud.server.ManagementService;
import com.cloud.utils.Pair;
import com.cloud.utils.cisco.n1kv.vsm.NetconfHelper;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

public class CiscoNexusVSMElement extends CiscoNexusVSMDeviceManagerImpl implements CiscoNexusVSMElementService, NetworkElement, Manager {


    @Inject
    CiscoNexusVSMDeviceDao _vsmDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    ClusterVSMMapDao _clusterVSMDao;
    @Inject
    ManagementService _mgr;

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return null;
    }

    @Override
    public Provider getProvider() {
        return null;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DELETE, eventDescription = "deleting VSM", async = true)
    public boolean deleteCiscoNexusVSM(DeleteCiscoNexusVSMCmd cmd) {
        boolean result;
        try {
            result = deleteCiscoNexusVSM(cmd.getCiscoNexusVSMDeviceId());
        } catch (ResourceInUseException e) {
            logger.info("VSM could not be deleted");
            // TODO: Throw a better exception here.
            throw new CloudRuntimeException("Failed to delete specified VSM");
        }
        return result;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_ENABLE, eventDescription = "deleting VSM", async = true)
    public CiscoNexusVSMDeviceVO enableCiscoNexusVSM(EnableCiscoNexusVSMCmd cmd) {
        CiscoNexusVSMDeviceVO result;
        result = enableCiscoNexusVSM(cmd.getCiscoNexusVSMDeviceId());
        return result;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTERNAL_SWITCH_MGMT_DEVICE_DISABLE, eventDescription = "deleting VSM", async = true)
    public CiscoNexusVSMDeviceVO disableCiscoNexusVSM(DisableCiscoNexusVSMCmd cmd) {
        CiscoNexusVSMDeviceVO result;
        result = disableCiscoNexusVSM(cmd.getCiscoNexusVSMDeviceId());
        return result;
    }

    @Override
    public List<CiscoNexusVSMDeviceVO> getCiscoNexusVSMs(ListCiscoNexusVSMsCmd cmd) {
        // If clusterId is defined, then it takes precedence, and we will return
        // the VSM associated with this cluster.

        Long clusterId = cmd.getClusterId();
        Long zoneId = cmd.getZoneId();
        List<CiscoNexusVSMDeviceVO> result = new ArrayList<CiscoNexusVSMDeviceVO>();
        if (clusterId != null && clusterId.longValue() != 0) {
            // Find the VSM associated with this clusterId and return a list.
            CiscoNexusVSMDeviceVO vsm = getCiscoVSMbyClusId(cmd.getClusterId());
            if (vsm == null) {
                throw new CloudRuntimeException("No Cisco VSM associated with specified Cluster Id");
            }
            // Else, add it to a list and return the list.
            result.add(vsm);
            return result;
        }
        // Else if there is only a zoneId defined, get a list of all vmware clusters
        // in the zone, and then for each cluster, pull the VSM and prepare a list.
        if (zoneId != null && zoneId.longValue() != 0) {
            ManagementService ref = _mgr;
            ;
            List<? extends Cluster> clusterList = ref.searchForClusters(zoneId, cmd.getStartIndex(), cmd.getPageSizeVal(), "VMware");

            if (clusterList.size() == 0) {
                throw new CloudRuntimeException("No VMWare clusters found in the specified zone!");
            }
            // Else, iterate through each vmware cluster, pull its VSM if it has one, and add to the list.
            for (Cluster clus : clusterList) {
                CiscoNexusVSMDeviceVO vsm = getCiscoVSMbyClusId(clus.getId());
                if (vsm != null)
                    result.add(vsm);
            }
            return result;
        }

        // If neither is defined, we will simply return the entire list of VSMs
        // configured in the management server.
        // TODO: Is this a safe thing to do? Only ROOT admin can invoke this call.
        result = _vsmDao.listAllVSMs();
        return result;
    }

    @Override
    public CiscoNexusVSMResponse createCiscoNexusVSMResponse(CiscoNexusVSMDevice vsmDeviceVO) {
        CiscoNexusVSMResponse response = new CiscoNexusVSMResponse();
        response.setId(vsmDeviceVO.getUuid());
        response.setMgmtIpAddress(vsmDeviceVO.getipaddr());
        return response;
    }

    @Override
    public CiscoNexusVSMResponse createCiscoNexusVSMDetailedResponse(CiscoNexusVSMDevice vsmDeviceVO) {
        CiscoNexusVSMResponse response = new CiscoNexusVSMResponse();
        response.setId(vsmDeviceVO.getUuid());
        response.setDeviceName(vsmDeviceVO.getvsmName());
        response.setDeviceState(vsmDeviceVO.getvsmDeviceState().toString());
        response.setMgmtIpAddress(vsmDeviceVO.getipaddr());
        // The following values can be null, so check for that.
        if (vsmDeviceVO.getvsmConfigMode() != null)
            response.setVSMConfigMode(vsmDeviceVO.getvsmConfigMode().toString());
        if (vsmDeviceVO.getvsmConfigState() != null)
            response.setVSMConfigState(vsmDeviceVO.getvsmConfigState().toString());
        if (vsmDeviceVO.getvsmDeviceState() != null)
            response.setVSMDeviceState(vsmDeviceVO.getvsmDeviceState().toString());
        response.setVSMCtrlVlanId(vsmDeviceVO.getManagementVlan());
        response.setVSMPktVlanId(vsmDeviceVO.getPacketVlan());
        response.setVSMStorageVlanId(vsmDeviceVO.getStorageVlan());
        return response;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListCiscoNexusVSMsCmd.class);
        cmdList.add(EnableCiscoNexusVSMCmd.class);
        cmdList.add(DisableCiscoNexusVSMCmd.class);
        cmdList.add(DeleteCiscoNexusVSMCmd.class);
        return cmdList;
    }

    @Override
    @DB
    public Pair<Boolean, Long> validateAndAddVsm(final String vsmIp, final String vsmUser, final String vsmPassword, final long clusterId, String clusterName)
        throws ResourceInUseException {
        CiscoNexusVSMDeviceVO vsm = null;
        boolean vsmAdded = false;
        Long vsmId = 0L;
        if (vsmIp != null && vsmUser != null && vsmPassword != null) {
            NetconfHelper netconfClient;
            try {
                netconfClient = new NetconfHelper(vsmIp, vsmUser, vsmPassword);
                netconfClient.disconnect();
            } catch (CloudRuntimeException e) {
                String msg = "Invalid credentials supplied for user " + vsmUser + " for Cisco Nexus 1000v VSM at " + vsmIp;
                logger.error(msg);
                _clusterDao.remove(clusterId);
                throw new CloudRuntimeException(msg);
            }

            // If VSM already exists and is mapped to a cluster, fail this operation.
            vsm = _vsmDao.getVSMbyIpaddress(vsmIp);
            if (vsm != null) {
                List<ClusterVSMMapVO> clusterList = _clusterVSMDao.listByVSMId(vsm.getId());
                if (clusterList != null && !clusterList.isEmpty()) {
                    logger.error("Failed to add cluster: specified Nexus VSM is already associated with another cluster");
                    ResourceInUseException ex =
                        new ResourceInUseException("Failed to add cluster: specified Nexus VSM is already associated with another cluster with specified Id");
                    // get clusterUuid to report error
                    ClusterVO cluster = _clusterDao.findById(clusterList.get(0).getClusterId());
                    ex.addProxyObject(cluster.getUuid());
                    _clusterDao.remove(clusterId);
                    throw ex;
                }
            }
            // persist credentials to database if the VSM entry is not already in the db.
            vsm = Transaction.execute(new TransactionCallback<CiscoNexusVSMDeviceVO>() {
                @Override
                public CiscoNexusVSMDeviceVO doInTransaction(TransactionStatus status) {
                    CiscoNexusVSMDeviceVO vsm = null;
                    if (_vsmDao.getVSMbyIpaddress(vsmIp) == null) {
                        vsm = new CiscoNexusVSMDeviceVO(vsmIp, vsmUser, vsmPassword);
                        _vsmDao.persist(vsm);
                    }
                    // Create a mapping between the cluster and the vsm.
                    vsm = _vsmDao.getVSMbyIpaddress(vsmIp);
                    if (vsm != null) {
                        ClusterVSMMapVO connectorObj = new ClusterVSMMapVO(clusterId, vsm.getId());
                        _clusterVSMDao.persist(connectorObj);
                    }
                    return vsm;
                }
            });

        } else {
            String msg;
            msg = "The global parameter " + Config.VmwareUseNexusVSwitch.toString() + " is set to \"true\". Following mandatory parameters are not specified. ";
            if (vsmIp == null) {
                msg += "vsmipaddress: Management IP address of Cisco Nexus 1000v dvSwitch. ";
            }
            if (vsmUser == null) {
                msg += "vsmusername: Name of a user account with admin privileges over Cisco Nexus 1000v dvSwitch. ";
            }
            if (vsmPassword == null) {
                if (vsmUser != null) {
                    msg += "vsmpassword: Password of user account " + vsmUser + ". ";
                } else {
                    msg += "vsmpassword: Password of user account with admin privileges over Cisco Nexus 1000v dvSwitch. ";
                }
            }
            logger.error(msg);
            // Cleaning up the cluster record as addCluster operation failed because of invalid credentials of Nexus dvSwitch.
            _clusterDao.remove(clusterId);
            throw new CloudRuntimeException(msg);
        }
        if (vsm != null) {
            vsmAdded = true;
            vsmId = vsm.getId();
        }
        return new Pair<Boolean, Long>(vsmAdded, vsmId);
    }
}
