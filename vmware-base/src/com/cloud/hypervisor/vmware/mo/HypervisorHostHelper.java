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
package com.cloud.hypervisor.vmware.mo;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.utils.ActionDelegate;
import com.cloud.utils.Pair;
import com.cloud.utils.cisco.n1kv.vsm.NetconfHelper;
import com.cloud.utils.cisco.n1kv.vsm.PolicyMap;
import com.cloud.utils.cisco.n1kv.vsm.PortProfile;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.BindingType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.OperationType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.PortProfileType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.vmware.vim25.BoolPolicy;
import com.vmware.vim25.DVPortSetting;
import com.vmware.vim25.DVPortgroupConfigInfo;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.DVSSecurityPolicy;
import com.vmware.vim25.DVSTrafficShapingPolicy;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HostNetworkSecurityPolicy;
import com.vmware.vim25.HostNetworkTrafficShapingPolicy;
import com.vmware.vim25.HostPortGroupSpec;
import com.vmware.vim25.HostVirtualSwitch;
import com.vmware.vim25.HttpNfcLeaseDeviceUrl;
import com.vmware.vim25.HttpNfcLeaseInfo;
import com.vmware.vim25.HttpNfcLeaseState;
import com.vmware.vim25.LongPolicy;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.OvfCreateImportSpecParams;
import com.vmware.vim25.OvfCreateImportSpecResult;
import com.vmware.vim25.OvfFileItem;
import com.vmware.vim25.VMwareDVSPortSetting;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineVideoCard;
import com.vmware.vim25.VirtualSCSISharing;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanIdSpec;
import com.vmware.vim25.VmwareDistributedVirtualSwitchVlanSpec;

public class HypervisorHostHelper {
    private static final Logger s_logger = Logger.getLogger(HypervisorHostHelper.class);
    private static final int DEFAULT_LOCK_TIMEOUT_SECONDS = 600;
    private static final String s_policyNamePrefix = "cloud.policy.";

    // make vmware-base loosely coupled with cloud-specific stuff, duplicate VLAN.UNTAGGED constant here
    private static final String UNTAGGED_VLAN_NAME = "untagged";

	public static VirtualMachineMO findVmFromObjectContent(VmwareContext context,
		ObjectContent[] ocs, String name) {

		if(ocs != null && ocs.length > 0) {
			for(ObjectContent oc : ocs) {
				DynamicProperty prop = oc.getPropSet().get(0);
				assert(prop != null);
				if(prop.getVal().toString().equals(name))
					return new VirtualMachineMO(context, oc.getObj());
			}
		}
		return null;
	}

	public static ManagedObjectReference findDatastoreWithBackwardsCompatibility(VmwareHypervisorHost hyperHost, String uuidName) throws Exception {
	    ManagedObjectReference morDs = hyperHost.findDatastore(uuidName.replace("-", ""));
	    if(morDs == null)
	        morDs = hyperHost.findDatastore(uuidName);

	    return morDs;
	}

	public static DatastoreMO getHyperHostDatastoreMO(VmwareHypervisorHost hyperHost, String datastoreName) throws Exception {
		ObjectContent[] ocs = hyperHost.getDatastorePropertiesOnHyperHost(new String[] { "name"} );
		if(ocs != null && ocs.length > 0) {
    		for(ObjectContent oc : ocs) {
		        List<DynamicProperty> objProps = oc.getPropSet();
		        if(objProps != null) {
		        	for(DynamicProperty objProp : objProps) {
		        		if(objProp.getVal().toString().equals(datastoreName))
		        			return new DatastoreMO(hyperHost.getContext(), oc.getObj());
		        	}
		        }
    		}
		}
		return null;
	}

	public static String getPublicNetworkNamePrefix(String vlanId) {
	    if (UNTAGGED_VLAN_NAME.equalsIgnoreCase(vlanId)) {
	        return "cloud.public.untagged";
	    } else {
	        return "cloud.public." + vlanId;
	    }
	}

	public static String composeCloudNetworkName(String prefix, String vlanId, Integer networkRateMbps, String vSwitchName) {
		StringBuffer sb = new StringBuffer(prefix);
		if(vlanId == null || UNTAGGED_VLAN_NAME.equalsIgnoreCase(vlanId))
			sb.append(".untagged");
		else
			sb.append(".").append(vlanId);

		if(networkRateMbps != null && networkRateMbps.intValue() > 0)
			sb.append(".").append(String.valueOf(networkRateMbps));
		else
			sb.append(".0");
		sb.append(".").append(VersioningContants.PORTGROUP_NAMING_VERSION);
		sb.append("-").append(vSwitchName);

		return sb.toString();
	}

    public static Map<String, String> getValidatedVsmCredentials(VmwareContext context) throws Exception {
        Map<String, String> vsmCredentials = context.getStockObject("vsmcredentials");
        String msg;
        if (vsmCredentials == null || vsmCredentials.size() != 3) {
            msg = "Failed to retrieve required credentials of Nexus VSM from database.";
            s_logger.error(msg);
            throw new Exception(msg);
        }

        String vsmIp = vsmCredentials.containsKey("vsmip") ? vsmCredentials.get("vsmip") : null;
        String vsmUserName = vsmCredentials.containsKey("vsmusername") ? vsmCredentials.get("vsmusername") : null;
        String vsmPassword = vsmCredentials.containsKey("vsmpassword") ? vsmCredentials.get("vsmpassword") : null;
        if (vsmIp == null || vsmIp.isEmpty() || vsmUserName == null || vsmUserName.isEmpty() || vsmPassword == null
                || vsmPassword.isEmpty()) {
            msg = "Detected invalid credentials for Nexus 1000v.";
            s_logger.error(msg);
            throw new Exception(msg);
        }
        return vsmCredentials;
    }

