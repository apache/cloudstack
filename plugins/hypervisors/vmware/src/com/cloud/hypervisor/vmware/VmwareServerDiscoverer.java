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

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.ApiConstants;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.dao.HypervisorCapabilitiesDao;
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
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.utils.UriUtils;

import com.vmware.vim25.ClusterDasConfigInfo;
import com.vmware.vim25.ManagedObjectReference;


@Local(value = Discoverer.class)
public class VmwareServerDiscoverer extends DiscovererBase implements
		Discoverer, ResourceStateAdapter {
	private static final Logger s_logger = Logger
			.getLogger(VmwareServerDiscoverer.class);

	@Inject
	ClusterDao _clusterDao;
	@Inject
	VmwareManager _vmwareMgr;
	@Inject
	AlertManager _alertMgr;
	@Inject
	VMTemplateDao _tmpltDao;
	@Inject
	ClusterDetailsDao _clusterDetailsDao;
	@Inject
	HostDao _hostDao;
	@Inject
	DataCenterDao _dcDao;
	@Inject
	ResourceManager _resourceMgr;
	@Inject
	CiscoNexusVSMDeviceDao _nexusDao;
	@Inject
    NetworkModel _netmgr;
    @Inject
    HypervisorCapabilitiesDao _hvCapabilitiesDao;
    protected Map<String, String> _urlParams;
    protected boolean useDVS = false;
    protected boolean nexusDVS = false;

	public VmwareServerDiscoverer() {
		s_logger.info("VmwareServerDiscoverer is constructed");
	}
	
	@Override
    public Map<? extends ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI url, 
    	String username, String password, List<String> hostTags) throws DiscoveryException {

    	if(s_logger.isInfoEnabled())
    		s_logger.info("Discover host. dc: " + dcId + ", pod: " + podId + ", cluster: " + clusterId + ", uri host: " + url.getHost());

    	if(podId == null) {
        	if(s_logger.isInfoEnabled())
				s_logger.info("No pod is assigned, assuming that it is not for vmware and skip it to next discoverer");
			return null;
		}

		ClusterVO cluster = _clusterDao.findById(clusterId);
        if(cluster == null || cluster.getHypervisorType() != HypervisorType.VMware) {
        	if(s_logger.isInfoEnabled())
				s_logger.info("invalid cluster id or cluster is not for VMware hypervisors");
			return null;
		}

		List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(clusterId);
        if (hosts != null && hosts.size() > 0) {
            int maxHostsPerCluster = _hvCapabilitiesDao.getMaxHostsPerCluster(hosts.get(0).getHypervisorType(), hosts.get(0).getHypervisorVersion());
            if (hosts.size() > maxHostsPerCluster) {
                   String msg = "VMware cluster " + cluster.getName() + " is too big to add new host now. (current configured cluster size: " + maxHostsPerCluster + ")";
			s_logger.error(msg);
			throw new DiscoveredWithErrorException(msg);
		}
        }

		String privateTrafficLabel = null;
		String publicTrafficLabel = null;
		String guestTrafficLabel = null;
		Map<String, String> vsmCredentials = null;

        VirtualSwitchType defaultVirtualSwitchType = VirtualSwitchType.StandardVirtualSwitch;

        String paramGuestVswitchType = null;
        String paramGuestVswitchName = null;
        String paramPublicVswitchType = null;
        String paramPublicVswitchName = null;

        VmwareTrafficLabel guestTrafficLabelObj = new VmwareTrafficLabel(TrafficType.Guest);
        VmwareTrafficLabel publicTrafficLabelObj = new VmwareTrafficLabel(TrafficType.Public);
        Map<String, String> clusterDetails = _clusterDetailsDao.findDetails(clusterId);
        DataCenterVO zone = _dcDao.findById(dcId);
        NetworkType zoneType = zone.getNetworkType();
        _readGlobalConfigParameters();

        // Set default physical network end points for public and guest traffic
        // Private traffic will be only on standard vSwitch for now.
        if (useDVS) {
            // Parse url parameters for type of vswitch and name of vswitch specified at cluster level
            paramGuestVswitchType = _urlParams.get(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC);
            paramGuestVswitchName = _urlParams.get(ApiConstants.VSWITCH_NAME_GUEST_TRAFFIC);
            paramPublicVswitchType = _urlParams.get(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC);
            paramPublicVswitchName = _urlParams.get(ApiConstants.VSWITCH_NAME_PUBLIC_TRAFFIC);
            defaultVirtualSwitchType = getDefaultVirtualSwitchType();
        }

        // Zone level vSwitch Type depends on zone level traffic labels
        //
        // User can override Zone wide vswitch type (for public and guest) by providing following optional parameters in addClusterCmd
        // param "guestvswitchtype" with valid values vmwaredvs, vmwaresvs, nexusdvs
        // param "publicvswitchtype" with valid values vmwaredvs, vmwaresvs, nexusdvs
        //
        // Format of label is <VSWITCH>,<VLANID>,<VSWITCHTYPE>
        // If a field <VLANID> OR <VSWITCHTYPE> is not present leave it empty.
        // Ex: 1) vswitch0
        // 2) dvswitch0,200,vmwaredvs
        // 3) nexusepp0,300,nexusdvs
        // 4) vswitch1,400,vmwaresvs
        // 5) vswitch0
        // default vswitchtype is 'vmwaresvs'.
        // <VSWITCHTYPE> 'vmwaresvs' is for vmware standard vswitch
        // <VSWITCHTYPE> 'vmwaredvs' is for vmware distributed virtual switch
        // <VSWITCHTYPE> 'nexusdvs' is for cisco nexus distributed virtual switch
        // Get zone wide traffic labels for Guest traffic and Public traffic
        guestTrafficLabel = _netmgr.getDefaultGuestTrafficLabel(dcId, HypervisorType.VMware);

        // Process traffic label information provided at zone level and cluster level
        guestTrafficLabelObj = getTrafficInfo(TrafficType.Guest, guestTrafficLabel, defaultVirtualSwitchType, paramGuestVswitchType, paramGuestVswitchName, clusterId);

        if (zoneType == NetworkType.Advanced) {
            // Get zone wide traffic label for Public traffic
            publicTrafficLabel = _netmgr.getDefaultPublicTrafficLabel(dcId, HypervisorType.VMware);

            // Process traffic label information provided at zone level and cluster level
            publicTrafficLabelObj = getTrafficInfo(TrafficType.Public, publicTrafficLabel, defaultVirtualSwitchType, paramPublicVswitchType, paramPublicVswitchName, clusterId);

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
            if (publicTrafficLabelObj.getVirtualSwitchType() != guestTrafficLabelObj.getVirtualSwitchType()) {
                String msg = "Both public traffic and guest traffic is over same physical network " + pNetworkPublic +
                        ". And virtual switch type chosen for each traffic is different" +
                        ". A physical network cannot be shared by different types of virtual switches.";
                s_logger.error(msg);
                throw new InvalidParameterValueException(msg);
            }
        }
        } else {
            // Distributed virtual switch is not supported in Basic zone for now.
            // Private / Management network traffic is not yet supported over distributed virtual switch.
            if (guestTrafficLabelObj.getVirtualSwitchType() != VirtualSwitchType.StandardVirtualSwitch) {
                String msg = "Detected that Guest traffic is over Distributed virtual switch in Basic zone. Only Standard vSwitch is supported in Basic zone.";
                s_logger.error(msg);
                throw new DiscoveredWithErrorException(msg);
            }
        }

        privateTrafficLabel = _netmgr.getDefaultManagementTrafficLabel(dcId, HypervisorType.VMware);
		if (privateTrafficLabel != null) {
            s_logger.info("Detected private network label : " + privateTrafficLabel);
		}

        if (nexusDVS) {
			if (zoneType != NetworkType.Basic) {
                publicTrafficLabel = _netmgr.getDefaultPublicTrafficLabel(dcId, HypervisorType.VMware);
				if (publicTrafficLabel != null) {
                    s_logger.info("Detected public network label : " + publicTrafficLabel);
				}
			}
			// Get physical network label
            guestTrafficLabel = _netmgr.getDefaultGuestTrafficLabel(dcId, HypervisorType.VMware);
			if (guestTrafficLabel != null) {
                s_logger.info("Detected guest network label : " + guestTrafficLabel);
			}
            vsmCredentials = _vmwareMgr.getNexusVSMCredentialsByClusterId(clusterId);
		}

		VmwareContext context = null;
		try {
			context = VmwareContextFactory.create(url.getHost(), username,
					password);
			if (privateTrafficLabel != null)
				context.registerStockObject("privateTrafficLabel",
						privateTrafficLabel);

            if (nexusDVS) {
				if (vsmCredentials != null) {
					s_logger.info("Stocking credentials of Nexus VSM");
					context.registerStockObject("vsmcredentials",
							vsmCredentials);
				}
			}
			List<ManagedObjectReference> morHosts = _vmwareMgr
					.addHostToPodCluster(context, dcId, podId, clusterId,
							URLDecoder.decode(url.getPath()));
			if (morHosts == null)
				s_logger.info("Found 0 hosts.");
			if (privateTrafficLabel != null)
				context.uregisterStockObject("privateTrafficLabel");

			if (morHosts == null) {
				s_logger.error("Unable to find host or cluster based on url: "
						+ URLDecoder.decode(url.getPath()));
				return null;
			}

			ManagedObjectReference morCluster = null;
            clusterDetails = _clusterDetailsDao.findDetails(clusterId);
			if (clusterDetails.get("url") != null) {
				URI uriFromCluster = new URI(
						UriUtils.encodeURIComponent(clusterDetails.get("url")));
				morCluster = context.getHostMorByPath(URLDecoder
						.decode(uriFromCluster.getPath()));

				if (morCluster == null
						|| !morCluster.getType().equalsIgnoreCase(
								"ClusterComputeResource")) {
					s_logger.warn("Cluster url does not point to a valid vSphere cluster, url: "
							+ clusterDetails.get("url"));
					return null;
				} else {
					ClusterMO clusterMo = new ClusterMO(context, morCluster);
					ClusterDasConfigInfo dasConfig = clusterMo.getDasConfig();
					if (dasConfig != null && dasConfig.isEnabled() != null
							&& dasConfig.isEnabled().booleanValue()) {
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
				String guid = morHost.getType() + ":" + morHost.getValue()
						+ "@" + url.getHost();
				details.put("guid", guid);

				params.put("url", hostMo.getHostName());
				params.put("username", username);
				params.put("password", password);
				params.put("zone", Long.toString(dcId));
				params.put("pod", Long.toString(podId));
				params.put("cluster", Long.toString(clusterId));
				params.put("guid", guid);
				if (privateTrafficLabel != null) {
					params.put("private.network.vswitch.name",
							privateTrafficLabel);
				}
                params.put("guestTrafficInfo", guestTrafficLabelObj);
                params.put("publicTrafficInfo", publicTrafficLabelObj);

				VmwareResource resource = new VmwareResource();
				try {
					resource.configure("VMware", params);
				} catch (ConfigurationException e) {
					_alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId,
							podId, "Unable to add " + url.getHost(),
							"Error is " + e.getMessage());
					s_logger.warn("Unable to instantiate " + url.getHost(), e);
				}
				resource.start();

				resources.put(resource, details);
			}

			// place a place holder guid derived from cluster ID
			cluster.setGuid(UUID.nameUUIDFromBytes(
					String.valueOf(clusterId).getBytes()).toString());
			_clusterDao.update(clusterId, cluster);

			return resources;
		} catch (DiscoveredWithErrorException e) {
			throw e;
		} catch (Exception e) {
			s_logger.warn("Unable to connect to Vmware vSphere server. service address: "
					+ url.getHost());
			return null;
		} finally {
			if (context != null)
				context.close();
		}
	}

	private boolean validateDiscoveredHosts(VmwareContext context,
			ManagedObjectReference morCluster,
			List<ManagedObjectReference> morHosts) throws Exception {
		if (morCluster == null) {
			for (ManagedObjectReference morHost : morHosts) {
				ManagedObjectReference morParent = (ManagedObjectReference) context
						.getVimClient().getDynamicProperty(morHost, "parent");
				if (morParent.getType().equalsIgnoreCase(
						"ClusterComputeResource"))
					return false;
			}
		} else {
			for (ManagedObjectReference morHost : morHosts) {
				ManagedObjectReference morParent = (ManagedObjectReference) context
						.getVimClient().getDynamicProperty(morHost, "parent");
				if (!morParent.getType().equalsIgnoreCase(
						"ClusterComputeResource"))
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

		return Hypervisor.HypervisorType.VMware.toString().equalsIgnoreCase(
				hypervisor);
	}

	@Override
	public Hypervisor.HypervisorType getHypervisorType() {
		return Hypervisor.HypervisorType.VMware;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		if (s_logger.isInfoEnabled())
			s_logger.info("Configure VmwareServerDiscoverer, discover name: "
					+ name);

		super.configure(name, params);

		createVmwareToolsIso();

		if (s_logger.isInfoEnabled()) {
			s_logger.info("VmwareServerDiscoverer has been successfully configured");
		}
		_resourceMgr.registerResourceStateAdapter(this.getClass()
				.getSimpleName(), this);
		return true;
	}

	private void createVmwareToolsIso() {
		String isoName = "vmware-tools.iso";
		VMTemplateVO tmplt = _tmpltDao.findByTemplateName(isoName);
		Long id;
		if (tmplt == null) {
			id = _tmpltDao.getNextInSequence(Long.class, "id");
			VMTemplateVO template = new VMTemplateVO(id, isoName, isoName,
					ImageFormat.ISO, true, true, TemplateType.PERHOST, null,
					null, true, 64, Account.ACCOUNT_ID_SYSTEM, null,
					"VMware Tools Installer ISO", false, 1, false,
					HypervisorType.VMware);
			_tmpltDao.persist(template);
		} else {
			id = tmplt.getId();
			tmplt.setTemplateType(TemplateType.PERHOST);
			tmplt.setUrl(null);
			_tmpltDao.update(id, tmplt);
		}
	}


	@Override
	public HostVO createHostVOForConnectedAgent(HostVO host,
			StartupCommand[] cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HostVO createHostVOForDirectConnectAgent(HostVO host,
			StartupCommand[] startup, ServerResource resource,
			Map<String, String> details, List<String> hostTags) {
		StartupCommand firstCmd = startup[0];
		if (!(firstCmd instanceof StartupRoutingCommand)) {
			return null;
		}

		StartupRoutingCommand ssCmd = ((StartupRoutingCommand) firstCmd);
		if (ssCmd.getHypervisorType() != HypervisorType.VMware) {
			return null;
		}

		return _resourceMgr.fillRoutingHostVO(host, ssCmd,
				HypervisorType.VMware, details, hostTags);
	}

	@Override
	public DeleteHostAnswer deleteHost(HostVO host, boolean isForced,
			boolean isForceDeleteStorage) throws UnableDeleteHostException {
		if (host.getType() != com.cloud.host.Host.Type.Routing
				|| host.getHypervisorType() != HypervisorType.VMware) {
			return null;
		}

		_resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
		return new DeleteHostAnswer(true);

	}

	@Override
	public boolean stop() {
		_resourceMgr.unregisterResourceStateAdapter(this.getClass()
				.getSimpleName());
		return super.stop();
	}

    private VmwareTrafficLabel getTrafficInfo(TrafficType trafficType, String zoneWideTrafficLabel, VirtualSwitchType defaultVirtualSwitchType, String vSwitchType, String vSwitchName, Long clusterId) {
        VmwareTrafficLabel trafficLabelObj = null;
        Map<String, String> clusterDetails = null;
        try {
            trafficLabelObj = new VmwareTrafficLabel(zoneWideTrafficLabel, trafficType, defaultVirtualSwitchType);
        } catch (InvalidParameterValueException e) {
            s_logger.error("Failed to recognize virtual switch type specified for " + trafficType +
                           " traffic due to " + e.getMessage());
            throw e;
        }

        if (defaultVirtualSwitchType.equals(VirtualSwitchType.StandardVirtualSwitch)|| (vSwitchType == null && vSwitchName == null)) {
            // Case of no cluster level override configuration defined.
            // Depend only on zone wide traffic label
            // If global param for dvSwitch is false return default traffic info object with vmware standard vswitch
            return trafficLabelObj;
        } else {
            // Need to persist cluster level override configuration to db
            clusterDetails = _clusterDetailsDao.findDetails(clusterId);
        }

        if (vSwitchName != null) {
            trafficLabelObj.setVirtualSwitchName(vSwitchName);
            if (trafficType == TrafficType.Guest) {
                clusterDetails.put(ApiConstants.VSWITCH_NAME_GUEST_TRAFFIC, vSwitchName);
            } else {
                clusterDetails.put(ApiConstants.VSWITCH_NAME_PUBLIC_TRAFFIC, vSwitchName);
            }
        }

        if (vSwitchType != null) {
            validateVswitchType(vSwitchType);
            trafficLabelObj.setVirtualSwitchType(VirtualSwitchType.getType(vSwitchType));
            if (trafficType == TrafficType.Guest) {
                clusterDetails.put(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC, vSwitchType);
            } else {
                clusterDetails.put(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC, vSwitchType);
            }
        }

        // Save cluster level override configuration to cluster details
        _clusterDetailsDao.persist(clusterId, clusterDetails);

        return trafficLabelObj;
    }

    private VmwareTrafficLabel getTrafficInfo(TrafficType trafficType, String zoneWideTrafficLabel, Map<String, String> clusterDetails, VirtualSwitchType defVirtualSwitchType) {
        VmwareTrafficLabel trafficLabelObj = null;
        try {
            trafficLabelObj = new VmwareTrafficLabel(zoneWideTrafficLabel, trafficType, defVirtualSwitchType);
        } catch (InvalidParameterValueException e) {
            s_logger.error("Failed to recognize virtual switch type specified for " + trafficType +
                           " traffic due to " + e.getMessage());
            throw e;
        }

        if(defVirtualSwitchType.equals(VirtualSwitchType.StandardVirtualSwitch)) {
            return trafficLabelObj;
        }

        if (trafficType == TrafficType.Guest) {
            if(clusterDetails.containsKey(ApiConstants.VSWITCH_NAME_GUEST_TRAFFIC)) {
                trafficLabelObj.setVirtualSwitchName(clusterDetails.get(ApiConstants.VSWITCH_NAME_GUEST_TRAFFIC));
            }
            if(clusterDetails.containsKey(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC)) {
                trafficLabelObj.setVirtualSwitchType(VirtualSwitchType.getType(clusterDetails.get(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC)));
            }
        } else if (trafficType == TrafficType.Public) {
            if(clusterDetails.containsKey(ApiConstants.VSWITCH_NAME_PUBLIC_TRAFFIC)) {
                trafficLabelObj.setVirtualSwitchName(clusterDetails.get(ApiConstants.VSWITCH_NAME_PUBLIC_TRAFFIC));
           }
           if(clusterDetails.containsKey(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC)) {
               trafficLabelObj.setVirtualSwitchType(VirtualSwitchType.getType(clusterDetails.get(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC)));
           }
        }

        return trafficLabelObj;
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
        // Get zone wide traffic labels from guest traffic and public traffic
        String guestTrafficLabel = _netmgr.getDefaultGuestTrafficLabel(host.getDataCenterId(), HypervisorType.VMware);
        String publicTrafficLabel = _netmgr.getDefaultPublicTrafficLabel(host.getDataCenterId(), HypervisorType.VMware);
        _readGlobalConfigParameters();
        VirtualSwitchType defaultVirtualSwitchType = getDefaultVirtualSwitchType();

        params.put("guestTrafficInfo", getTrafficInfo(TrafficType.Guest, guestTrafficLabel, clusterDetails, defaultVirtualSwitchType));
        params.put("publicTrafficInfo", getTrafficInfo(TrafficType.Public, publicTrafficLabel, clusterDetails, defaultVirtualSwitchType));

        return params;
    }

    private VirtualSwitchType getDefaultVirtualSwitchType() {
        if (nexusDVS)
            return VirtualSwitchType.NexusDistributedVirtualSwitch;
        else if(useDVS)
            return VirtualSwitchType.VMwareDistributedVirtualSwitch;
        else 
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
        VirtualSwitchType vSwitchType = VirtualSwitchType.getType(inputVswitchType);
        if (vSwitchType == VirtualSwitchType.None) {
            s_logger.error("Unable to resolve " + inputVswitchType + " to a valid virtual switch type in VMware environment.");
            throw new InvalidParameterValueException("Invalid virtual switch type : " + inputVswitchType);
        }
    }

    @Override
    public void putParam(Map<String, String> params) {
        if (_urlParams == null) {
            _urlParams = new HashMap<String, String>();
        }
        _urlParams.putAll(params);
    }
}
