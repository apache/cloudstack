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
package com.cloud.network;

import java.util.List;

import javax.inject.Inject;


import com.cloud.agent.api.StartupCommand;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.ClusterVSMMapVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterVSMMapDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.network.dao.PortProfileDao;
import com.cloud.resource.ResourceManager;
import com.cloud.utils.cisco.n1kv.vsm.NetconfHelper;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;

public abstract class CiscoNexusVSMDeviceManagerImpl extends AdapterBase {

    @Inject
    CiscoNexusVSMDeviceDao _ciscoNexusVSMDeviceDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    ClusterVSMMapDao _clusterVSMDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    VmwareManager _vmwareMgr;
    @Inject
    ClusterDetailsDao _clusterDetailsDao;
    @Inject
    HostDetailsDao _hostDetailDao;
    @Inject
    PortProfileDao _ppDao;


    @DB
    //public CiscoNexusVSMDeviceVO addCiscoNexusVSM(long clusterId, String ipaddress, String username, String password, ServerResource resource, String vsmName) {
        public
        CiscoNexusVSMDeviceVO addCiscoNexusVSM(long clusterId, String ipaddress, String username, String password, String vCenterIpaddr, String vCenterDcName) {

        // In this function, we associate this VSM with each host
        // in the clusterId specified.

        // First check if the cluster is of type vmware. If not,
        // throw an exception. VSMs are tightly integrated with vmware clusters.

        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null) {
            throw new InvalidParameterValueException("Cluster with specified ID not found!");
        }
        if (cluster.getHypervisorType() != HypervisorType.VMware) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Cluster with specified id is not a VMWare hypervisor cluster");
            throw ex;
        }

        // Next, check if the cluster already has a VSM associated with it.
        // If so, throw an exception disallowing this operation. The user must first
        // delete the current VSM and then only attempt to add the new one.

        if (_clusterVSMDao.findByClusterId(clusterId) != null) {
            // We can't have two VSMs for the same cluster. Throw exception.
            throw new InvalidParameterValueException("Cluster with specified id already has a VSM tied to it. Please remove that first and retry the operation.");
        }

        // TODO: Confirm whether we should be checking for VSM reachability here.

        // Next, check if this VSM is reachable. Use the XML-RPC VSM API Java bindings to talk to
        // the VSM.
        //NetconfHelper (String ip, String username, String password)

        NetconfHelper netconfClient;
        try {
            netconfClient = new NetconfHelper(ipaddress, username, password);
        } catch (CloudRuntimeException e) {
            String msg = "Failed to connect to Nexus VSM " + ipaddress + " with credentials of user " + username;
            logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        // Disconnect from the VSM. A VSM has a default of 8 maximum parallel connections that it allows.
        netconfClient.disconnect();

        // Now, go ahead and associate the cluster with this VSM.
        // First, check if VSM already exists in the table "virtual_supervisor_module".
        // If it's not there already, create it.
        // If it's there already, return success.

        // TODO - Right now, we only check if the ipaddress matches for both requests.
        // We must really check whether every field of the VSM matches. Anyway, the
        // advantage of our approach for now is that existing infrastructure using
        // the existing VSM won't be affected if the new request to add the VSM
        // assumed different information on the VSM (mgmt vlan, username, password etc).
        CiscoNexusVSMDeviceVO VSMObj;
        try {
            VSMObj = _ciscoNexusVSMDeviceDao.getVSMbyIpaddress(ipaddress);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        if (VSMObj == null) {
            // Create the VSM record. For now, we aren't using the vsmName field.
            VSMObj = new CiscoNexusVSMDeviceVO(ipaddress, username, password);
            _ciscoNexusVSMDeviceDao.persist(VSMObj);
        }

        // At this stage, we have a VSM record for sure. Connect the VSM to the cluster Id.
        long vsmId = _ciscoNexusVSMDeviceDao.getVSMbyIpaddress(ipaddress).getId();
        ClusterVSMMapVO connectorObj = new ClusterVSMMapVO(clusterId, vsmId);
        _clusterVSMDao.persist(connectorObj);

        // Now, get a list of all the ESXi servers in this cluster.
        // This is effectively a select * from host where cluster_id=clusterId;
        // All ESXi servers are stored in the host table, and their resource
        // type is vmwareresource.

        //List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(clusterId);

        //TODO: Activate the code below if we make the Nexus VSM a separate resource.
        // Iterate through each of the hosts in this list. Each host has a host id.
        // Given this host id, we can reconfigure the in-memory resource representing
        // the host via the agent manager. Thus we inject VSM related information
        // into each host's resource. Also, we first configure each resource's
        // entries in the database to contain this VSM information before the injection.

        //for (HostVO host : hosts) {
        // Create a host details VO object and write it out for this hostid.
        //Long hostid = new Long(vsmId);
        //DetailVO vsmDetail = new DetailVO(host.getId(), "vsmId", hostid.toString());
        //Transaction tx = Transaction.currentTxn();
        //try {
        //tx.start();
        //_hostDetailDao.persist(vsmDetail);
        //tx.commit();
        //} catch (Exception e) {
        //tx.rollback();
        //throw new CloudRuntimeException(e.getMessage());
        //}
        //}
        // Reconfigure the resource.
        //Map hostDetails = new HashMap<String, String>();
        //hostDetails.put(ApiConstants.ID, vsmId);
        //hostDetails.put(ApiConstants.IP_ADDRESS, ipaddress);
        //hostDetails.put(ApiConstants.USERNAME, username);
        //hostDetails.put(ApiConstants.PASSWORD, password);
        //_agentMrg.send(host.getId(), )

        return VSMObj;

    }

    @DB
    public boolean deleteCiscoNexusVSM(final long vsmId) throws ResourceInUseException {
        CiscoNexusVSMDeviceVO cisconexusvsm = _ciscoNexusVSMDeviceDao.findById(vsmId);
        if (cisconexusvsm == null) {
            // This entry is already not present. Return success.
            return true;
        }

        // First, check whether this VSM is part of any non-empty cluster.
        // Search ClusterVSMMap's table for a list of clusters using this vsmId.

        List<ClusterVSMMapVO> clusterList = _clusterVSMDao.listByVSMId(vsmId);

        if (clusterList != null) {
            for (ClusterVSMMapVO record : clusterList) {
                // If this cluster id has any hosts in it, fail this operation.
                Long clusterId = record.getClusterId();
                List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(clusterId);
                if (hosts != null && hosts.size() > 0) {
                    for (Host host : hosts) {
                        if (host.getType() == Host.Type.Routing) {
                            logger.info("Non-empty cluster with id" + clusterId + "still has a host that uses this VSM. Please empty the cluster first");
                            throw new ResourceInUseException("Non-empty cluster with id" + clusterId +
                                "still has a host that uses this VSM. Please empty the cluster first");
                        }
                    }
                }
            }
        }

        // Iterate through the cluster list again, this time, delete the VSM.
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                // Remove the VSM entry in CiscoNexusVSMDeviceVO's table.
                _ciscoNexusVSMDeviceDao.remove(vsmId);
                // Remove the current record as well from ClusterVSMMapVO's table.
                _clusterVSMDao.removeByVsmId(vsmId);
            }
        });

        return true;
    }

    @DB
    public CiscoNexusVSMDeviceVO enableCiscoNexusVSM(long vsmId) {
        CiscoNexusVSMDeviceVO cisconexusvsm = _ciscoNexusVSMDeviceDao.findById(vsmId);
        if (cisconexusvsm == null) {
            throw new InvalidParameterValueException("Invalid vsm Id specified");
        }
        // Else, check if this db record shows that this VSM is enabled or not.
        if (cisconexusvsm.getvsmDeviceState() == CiscoNexusVSMDeviceVO.VSMDeviceState.Disabled) {
            // it's currently disabled. So change it to enabled and write it out to the db.
            cisconexusvsm.setVsmDeviceState(CiscoNexusVSMDeviceVO.VSMDeviceState.Enabled);
            _ciscoNexusVSMDeviceDao.persist(cisconexusvsm);
        }

        return cisconexusvsm;
    }

    @DB
    public CiscoNexusVSMDeviceVO disableCiscoNexusVSM(long vsmId) {
        CiscoNexusVSMDeviceVO cisconexusvsm = _ciscoNexusVSMDeviceDao.findById(vsmId);
        if (cisconexusvsm == null) {
            throw new InvalidParameterValueException("Invalid vsm Id specified");
        }
        // Else, check if this db record shows that this VSM is enabled or not.
        if (cisconexusvsm.getvsmDeviceState() == CiscoNexusVSMDeviceVO.VSMDeviceState.Enabled) {
            // it's currently disabled. So change it to enabled and write it out to the db.
            cisconexusvsm.setVsmDeviceState(CiscoNexusVSMDeviceVO.VSMDeviceState.Disabled);
            _ciscoNexusVSMDeviceDao.persist(cisconexusvsm);
        }

        return cisconexusvsm;
    }

    @DB
    public CiscoNexusVSMDeviceVO getCiscoVSMbyVSMId(long vsmId) {
        return _ciscoNexusVSMDeviceDao.findById(vsmId);
    }

    @DB
    public CiscoNexusVSMDeviceVO getCiscoVSMbyClusId(long clusterId) {
        ClusterVSMMapVO mapVO = _clusterVSMDao.findByClusterId(clusterId);
        if (mapVO == null) {
            logger.info("Couldn't find a VSM associated with the specified cluster Id");
            return null;
        }
        // Else, pull out the VSM associated with the VSM id in mapVO.
        CiscoNexusVSMDeviceVO result = _ciscoNexusVSMDeviceDao.findById(mapVO.getVsmId());
        return result;
    }

    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }
}