    public static void createPortProfile(VmwareContext context, String ethPortProfileName, String networkName,
            Integer vlanId, Integer networkRateMbps, long peakBandwidth, long burstSize) throws Exception {
        Map<String, String> vsmCredentials = getValidatedVsmCredentials(context);
        String vsmIp = vsmCredentials.get("vsmip");
        String vsmUserName = vsmCredentials.get("vsmusername");
        String vsmPassword = vsmCredentials.get("vsmpassword");
        String msg;

        NetconfHelper netconfClient;
        try {
            s_logger.info("Connecting to Nexus 1000v: " + vsmIp);
            netconfClient = new NetconfHelper(vsmIp, vsmUserName, vsmPassword);
            s_logger.info("Successfully connected to Nexus 1000v : " + vsmIp);
        } catch (CloudRuntimeException e) {
            msg = "Failed to connect to Nexus 1000v " + vsmIp + " with credentials of user " + vsmUserName
                    + ". Exception: " + e.toString();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        String policyName = s_policyNamePrefix;
        int averageBandwidth = 0;
        if (networkRateMbps != null) {
            averageBandwidth = networkRateMbps.intValue();
            policyName += averageBandwidth;
        }

        try {
            // TODO(sateesh): Change the type of peakBandwidth & burstRate in
            // PolicyMap to long.
            if (averageBandwidth > 0) {
                s_logger.debug("Adding policy map " + policyName);
                netconfClient.addPolicyMap(policyName, averageBandwidth, (int) peakBandwidth, (int) burstSize);
            }
        } catch (CloudRuntimeException e) {
            msg = "Failed to add policy map of " + policyName + " with parameters " + "committed rate = "
                    + averageBandwidth + "peak bandwidth = " + peakBandwidth + "burst size = " + burstSize
                    + ". Exception: " + e.toString();
            s_logger.error(msg);
            if (netconfClient != null) {
                netconfClient.disconnect();
                s_logger.debug("Disconnected Nexus 1000v session.");
            }
            throw new CloudRuntimeException(msg);
        }

        List<Pair<OperationType, String>> params = new ArrayList<Pair<OperationType, String>>();
        if (vlanId != null) {
            // No need to update ethernet port profile for untagged vlans
            params.add(new Pair<OperationType, String>(OperationType.addvlanid, vlanId.toString()));
            try {
                s_logger.info("Updating Ethernet port profile " + ethPortProfileName + " with VLAN " + vlanId);
                netconfClient.updatePortProfile(ethPortProfileName, SwitchPortMode.trunk, params);
                s_logger.info("Added " + vlanId + " to Ethernet port profile " + ethPortProfileName);
            } catch (CloudRuntimeException e) {
                msg = "Failed to update Ethernet port profile " + ethPortProfileName + " with VLAN " + vlanId
                        + ". Exception: " + e.toString();
                s_logger.error(msg);
                if(netconfClient != null) {
                	netconfClient.disconnect();
                	s_logger.debug("Disconnected Nexus 1000v session.");
                }
                throw new CloudRuntimeException(msg);
            }
        }

        try {
            if (vlanId == null) {
                s_logger.info("Adding port profile configured over untagged VLAN.");
                netconfClient.addPortProfile(networkName, PortProfileType.vethernet, BindingType.portbindingstatic, SwitchPortMode.access, 0);
            } else {
                s_logger.info("Adding port profile configured over VLAN : " + vlanId.toString());
                netconfClient.addPortProfile(networkName, PortProfileType.vethernet, BindingType.portbindingstatic, SwitchPortMode.access, vlanId.intValue());
            }
        } catch (CloudRuntimeException e) {
            msg = "Failed to add vEthernet port profile " + networkName + "." + ". Exception: " + e.toString();
            s_logger.error(msg);
            if(netconfClient != null) {
                netconfClient.disconnect();
                s_logger.debug("Disconnected Nexus 1000v session.");
            }
            throw new CloudRuntimeException(msg);
        }

        try {
            if (averageBandwidth > 0) {
                s_logger.info("Associating policy map " + policyName + " with port profile " + networkName + ".");
                netconfClient.attachServicePolicy(policyName, networkName);
            }
        }
        catch(CloudRuntimeException e) {
            msg = "Failed to associate policy map " + policyName + " with port profile " + networkName
                    + ". Exception: " + e.toString();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        } finally {
            if (netconfClient != null) {
                netconfClient.disconnect();
                s_logger.debug("Disconnected Nexus 1000v session.");
            }
        }
    }

    public static void updatePortProfile(VmwareContext context, String ethPortProfileName, String vethPortProfileName,
            Integer vlanId, Integer networkRateMbps, long peakBandwidth, long burstRate) throws Exception {
        NetconfHelper netconfClient = null;
        Map<String, String> vsmCredentials = getValidatedVsmCredentials(context);
        String vsmIp = vsmCredentials.get("vsmip");
        String vsmUserName = vsmCredentials.get("vsmusername");
        String vsmPassword = vsmCredentials.get("vsmpassword");

        String msg;
        try {
            netconfClient = new NetconfHelper(vsmIp, vsmUserName, vsmPassword);
        } catch (CloudRuntimeException e) {
            msg = "Failed to connect to Nexus 1000v " + vsmIp + " with credentials of user " + vsmUserName
                    + ". Exception: " + e.toString();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg);
        }

        PortProfile portProfile = netconfClient.getPortProfileByName(vethPortProfileName);
        int averageBandwidth = 0;
        String policyName = s_policyNamePrefix;
        if (networkRateMbps != null) {
            averageBandwidth = networkRateMbps.intValue();
            policyName += averageBandwidth;
        }

        if (averageBandwidth > 0) {
            PolicyMap policyMap = netconfClient.getPolicyMapByName(portProfile.inputPolicyMap);
            if (policyMap.committedRate == averageBandwidth && policyMap.peakRate == peakBandwidth
                    && policyMap.burstRate == burstRate) {
                s_logger.debug("Detected that policy map is already applied to port profile " + vethPortProfileName);
                if (netconfClient != null) {
                    netconfClient.disconnect();
                    s_logger.debug("Disconnected Nexus 1000v session.");
                }
                return;
            } else {
                try {
                    // TODO(sateesh): Change the type of peakBandwidth &
                    // burstRate in PolicyMap to long.
                    s_logger.info("Adding policy map " + policyName);
                    netconfClient.addPolicyMap(policyName, averageBandwidth, (int) peakBandwidth, (int) burstRate);
                } catch (CloudRuntimeException e) {
                    msg = "Failed to add policy map of " + policyName + " with parameters " + "committed rate = "
                            + averageBandwidth + "peak bandwidth = " + peakBandwidth + "burst size = " + burstRate
                            + ". Exception: " + e.toString();
                    s_logger.error(msg);
                    if (netconfClient != null) {
                        netconfClient.disconnect();
                        s_logger.debug("Disconnected Nexus 1000v session.");
                    }
                    throw new CloudRuntimeException(msg);
                }

                try {
                    s_logger.info("Associating policy map " + policyName + " with port profile " + vethPortProfileName
                            + ".");
                    netconfClient.attachServicePolicy(policyName, vethPortProfileName);
                } catch (CloudRuntimeException e) {
                    msg = "Failed to associate policy map " + policyName + " with port profile " + vethPortProfileName
                            + ". Exception: " + e.toString();
                    s_logger.error(msg);
                    if (netconfClient != null) {
                        netconfClient.disconnect();
                        s_logger.debug("Disconnected Nexus 1000v session.");
                    }
                    throw new CloudRuntimeException(msg);
                }
            }
        }

        if (vlanId == null) {
            s_logger.info("Skipping update operation over ethernet port profile " + ethPortProfileName
                    + " for untagged VLAN.");
            if (netconfClient != null) {
                netconfClient.disconnect();
                s_logger.debug("Disconnected Nexus 1000v session.");
            }
            return;
        }

        String currentVlan = portProfile.vlan;
        String newVlan = Integer.toString(vlanId.intValue());
        if (currentVlan.equalsIgnoreCase(newVlan)) {
            if (netconfClient != null) {
                netconfClient.disconnect();
                s_logger.debug("Disconnected Nexus 1000v session.");
            }
            return;
        }

        List<Pair<OperationType, String>> params = new ArrayList<Pair<OperationType, String>>();
        params.add(new Pair<OperationType, String>(OperationType.addvlanid, newVlan));
        try {
            s_logger.info("Updating vEthernet port profile with VLAN " + vlanId.toString());
            netconfClient.updatePortProfile(ethPortProfileName, SwitchPortMode.trunk, params);
        } catch (CloudRuntimeException e) {
            msg = "Failed to update ethernet port profile " + ethPortProfileName + " with parameters "
                    + params.toString() + ". Exception: " + e.toString();
            s_logger.error(msg);
            if (netconfClient != null) {
                netconfClient.disconnect();
                s_logger.debug("Disconnected Nexus 1000v session.");
            }
            throw new CloudRuntimeException(msg);
        }

        try {
            netconfClient.updatePortProfile(vethPortProfileName, SwitchPortMode.access, params);
        } catch (CloudRuntimeException e) {
            msg = "Failed to update vEthernet port profile " + vethPortProfileName + " with parameters "
                    + params.toString() + ". Exception: " + e.toString();
            s_logger.error(msg);
            if (netconfClient != null) {
                netconfClient.disconnect();
                s_logger.debug("Disconnected Nexus 1000v session.");
            }
            throw new CloudRuntimeException(msg);
        }
    }

