/*
 * Copyright (c) Citrix Systems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   1) Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *   2) Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.xensource.xenapi;

import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VersionException;
import com.xensource.xenapi.Types.XenAPIException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;

/**
 * A physical network interface (note separate VLANs are represented as several PIFs)
 *
 * @author Citrix Systems, Inc.
 */
public class PIF extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    PIF(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a PIF, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof PIF)
        {
            PIF other = (PIF) obj;
            return other.ref.equals(this.ref);
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return ref.hashCode();
    }

    /**
     * Represents all the fields in a PIF
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "device", this.device);
            print.printf("%1$20s: %2$s\n", "network", this.network);
            print.printf("%1$20s: %2$s\n", "host", this.host);
            print.printf("%1$20s: %2$s\n", "MAC", this.MAC);
            print.printf("%1$20s: %2$s\n", "MTU", this.MTU);
            print.printf("%1$20s: %2$s\n", "VLAN", this.VLAN);
            print.printf("%1$20s: %2$s\n", "metrics", this.metrics);
            print.printf("%1$20s: %2$s\n", "physical", this.physical);
            print.printf("%1$20s: %2$s\n", "currentlyAttached", this.currentlyAttached);
            print.printf("%1$20s: %2$s\n", "ipConfigurationMode", this.ipConfigurationMode);
            print.printf("%1$20s: %2$s\n", "IP", this.IP);
            print.printf("%1$20s: %2$s\n", "netmask", this.netmask);
            print.printf("%1$20s: %2$s\n", "gateway", this.gateway);
            print.printf("%1$20s: %2$s\n", "DNS", this.DNS);
            print.printf("%1$20s: %2$s\n", "bondSlaveOf", this.bondSlaveOf);
            print.printf("%1$20s: %2$s\n", "bondMasterOf", this.bondMasterOf);
            print.printf("%1$20s: %2$s\n", "VLANMasterOf", this.VLANMasterOf);
            print.printf("%1$20s: %2$s\n", "VLANSlaveOf", this.VLANSlaveOf);
            print.printf("%1$20s: %2$s\n", "management", this.management);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "disallowUnplug", this.disallowUnplug);
            print.printf("%1$20s: %2$s\n", "tunnelAccessPIFOf", this.tunnelAccessPIFOf);
            print.printf("%1$20s: %2$s\n", "tunnelTransportPIFOf", this.tunnelTransportPIFOf);
            print.printf("%1$20s: %2$s\n", "ipv6ConfigurationMode", this.ipv6ConfigurationMode);
            print.printf("%1$20s: %2$s\n", "IPv6", this.IPv6);
            print.printf("%1$20s: %2$s\n", "ipv6Gateway", this.ipv6Gateway);
            print.printf("%1$20s: %2$s\n", "primaryAddressType", this.primaryAddressType);
            return writer.toString();
        }

        /**
         * Convert a PIF.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("device", this.device == null ? "" : this.device);
            map.put("network", this.network == null ? new Network("OpaqueRef:NULL") : this.network);
            map.put("host", this.host == null ? new Host("OpaqueRef:NULL") : this.host);
            map.put("MAC", this.MAC == null ? "" : this.MAC);
            map.put("MTU", this.MTU == null ? 0 : this.MTU);
            map.put("VLAN", this.VLAN == null ? 0 : this.VLAN);
            map.put("metrics", this.metrics == null ? new PIFMetrics("OpaqueRef:NULL") : this.metrics);
            map.put("physical", this.physical == null ? false : this.physical);
            map.put("currently_attached", this.currentlyAttached == null ? false : this.currentlyAttached);
            map.put("ip_configuration_mode", this.ipConfigurationMode == null ? Types.IpConfigurationMode.UNRECOGNIZED : this.ipConfigurationMode);
            map.put("IP", this.IP == null ? "" : this.IP);
            map.put("netmask", this.netmask == null ? "" : this.netmask);
            map.put("gateway", this.gateway == null ? "" : this.gateway);
            map.put("DNS", this.DNS == null ? "" : this.DNS);
            map.put("bond_slave_of", this.bondSlaveOf == null ? new Bond("OpaqueRef:NULL") : this.bondSlaveOf);
            map.put("bond_master_of", this.bondMasterOf == null ? new LinkedHashSet<Bond>() : this.bondMasterOf);
            map.put("VLAN_master_of", this.VLANMasterOf == null ? new VLAN("OpaqueRef:NULL") : this.VLANMasterOf);
            map.put("VLAN_slave_of", this.VLANSlaveOf == null ? new LinkedHashSet<VLAN>() : this.VLANSlaveOf);
            map.put("management", this.management == null ? false : this.management);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            map.put("disallow_unplug", this.disallowUnplug == null ? false : this.disallowUnplug);
            map.put("tunnel_access_PIF_of", this.tunnelAccessPIFOf == null ? new LinkedHashSet<Tunnel>() : this.tunnelAccessPIFOf);
            map.put("tunnel_transport_PIF_of", this.tunnelTransportPIFOf == null ? new LinkedHashSet<Tunnel>() : this.tunnelTransportPIFOf);
            map.put("ipv6_configuration_mode", this.ipv6ConfigurationMode == null ? Types.Ipv6ConfigurationMode.UNRECOGNIZED : this.ipv6ConfigurationMode);
            map.put("IPv6", this.IPv6 == null ? new LinkedHashSet<String>() : this.IPv6);
            map.put("ipv6_gateway", this.ipv6Gateway == null ? "" : this.ipv6Gateway);
            map.put("primary_address_type", this.primaryAddressType == null ? Types.PrimaryAddressType.UNRECOGNIZED : this.primaryAddressType);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * machine-readable name of the interface (e.g. eth0)
         */
        public String device;
        /**
         * virtual network to which this pif is connected
         */
        public Network network;
        /**
         * physical machine to which this pif is connected
         */
        public Host host;
        /**
         * ethernet MAC address of physical interface
         */
        public String MAC;
        /**
         * MTU in octets
         */
        public Long MTU;
        /**
         * VLAN tag for all traffic passing through this interface
         */
        public Long VLAN;
        /**
         * metrics associated with this PIF
         */
        public PIFMetrics metrics;
        /**
         * true if this represents a physical network interface
         */
        public Boolean physical;
        /**
         * true if this interface is online
         */
        public Boolean currentlyAttached;
        /**
         * Sets if and how this interface gets an IP address
         */
        public Types.IpConfigurationMode ipConfigurationMode;
        /**
         * IP address
         */
        public String IP;
        /**
         * IP netmask
         */
        public String netmask;
        /**
         * IP gateway
         */
        public String gateway;
        /**
         * IP address of DNS servers to use
         */
        public String DNS;
        /**
         * Indicates which bond this interface is part of
         */
        public Bond bondSlaveOf;
        /**
         * Indicates this PIF represents the results of a bond
         */
        public Set<Bond> bondMasterOf;
        /**
         * Indicates wich VLAN this interface receives untagged traffic from
         */
        public VLAN VLANMasterOf;
        /**
         * Indicates which VLANs this interface transmits tagged traffic to
         */
        public Set<VLAN> VLANSlaveOf;
        /**
         * Indicates whether the control software is listening for connections on this interface
         */
        public Boolean management;
        /**
         * Additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * Prevent this PIF from being unplugged; set this to notify the management tool-stack that the PIF has a special use and should not be unplugged under any circumstances (e.g. because you're running storage traffic over it)
         */
        public Boolean disallowUnplug;
        /**
         * Indicates to which tunnel this PIF gives access
         */
        public Set<Tunnel> tunnelAccessPIFOf;
        /**
         * Indicates to which tunnel this PIF provides transport
         */
        public Set<Tunnel> tunnelTransportPIFOf;
        /**
         * Sets if and how this interface gets an IPv6 address
         */
        public Types.Ipv6ConfigurationMode ipv6ConfigurationMode;
        /**
         * IPv6 address
         */
        public Set<String> IPv6;
        /**
         * IPv6 gateway
         */
        public String ipv6Gateway;
        /**
         * Which protocol should define the primary address of this interface
         */
        public Types.PrimaryAddressType primaryAddressType;
    }

    /**
     * Get a record containing the current state of the given PIF.
     *
     * @return all fields from the object
     */
    public PIF.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPIFRecord(result);
    }

    /**
     * Get a reference to the PIF instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static PIF getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPIF(result);
    }

    /**
     * Get the uuid field of the given PIF.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the device field of the given PIF.
     *
     * @return value of the field
     */
    public String getDevice(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_device";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the network field of the given PIF.
     *
     * @return value of the field
     */
    public Network getNetwork(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_network";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toNetwork(result);
    }

    /**
     * Get the host field of the given PIF.
     *
     * @return value of the field
     */
    public Host getHost(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_host";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toHost(result);
    }

    /**
     * Get the MAC field of the given PIF.
     *
     * @return value of the field
     */
    public String getMAC(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_MAC";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the MTU field of the given PIF.
     *
     * @return value of the field
     */
    public Long getMTU(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_MTU";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the VLAN field of the given PIF.
     *
     * @return value of the field
     */
    public Long getVLAN(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the metrics field of the given PIF.
     *
     * @return value of the field
     */
    public PIFMetrics getMetrics(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_metrics";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPIFMetrics(result);
    }

    /**
     * Get the physical field of the given PIF.
     *
     * @return value of the field
     */
    public Boolean getPhysical(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_physical";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the currently_attached field of the given PIF.
     *
     * @return value of the field
     */
    public Boolean getCurrentlyAttached(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_currently_attached";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the ip_configuration_mode field of the given PIF.
     *
     * @return value of the field
     */
    public Types.IpConfigurationMode getIpConfigurationMode(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_ip_configuration_mode";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toIpConfigurationMode(result);
    }

    /**
     * Get the IP field of the given PIF.
     *
     * @return value of the field
     */
    public String getIP(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_IP";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the netmask field of the given PIF.
     *
     * @return value of the field
     */
    public String getNetmask(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_netmask";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the gateway field of the given PIF.
     *
     * @return value of the field
     */
    public String getGateway(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_gateway";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the DNS field of the given PIF.
     *
     * @return value of the field
     */
    public String getDNS(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_DNS";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the bond_slave_of field of the given PIF.
     *
     * @return value of the field
     */
    public Bond getBondSlaveOf(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_bond_slave_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBond(result);
    }

    /**
     * Get the bond_master_of field of the given PIF.
     *
     * @return value of the field
     */
    public Set<Bond> getBondMasterOf(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_bond_master_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfBond(result);
    }

    /**
     * Get the VLAN_master_of field of the given PIF.
     *
     * @return value of the field
     */
    public VLAN getVLANMasterOf(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_VLAN_master_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVLAN(result);
    }

    /**
     * Get the VLAN_slave_of field of the given PIF.
     *
     * @return value of the field
     */
    public Set<VLAN> getVLANSlaveOf(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_VLAN_slave_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVLAN(result);
    }

    /**
     * Get the management field of the given PIF.
     *
     * @return value of the field
     */
    public Boolean getManagement(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_management";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the other_config field of the given PIF.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the disallow_unplug field of the given PIF.
     *
     * @return value of the field
     */
    public Boolean getDisallowUnplug(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_disallow_unplug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the tunnel_access_PIF_of field of the given PIF.
     *
     * @return value of the field
     */
    public Set<Tunnel> getTunnelAccessPIFOf(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_tunnel_access_PIF_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfTunnel(result);
    }

    /**
     * Get the tunnel_transport_PIF_of field of the given PIF.
     *
     * @return value of the field
     */
    public Set<Tunnel> getTunnelTransportPIFOf(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_tunnel_transport_PIF_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfTunnel(result);
    }

    /**
     * Get the ipv6_configuration_mode field of the given PIF.
     *
     * @return value of the field
     */
    public Types.Ipv6ConfigurationMode getIpv6ConfigurationMode(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_ipv6_configuration_mode";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toIpv6ConfigurationMode(result);
    }

    /**
     * Get the IPv6 field of the given PIF.
     *
     * @return value of the field
     */
    public Set<String> getIPv6(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_IPv6";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Get the ipv6_gateway field of the given PIF.
     *
     * @return value of the field
     */
    public String getIpv6Gateway(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_ipv6_gateway";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the primary_address_type field of the given PIF.
     *
     * @return value of the field
     */
    public Types.PrimaryAddressType getPrimaryAddressType(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_primary_address_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPrimaryAddressType(result);
    }

    /**
     * Set the other_config field of the given PIF.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the other_config field of the given PIF.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given PIF.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the disallow_unplug field of the given PIF.
     *
     * @param disallowUnplug New value to set
     */
    public void setDisallowUnplug(Connection c, Boolean disallowUnplug) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.set_disallow_unplug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(disallowUnplug)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Create a VLAN interface from an existing physical interface. This call is deprecated: use VLAN.create instead
     * @deprecated
     *
     * @param device physical interface on which to create the VLAN interface
     * @param network network to which this interface should be connected
     * @param host physical machine to which this PIF is connected
     * @param VLAN VLAN tag for the new interface
     * @return Task
     */
   @Deprecated public static Task createVLANAsync(Connection c, String device, Network network, Host host, Long VLAN) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "Async.PIF.create_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a VLAN interface from an existing physical interface. This call is deprecated: use VLAN.create instead
     * @deprecated
     *
     * @param device physical interface on which to create the VLAN interface
     * @param network network to which this interface should be connected
     * @param host physical machine to which this PIF is connected
     * @param VLAN VLAN tag for the new interface
     * @return The reference of the created PIF object
     */
   @Deprecated public static PIF createVLAN(Connection c, String device, Network network, Host host, Long VLAN) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VlanTagInvalid {
        String method_call = "PIF.create_VLAN";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(VLAN)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPIF(result);
    }

    /**
     * Destroy the PIF object (provided it is a VLAN interface). This call is deprecated: use VLAN.destroy or Bond.destroy instead
     * @deprecated
     *
     * @return Task
     */
   @Deprecated public Task destroyAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.PifIsPhysical {
        String method_call = "Async.PIF.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Destroy the PIF object (provided it is a VLAN interface). This call is deprecated: use VLAN.destroy or Bond.destroy instead
     * @deprecated
     *
     */
   @Deprecated public void destroy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.PifIsPhysical {
        String method_call = "PIF.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Reconfigure the IP address settings for this interface
     *
     * @param mode whether to use dynamic/static/no-assignment
     * @param IP the new IP address
     * @param netmask the new netmask
     * @param gateway the new gateway
     * @param DNS the new DNS settings
     * @return Task
     */
    public Task reconfigureIpAsync(Connection c, Types.IpConfigurationMode mode, String IP, String netmask, String gateway, String DNS) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PIF.reconfigure_ip";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(mode), Marshalling.toXMLRPC(IP), Marshalling.toXMLRPC(netmask), Marshalling.toXMLRPC(gateway), Marshalling.toXMLRPC(DNS)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Reconfigure the IP address settings for this interface
     *
     * @param mode whether to use dynamic/static/no-assignment
     * @param IP the new IP address
     * @param netmask the new netmask
     * @param gateway the new gateway
     * @param DNS the new DNS settings
     */
    public void reconfigureIp(Connection c, Types.IpConfigurationMode mode, String IP, String netmask, String gateway, String DNS) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.reconfigure_ip";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(mode), Marshalling.toXMLRPC(IP), Marshalling.toXMLRPC(netmask), Marshalling.toXMLRPC(gateway), Marshalling.toXMLRPC(DNS)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Reconfigure the IPv6 address settings for this interface
     *
     * @param mode whether to use dynamic/static/no-assignment
     * @param IPv6 the new IPv6 address (in <addr>/<prefix length> format)
     * @param gateway the new gateway
     * @param DNS the new DNS settings
     * @return Task
     */
    public Task reconfigureIpv6Async(Connection c, Types.Ipv6ConfigurationMode mode, String IPv6, String gateway, String DNS) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PIF.reconfigure_ipv6";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(mode), Marshalling.toXMLRPC(IPv6), Marshalling.toXMLRPC(gateway), Marshalling.toXMLRPC(DNS)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Reconfigure the IPv6 address settings for this interface
     *
     * @param mode whether to use dynamic/static/no-assignment
     * @param IPv6 the new IPv6 address (in <addr>/<prefix length> format)
     * @param gateway the new gateway
     * @param DNS the new DNS settings
     */
    public void reconfigureIpv6(Connection c, Types.Ipv6ConfigurationMode mode, String IPv6, String gateway, String DNS) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.reconfigure_ipv6";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(mode), Marshalling.toXMLRPC(IPv6), Marshalling.toXMLRPC(gateway), Marshalling.toXMLRPC(DNS)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Change the primary address type used by this PIF
     *
     * @param primaryAddressType Whether to prefer IPv4 or IPv6 connections
     * @return Task
     */
    public Task setPrimaryAddressTypeAsync(Connection c, Types.PrimaryAddressType primaryAddressType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PIF.set_primary_address_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(primaryAddressType)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Change the primary address type used by this PIF
     *
     * @param primaryAddressType Whether to prefer IPv4 or IPv6 connections
     */
    public void setPrimaryAddressType(Connection c, Types.PrimaryAddressType primaryAddressType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.set_primary_address_type";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(primaryAddressType)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Scan for physical interfaces on a host and create PIF objects to represent them
     *
     * @param host The host on which to scan
     * @return Task
     */
    public static Task scanAsync(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PIF.scan";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Scan for physical interfaces on a host and create PIF objects to represent them
     *
     * @param host The host on which to scan
     */
    public static void scan(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.scan";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Create a PIF object matching a particular network interface
     *
     * @param host The host on which the interface exists
     * @param MAC The MAC address of the interface
     * @param device The device name to use for the interface
     * @return Task
     */
    public static Task introduceAsync(Connection c, Host host, String MAC, String device) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PIF.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(MAC), Marshalling.toXMLRPC(device)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a PIF object matching a particular network interface
     *
     * @param host The host on which the interface exists
     * @param MAC The MAC address of the interface
     * @param device The device name to use for the interface
     * @return The reference of the created PIF object
     */
    public static PIF introduce(Connection c, Host host, String MAC, String device) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(MAC), Marshalling.toXMLRPC(device)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPIF(result);
    }

    /**
     * Destroy the PIF object matching a particular network interface
     *
     * @return Task
     */
    public Task forgetAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.PifTunnelStillExists {
        String method_call = "Async.PIF.forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Destroy the PIF object matching a particular network interface
     *
     */
    public void forget(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.PifTunnelStillExists {
        String method_call = "PIF.forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Attempt to bring down a physical interface
     *
     * @return Task
     */
    public Task unplugAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PIF.unplug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Attempt to bring down a physical interface
     *
     */
    public void unplug(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.unplug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Attempt to bring up a physical interface
     *
     * @return Task
     */
    public Task plugAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.TransportPifNotConfigured {
        String method_call = "Async.PIF.plug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Attempt to bring up a physical interface
     *
     */
    public void plug(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.TransportPifNotConfigured {
        String method_call = "PIF.plug";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Create a new PIF record in the database only
     *
     * @param device
     * @param network
     * @param host
     * @param MAC
     * @param MTU
     * @param VLAN
     * @param physical
     * @param ipConfigurationMode
     * @param IP
     * @param netmask
     * @param gateway
     * @param DNS
     * @param bondSlaveOf
     * @param VLANMasterOf
     * @param management
     * @param otherConfig
     * @param disallowUnplug
     * @param ipv6ConfigurationMode
     * @param IPv6
     * @param ipv6Gateway
     * @param primaryAddressType
     * @return Task
     */
    public static Task dbIntroduceAsync(Connection c, String device, Network network, Host host, String MAC, Long MTU, Long VLAN, Boolean physical, Types.IpConfigurationMode ipConfigurationMode, String IP, String netmask, String gateway, String DNS, Bond bondSlaveOf, VLAN VLANMasterOf, Boolean management, Map<String, String> otherConfig, Boolean disallowUnplug, Types.Ipv6ConfigurationMode ipv6ConfigurationMode, Set<String> IPv6, String ipv6Gateway, Types.PrimaryAddressType primaryAddressType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PIF.db_introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(MAC), Marshalling.toXMLRPC(MTU), Marshalling.toXMLRPC(VLAN), Marshalling.toXMLRPC(physical), Marshalling.toXMLRPC(ipConfigurationMode), Marshalling.toXMLRPC(IP), Marshalling.toXMLRPC(netmask), Marshalling.toXMLRPC(gateway), Marshalling.toXMLRPC(DNS), Marshalling.toXMLRPC(bondSlaveOf), Marshalling.toXMLRPC(VLANMasterOf), Marshalling.toXMLRPC(management), Marshalling.toXMLRPC(otherConfig), Marshalling.toXMLRPC(disallowUnplug), Marshalling.toXMLRPC(ipv6ConfigurationMode), Marshalling.toXMLRPC(IPv6), Marshalling.toXMLRPC(ipv6Gateway), Marshalling.toXMLRPC(primaryAddressType)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a new PIF record in the database only
     *
     * @param device
     * @param network
     * @param host
     * @param MAC
     * @param MTU
     * @param VLAN
     * @param physical
     * @param ipConfigurationMode
     * @param IP
     * @param netmask
     * @param gateway
     * @param DNS
     * @param bondSlaveOf
     * @param VLANMasterOf
     * @param management
     * @param otherConfig
     * @param disallowUnplug
     * @param ipv6ConfigurationMode
     * @param IPv6
     * @param ipv6Gateway
     * @param primaryAddressType
     * @return The ref of the newly created PIF record.
     */
    public static PIF dbIntroduce(Connection c, String device, Network network, Host host, String MAC, Long MTU, Long VLAN, Boolean physical, Types.IpConfigurationMode ipConfigurationMode, String IP, String netmask, String gateway, String DNS, Bond bondSlaveOf, VLAN VLANMasterOf, Boolean management, Map<String, String> otherConfig, Boolean disallowUnplug, Types.Ipv6ConfigurationMode ipv6ConfigurationMode, Set<String> IPv6, String ipv6Gateway, Types.PrimaryAddressType primaryAddressType) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.db_introduce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(device), Marshalling.toXMLRPC(network), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(MAC), Marshalling.toXMLRPC(MTU), Marshalling.toXMLRPC(VLAN), Marshalling.toXMLRPC(physical), Marshalling.toXMLRPC(ipConfigurationMode), Marshalling.toXMLRPC(IP), Marshalling.toXMLRPC(netmask), Marshalling.toXMLRPC(gateway), Marshalling.toXMLRPC(DNS), Marshalling.toXMLRPC(bondSlaveOf), Marshalling.toXMLRPC(VLANMasterOf), Marshalling.toXMLRPC(management), Marshalling.toXMLRPC(otherConfig), Marshalling.toXMLRPC(disallowUnplug), Marshalling.toXMLRPC(ipv6ConfigurationMode), Marshalling.toXMLRPC(IPv6), Marshalling.toXMLRPC(ipv6Gateway), Marshalling.toXMLRPC(primaryAddressType)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toPIF(result);
    }

    /**
     * Destroy a PIF database record.
     *
     * @return Task
     */
    public Task dbForgetAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.PIF.db_forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Destroy a PIF database record.
     *
     */
    public void dbForget(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.db_forget";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Return a list of all the PIFs known to the system.
     *
     * @return references to all objects
     */
    public static Set<PIF> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfPIF(result);
    }

    /**
     * Return a map of PIF references to PIF records for all PIFs known to the system.
     *
     * @return records of all objects
     */
    public static Map<PIF, PIF.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "PIF.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfPIFPIFRecord(result);
    }

}