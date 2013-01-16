package com.cloud.network.cisco;

import java.util.List;

import com.cloud.utils.exception.ExecutionException;

public interface CiscoVnmcConnection {

	public boolean createTenant(String tenantName) throws ExecutionException;

	public boolean createTenantVDC(String tenantName) throws ExecutionException;

	public boolean createTenantVDCEdgeDeviceProfile(String tenantName)
			throws ExecutionException;

	public boolean createTenantVDCEdgeStaticRoutePolicy(String tenantName)
			throws ExecutionException;

	public boolean createTenantVDCEdgeStaticRoute(String tenantName,
			String nextHopIp, String outsideIntf, String destination,
			String netmask) throws ExecutionException;

	public boolean associateTenantVDCEdgeStaticRoutePolicy(String tenantName)
			throws ExecutionException;

	public boolean associateTenantVDCEdgeDhcpPolicy(String tenantName,
			String intfName) throws ExecutionException;

	public boolean createTenantVDCEdgeDhcpPolicy(String tenantName,
			String startIp, String endIp, String subnet, String nameServerIp,
			String domain) throws ExecutionException;

	public boolean associateTenantVDCEdgeDhcpServerPolicy(String tenantName,
			String intfName) throws ExecutionException;

	public boolean createTenantVDCEdgeSecurityProfile(String tenantName)
			throws ExecutionException;

	public boolean createTenantVDCSourceNATPool(String tenantName,
			String publicIp) throws ExecutionException;

	public boolean createTenantVDCSourceNATPolicy(String tenantName,
			String startSourceIp, String endSourceIp) throws ExecutionException;

	public boolean createTenantVDCNatPolicySet(String tenantName)
			throws ExecutionException;

	public boolean associateNatPolicySet(String tenantName)
			throws ExecutionException;

	public boolean createEdgeFirewall(String tenantName, String publicIp,
			String insideIp, String insideSubnet, String outsideSubnet)
			throws ExecutionException;

	public List<String> listUnAssocAsa1000v() throws ExecutionException;

	public boolean assocAsa1000v(String tenantName, String firewallDn)
			throws ExecutionException;

}