	/**
	 * @param ethPortProfileName
	 * @param namePrefix
	 * @param hostMo
	 * @param vlanId
	 * @param networkRateMbps
	 * @param networkRateMulticastMbps
	 * @param timeOutMs
     * @param vSwitchType
     * @param numPorts
	 * @return
	 * @throws Exception
	 */

    public static Pair<ManagedObjectReference, String> prepareNetwork(String physicalNetwork, String namePrefix,
            HostMO hostMo, String vlanId, Integer networkRateMbps, Integer networkRateMulticastMbps, long timeOutMs,
            VirtualSwitchType vSwitchType, int numPorts) throws Exception {
        ManagedObjectReference morNetwork = null;
        VmwareContext context = hostMo.getContext();
        ManagedObjectReference dcMor = hostMo.getHyperHostDatacenter();
        DatacenterMO dataCenterMo = new DatacenterMO(context, dcMor);
        DistributedVirtualSwitchMO dvSwitchMo = null;
        ManagedObjectReference morEthernetPortProfile = null;
        String ethPortProfileName = null;
        ManagedObjectReference morDvSwitch = null;
        ManagedObjectReference morDvPortGroup = null;
        String dvSwitchName = null;
        boolean bWaitPortGroupReady = false;
        boolean createGCTag = false;
        String networkName;
        Integer vid = null;

        if(vlanId != null && !UNTAGGED_VLAN_NAME.equalsIgnoreCase(vlanId)) {
            createGCTag = true;
            vid = Integer.parseInt(vlanId);
        }
        networkName = composeCloudNetworkName(namePrefix, vlanId, networkRateMbps, physicalNetwork);

        if (vSwitchType == VirtualSwitchType.VMwareDistributedVirtualSwitch) {
            DVSTrafficShapingPolicy shapingPolicy;
            VmwareDistributedVirtualSwitchVlanSpec vlanSpec;
            DVSSecurityPolicy secPolicy;
            VMwareDVSPortSetting dvsPortSetting;
            DVPortgroupConfigSpec dvPortGroupSpec;
            DVPortgroupConfigInfo dvPortgroupInfo;

            dvSwitchName = physicalNetwork;
            // TODO(sateesh): Remove this after ensuring proper default value for vSwitchName throughout traffic types
            // and switch types.
            if (dvSwitchName == null) {
                s_logger.warn("Detected null dvSwitch. Defaulting to dvSwitch0");
                dvSwitchName = "dvSwitch0";
            }
            morDvSwitch = dataCenterMo.getDvSwitchMor(dvSwitchName);
            if (morDvSwitch == null) {
                String msg = "Unable to find distributed vSwitch " + morDvSwitch;
                s_logger.error(msg);
                throw new Exception(msg);
            } else {
                s_logger.info("Found distributed vSwitch " + morDvSwitch);
            }

            dvSwitchMo = new DistributedVirtualSwitchMO(context, morDvSwitch);

            shapingPolicy = getDVSShapingPolicy(networkRateMbps);
            if (vid != null) {
                vlanSpec = createDVPortVlanIdSpec(vid);
            } else {
                vlanSpec = createDVPortVlanSpec();
            }
            secPolicy = createDVSSecurityPolicy();
            dvsPortSetting = createVmwareDVPortSettingSpec(shapingPolicy, secPolicy, vlanSpec);
            dvPortGroupSpec = createDvPortGroupSpec(networkName, dvsPortSetting, numPorts);

            if (!dataCenterMo.hasDvPortGroup(networkName)) {
                s_logger.info("Distributed Virtual Port group " + networkName + " not found.");
                // TODO(sateesh): Handle Exceptions
                try {
                    dvSwitchMo.createDVPortGroup(dvPortGroupSpec);
                } catch (Exception e) {
                    String msg = "Failed to create distributed virtual port group " + networkName + " on dvSwitch " + physicalNetwork;
                    throw new Exception(msg);
                }
                bWaitPortGroupReady = true;
            } else {
                s_logger.info("Found Distributed Virtual Port group " + networkName);
                // TODO(sateesh): Handle Exceptions
                dvPortgroupInfo = dataCenterMo.getDvPortGroupSpec(networkName);
                if (!isSpecMatch(dvPortgroupInfo, vid, shapingPolicy)) {
                    s_logger.info("Updating Distributed Virtual Port group " + networkName);
                    dvPortGroupSpec.setDefaultPortConfig(dvsPortSetting);
                    dvPortGroupSpec.setConfigVersion(dvPortgroupInfo.getConfigVersion());
                    morDvPortGroup = dataCenterMo.getDvPortGroupMor(networkName);
                    try {
                        dvSwitchMo.updateDvPortGroup(morDvPortGroup, dvPortGroupSpec);
                    } catch (Exception e) {
                        String msg = "Failed to update distributed virtual port group " + networkName + " on dvSwitch " + physicalNetwork;
                        throw new Exception(msg);
                    }
                    bWaitPortGroupReady = true;
                }
            }
        } else if (vSwitchType == VirtualSwitchType.NexusDistributedVirtualSwitch) {
            ethPortProfileName = physicalNetwork;
            // TODO(sateesh): Remove this after ensuring proper default value for vSwitchName throughout traffic types
            // and switch types.
            if (ethPortProfileName == null) {
                s_logger.warn("Detected null ethrenet port profile. Defaulting to epp0.");
                ethPortProfileName = "epp0";
            }
            morEthernetPortProfile = dataCenterMo.getDvPortGroupMor(ethPortProfileName);
            if (morEthernetPortProfile == null) {
                String msg = "Unable to find Ethernet port profile " + ethPortProfileName;
                s_logger.error(msg);
                throw new Exception(msg);
            } else {
                s_logger.info("Found Ethernet port profile " + ethPortProfileName);
            }
        long averageBandwidth = 0L;
        if (networkRateMbps != null && networkRateMbps.intValue() > 0) {
            averageBandwidth = (long) (networkRateMbps.intValue() * 1024L * 1024L);
        }
        // We chose 50% higher allocation than average bandwidth.
            // TODO(sateesh): Optionally let user specify the peak coefficient
        long peakBandwidth = (long) (averageBandwidth * 1.5);
            // TODO(sateesh): Optionally let user specify the burst coefficient
        long burstSize = 5 * averageBandwidth / 8;

        if (!dataCenterMo.hasDvPortGroup(networkName)) {
            s_logger.info("Port profile " + networkName + " not found.");
                createPortProfile(context, physicalNetwork, networkName, vid, networkRateMbps, peakBandwidth, burstSize);
            bWaitPortGroupReady = true;
        } else {
            s_logger.info("Port profile " + networkName + " found.");
                updatePortProfile(context, physicalNetwork, networkName, vid, networkRateMbps, peakBandwidth, burstSize);
            }
        }
        // Wait for dvPortGroup on vCenter
        if (bWaitPortGroupReady)
            morNetwork = waitForDvPortGroupReady(dataCenterMo, networkName, timeOutMs);
        else
            morNetwork = dataCenterMo.getDvPortGroupMor(networkName);
        if (morNetwork == null) {
            String msg = "Failed to create guest network " + networkName;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        if(createGCTag) {
            NetworkMO networkMo = new NetworkMO(hostMo.getContext(), morNetwork);
            networkMo.setCustomFieldValue(CustomFieldConstants.CLOUD_GC_DVP, "true");
            s_logger.debug("Added custom field : " + CustomFieldConstants.CLOUD_GC_DVP);
        }

        return new Pair<ManagedObjectReference, String>(morNetwork, networkName);
    }

    public static ManagedObjectReference waitForDvPortGroupReady(
			DatacenterMO dataCenterMo, String dvPortGroupName, long timeOutMs) throws Exception {
		ManagedObjectReference morDvPortGroup = null;

		// if DvPortGroup is just created, we may fail to retrieve it, we
		// need to retry
		long startTick = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTick <= timeOutMs) {
			morDvPortGroup = dataCenterMo.getDvPortGroupMor(dvPortGroupName);
			if (morDvPortGroup != null) {
				break;
			}

			s_logger.info("Waiting for dvPortGroup " + dvPortGroupName + " to be ready");
			Thread.sleep(1000);
		}
		return morDvPortGroup;
	}

