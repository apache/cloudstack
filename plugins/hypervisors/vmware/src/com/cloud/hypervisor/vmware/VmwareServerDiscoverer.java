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
package com.cloud.hypervisor.vmware;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.vmware.vim25.ManagedObjectReference;

import org.apache.cloudstack.api.ApiConstants;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterPhysicalNetworkTrafficInfoDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;
import com.cloud.hypervisor.vmware.manager.VmwareManager;
import com.cloud.hypervisor.vmware.mo.ClusterMO;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.VirtualSwitchType;
import com.cloud.hypervisor.vmware.resource.VmwareContextFactory;
import com.cloud.hypervisor.vmware.resource.VmwareResource;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.VmwareTrafficLabel;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.element.CiscoNexusVSMElement;
import com.cloud.network.element.NetworkElement;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.UriUtils;

public class VmwareServerDiscoverer extends DiscovererBase implements Discoverer, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(VmwareServerDiscoverer.class);
    private static final int MAX_FIELDS_VMWARE_LABEL_AT_CLUSTER = 1;

    @Inject
    VmwareManager _vmwareMgr;
    @Inject
    AlertManager _alertMgr;
    @Inject
    VMTemplateDao _tmpltDao;
    @Inject
    ClusterDetailsDao _clusterDetailsDao;
    @Inject
    ClusterPhysicalNetworkTrafficInfoDao _clusterPhysicalNetworkTrafficInfoDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _physicalNetworkTrafficTypeDao;
    @Inject
    CiscoNexusVSMDeviceDao _nexusDao;
    @Inject
    NetworkModel _netmgr;
    @Inject
    HypervisorCapabilitiesDao _hvCapabilitiesDao;
    @Inject
    VmwareDatacenterZoneMapDao _vmwareDcZoneMapDao;
    @Inject
    VmwareDatacenterDao _vmwareDcDao;

    protected Map<String, String> _urlParams;
    protected boolean useDVS = false;
    protected boolean nexusDVS = false;
    CiscoNexusVSMElement _nexusElement;
    List<NetworkElement> networkElements;

    public VmwareServerDiscoverer() {
        s_logger.info("VmwareServerDiscoverer is constructed");
    }

    @Override
    public Map<? extends ServerResource, Map<String, String>>
    find(long dcId, Long podId, Long clusterId, URI url, String username, String password, List<String> hostTags) throws DiscoveryException {

        if (s_logger.isInfoEnabled())
            s_logger.info("Discover host. dc: " + dcId + ", pod: " + podId + ", cluster: " + clusterId + ", uri host: " + url.getHost());

        if (podId == null) {
            if (s_logger.isInfoEnabled())
                s_logger.info("No pod is assigned, assuming that it is not for vmware and skip it to next discoverer");
            return null;
        }
        boolean failureInClusterDiscovery = true;
        String vsmIp = "";
        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null || cluster.getHypervisorType() != HypervisorType.VMware) {
            if (s_logger.isInfoEnabled())
                s_logger.info("invalid cluster id or cluster is not for VMware hypervisors");
            return null;
        }

        Map<String, String> clusterDetails = _clusterDetailsDao.findDetails(clusterId);
        boolean legacyZone = _vmwareMgr.isLegacyZone(dcId);
        boolean usernameNotProvided = (username == null || username.isEmpty());
        boolean passwordNotProvided = (password == null || password.isEmpty());
        //Check if NOT a legacy zone.
        if (!legacyZone) {
            // Retrieve VMware DC associated with specified zone
            VmwareDatacenterVO vmwareDc = fetchVmwareDatacenterByZone(dcId);
            // Ensure username & password provided.
            // If either or both not provided, try to retrieve & use the credentials from database, which are provided earlier while adding VMware DC to zone.
            if (usernameNotProvided || passwordNotProvided) {
                // Retrieve credentials associated with VMware DC
                s_logger.info("Username and/or Password not provided while adding cluster to cloudstack zone. "
                        + "Hence using both username & password provided while adding VMware DC to CloudStack zone.");
                username = vmwareDc.getUser();
                password = vmwareDc.getPassword();
                clusterDetails.put("username", username);
                clusterDetails.put("password", password);
                _clusterDetailsDao.persist(clusterId, clusterDetails);
            }
            String updatedInventoryPath = validateCluster(url, vmwareDc);
            try {
                if (!URLDecoder.decode(url.getPath(), "UTF-8").equals(updatedInventoryPath)) {
                    // If url from API doesn't specify DC then update url in database with DC associated with this zone.
                    clusterDetails.put("url", url.getScheme() + "://" + url.getHost() + updatedInventoryPath);
                    _clusterDetailsDao.persist(clusterId, clusterDetails);
                }
            } catch(UnsupportedEncodingException e) {
                throw new DiscoveredWithErrorException("Unable to decode URL path, URL path : " + url.getPath(), e);
            }
        } else {
            // For legacy zones insist on the old model of asking for credentials for each cluster being added.
            if (usernameNotProvided) {
                if (passwordNotProvided) {
                    throw new InvalidParameterValueException("Please provide username & password to add this cluster to zone");
                } else {
                    throw new InvalidParameterValueException("Please provide username to add this cluster to zone");
                }
            } else if (passwordNotProvided) {
                throw new InvalidParameterValueException("Please provide password to add this cluster to zone");
            }
        }

        List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(clusterId);
        if (hosts != null && hosts.size() > 0) {
            int maxHostsPerCluster = _hvCapabilitiesDao.getMaxHostsPerCluster(hosts.get(0).getHypervisorType(), hosts.get(0).getHypervisorVersion());
            if (hosts.size() >= maxHostsPerCluster) {
                String msg = "VMware cluster " + cluster.getName() + " is too big to add new host, current size: " + hosts.size() + ", max. size: " + maxHostsPerCluster;
                s_logger.error(msg);
                throw new DiscoveredWithErrorException(msg);
            }
        }

        String privateTrafficLabel = null;
        Map<String, String> vsmCredentials = null;

        DataCenterVO zone = _dcDao.findById(dcId);
        NetworkType zoneType = zone.getNetworkType();
        _readGlobalConfigParameters();

        // Zone level vSwitch type depends on zone level traffic labels
        //
        // User can override Zone wide vswitch type and name (for public and guest) by providing following optional parameters in addClusterCmd
        // param "guestvswitchtype" with valid values vmwaredvs, vmwaresvs, nexusdvs
        // param "publicvswitchtype" with valid values vmwaredvs, vmwaresvs, nexusdvs
        // param "guestvswitchname" with valid name for the vswitch type for the first guest traffic
        // param "publicvswitchname" with valid name for the vswitch type for the first public traffic
        // param "physicalnetworktrafficlabels" - Map of physical network traffic id (of guest and public traffic in the zone) and vmware network label(valid vswitch name) for the vswitch type
        // param "physicalnetworktrafficlabels" would precede over the params "guestvswitchname" and "publicvswitchname" for the first guest and public traffic respectively
        //
        // Format of zone level network label is <VSWITCH-NAME>,<VLANID>,<VSWITCH-TYPE>
        // If a field <VLANID> OR <VSWITCH-TYPE> is not present leave it empty.
        // Ex: 1) vswitch0
        // 2) dvswitch0,200,vmwaredvs
        // 3) nexusepp0,300,nexusdvs
        // 4) vswitch1,400,vmwaresvs
        // 5) vswitch0
        // default vswitchtype is 'vmwaresvs'.
        // <VSWITCH-TYPE> 'vmwaresvs' is for vmware standard vswitch
        // <VSWITCH-TYPE> 'vmwaredvs' is for vmware distributed virtual switch
        // <VSWITCH-TYPE> 'nexusdvs' is for cisco nexus distributed virtual switch
        //
        // Format of cluster level network label is <VSWITCH-NAME>

        // Parse url parameters for vswitch type and name specified at cluster level
        boolean vswitchTypeAtClusterLevelUpdated = false;
        String guestVswitchTypeAtClusterLevel = _urlParams.get(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC);
        validateVswitchType(guestVswitchTypeAtClusterLevel);
        if (guestVswitchTypeAtClusterLevel != null) {
            clusterDetails.put(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC, guestVswitchTypeAtClusterLevel);
            vswitchTypeAtClusterLevelUpdated = true;
        } else {
            guestVswitchTypeAtClusterLevel = clusterDetails.get(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC);
        }

        String publicVswitchTypeAtClusterLevel = _urlParams.get(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC);
        validateVswitchType(publicVswitchTypeAtClusterLevel);
        if (publicVswitchTypeAtClusterLevel != null) {
            clusterDetails.put(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC, publicVswitchTypeAtClusterLevel);
            vswitchTypeAtClusterLevelUpdated = true;
        } else {
            publicVswitchTypeAtClusterLevel = clusterDetails.get(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC);
        }

        if (vswitchTypeAtClusterLevelUpdated) {
            // Save cluster level override configuration to cluster details
            _clusterDetailsDao.persist(clusterId, clusterDetails);
        }

        String guestVswitchNameAtClusterLevel = _urlParams.get(ApiConstants.VSWITCH_NAME_GUEST_TRAFFIC);
        validateVswitchNameAtCluster(guestVswitchTypeAtClusterLevel);

        String publicVswitchNameAtClusterLevel = _urlParams.get(ApiConstants.VSWITCH_NAME_PUBLIC_TRAFFIC);
        validateVswitchNameAtCluster(guestVswitchTypeAtClusterLevel);

        // Cluster level traffic info with physical network traffic id and network label (vswitch name)
        Map<Long, String> guestTrafficInfoAtClusterLevel = getGuestTrafficInfoForCluster(clusterId);
        if ((guestVswitchNameAtClusterLevel != null || (guestTrafficInfoAtClusterLevel != null && !guestTrafficInfoAtClusterLevel.isEmpty()))
                && guestVswitchTypeAtClusterLevel == null) {
            throw new InvalidParameterValueException("Guest vswitch type at cluster is required for the specifed physical network traffic label(s)");
        }

        Map<Long, String> publicTrafficInfoAtClusterLevel = getPublicTrafficInfoForCluster(clusterId);
        if ((publicVswitchNameAtClusterLevel != null || (publicTrafficInfoAtClusterLevel != null && !publicTrafficInfoAtClusterLevel.isEmpty()))
                && publicVswitchTypeAtClusterLevel == null) {
            throw new InvalidParameterValueException("Public vswitch type at cluster is required for the specifed physical network traffic label(s)");
        }

        // Zone level traffic info with physical network traffic id and network label
        Map<Long, String> guestTrafficInfoAtZoneLevel = _netmgr.getGuestTrafficInfo(dcId, HypervisorType.VMware);
        Map<Long, String> publicTrafficInfoAtZoneLevel = _netmgr.getPublicTrafficInfo(dcId, HypervisorType.VMware);

        Map<Long, VmwareTrafficLabel> guestTrafficLabelInfo = null;
        Map<Long, VmwareTrafficLabel> publicTrafficLabelInfo = null;

        // When "guestvswitchname" param is defined, add the vswitch name to the first guest traffic when the same is not defined in "physicalnetworktrafficlabels" param
        if (guestVswitchNameAtClusterLevel != null) {
            // Get zone wide traffic label for first guest traffic
            Pair<Long, String> firstGuestTrafficInfoAtZone = _netmgr.getDefaultGuestTrafficInfo(dcId, HypervisorType.VMware); //Returns first guest traffic found in the physical networks on that zone
            if (firstGuestTrafficInfoAtZone != null) {
                s_logger.debug("Zone level first guest traffic id: " + firstGuestTrafficInfoAtZone.first() + " and label: " + firstGuestTrafficInfoAtZone.second());

                long firstGuestPhysicalNetworkTrafficIdAtZoneLevel = firstGuestTrafficInfoAtZone.first();
                if (guestTrafficInfoAtClusterLevel == null || !guestTrafficInfoAtClusterLevel.containsKey(firstGuestPhysicalNetworkTrafficIdAtZoneLevel)) {
                    _clusterPhysicalNetworkTrafficInfoDao.persist(clusterId, firstGuestPhysicalNetworkTrafficIdAtZoneLevel, guestVswitchNameAtClusterLevel);
                    guestTrafficInfoAtClusterLevel = getGuestTrafficInfoForCluster(clusterId); // Update cluster level traffic info for guest traffic
                }
            }
        }

        // Process guest traffic label information provided at zone level and cluster level
        guestTrafficLabelInfo = getTrafficInfo(TrafficType.Guest, guestTrafficInfoAtZoneLevel, guestTrafficInfoAtClusterLevel, guestVswitchTypeAtClusterLevel);

        if (zoneType == NetworkType.Advanced) {
            // When "publicvswitchname" param is defined, add the vswitch name to the first public traffic when the same is not defined in "physicalnetworktrafficlabels" param
            if (publicVswitchNameAtClusterLevel != null) {
                // Get zone wide traffic label for first public traffic
                Pair<Long, String> firstPublicTrafficInfoAtZone = _netmgr.getDefaultPublicTrafficInfo(dcId, HypervisorType.VMware); ////Returns first public traffic found in the physical networks on that zone
                if (firstPublicTrafficInfoAtZone != null) {
                    s_logger.debug("Zone level first public traffic id: " + firstPublicTrafficInfoAtZone.first() + " and label: " + firstPublicTrafficInfoAtZone.second());

                    long firstPublicPhysicalNetworkTrafficIdAtZoneLevel = firstPublicTrafficInfoAtZone.first();
                    if (publicTrafficInfoAtClusterLevel == null || !publicTrafficInfoAtClusterLevel.containsKey(firstPublicPhysicalNetworkTrafficIdAtZoneLevel)) {
                        _clusterPhysicalNetworkTrafficInfoDao.persist(clusterId, firstPublicPhysicalNetworkTrafficIdAtZoneLevel, publicVswitchNameAtClusterLevel);
                        publicTrafficInfoAtClusterLevel = getPublicTrafficInfoForCluster(clusterId); // Update cluster level traffic info for public traffic
                    }
                }
            }

            // Process public traffic label information provided at zone level and cloud level
            publicTrafficLabelInfo = getTrafficInfo(TrafficType.Public, publicTrafficInfoAtZoneLevel, publicTrafficInfoAtClusterLevel, publicVswitchTypeAtClusterLevel);

            // Configuration Check: A physical network cannot be shared by different types of virtual switches.
            //
            // Check if different vswitch types are chosen for same physical network
            // 1. Get physical network for guest traffic - multiple networks
            // 2. Get physical network for public traffic - single network
            // See if 2 is in 1
            //  if no - pass
            //  if yes - compare publicTrafficLabelObj.getVirtualSwitchType() == guestTrafficLabelObj.getVirtualSwitchType()
            //      true  - pass
            //      false - throw exception - fail cluster add operation

            List<? extends PhysicalNetwork> pNetworkListGuestTraffic = _netmgr.getPhysicalNtwksSupportingTrafficType(dcId, TrafficType.Guest);
            List<? extends PhysicalNetwork> pNetworkListPublicTraffic = _netmgr.getPhysicalNtwksSupportingTrafficType(dcId, TrafficType.Public);
            // Public network would be on single physical network hence getting first object of the list would suffice.
            PhysicalNetwork pNetworkPublic = pNetworkListPublicTraffic.get(0);
            if (pNetworkListGuestTraffic.contains(pNetworkPublic)) {
                VmwareTrafficLabel publicTrafficLabelObj = (publicTrafficLabelInfo != null) ? (VmwareTrafficLabel)publicTrafficLabelInfo.values().toArray()[0] : null;
                VirtualSwitchType publicTrafficVirtualSwitchType = (publicTrafficLabelObj != null) ? publicTrafficLabelObj.getVirtualSwitchType() : VirtualSwitchType.None;

                long guestTrafficId = _physicalNetworkTrafficTypeDao.getPhysicalNetworkTrafficId(pNetworkPublic.getId(), TrafficType.Guest);
                VmwareTrafficLabel guestTrafficLabelObj = (guestTrafficLabelInfo != null) ? guestTrafficLabelInfo.get(guestTrafficId) : null;
                VirtualSwitchType guestTrafficVirtualSwitchType = (guestTrafficLabelObj != null) ? guestTrafficLabelObj.getVirtualSwitchType() : VirtualSwitchType.None;
                if (publicTrafficLabelObj.getVirtualSwitchType() != guestTrafficLabelObj.getVirtualSwitchType()) {
                    String msg =
                            "Both public traffic and guest traffic is over same physical network " + pNetworkPublic +
                            ". And virtual switch type chosen for each traffic is different" +
                            ". A physical network cannot be shared by different types of virtual switches.";
                    s_logger.error(msg);
                    throw new InvalidParameterValueException(msg);
                }
            }
        }

        privateTrafficLabel = _netmgr.getDefaultManagementTrafficLabel(dcId, HypervisorType.VMware);
        if (privateTrafficLabel != null) {
            s_logger.info("Detected private network label : " + privateTrafficLabel);
        }
        Pair<Boolean, Long> vsmInfo = new Pair<Boolean, Long>(false, 0L);
        if (nexusDVS) {
            // Expect Cisco Nexus VSM details only if following 2 condition met
            // 1) The global config parameter vmware.use.nexus.vswitch
            // 2) Atleast 1 traffic type uses Nexus distributed virtual switch as backend.
            boolean nexusDVSUsedAtGuestTraffic = false;
            for (Map.Entry<Long, VmwareTrafficLabel> guestTrafficLabel : guestTrafficLabelInfo.entrySet()) {
                if (guestTrafficLabel.getValue().getVirtualSwitchType() == VirtualSwitchType.NexusDistributedVirtualSwitch) {
                    nexusDVSUsedAtGuestTraffic = true;
                    // Get physical network label
                    String guestTrafficNWLabel = guestTrafficLabel.getValue().getVirtualSwitchName();
                    if (guestTrafficNWLabel != null) {
                        s_logger.info("Detected guest network label : " + guestTrafficNWLabel);
                    }
                }
            }

            boolean nexusDVSUsedAtPublicTraffic = false;
            if (zoneType == NetworkType.Advanced) {
                for (Map.Entry<Long, VmwareTrafficLabel> publicTrafficLabel : publicTrafficLabelInfo.entrySet()) {
                    if (publicTrafficLabel.getValue().getVirtualSwitchType() == VirtualSwitchType.NexusDistributedVirtualSwitch) {
                        nexusDVSUsedAtPublicTraffic = true;
                        // Get physical network label
                        String publicTrafficNWLabel = publicTrafficLabel.getValue().getVirtualSwitchName();
                        if (publicTrafficNWLabel != null) {
                            s_logger.info("Detected public network label : " + publicTrafficNWLabel);
                        }
                    }
                }
            }

            if (nexusDVSUsedAtGuestTraffic || nexusDVSUsedAtPublicTraffic) {
                // Before proceeding with validation of Nexus 1000v VSM check if an instance of Nexus 1000v VSM is already associated with this cluster.
                boolean clusterHasVsm = _vmwareMgr.hasNexusVSM(clusterId);
                if (!clusterHasVsm) {
                    vsmIp = _urlParams.get("vsmipaddress");
                    String vsmUser = _urlParams.get("vsmusername");
                    String vsmPassword = _urlParams.get("vsmpassword");
                    String clusterName = cluster.getName();
                    try {
                        vsmInfo = _nexusElement.validateAndAddVsm(vsmIp, vsmUser, vsmPassword, clusterId, clusterName);
                    } catch (ResourceInUseException ex) {
                        DiscoveryException discEx = new DiscoveryException(ex.getLocalizedMessage() + ". The resource is " + ex.getResourceName());
                        throw discEx;
                    }
                }
                vsmCredentials = _vmwareMgr.getNexusVSMCredentialsByClusterId(clusterId);
            }
        }

        VmwareContext context = null;
        try {
            context = VmwareContextFactory.create(url.getHost(), username, password);
            if (privateTrafficLabel != null)
                context.registerStockObject("privateTrafficLabel", privateTrafficLabel);

            if (nexusDVS) {
                if (vsmCredentials != null) {
                    s_logger.info("Stocking credentials of Nexus VSM");
                    context.registerStockObject("vsmcredentials", vsmCredentials);
                }
            }
            List<ManagedObjectReference> morHosts = _vmwareMgr.addHostToPodCluster(context, dcId, podId, clusterId, URLDecoder.decode(url.getPath(), "UTF-8"));
            if (morHosts == null)
                s_logger.info("Found 0 hosts.");
            if (privateTrafficLabel != null)
                context.uregisterStockObject("privateTrafficLabel");

            if (morHosts == null) {
                s_logger.error("Unable to find host or cluster based on url: " + URLDecoder.decode(url.getPath(), "UTF-8"));
                return null;
            }

            ManagedObjectReference morCluster = null;
            clusterDetails = _clusterDetailsDao.findDetails(clusterId);
            if (clusterDetails.get("url") != null) {
                URI uriFromCluster = new URI(UriUtils.encodeURIComponent(clusterDetails.get("url")));
                morCluster = context.getHostMorByPath(URLDecoder.decode(uriFromCluster.getPath(), "UTF-8"));

                if (morCluster == null || !morCluster.getType().equalsIgnoreCase("ClusterComputeResource")) {
                    s_logger.warn("Cluster url does not point to a valid vSphere cluster, url: " + clusterDetails.get("url"));
                    return null;
                } else {
                    ClusterMO clusterMo = new ClusterMO(context, morCluster);
                    if (clusterMo.isHAEnabled()) {
                        clusterDetails.put("NativeHA", "true");
                        _clusterDetailsDao.persist(clusterId, clusterDetails);
                    }
                }
            }

            if (!validateDiscoveredHosts(context, morCluster, morHosts)) {
                if (morCluster == null)
                    s_logger.warn("The discovered host is not standalone host, can not be added to a standalone cluster");
                else
                    s_logger.warn("The discovered host does not belong to the cluster");
                return null;
            }

            Map<VmwareResource, Map<String, String>> resources = new HashMap<VmwareResource, Map<String, String>>();
            for (ManagedObjectReference morHost : morHosts) {
                Map<String, String> details = new HashMap<String, String>();
                Map<String, Object> params = new HashMap<String, Object>();

                HostMO hostMo = new HostMO(context, morHost);
                details.put("url", hostMo.getHostName());
                details.put("username", username);
                details.put("password", password);
                String guid = morHost.getType() + ":" + morHost.getValue() + "@" + url.getHost();
                details.put("guid", guid);

                params.put("url", hostMo.getHostName());
                params.put("username", username);
                params.put("password", password);
                params.put("zone", Long.toString(dcId));
                params.put("pod", Long.toString(podId));
                params.put("cluster", Long.toString(clusterId));
                params.put("guid", guid);
                if (privateTrafficLabel != null) {
                    params.put("private.network.vswitch.name", privateTrafficLabel);
                }
                params.put("guestTrafficInfo", guestTrafficLabelInfo);
                params.put("publicTrafficInfo", publicTrafficLabelInfo);

                params.put("router.aggregation.command.each.timeout", _configDao.getValue(Config.RouterAggregationCommandEachTimeout.toString()));

                VmwareResource resource = new VmwareResource();
                try {
                    resource.configure("VMware", params);
                } catch (ConfigurationException e) {
                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + url.getHost(), "Error is " + e.getMessage());
                    s_logger.warn("Unable to instantiate " + url.getHost(), e);
                }
                resource.start();

                resources.put(resource, details);
            }

            // place a place holder guid derived from cluster ID
            try{
                cluster.setGuid(UUID.nameUUIDFromBytes(String.valueOf(clusterId).getBytes("UTF-8")).toString());
            }catch(UnsupportedEncodingException e){
                throw new DiscoveredWithErrorException("Unable to create UUID based on string " + String.valueOf(clusterId) + ". Bad clusterId or UTF-8 encoding error.");
            }
            _clusterDao.update(clusterId, cluster);
            // Flag cluster discovery success
            failureInClusterDiscovery = false;
            return resources;
        } catch (DiscoveredWithErrorException e) {
            throw e;
        } catch (Exception e) {
            s_logger.warn("Unable to connect to Vmware vSphere server. service address: " + url.getHost() + ". " + e);
            return null;
        } finally {
            if (context != null)
                context.close();
            if (failureInClusterDiscovery && vsmInfo.first()) {
                try {
                    s_logger.debug("Deleting Nexus 1000v VSM " + vsmIp + " because cluster discovery and addition to zone has failed.");
                    _nexusElement.deleteCiscoNexusVSM(vsmInfo.second().longValue());
                } catch (Exception e) {
                    s_logger.warn("Deleting Nexus 1000v VSM " + vsmIp + " failed.");
                }
            }
        }
    }

    protected CiscoNexusVSMElement getCiscoNexusVSMElement() {
        for (NetworkElement networkElement : networkElements) {
            if (networkElement instanceof CiscoNexusVSMElement)
                return (CiscoNexusVSMElement)networkElement;
        }

        throw new IllegalStateException("Failed to CiscoNexusVSMElement");
    }

    private VmwareDatacenterVO fetchVmwareDatacenterByZone(Long dcId) throws DiscoveryException {
        VmwareDatacenterVO vmwareDc;
        VmwareDatacenterZoneMapVO vmwareDcZone;
        long vmwareDcId;
        String msg;

        // Check if zone is associated with DC
        vmwareDcZone = _vmwareDcZoneMapDao.findByZoneId(dcId);
        if (vmwareDcZone == null) {
            msg = "Zone " + dcId + " is not associated with any VMware DC yet. " + "Please add VMware DC to this zone first and then try to add clusters.";
            s_logger.error(msg);
            throw new DiscoveryException(msg);
        }

        // Retrieve DC added to this zone from database
        vmwareDcId = vmwareDcZone.getVmwareDcId();
        vmwareDc = _vmwareDcDao.findById(vmwareDcId);

        return vmwareDc;
    }

    private String validateCluster(URI url, VmwareDatacenterVO vmwareDc) throws DiscoveryException {
        String msg;
        String vmwareDcNameFromDb;
        String vmwareDcNameFromApi;
        String vCenterHost;
        String updatedInventoryPath;
        String clusterName = null;
        String inventoryPath;

        vmwareDcNameFromApi = vmwareDcNameFromDb = vmwareDc.getVmwareDatacenterName();
        vCenterHost = vmwareDc.getVcenterHost();
        try {
            inventoryPath = updatedInventoryPath = URLDecoder.decode(url.getPath(), "UTF-8");
        } catch(UnsupportedEncodingException e) {
            throw new DiscoveredWithErrorException("Unable to decode URL path, URL path : " + url.getPath(), e);
        }

        assert (inventoryPath != null);

        String[] pathTokens = inventoryPath.split("/");
        if (pathTokens.length == 2) {
            // DC name is not present in url.
            // Using DC name read from database.
            clusterName = pathTokens[1];
            updatedInventoryPath = "/" + vmwareDcNameFromDb + "/" + clusterName;
        } else if (pathTokens.length == 3) {
            vmwareDcNameFromApi = pathTokens[1];
            clusterName = pathTokens[2];
        }

        if (!vCenterHost.equalsIgnoreCase(url.getHost())) {
            msg =
                    "This cluster " + clusterName + " belongs to vCenter " + url.getHost() + ". But this zone is associated with VMware DC from vCenter " + vCenterHost +
                    ". Make sure the cluster being added belongs to vCenter " + vCenterHost + " and VMware DC " + vmwareDcNameFromDb;
            s_logger.error(msg);
            throw new DiscoveryException(msg);
        } else if (!vmwareDcNameFromDb.equalsIgnoreCase(vmwareDcNameFromApi)) {
            msg =
                    "This cluster " + clusterName + " belongs to VMware DC " + vmwareDcNameFromApi + " .But this zone is associated with VMware DC " + vmwareDcNameFromDb +
                    ". Make sure the cluster being added belongs to VMware DC " + vmwareDcNameFromDb + " in vCenter " + vCenterHost;
            s_logger.error(msg);
            throw new DiscoveryException(msg);
        }
        return updatedInventoryPath;
    }

    private boolean validateDiscoveredHosts(VmwareContext context, ManagedObjectReference morCluster, List<ManagedObjectReference> morHosts) throws Exception {
        if (morCluster == null) {
            for (ManagedObjectReference morHost : morHosts) {
                ManagedObjectReference morParent = (ManagedObjectReference)context.getVimClient().getDynamicProperty(morHost, "parent");
                if (morParent.getType().equalsIgnoreCase("ClusterComputeResource"))
                    return false;
            }
        } else {
            for (ManagedObjectReference morHost : morHosts) {
                ManagedObjectReference morParent = (ManagedObjectReference)context.getVimClient().getDynamicProperty(morHost, "parent");
                if (!morParent.getType().equalsIgnoreCase("ClusterComputeResource"))
                    return false;

                if (!morParent.getValue().equals(morCluster.getValue()))
                    return false;
            }
        }

        return true;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {
        // do nothing
    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        if (hypervisor == null)
            return true;

        return Hypervisor.HypervisorType.VMware.toString().equalsIgnoreCase(hypervisor);
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.VMware;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (s_logger.isInfoEnabled())
            s_logger.info("Configure VmwareServerDiscoverer, discover name: " + name);

        super.configure(name, params);

        createVmwareToolsIso();

        if (s_logger.isInfoEnabled()) {
            s_logger.info("VmwareServerDiscoverer has been successfully configured");
        }
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    private void createVmwareToolsIso() {
        String isoName = "vmware-tools.iso";
        VMTemplateVO tmplt = _tmpltDao.findByTemplateName(isoName);
        Long id;
        if (tmplt == null) {
            id = _tmpltDao.getNextInSequence(Long.class, "id");
            VMTemplateVO template =
                    VMTemplateVO.createPreHostIso(id, isoName, isoName, ImageFormat.ISO, true, true, TemplateType.PERHOST, null, null, true, 64, Account.ACCOUNT_ID_SYSTEM,
                            null, "VMware Tools Installer ISO", false, 1, false, HypervisorType.VMware);
            _tmpltDao.persist(template);
        } else {
            id = tmplt.getId();
            tmplt.setTemplateType(TemplateType.PERHOST);
            tmplt.setUrl(null);
            _tmpltDao.update(id, tmplt);
        }
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }

        StartupRoutingCommand ssCmd = ((StartupRoutingCommand)firstCmd);
        if (ssCmd.getHypervisorType() != HypervisorType.VMware) {
            return null;
        }

        return _resourceMgr.fillRoutingHostVO(host, ssCmd, HypervisorType.VMware, details, hostTags);
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != com.cloud.host.Host.Type.Routing || host.getHypervisorType() != HypervisorType.VMware) {
            return null;
        }

        _resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
        return new DeleteHostAnswer(true);

    }

    @Override
    public boolean start() {
        if (!super.start())
            return false;

        _nexusElement = getCiscoNexusVSMElement();

        return true;
    }

    @Override
    public boolean stop() {
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return super.stop();
    }

    private Map<Long, VmwareTrafficLabel> getTrafficInfo(TrafficType trafficType, Map<Long, String> zoneLevelTrafficInfo, Map<Long, String> clusterLevelTrafficInfo,
            String clusterLevelSwitchType) {
        Map<Long, VmwareTrafficLabel> trafficInfo = new HashMap<Long, VmwareTrafficLabel>();

        // Iterate through each zone level entry and Apply the cluster level overridden settings found with the physical network traffic id
        for (Map.Entry<Long, String> zoneLevelTrafficDetail : zoneLevelTrafficInfo.entrySet()) {
            VmwareTrafficLabel trafficLabelObj = null;
            try {
                trafficLabelObj = new VmwareTrafficLabel(zoneLevelTrafficDetail.getValue(), trafficType, getDefaultVirtualSwitchTypeAtCloudLevel());
            } catch (InvalidParameterValueException e) {
                s_logger.error("Failed to recognize virtual switch type specified for " + trafficType + " traffic due to " + e.getMessage());
                throw e;
            }

            // Check if any cluster level traffic override settings for that traffic id
            if (clusterLevelTrafficInfo != null && clusterLevelSwitchType != null && VirtualSwitchType.getType(clusterLevelSwitchType) != VirtualSwitchType.None
                    && clusterLevelTrafficInfo.containsKey(zoneLevelTrafficDetail.getKey())) {
                s_logger.debug("Cluster level traffic override found for the physical network traffic id: " + zoneLevelTrafficDetail.getKey());
                s_logger.debug("Replacing zone level label: " + zoneLevelTrafficDetail.getValue() + " with cluster vswitch type: " + clusterLevelSwitchType + " and name: "
                        + clusterLevelTrafficInfo.get(zoneLevelTrafficDetail.getKey()));
                trafficLabelObj.setVirtualSwitchType(VirtualSwitchType.getType(clusterLevelSwitchType));
                trafficLabelObj.setVirtualSwitchName(clusterLevelTrafficInfo.get(zoneLevelTrafficDetail.getKey()));
            }

            trafficInfo.put(zoneLevelTrafficDetail.getKey(), trafficLabelObj);
        }

        return trafficInfo;
    }

    private void _readGlobalConfigParameters() {
        String value;
        if (_configDao != null) {
            value = _configDao.getValue(Config.VmwareUseDVSwitch.key());
            useDVS = Boolean.parseBoolean(value);
            value = _configDao.getValue(Config.VmwareUseNexusVSwitch.key());
            nexusDVS = Boolean.parseBoolean(value);
        }
    }

    @Override
    protected HashMap<String, Object> buildConfigParams(HostVO host) {
        HashMap<String, Object> params = super.buildConfigParams(host);

        Map<String, String> clusterDetails = _clusterDetailsDao.findDetails(host.getClusterId());

        // Zone level traffic info with physical network traffic id and network label
        Map<Long, String> guestTrafficInfoAtZoneLevel = _netmgr.getGuestTrafficInfo(host.getDataCenterId(), HypervisorType.VMware);
        Map<Long, String> publicTrafficInfoAtZoneLevel = _netmgr.getPublicTrafficInfo(host.getDataCenterId(), HypervisorType.VMware);

        // Cluster level traffic info with physical network traffic id and network label (vswitch name)
        Map<Long, String> guestTrafficInfoAtClusterLevel = getGuestTrafficInfoForCluster(host.getClusterId());
        Map<Long, String> publicTrafficInfoAtClusterLevel = getPublicTrafficInfoForCluster(host.getClusterId());

        String guestVswitchTypeAtClusterLevel = clusterDetails.get(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC);
        String publicVswitchTypeAtClusterLevel = clusterDetails.get(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC);

        _readGlobalConfigParameters();

        params.put("guestTrafficInfo", getTrafficInfo(TrafficType.Guest, guestTrafficInfoAtZoneLevel, guestTrafficInfoAtClusterLevel, guestVswitchTypeAtClusterLevel));
        params.put("publicTrafficInfo", getTrafficInfo(TrafficType.Public, publicTrafficInfoAtZoneLevel, publicTrafficInfoAtClusterLevel, publicVswitchTypeAtClusterLevel));

        return params;
    }

    private VirtualSwitchType getDefaultVirtualSwitchTypeAtCloudLevel() {
        if (useDVS) {
            return (nexusDVS) ? VirtualSwitchType.NexusDistributedVirtualSwitch : VirtualSwitchType.VMwareDistributedVirtualSwitch;
        }

        return VirtualSwitchType.StandardVirtualSwitch;
    }

    @Override
    public ServerResource reloadResource(HostVO host) {
        String resourceName = host.getResource();
        ServerResource resource = getResource(resourceName);

        if (resource != null) {
            _hostDao.loadDetails(host);

            HashMap<String, Object> params = buildConfigParams(host);
            try {
                resource.configure(host.getName(), params);
            } catch (ConfigurationException e) {
                s_logger.warn("Unable to configure resource due to " + e.getMessage());
                return null;
            }
            if (!resource.start()) {
                s_logger.warn("Unable to start the resource");
                return null;
            }
        }
        return resource;
    }

    private void validateVswitchType(String inputVswitchType) {
        if (inputVswitchType == null) {
            return;
        }

        if (inputVswitchType.isEmpty()) {
            throw new InvalidParameterValueException("Switch type shouldn't be empty");
        }

        VirtualSwitchType vSwitchType = VirtualSwitchType.getType(inputVswitchType);
        if (vSwitchType == VirtualSwitchType.None) {
            s_logger.error("Unable to resolve " + inputVswitchType + " to a valid virtual switch type in VMware environment.");
            throw new InvalidParameterValueException("Invalid virtual switch type : " + inputVswitchType);
        }
    }

    private void validateVswitchNameAtCluster(String inputVswitchName) {
        if (inputVswitchName == null) {
            return;
        }

        if (inputVswitchName.isEmpty()) {
            throw new InvalidParameterValueException("Switch name shouldn't be empty");
        }

        String[] tokens = inputVswitchName.split(",");
        if (tokens.length > MAX_FIELDS_VMWARE_LABEL_AT_CLUSTER) {
            throw new InvalidParameterValueException("Found extraneous fields in vmware traffic label at cluster: " + inputVswitchName);
        }
    }

    @Override
    public void putParam(Map<String, String> params) {
        if (_urlParams == null) {
            _urlParams = new HashMap<String, String>();
        }
        _urlParams.putAll(params);
    }

    @Override
    public void clearParam() {
        if (_urlParams == null) {
            return;
        }
        _urlParams.clear();
    }

    public List<NetworkElement> getNetworkElements() {
        return networkElements;
    }

    @Inject
    public void setNetworkElements(List<NetworkElement> networkElements) {
        this.networkElements = networkElements;
    }

    private Map<Long, String> getTrafficInfoForCluster(long clusterId, TrafficType trafficType) {
        Map<Long, String> trafficInfo = _clusterPhysicalNetworkTrafficInfoDao.getTrafficInfo(clusterId);
        if (trafficInfo == null || trafficInfo.size() == 0) {
            return null;
        }

        Map<Long, String> actualtrafficInfo = new HashMap<Long, String>();
        for (Map.Entry<Long, String> trafficDetail : trafficInfo.entrySet()) {
            if (_physicalNetworkTrafficTypeDao.getTrafficType(trafficDetail.getKey().longValue()) == trafficType) {
                actualtrafficInfo.put(trafficDetail.getKey(), trafficDetail.getValue());
            }
        }

        return actualtrafficInfo;
    }

    private Map<Long, String> getGuestTrafficInfoForCluster(long clusterId) {
        return getTrafficInfoForCluster(clusterId, TrafficType.Guest);
    }

    private Map<Long, String> getPublicTrafficInfoForCluster(long clusterId) {
        return getTrafficInfoForCluster(clusterId, TrafficType.Public);
    }
}
