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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * owned by an account.
 */
public interface Network extends ControlledEntity, StateObject<Network.State>, InternalIdentity, Identity {

    public enum GuestType {
        Shared,
        Isolated
    }

    public static class Service {
        private static List<Service> supportedServices = new ArrayList<Service>();

        public static final Service Vpn = new Service("Vpn", Capability.SupportedVpnProtocols, Capability.VpnTypes);
        public static final Service Dhcp = new Service("Dhcp");
        public static final Service Dns = new Service("Dns", Capability.AllowDnsSuffixModification);
        public static final Service Gateway = new Service("Gateway");
        public static final Service Firewall = new Service("Firewall", Capability.SupportedProtocols,
                Capability.MultipleIps, Capability.TrafficStatistics, Capability.SupportedTrafficDirection, Capability.SupportedEgressProtocols);
        public static final Service Lb = new Service("Lb", Capability.SupportedLBAlgorithms, Capability.SupportedLBIsolation,
                Capability.SupportedProtocols, Capability.TrafficStatistics, Capability.LoadBalancingSupportedIps,
                Capability.SupportedStickinessMethods, Capability.ElasticLb);
        public static final Service UserData = new Service("UserData");
        public static final Service SourceNat = new Service("SourceNat", Capability.SupportedSourceNatTypes, Capability.RedundantRouter);
        public static final Service StaticNat = new Service("StaticNat", Capability.ElasticIp);
        public static final Service PortForwarding = new Service("PortForwarding");
        public static final Service SecurityGroup = new Service("SecurityGroup");
        public static final Service NetworkACL = new Service("NetworkACL", Capability.SupportedProtocols);
        public static final Service Connectivity = new Service("Connectivity");


        private String name;
        private Capability[] caps;

        public Service(String name, Capability... caps) {
            this.name = name;
            this.caps = caps;
            supportedServices.add(this);
        }

        public String getName() {
            return name;
        }

        public Capability[] getCapabilities() {
            return caps;
        }

        public boolean containsCapability(Capability cap) {
            boolean success = false;
            if (caps != null) {
                int length = caps.length;
                for (int i = 0; i< length; i++) {
                    if (caps[i].getName().equalsIgnoreCase(cap.getName())) {
                        success = true;
                        break;
                    }
                }
            }

            return success;
        }

        public static Service getService(String serviceName) {
            for (Service service : supportedServices) {
                if (service.getName().equalsIgnoreCase(serviceName)) {
                    return service;
                }
            }
            return null;
        }

        public static List<Service> listAllServices(){
            return supportedServices;
        }
    }

    /**
     * Provider -> NetworkElement must always be one-to-one mapping. Thus for each NetworkElement we need a separate Provider added in here.
     */
    public static class Provider {
        private static List<Provider> supportedProviders = new ArrayList<Provider>();

        public static final Provider VirtualRouter = new Provider("VirtualRouter", false);
        public static final Provider JuniperSRX = new Provider("JuniperSRX", true);
        public static final Provider F5BigIp = new Provider("F5BigIp", true);
        public static final Provider Netscaler = new Provider("Netscaler", true);
        public static final Provider ExternalDhcpServer = new Provider("ExternalDhcpServer", true);
        public static final Provider ExternalGateWay = new Provider("ExternalGateWay", true);
        public static final Provider ElasticLoadBalancerVm = new Provider("ElasticLoadBalancerVm", false);
        public static final Provider SecurityGroupProvider = new Provider("SecurityGroupProvider", false);
        public static final Provider VPCVirtualRouter = new Provider("VpcVirtualRouter", false);
        public static final Provider None = new Provider("None", false);
        // NiciraNvp is not an "External" provider, otherwise we get in trouble with NetworkServiceImpl.providersConfiguredForExternalNetworking 
        public static final Provider NiciraNvp = new Provider("NiciraNvp", false);  
        public static final Provider MidokuraMidonet = new Provider("MidokuraMidonet", true);

        private String name;
        private boolean isExternal;

        public Provider(String name, boolean isExternal) {
            this.name = name;
            this.isExternal = isExternal;
            supportedProviders.add(this);
        }

        public String getName() {
            return name;
        }

        public boolean isExternal() {
            return isExternal;
        }

        public static Provider getProvider(String providerName) {
            for (Provider provider : supportedProviders) {
                if (provider.getName().equalsIgnoreCase(providerName)) {
                    return provider;
                }
            }
            return null;
        }
    }

    public static class Capability {

        private static List<Capability> supportedCapabilities = new ArrayList<Capability>();