    public static boolean isSpecMatch(DVPortgroupConfigInfo configInfo, Integer vid, DVSTrafficShapingPolicy shapingPolicy) {
		DVSTrafficShapingPolicy currentTrafficShapingPolicy;
        currentTrafficShapingPolicy = configInfo.getDefaultPortConfig().getInShapingPolicy();

		assert(currentTrafficShapingPolicy != null);

		LongPolicy averageBandwidth = currentTrafficShapingPolicy.getAverageBandwidth();
		LongPolicy burstSize = currentTrafficShapingPolicy.getBurstSize();
		LongPolicy peakBandwidth = currentTrafficShapingPolicy.getPeakBandwidth();
		BoolPolicy isEnabled = currentTrafficShapingPolicy.getEnabled();

        if (!isEnabled.equals(shapingPolicy.getEnabled())) {
			return false;
        }

		if(averageBandwidth != null && !averageBandwidth.equals(shapingPolicy.getAverageBandwidth())) {
			if(s_logger.isInfoEnabled()) {
				s_logger.info("Average bandwidth setting in shaping policy doesn't match with existing setting.");
			}
			return false;
		} else if(burstSize != null && !burstSize.equals(shapingPolicy.getBurstSize())) {
			if(s_logger.isInfoEnabled()) {
				s_logger.info("Burst size setting in shaping policy doesn't match with existing setting.");
			}
			return false;
		} else if(peakBandwidth != null && !peakBandwidth.equals(shapingPolicy.getPeakBandwidth())) {
			if(s_logger.isInfoEnabled()) {
				s_logger.info("Peak bandwidth setting in shaping policy doesn't match with existing setting.");
			}
			return false;
		}

		return true;
	}

