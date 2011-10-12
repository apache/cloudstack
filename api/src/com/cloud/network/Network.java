/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

/**
 * 
 */
package com.cloud.network;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.cloud.acl.ControlledEntity;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.fsm.FiniteState;
import com.cloud.utils.fsm.StateMachine;

/**
 * A NetworkProfile defines the specifics of a network
 * owned by an account. 
 */
public interface Network extends ControlledEntity {

    public enum GuestIpType {
        Virtual,
        Direct,
    }

    public static class Service {
        private static List<Service> supportedServices = new ArrayList<Service>();

        public static final Service Vpn = new Service("Vpn", Capability.SupportedVpnTypes);
        public static final Service Dhcp = new Service("Dhcp");
        public static final Service Dns = new Service("Dns", Capability.AllowDnsSuffixModification);
        public static final Service Gateway = new Service("Gateway");
        public static final Service Firewall = new Service("Firewall", Capability.PortForwarding, Capability.StaticNat, Capability.SupportedProtocols, Capability.MultipleIps, Capability.SupportedSourceNatTypes, Capability.TrafficStatistics);
        public static final Service Lb = new Service("Lb", Capability.SupportedLBAlgorithms, Capability.SupportedProtocols, Capability.TrafficStatistics, Capability.LoadBalancingSupportedIps);
        public static final Service UserData = new Service("UserData");
        public static final Service SourceNat = new Service("SourceNat");

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
    }

    public static class Provider {
        private static List<Provider> supportedProviders = new ArrayList<Provider>();

        public static final Provider VirtualRouter = new Provider("VirtualRouter");
        public static final Provider DhcpServer = new Provider("DhcpServer");
        public static final Provider JuniperSRX = new Provider("JuniperSRX");
        public static final Provider F5BigIp = new Provider("F5BigIp");
        public static final Provider NetscalerMPX = new Provider("NetscalerMPX");        
        public static final Provider ExternalDhcpServer = new Provider("ExternalDhcpServer");
        public static final Provider ExternalGateWay = new Provider("ExternalGateWay");
        public static final Provider ElasticLoadBalancerVm = new Provider("ElasticLoadBalancerVm");

        public static final Provider None = new Provider("None");

        private String name;

        public Provider(String name) {
            this.name = name;
            supportedProviders.add(this);
        }

        public String getName() {
            return name;
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

        public static final Capability PortForwarding = new Capability("PortForwarding");
        public static final Capability StaticNat = new Capability("StaticNat");
        public static final Capability SupportedProtocols = new Capability("SupportedProtocols");
        public static final Capability SupportedLBAlgorithms = new Capability("SupportedLbAlgorithms");
        public static final Capability MultipleIps = new Capability("MultipleIps");
        public static final Capability SupportedSourceNatTypes = new Capability("SupportedSourceNatTypes");
        public static final Capability SupportedVpnTypes = new Capability("SupportedVpnTypes");
        public static final Capability TrafficStatistics = new Capability("TrafficStatistics");
        public static final Capability LoadBalancingSupportedIps = new Capability("LoadBalancingSupportedIps");
        public static final Capability AllowDnsSuffixModification = new Capability("AllowDnsSuffixModification");

        private String name;

        public Capability(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    enum Event {
        ImplementNetwork,
        DestroyNetwork,
        OperationSucceeded,
        OperationFailed;
    }

    enum State implements FiniteState<State, Event> {
        Allocated("Indicates the network configuration is in allocated but not setup"),
        Setup("Indicates the network configuration is setup"),
        Implementing("Indicates the network configuration is being implemented"),
        Implemented("Indicates the network configuration is in use"),
        Shutdown("Indicates the network configuration is being destroyed"),
        Destroy("Indicates that the network is destroyed");


        @Override
        public StateMachine<State, Event> getStateMachine() {
            return s_fsm;
        }

        @Override
        public State getNextState(Event event) {
            return s_fsm.getNextState(this, event);
        }

        @Override
        public List<State> getFromStates(Event event) {
            return s_fsm.getFromStates(this, event);
        }

        @Override
        public Set<Event> getPossibleEvents() {
            return s_fsm.getPossibleEvents(this);
        }

        String _description;

        @Override
        public String getDescription() {
            return _description;
        }

        private State(String description) {
            _description = description;
        }

        private static StateMachine<State, Event> s_fsm = new StateMachine<State, Event>();
        static {
            s_fsm.addTransition(State.Allocated, Event.ImplementNetwork, State.Implementing);
            s_fsm.addTransition(State.Implementing, Event.OperationSucceeded, State.Implemented);
            s_fsm.addTransition(State.Implementing, Event.OperationFailed, State.Shutdown);
            s_fsm.addTransition(State.Implemented, Event.DestroyNetwork, State.Shutdown);
            s_fsm.addTransition(State.Shutdown, Event.OperationSucceeded, State.Allocated);
            s_fsm.addTransition(State.Shutdown, Event.OperationFailed, State.Implemented);
        }
    }

    /**
     * @return id of the network profile.  Null means the network profile is not from the database.
     */
    long getId();

    String getName();

    Mode getMode();

    BroadcastDomainType getBroadcastDomainType();

    TrafficType getTrafficType();

    String getGateway();

    String getCidr();

    long getDataCenterId();

    long getNetworkOfferingId();

    State getState();

    long getRelated();

    URI getBroadcastUri();

    GuestIpType getGuestType();

    String getDisplayText();

    boolean getIsShared();

    String getReservationId();

    boolean isDefault();

    String getNetworkDomain();

    boolean isSecurityGroupEnabled();

    List<String> getTags();

}