        public static final Capability SupportedProtocols = new Capability("SupportedProtocols");
        public static final Capability SupportedLBAlgorithms = new Capability("SupportedLbAlgorithms");
        public static final Capability SupportedLBIsolation = new Capability("SupportedLBIsolation");
        public static final Capability SupportedStickinessMethods = new Capability("SupportedStickinessMethods");
        public static final Capability MultipleIps = new Capability("MultipleIps");
        public static final Capability SupportedSourceNatTypes = new Capability("SupportedSourceNatTypes");
        public static final Capability SupportedVpnProtocols = new Capability("SupportedVpnTypes");
        public static final Capability VpnTypes = new Capability("VpnTypes");
        public static final Capability TrafficStatistics = new Capability("TrafficStatistics");
        public static final Capability LoadBalancingSupportedIps = new Capability("LoadBalancingSupportedIps");
        public static final Capability AllowDnsSuffixModification = new Capability("AllowDnsSuffixModification");
        public static final Capability RedundantRouter = new Capability("RedundantRouter");
        public static final Capability ElasticIp = new Capability("ElasticIp");
        public static final Capability ElasticLb = new Capability("ElasticLb");
        public static final Capability AutoScaleCounters = new Capability("AutoScaleCounters");
        public static final Capability InlineMode = new Capability("InlineMode");
        public static final Capability SupportedTrafficDirection = new Capability("SupportedTrafficDirection");
        public static final Capability SupportedEgressProtocols = new Capability("SupportedEgressProtocols");
        public static final Capability HealthCheckPolicy = new Capability("HealthCheckPolicy");

        private String name;

        public Capability(String name) {
            this.name = name;
            supportedCapabilities.add(this);
        }

        public String getName() {
            return name;
        }

        public static Capability getCapability(String capabilityName) {
            for (Capability capability : supportedCapabilities) {
                if (capability.getName().equalsIgnoreCase(capabilityName)) {
                    return capability;
                }
            }
            return null;
        }
    }

    enum Event {
        ImplementNetwork,
        DestroyNetwork,
        OperationSucceeded,
        OperationFailed;
    }

    public enum State {

        Allocated("Indicates the network configuration is in allocated but not setup"),
        Setup("Indicates the network configuration is setup"),
        Implementing("Indicates the network configuration is being implemented"),
        Implemented("Indicates the network configuration is in use"),
        Shutdown("Indicates the network configuration is being destroyed"),
        Destroy("Indicates that the network is destroyed");

        protected static final StateMachine2<State, Network.Event, Network> s_fsm = new StateMachine2<State, Network.Event, Network>();

        static {
            s_fsm.addTransition(State.Allocated, Event.ImplementNetwork, State.Implementing);
            s_fsm.addTransition(State.Implementing, Event.OperationSucceeded, State.Implemented);
            s_fsm.addTransition(State.Implementing, Event.OperationFailed, State.Shutdown);
            s_fsm.addTransition(State.Implemented, Event.DestroyNetwork, State.Shutdown);
            s_fsm.addTransition(State.Shutdown, Event.OperationSucceeded, State.Allocated);
            s_fsm.addTransition(State.Shutdown, Event.OperationFailed, State.Implemented);
        }

        public static StateMachine2<State, Network.Event, Network> getStateMachine() {
            return s_fsm;
        }

        String _description;
        private State(String description) {
            _description = description;
        }
    }

    public class IpAddresses {
    	private String ip4Address;
    	private String ip6Address;
    	
    	public IpAddresses(String ip4Address, String ip6Address) {
    		this.setIp4Address(ip4Address);
    		this.setIp6Address(ip6Address);
    	}

		public String getIp4Address() {
			return ip4Address;
		}

		public void setIp4Address(String ip4Address) {
			this.ip4Address = ip4Address;
		}

		public String getIp6Address() {
			return ip6Address;
		}

		public void setIp6Address(String ip6Address) {
			this.ip6Address = ip6Address;
		}
    }
    
    String getName();

    Mode getMode();

    BroadcastDomainType getBroadcastDomainType();

    TrafficType getTrafficType();

    String getGateway();

    // "cidr" is the Cloudstack managed address space, all CloudStack managed vms get IP address from "cidr",
    // In general "cidr" also serves as the network CIDR
    // But in case IP reservation is configured for a Guest network, "networkcidr" is the Effective network CIDR for that network,
    // "cidr" will still continue to be the effective address space for CloudStack managed vms in that Guest network
    String getCidr();

    // "networkcidr" is the network CIDR of the guest network which uses IP reservation.
    // It is the summation of "cidr" and the reservedIPrange(the address space used for non CloudStack purposes).
    // For networks not configured with IP reservation, "networkcidr" is always null
    String getNetworkCidr();

    String getIp6Gateway();

    String getIp6Cidr();

    long getDataCenterId();

    long getNetworkOfferingId();

    State getState();

    long getRelated();

    URI getBroadcastUri();

    String getDisplayText();

    String getReservationId();

    String getNetworkDomain();

    GuestType getGuestType();

    Long getPhysicalNetworkId();

    void setPhysicalNetworkId(Long physicalNetworkId);

    public void setTrafficType(TrafficType type);

    ACLType getAclType();

    boolean isRestartRequired();

    boolean getSpecifyIpRanges();

    /**
     * @return
     */
    Long getVpcId();

}