    public static DVPortgroupConfigSpec createDvPortGroupSpec(String dvPortGroupName, DVPortSetting portSetting, int numPorts) {
        DVPortgroupConfigSpec spec = new DVPortgroupConfigSpec();
        spec.setName(dvPortGroupName);
        spec.setDefaultPortConfig(portSetting);
        spec.setPortNameFormat("vnic<portIndex>");
        spec.setType("earlyBinding");
        spec.setNumPorts(numPorts);
        // TODO(sateesh): Get vSphere API version and
        // if >= 5.0 set autoExpand property of dvPortGroup config spec to true.
        // spec.setAutoExpand(true);
        return spec;
    }

    public static VMwareDVSPortSetting createVmwareDVPortSettingSpec(DVSTrafficShapingPolicy shapingPolicy, DVSSecurityPolicy secPolicy, VmwareDistributedVirtualSwitchVlanSpec vlanSpec) {
        VMwareDVSPortSetting dvsPortSetting = new VMwareDVSPortSetting();
        dvsPortSetting.setVlan(vlanSpec);
        dvsPortSetting.setSecurityPolicy(secPolicy);
        dvsPortSetting.setInShapingPolicy(shapingPolicy);
        dvsPortSetting.setOutShapingPolicy(shapingPolicy);

        return dvsPortSetting;
    }

    public static DVSTrafficShapingPolicy getDVSShapingPolicy(Integer networkRateMbps) {
        DVSTrafficShapingPolicy shapingPolicy = new DVSTrafficShapingPolicy();
        if (networkRateMbps == null || networkRateMbps.intValue() <= 0) {
            return shapingPolicy;
        }
        shapingPolicy = new DVSTrafficShapingPolicy();
        BoolPolicy isEnabled = new BoolPolicy();
        LongPolicy averageBandwidth = new LongPolicy();
        LongPolicy peakBandwidth = new LongPolicy();
        LongPolicy burstSize = new LongPolicy();

        isEnabled.setValue(true);
        averageBandwidth.setValue((long) networkRateMbps.intValue() * 1024L * 1024L);
        // We chose 50% higher allocation than average bandwidth.
        // TODO(sateesh): Also let user specify the peak coefficient
        peakBandwidth.setValue((long) (averageBandwidth.getValue() * 1.5));
        // TODO(sateesh): Also let user specify the burst coefficient
        burstSize.setValue((long) (5 * averageBandwidth.getValue() / 8));

        shapingPolicy.setEnabled(isEnabled);
        shapingPolicy.setAverageBandwidth(averageBandwidth);
        shapingPolicy.setPeakBandwidth(peakBandwidth);
        shapingPolicy.setBurstSize(burstSize);

        return shapingPolicy;
    }

    public static VmwareDistributedVirtualSwitchVlanIdSpec createDVPortVlanIdSpec(int vlanId) {
        VmwareDistributedVirtualSwitchVlanIdSpec vlanIdSpec = new VmwareDistributedVirtualSwitchVlanIdSpec();
        vlanIdSpec.setVlanId(vlanId);
        return vlanIdSpec;
    }

    public static VmwareDistributedVirtualSwitchVlanSpec createDVPortVlanSpec() {
        VmwareDistributedVirtualSwitchVlanSpec vlanSpec = new VmwareDistributedVirtualSwitchVlanSpec();
        return vlanSpec;
    }

    public static DVSSecurityPolicy createDVSSecurityPolicy() {
        DVSSecurityPolicy secPolicy = new DVSSecurityPolicy();
        BoolPolicy allow = new BoolPolicy();
        allow.setValue(true);

        secPolicy.setForgedTransmits(allow);
        secPolicy.setAllowPromiscuous(allow);
        secPolicy.setMacChanges(allow);
        return secPolicy;
    }

	public static Pair<ManagedObjectReference, String> prepareNetwork(String vSwitchName, String namePrefix,
            HostMO hostMo, String vlanId, Integer networkRateMbps, Integer networkRateMulticastMbps,
            long timeOutMs, boolean syncPeerHosts) throws Exception {

        HostVirtualSwitch vSwitch;
        if (vSwitchName == null) {
            s_logger.info("Detected vswitch name as undefined. Defaulting to vSwitch0");
            vSwitchName = "vSwitch0";
        }
        vSwitch = hostMo.getHostVirtualSwitchByName(vSwitchName);

        if (vSwitch == null) {
            String msg = "Unable to find vSwitch" + vSwitchName;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        boolean createGCTag = false;
        String networkName;
        Integer vid = null;

        if(vlanId != null && !UNTAGGED_VLAN_NAME.equalsIgnoreCase(vlanId)) {
            createGCTag = true;
            vid = Integer.parseInt(vlanId);
        }

        networkName = composeCloudNetworkName(namePrefix, vlanId, networkRateMbps, vSwitchName);
        HostNetworkSecurityPolicy secPolicy = null;
        if (namePrefix.equalsIgnoreCase("cloud.private")) {
            secPolicy = new HostNetworkSecurityPolicy();
            secPolicy.setAllowPromiscuous(Boolean.TRUE);
            secPolicy.setForgedTransmits(Boolean.TRUE);
            secPolicy.setMacChanges(Boolean.TRUE);
        }
        HostNetworkTrafficShapingPolicy shapingPolicy = null;
        if(networkRateMbps != null && networkRateMbps.intValue() > 0) {
            shapingPolicy = new HostNetworkTrafficShapingPolicy();
            shapingPolicy.setEnabled(true);
            shapingPolicy.setAverageBandwidth((long)networkRateMbps.intValue()*1024L*1024L);

            //
            // TODO : people may have different opinion on how to set the following
            //

            // give 50% premium to peek
            shapingPolicy.setPeakBandwidth((long)(shapingPolicy.getAverageBandwidth()*1.5));

            // allow 5 seconds of burst transfer
            shapingPolicy.setBurstSize(5*shapingPolicy.getAverageBandwidth()/8);
        }

        boolean bWaitPortGroupReady = false;
        if (!hostMo.hasPortGroup(vSwitch, networkName)) {
            hostMo.createPortGroup(vSwitch, networkName, vid, secPolicy, shapingPolicy);
            bWaitPortGroupReady = true;
        } else {
            HostPortGroupSpec spec = hostMo.getPortGroupSpec(networkName);
            if(!isSpecMatch(spec, vid, shapingPolicy)) {
                hostMo.updatePortGroup(vSwitch, networkName, vid, secPolicy, shapingPolicy);
                bWaitPortGroupReady = true;
            }
        }

        ManagedObjectReference morNetwork;
        if(bWaitPortGroupReady)
            morNetwork = waitForNetworkReady(hostMo, networkName, timeOutMs);
        else
            morNetwork = hostMo.getNetworkMor(networkName);
        if (morNetwork == null) {
            String msg = "Failed to create guest network " + networkName;
            s_logger.error(msg);
            throw new Exception(msg);
        }

        if(createGCTag) {
            NetworkMO networkMo = new NetworkMO(hostMo.getContext(), morNetwork);
            networkMo.setCustomFieldValue(CustomFieldConstants.CLOUD_GC, "true");
        }

        if(syncPeerHosts) {
            ManagedObjectReference morParent = hostMo.getParentMor();
            if(morParent != null && morParent.getType().equals("ClusterComputeResource")) {
                // to be conservative, lock cluster
                GlobalLock lock = GlobalLock.getInternLock("ClusterLock." + morParent.getValue());
                try {
                    if(lock.lock(DEFAULT_LOCK_TIMEOUT_SECONDS)) {
                        try {
                            List<ManagedObjectReference> hosts = (List<ManagedObjectReference>)hostMo.getContext().getVimClient().getDynamicProperty(morParent, "host");
                            if(hosts != null) {
                                for(ManagedObjectReference otherHost: hosts) {
                                    if(!otherHost.getValue().equals(hostMo.getMor().getValue())) {
                                        HostMO otherHostMo = new HostMO(hostMo.getContext(), otherHost);
                                        try {
                                            if(s_logger.isDebugEnabled())
                                                s_logger.debug("Prepare network on other host, vlan: " + vlanId + ", host: " + otherHostMo.getHostName());
                                            prepareNetwork(vSwitchName, namePrefix, otherHostMo, vlanId, networkRateMbps, networkRateMulticastMbps, timeOutMs, false);
                                        } catch(Exception e) {
                                            s_logger.warn("Unable to prepare network on other host, vlan: " + vlanId + ", host: " + otherHostMo.getHostName());
                                        }
                                    }
                                }
                            }
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        s_logger.warn("Unable to lock cluster to prepare guest network, vlan: " + vlanId);
                    }
                } finally {
                    lock.releaseRef();
                }
            }
        }

        s_logger.info("Network " + networkName + " is ready on vSwitch " + vSwitchName);
        return new Pair<ManagedObjectReference, String>(morNetwork, networkName);
    }

	private static boolean isSpecMatch(HostPortGroupSpec spec, Integer vlanId, HostNetworkTrafficShapingPolicy shapingPolicy) {
		// check VLAN configuration
		if(vlanId != null) {
			if(vlanId.intValue() != spec.getVlanId())
				return false;
		} else {
			if(spec.getVlanId() != 0)
				return false;
		}

		// check traffic shaping configuration
		HostNetworkTrafficShapingPolicy policyInSpec = null;
		if(spec.getPolicy() != null)
			policyInSpec = spec.getPolicy().getShapingPolicy();

		if(policyInSpec != null && shapingPolicy == null || policyInSpec == null && shapingPolicy != null)
			return false;

		if(policyInSpec == null && shapingPolicy == null)
			return true;

		// so far policyInSpec and shapingPolicy should both not be null
		if(policyInSpec.isEnabled() == null || !policyInSpec.isEnabled().booleanValue())
			return false;

		if(policyInSpec.getAverageBandwidth() == null || policyInSpec.getAverageBandwidth().longValue() != shapingPolicy.getAverageBandwidth().longValue())
			return false;

		if(policyInSpec.getPeakBandwidth() == null || policyInSpec.getPeakBandwidth().longValue() != shapingPolicy.getPeakBandwidth().longValue())
			return false;

		if(policyInSpec.getBurstSize() == null || policyInSpec.getBurstSize().longValue() != shapingPolicy.getBurstSize().longValue())
			return false;

		return true;
	}

	public static ManagedObjectReference waitForNetworkReady(HostMO hostMo,
			String networkName, long timeOutMs) throws Exception {

		ManagedObjectReference morNetwork = null;

		// if portGroup is just created, getNetwork may fail to retrieve it, we
		// need to retry
		long startTick = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTick <= timeOutMs) {
			morNetwork = hostMo.getNetworkMor(networkName);
			if (morNetwork != null) {
				break;
			}

			s_logger.info("Waiting for network " + networkName + " to be ready");
			Thread.sleep(1000);
		}

		return morNetwork;
	}

	public static boolean createBlankVm(VmwareHypervisorHost host, String vmName,
		int cpuCount, int cpuSpeedMHz, int cpuReservedMHz, boolean limitCpuUse, int memoryMB, int memoryReserveMB, String guestOsIdentifier,
		ManagedObjectReference morDs, boolean snapshotDirToParent) throws Exception {

		if(s_logger.isInfoEnabled())
			s_logger.info("Create blank VM. cpuCount: " + cpuCount + ", cpuSpeed(MHz): " + cpuSpeedMHz + ", mem(Mb): " + memoryMB);

		// VM config basics
		VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();
		vmConfig.setName(vmName);
		VmwareHelper.setBasicVmConfig(vmConfig, cpuCount, cpuSpeedMHz, cpuReservedMHz, memoryMB, memoryReserveMB, guestOsIdentifier, limitCpuUse);

		// Scsi controller
		VirtualLsiLogicController scsiController = new VirtualLsiLogicController();
		scsiController.setSharedBus(VirtualSCSISharing.NO_SHARING);
		scsiController.setBusNumber(0);
		scsiController.setKey(1);
		VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
		scsiControllerSpec.setDevice(scsiController);
		scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

		VirtualMachineFileInfo fileInfo = new VirtualMachineFileInfo();
		DatastoreMO dsMo = new DatastoreMO(host.getContext(), morDs);
		fileInfo.setVmPathName(String.format("[%s]", dsMo.getName()));
		vmConfig.setFiles(fileInfo);

		VirtualMachineVideoCard videoCard = new VirtualMachineVideoCard();
		videoCard.setControllerKey(100);
		videoCard.setUseAutoDetect(true);

		VirtualDeviceConfigSpec videoDeviceSpec = new VirtualDeviceConfigSpec();
		videoDeviceSpec.setDevice(videoCard);
		videoDeviceSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);

		vmConfig.getDeviceChange().add(scsiControllerSpec);
		vmConfig.getDeviceChange().add(videoDeviceSpec);
		if(host.createVm(vmConfig)) {
			VirtualMachineMO vmMo = host.findVmOnHyperHost(vmName);
			assert(vmMo != null);

			int ideControllerKey = -1;
			while(ideControllerKey < 0) {
				ideControllerKey = vmMo.tryGetIDEDeviceControllerKey();
				if(ideControllerKey >= 0)
					break;

				s_logger.info("Waiting for IDE controller be ready in VM: " + vmName);
				Thread.sleep(1000);
			}

			if(snapshotDirToParent) {
				String snapshotDir = String.format("/vmfs/volumes/%s/", dsMo.getName());

				s_logger.info("Switch snapshot working directory to " + snapshotDir + " for " + vmName);
				vmMo.setSnapshotDirectory(snapshotDir);

				// Don't have a good way to test if the VM is really ready for use through normal API after configuration file manipulation,
				// delay 3 seconds
				Thread.sleep(3000);
			}

			s_logger.info("Blank VM: " + vmName + " is ready for use");
			return true;
		}
		return false;
	}

	public static String resolveHostNameInUrl(DatacenterMO dcMo, String url) {

		s_logger.info("Resolving host name in url through vCenter, url: " + url);

		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			s_logger.warn("URISyntaxException on url " + url);
			return url;
		}

		String host = uri.getHost();
		if(NetUtils.isValidIp(host)) {
			s_logger.info("host name in url is already in IP address, url: " + url);
			return url;
		}

		try {
			ManagedObjectReference morHost = dcMo.findHost(host);
			if(morHost != null) {
				HostMO hostMo = new HostMO(dcMo.getContext(), morHost);
				String managementPortGroupName;
				if(hostMo.getHostType() == VmwareHostType.ESXi)
					managementPortGroupName = (String)dcMo.getContext().getStockObject("manageportgroup");
				else
					managementPortGroupName = (String)dcMo.getContext().getStockObject("serviceconsole");

				VmwareHypervisorHostNetworkSummary summary = hostMo.getHyperHostNetworkSummary(managementPortGroupName);
				if(summary == null) {
					s_logger.warn("Unable to resolve host name in url through vSphere, url: " + url);
					return url;
				}

				String hostIp = summary.getHostIp();

				try {
					URI resolvedUri = new URI(uri.getScheme(), uri.getUserInfo(), hostIp, uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());

					s_logger.info("url " + url + " is resolved to " + resolvedUri.toString() + " through vCenter");
					return resolvedUri.toString();
				} catch (URISyntaxException e) {
					assert(false);
					return url;
				}
			}
		} catch(Exception e) {
			s_logger.warn("Unexpected exception ", e);
		}

		return url;
	}

	public static void importVmFromOVF(VmwareHypervisorHost host, String ovfFilePath, String vmName, DatastoreMO dsMo, String diskOption,
		ManagedObjectReference morRp, ManagedObjectReference morHost) throws Exception {

		assert(morRp != null);

		OvfCreateImportSpecParams importSpecParams = new OvfCreateImportSpecParams();
		importSpecParams.setHostSystem(morHost);
		importSpecParams.setLocale("US");
		importSpecParams.setEntityName(vmName);
		importSpecParams.setDeploymentOption("");
		importSpecParams.setDiskProvisioning(diskOption); // diskOption: thin, thick, etc
		//importSpecParams.setPropertyMapping(null);

		String ovfDescriptor = HttpNfcLeaseMO.readOvfContent(ovfFilePath);
		VmwareContext context = host.getContext();
		OvfCreateImportSpecResult ovfImportResult = context.getService().createImportSpec(
			context.getServiceContent().getOvfManager(), ovfDescriptor, morRp,
			dsMo.getMor(), importSpecParams);

		if(ovfImportResult == null) {
			String msg = "createImportSpec() failed. ovfFilePath: " + ovfFilePath + ", vmName: "
				+ vmName + ", diskOption: " + diskOption;
			s_logger.error(msg);
			throw new Exception(msg);
		}

		DatacenterMO dcMo = new DatacenterMO(context, host.getHyperHostDatacenter());
		ManagedObjectReference morLease = context.getService().importVApp(morRp,
			ovfImportResult.getImportSpec(), dcMo.getVmFolder(), morHost);
		if(morLease == null) {
			String msg = "importVApp() failed. ovfFilePath: " + ovfFilePath + ", vmName: "
				+ vmName + ", diskOption: " + diskOption;
			s_logger.error(msg);
			throw new Exception(msg);
		}
        boolean importSuccess = true;
		final HttpNfcLeaseMO leaseMo = new HttpNfcLeaseMO(context, morLease);
		HttpNfcLeaseState state = leaseMo.waitState(
			new HttpNfcLeaseState[] { HttpNfcLeaseState.READY, HttpNfcLeaseState.ERROR });
		try {
			if(state == HttpNfcLeaseState.READY) {
				final long totalBytes = HttpNfcLeaseMO.calcTotalBytes(ovfImportResult);
				File ovfFile = new File(ovfFilePath);

				HttpNfcLeaseInfo httpNfcLeaseInfo = leaseMo.getLeaseInfo();
		        List<HttpNfcLeaseDeviceUrl> deviceUrls = httpNfcLeaseInfo.getDeviceUrl();
		        long bytesAlreadyWritten = 0;

		        final HttpNfcLeaseMO.ProgressReporter progressReporter = leaseMo.createProgressReporter();
		        try {
			        for (HttpNfcLeaseDeviceUrl deviceUrl : deviceUrls) {
			        	String deviceKey = deviceUrl.getImportKey();
			        	for (OvfFileItem ovfFileItem : ovfImportResult.getFileItem()) {
			        		if (deviceKey.equals(ovfFileItem.getDeviceId())) {
			        			String absoluteFile = ovfFile.getParent() + File.separator + ovfFileItem.getPath();
			        			String urlToPost = deviceUrl.getUrl();
			        			urlToPost = resolveHostNameInUrl(dcMo, urlToPost);

		        			  	context.uploadVmdkFile(ovfFileItem.isCreate() ? "PUT" : "POST", urlToPost, absoluteFile,
		    			  			bytesAlreadyWritten, new ActionDelegate<Long> () {
									public void action(Long param) {
										progressReporter.reportProgress((int)(param * 100 / totalBytes));
									}
		    			  		});

		        			  	bytesAlreadyWritten += ovfFileItem.getSize();
		        			 }
			        	 }
			        }
                } catch (Exception e) {
                    s_logger.error("Failed to complete file upload task. " + e.getMessage());
                    // Set flag to cleanup the stale template left due to failed import operation, if any
                    importSuccess = false;
                    throw e;
		        } finally {
		        	progressReporter.close();
		        }
                if (bytesAlreadyWritten == totalBytes) {
		        leaseMo.updateLeaseProgress(100);
			}
            }
		} finally {
            if (!importSuccess) {
                s_logger.error("Aborting the lease on " + vmName + " after import operation failed.");
                leaseMo.abortLease();
            } else {
			leaseMo.completeLease();
		}
	}
	}
}
