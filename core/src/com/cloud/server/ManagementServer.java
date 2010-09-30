/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 * 
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.server;

import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.cloud.alert.AlertVO;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.capacity.CapacityVO;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.domain.DomainVO;
import com.cloud.event.EventVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkRuleConfigVO;
import com.cloud.network.SecurityGroupVO;
import com.cloud.network.security.IngressRuleVO;
import com.cloud.network.security.NetworkGroupRulesVO;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.pricing.PricingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.DiskTemplateVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.InsufficientStorageCapacityException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao.TemplateFilter;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVm;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmStats;

/**
 * ManagementServer is the interface to talk to the Managment Server.
 * This will be the line drawn between the UI and MS.  If we need to build
 * a wire protocol, it will be built on top of this java interface.
 */
public interface ManagementServer {
    static final String Name = "management-server";
    
    /**
     * Creates a new user, encrypts the password on behalf of the caller
     * 
     * @param username username
     * @param password the user's password
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param domain the id of the domain that this user belongs to
     * @param accountName the name(a.k.a. id) of the account that this user belongs to
     * @param timezone the user's current timezone (default: PST)
     * @return a user object
     */
    User createUser(String username, String password, String firstName, String lastName, Long domain, String accountName, short userType, String email, String timezone);
	boolean reconnect(long hostId);
	long reconnectAsync(long hostId);
	
	ClusterVO findClusterById(long clusterId);
    List<ClusterVO> listClusterByPodId(long podId);
    
    ClusterVO createCluster(long dcId, long podId, String name);

    /**
     * Creates a new user, does not encrypt the password
     * 
     * @param username username
     * @param password the user's password
     * @param firstName the user's first name
     * @param lastName the user's last name
     * @param domain the id of the domain that this user belongs to FIXME: if we have account, do we also need domain?
     * @param accountName the name(a.k.a. id) of the account that this user belongs to
     * @param timezone the user's current timezone (default: PST)
     * @return a user object
     */
    User createUserAPI(String username, String password, String firstName, String lastName, Long domain, String accountName, short userType, String email, String timezone);

    /**
     * Gets a user by userId
     * 
     * @param userId
     * @return a user object
     */
    User getUser(long userId);

    /**
     * Gets a user and account by username and domain
     * 
     * @param username
     * @param domainId
     * @return a user object
     */
    UserAccount getUserAccount(String username, Long domainId);

    /**
     * Gets a user and account by username, password and domain
     * 
     * @param username
     * @param password
     * @param domainId
     * @return a user object
     */
    // UserAccount getUserAccount(String username, String password, Long domainId);

    /**
     * Authenticates a user when s/he logs in.
     * @param username required username for authentication
     * @param password password to use for authentication, can be null for single sign-on case
     * @param domainId id of domain where user with username resides
     * @param requestParameters the request parameters of the login request, which should contain timestamp of when the request signature is made, and the signature itself in the single sign-on case
     * @return a user object, null if the user failed to authenticate
     */
    UserAccount authenticateUser(String username, String password, Long domainId, Map<String, Object[]> requestParameters);

    /**
     * Deletes a user by userId
     * @param userId
     * @return true if delete was successful, false otherwise
     */
    boolean deleteUser(long userId);
    long deleteUserAsync(long userId);

    /**
     * Disables a user by userId
     * @param userId
     * @return true if disable was successful, false otherwise
     */
    boolean disableUser(long userId);
    long disableUserAsync(long userId);

    /**
     * Disables an account by accountId
     * @param accountId
     * @return true if disable was successful, false otherwise
     */
    boolean disableAccount(long accountId);
    long disableAccountAsync(long accountId);

    /**
     * Enables an account by accountId
     * @param accountId
     * @return true if enable was successful, false otherwise
     */
    boolean enableAccount(long accountId);

    /**
     * Locks an account by accountId.  A locked account cannot access the API, but will still have running VMs/IP addresses allocated/etc.
     * @param accountId
     * @return true if enable was successful, false otherwise
     */
    boolean lockAccount(long accountId);

    /**
     * Updates an account name by accountId
     * @param accountId
     * @param accountName
     * @return true if update was successful, false otherwise
     */
    
    boolean updateAccount(long accountId, String accountName);

    /**
     * Enables a user by userId
     * @param userId
     * @return true if enable was successful, false otherwise
     */
    boolean enableUser(long userId);

    /**
     * Locks a user by userId.  A locked user cannot access the API, but will still have running VMs/IP addresses allocated/etc.
     * @param userId
     * @return true if enable was successful, false otherwise
     */
    boolean lockUser(long userId);
    
    /**
     * registerPreallocatedLun registers a preallocated lun in our database.
     * 
     * @param targetIqn iqn for the storage server.
     * @param portal portal ip address for the storage server.
     * @param lun lun #
     * @param size size of the lun
     * @param dcId data center to attach to
     * @param tags tags to attach to the lun
     * @return the new PreAllocatedLun 
     */
    PreallocatedLunVO registerPreallocatedLun(String targetIqn, String portal, int lun, long size, long dcId, String tags);
    
    /**
     * Unregisters a preallocated lun in our database
     * @param id id of the lun
     * @return true if unregistered; false if not.
     * @throws IllegalArgumentException
     */
    boolean unregisterPreallocatedLun(long id) throws IllegalArgumentException;
    

    /**
     * Discovers new hosts given an url to locate the resource.
     * @param dcId id of the data center
     * @param podid id of the pod
     * @param clusterId id of the cluster
     * @param url url to use
     * @param username username to use to login
     * @param password password to use to login
     * @return true if hosts were found; false if not.
     * @throws IllegalArgumentException
     */
    List<? extends Host> discoverHosts(long dcId, Long podId, Long clusterId, String url, String username, String password) throws IllegalArgumentException, DiscoveryException;

    String updateAdminPassword(long userId, String oldPassword, String newPassword);

    /**
     * Updates a user's information
     * @param userId
     * @param username
     * @param password
     * @param firstname
     * @param lastname
     * @param email
     * @param timezone
     * @param apikey
     * @param secretkey
     * @return true if update was successful, false otherwise
     * @throws InvalidParameterValueException
     */
    boolean updateUser(long userId, String username, String password, String firstname, String lastname, String email, String timezone, String apiKey, String secretKey) throws InvalidParameterValueException;

    /**
     * Locate a user by their apiKey
     * @param apiKey that was created for a particular user
     * @return the user/account pair if one exact match was found, null otherwise
     */
    Pair<User, Account> findUserByApiKey(String apiKey);

    /**
     * Get an account by the accountId
     * @param accountId
     * @return the account, or null if not found
     */
    Account getAccount(long accountId);

    /**
     * Create an API key for a user; this key is used as the user's identity when making
     * calls to the developer API
     * @param userId
     * @return the new API key
     */
    String createApiKey(Long userId);

    /**
     * Create a secret key for a user, this key is used to sign requests made by the user
     * to the developer API.  When a request is received from a user, the secret key is
     * retrieved from the database (using the apiKey) and used to verify the request signature.
     * @param userId
     * @return the new secret key
     */
    String createSecretKey(Long userId);

    /**
     * Gets Storage statistics for a given host
     * 
     * @param hostId
     * @return StorageStats
     */
    StorageStats getStorageStatistics(long hostId);
    
    /**
     * Gets the guest OS category for a host
     * @param hostId
     * @return guest OS Category
     */
    GuestOSCategoryVO getHostGuestOSCategory(long hostId);
    
    
	/** Get storage statistics (used/available) for a pool
	 * @param id pool id
	 * @return storage statistics
	 */
	StorageStats getStoragePoolStatistics(long id);
    
    /**
     * prepares a host for maintenance.  This method can take a long time
     * depending on if there are any current operations on the host.
     * 
     * @param hostId id of the host to bring down.
     * @return true if the operation succeeds.
     */
    boolean prepareForMaintenance(long hostId);
    long prepareForMaintenanceAsync(long hostId) throws InvalidParameterValueException;
    
    /**
     * Marks the host as maintenance completed.  This actually will mark
     * the host as down and the state will be changed automatically once
     * the agent is up and running.
     * 
     * @param hostId
     * @return true if the state changed worked.  false if not.
     */
    boolean maintenanceCompleted(long hostId);
    long maintenanceCompletedAsync(long hostId);
    
    /**
     * Gets Host/VM statistics for a given host
     * 
     * @param hostId
     * @return HostStats/VMStats depending on the id passed
     */
    VmStats getVmStatistics(long hostId);
    
    /**
     * Gets Volume statistics.  The array returned will contain VolumeStats in the same order
     * as the array of volumes requested.
     * 
     * @param volId
     * @return array of VolumeStats
     */
    VolumeStats[] getVolumeStatistics(long[] volId);

    /**
     * Associate / allocate an  IP address to a user
     * @param userId
     * @param accountId
     * @param domainId
     * @param zoneId
     * @return allocated IP address in the zone specified
     * @throws InsufficientAddressCapacityException if no more addresses are available
     * @throws InvalidParameterValueException if no router for that user exists in the zone specified
     * @throws InternalErrorException  if the new address could not be sent down to the router
     */
    String associateIpAddress(long userId, long accountId, long domainId, long zoneId) throws ResourceAllocationException, InsufficientAddressCapacityException, InvalidParameterValueException, InternalErrorException;
    long associateIpAddressAsync(long userId, long accountId, long domainId, long zoneId);
   
    
    /**
     * Disassociate /unallocate an allocated IP address from a user
     * @param userId
     * @param accountId
     * @param ipAddress
     * @return success
     */
    boolean disassociateIpAddress(long userId, long accountId, String ipAddress) throws PermissionDeniedException;
    long disassociateIpAddressAsync(long userId, long accountId, String ipAddress);
   
    /**
	 * Adds a VLAN to the database, along with an IP address range. Can add three types of VLANs: (1) zone-wide VLANs on the virtual network (2) pod-wide direct attached VLANs (3) account-specific direct attached VLANs
	 * @param userId
	 * @param vlanType - either "DomR" (VLAN for a virtual network) or "DirectAttached" (VLAN for IPs that will be directly attached to UserVMs)
	 * @param zoneId
	 * @param accountId
	 * @param podId
	 * @param add
	 * @param vlanId
	 * @param gateway
	 * @param startIP
	 * @param endIP
	 * @return The new VlanVO object
     * @throws Exception 
	 */
	VlanVO createVlanAndPublicIpRange(long userId, VlanType vlanType, Long zoneId, Long accountId, Long podId, String vlanId, String vlanGateway, String vlanNetmask, String startIP, String endIP) throws Exception;
	
	/**
	 * Deletes a VLAN from the database, along with all of its IP addresses. Will not delete VLANs that have allocated IP addresses.
	 * @param userId
	 * @param vlanDbId
	 * @return success/failure
	 */
	boolean deleteVlanAndPublicIpRange(long userId, long vlanDbId) throws InvalidParameterValueException;
        
    /**
     * Searches for vlan by the specified search criteria
     * Can search by: "id", "vlan", "name", "zoneID"
     * @param c
     * @return List of Vlans
     */
    List<VlanVO> searchForVlans(Criteria c);
    
    /**
     * If the specified VLAN is associated with the pod, returns the pod ID. Else, returns null.
     * @param vlanDbId
     * @return pod ID, or null
     */
    Long getPodIdForVlan(long vlanDbId);
    
    /**
     * If the specified VLAN is associated with a specific account, returns the account ID. Else, returns null.
     * @param accountId
     * @return account ID, or null
     */
    Long getAccountIdForVlan(long vlanDbId);

    /**
     * Creates a data volume
     * @param accountId
     * @pparam userId
     * @param name - name for the volume
     * @param zoneId - id of the zone to create this volume on
     * @param diskOfferingId - id of the disk offering to create this volume with
     * @return true if success, false if not
     */
    VolumeVO createVolume(long accountId, long userId, String name, long zoneId, long diskOfferingId, long startEventId) throws InternalErrorException;
    long createVolumeAsync(long accountId, long userId, String name, long zoneId, long diskOfferingId) throws InvalidParameterValueException, InternalErrorException, ResourceAllocationException;
    
    /**
     * Finds the root volume of the VM
     * @param vmId
     * @return Volume
     */
    VolumeVO findRootVolume(long vmId);
    
    /**
     * Marks a data volume as destroyed
     * @param volumeId
     */
    void destroyVolume(long volumeId) throws InvalidParameterValueException;

    /**
     * Return a list of IP addresses
     * @param accountId
     * @param allocatedOnly - if true, will only list IPs that are allocated to the specified account
     * @param zoneId - if specified, will list IPs in this zone
     * @param vlanDbId - if specified, will list IPs in this VLAN
     * @return list of IP addresses
     */
    List<IPAddressVO> listPublicIpAddressesBy(Long accountId, boolean allocatedOnly, Long zoneId, Long vlanDbId);
    
    /**
     * Return a list of private IP addresses that have been allocated to the given pod and zone
     * @param podId
     * @param zoneId
     * @return list of private IP addresses
     */
    List<DataCenterIpAddressVO> listPrivateIpAddressesBy(Long podId, Long zoneId);
    
    /**
     * Create or update a  port forwarding rule
     * @param userId userId calling this api
     * @param accountId accountId calling this api
     * @param publicIp Ip address
     * @param publicPort port
     * @param privateIp private address (10.x.y.z) to be forwarded to
     * @param privatePort private port to be forwarded to
     * @param proto protocol (tcp/udp/icmp)
     * @return -1 if update/create failed, otherwise update/create succeeded
     * @throws PermissionDeniedException if user is not authorized to operate on the supplied IP address
     * @throws NetworkRuleConflictException  if the new rule conflicts with existing rules
     * @throws InvalidParameterValueException  if the supplied parameters have invalid values
     * @throws InternalErrorException  if the update could not be performed
     */
    // long createOrUpdateIpForwardingRule(long userId, long accountId, String publicIp, String publicPort, String privateIp, String privatePort, String proto) throws PermissionDeniedException, NetworkRuleConflictException, InvalidParameterValueException, InternalErrorException;
    
    /**
     * Create or update a load balancing rule
     * @param userId userId calling this api
     * @param accountId accountId calling this api
     * @param publicIp Ip address
     * @param publicPort port
     * @param privateIp private address (10.x.y.z) to be forwarded to
     * @param privatePort private port to be forwarded to
     * @param algo the load balancing algorithm
     * @return -1 if update/create failed, otherwise update/create succeeded
     * @throws PermissionDeniedException if user is not authorized to operate on the supplied IP address
     * @throws NetworkRuleConflictException  if the new rule conflicts with existing rules
     * @throws InvalidParameterValueException  if the supplied parameters have invalid values
     * @throws InternalErrorException  if the update could not be performed
     */
    // long createOrUpdateLoadBalancerRule(long userId, long accountId, String publicIp, String publicPort, String privateIp, String privatePort, String algo) throws PermissionDeniedException, NetworkRuleConflictException, InvalidParameterValueException, InternalErrorException;
    
    /**
     * Delete a ip forwarding rule
     * @param userId userId calling this api
     * @param accountId accountId calling this api
     * @param publicIp Ip address
     * @param publicPort port
     * @param privateIp private address (10.x.y.z) to be forwarded to
     * @param privatePort private port to be forwarded to
     * @param proto protocol (tcp/udp/icmp)
     * @return true if succeeded
     * @throws PermissionDeniedException if user is not authorized to operate on the supplied IP address
     * @throws InvalidParameterValueException  if the supplied parameters have invalid values
     * @throws InternalErrorException  if the update could not be performed
     */
    // boolean deleteIpForwardingRule(long userId, long accountId, String publicIp, String publicPort, String privateIp, String privatePort, String proto) throws PermissionDeniedException, InvalidParameterValueException, InternalErrorException;

    /**
     * Delete a load balancing rule
 	 * @param userId userId calling this api
     * @param accountId accountId calling this api
     * @param publicIp Ip address
     * @param publicPort port
     * @param privateIp private address (10.x.y.z) to be forwarded to
     * @param privatePort private port to be forwarded to
     * @param algo loadbalance algorithm (roundrobin/source/leastconn/etc)
     * @return true if succeeded
     * @throws PermissionDeniedException if user is not authorized to operate on the supplied IP address
     * @throws InvalidParameterValueException  if the supplied parameters have invalid values
     * @throws InternalErrorException  if the update could not be performed
     */
    // boolean deleteLoadBalancingRule(long userId, long accountId, String publicIp, String publicPort, String privateIp, String privatePort, String algo) throws PermissionDeniedException, InvalidParameterValueException, InternalErrorException;

    /**
     * Generates a random password that will be used (initially) by newly created and started virtual machines
     * @return a random password
     */
    String generateRandomPassword();
    
    /**
     * Resets the password for a virtual machine with a new password
     * @param userId, the user that's reseting the password
     * @param vmId the ID of the virtual machine
 	 * @param password the password for the virtual machine
     * @return true or false, based on the success of the method
     */
    boolean resetVMPassword(long userId, long vmId, String password);
    long resetVMPasswordAsync(long userId, long vmId, String password);
    
    /**
     * Attaches the specified volume to the specified VM
     * @param vmId
     * @param volumeId
     * @throws InvalidParameterValueException, InternalErrorException
     */
    void attachVolumeToVM(long vmId, long volumeId, Long deviceId, long startEventId) throws InternalErrorException;
    long attachVolumeToVMAsync(long vmId, long volumeId, Long deviceId) throws InvalidParameterValueException;
    
    /**
     * Detaches the specified volume from the VM it is currently attached to. If it is not attached to any VM, will return true.
     * @param vmId
     * @volumeId
     * @throws InvalidParameterValueException, InternalErrorException
     */
    void detachVolumeFromVM(long volumeId, long startEventId) throws InternalErrorException;
    long detachVolumeFromVMAsync(long volumeId) throws InvalidParameterValueException;
    
    /**
     * Attaches an ISO to the virtual CDROM device of the specified VM. Will fail if the VM already has an ISO mounted.
     * @param vmId
     * @param userId
     * @param isoId
     * @param attach whether to attach or detach the iso from the instance
     * @return
     */
    boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach, long startEventId);
    long attachISOToVMAsync(long vmId, long userId, long isoId) throws InvalidParameterValueException;
    long detachISOFromVMAsync(long vmId, long userId) throws InvalidParameterValueException;

    /**
     * Creates and starts a new Virtual Machine.
     * 
     * @param userId
     * @param accountId
     * @param dataCenterId
     * @param serviceOfferingId
     * @param templateId - the id of the template (or ISO) to use for creating the virtual machine
     * @param diskOfferingId - ID of the disk offering to use when creating the root disk (if deploying from an ISO) or the data disk (if deploying from a template). If deploying from a template and a disk offering ID is not passed in, the VM will have only a root disk.
     * @param domain the end user wants to use for this virtual machine. can be null.  If the virtual machine is already part of an existing network, the domain is ignored.
     * @param password the password that the user wants to use to access this virtual machine
     * @param displayName user-supplied name to be shown in the UI or returned in the API
     * @param groupName user-supplied groupname to be shown in the UI or returned in the API
     * @param userData user-supplied base64-encoded data that can be retrieved by the instance from the virtual router
     * @return VirtualMachine if successfully deployed, null otherwise
     * @throws InvalidParameterValueException if the parameter values are incorrect.
     * @throws ExecutionException
     * @throws StorageUnavailableException
     * @throws ConcurrentOperationException
     */
    UserVm deployVirtualMachine(long userId, long accountId, long dataCenterId, long serviceOfferingId, long templateId, Long diskOfferingId, String domain, String password, String displayName, String group, String userData, String [] groups, long startEventId) throws ResourceAllocationException, InvalidParameterValueException, InternalErrorException, InsufficientStorageCapacityException, PermissionDeniedException, ExecutionException, StorageUnavailableException, ConcurrentOperationException;
    long deployVirtualMachineAsync(long userId, long accountId, long dataCenterId, long serviceOfferingId, long templateId, Long diskOfferingId, String domain, String password, String displayName, String group, String userData, String [] groups) throws InvalidParameterValueException, PermissionDeniedException;
    
    /**
     * Starts a Virtual Machine
     * 
     * @param userId the id of the user performing the action
     * @param vmId
     * @param isoPath - path of the ISO file to boot this VM from (null to boot from root disk)
     * @return VirtualMachine if successfully started, null otherwise
     * @throws ExecutionException
     * @throws StorageUnavailableException
     * @throws ConcurrentOperationException
     */
    UserVm startVirtualMachine(long userId, long vmId, String isoPath) throws InternalErrorException, ExecutionException, StorageUnavailableException, ConcurrentOperationException;
    long startVirtualMachineAsync(long userId, long vmId, String isoPath);
    
    /**
     * Stops a Virtual Machine
     * 
     * @param userId the id of the user performing the action
     * @param vmId
     * @return true if successfully stopped, false otherwise
     */
    boolean stopVirtualMachine(long userId, long vmId);
    long stopVirtualMachineAsync(long userId, long vmId);
    
    /**
     * Reboots a Virtual Machine
     * 
     * @param vmId
     * @return true if successfully rebooted, false otherwise
     */
    boolean rebootVirtualMachine(long userId, long vmId);
    
    /**
     * Reboots a Virtual Machine
     * 
     * @param vmId
     * @return the async-call job id
     */
    long rebootVirtualMachineAsync(long userId, long vmId);
    

    /**
     * Destroys a Virtual Machine
     * @param vmId
     * @return true if destroyed, false otherwise
     */
    boolean destroyVirtualMachine(long userId, long vmId);
    
    /**
     * 
     * @param userId
     * @param vmId
     * @return the async-call job id
     */
    long destroyVirtualMachineAsync(long userId, long vmId);
    
    /**
     * Recovers a destroyed virtual machine.
     * @param vmId
     * @return true if recovered, false otherwise
     */
    boolean recoverVirtualMachine(long vmId) throws ResourceAllocationException;

    /**
     * Upgrade the virtual machine to a new service offering
     * @param vmId
     * @param serviceOfferingId
     * @return success/failure
     */
    boolean upgradeVirtualMachine(long userId, long vmId, long serviceOfferingId, long startEventId);
    long upgradeVirtualMachineAsync(long userId, long vmId, long serviceOfferingId) throws InvalidParameterValueException;
    
    
    /**
     * Updates display name and group for virtual machine; enables/disabled ha
     * @param vmId
     * @param group, displayName
     * @param enable true to enable HA, false otherwise
     * @param userId - id of user performing the update on the virtual machine
     * @param accountId - id of the account that owns the virtual machine
     */
    void updateVirtualMachine(long vmId, String displayName, String group, boolean enable, Long userId, long accountId);

    /**
     * Updates a storage pool.
     * @param poolId ID of the storage pool to be updated
     * @param tags Tags that will be added to the storage pool
     */
    StoragePoolVO updateStoragePool(long poolId, String tags);
    
    /**
     * Starts a Domain Router
     * 
     * @param routerId
     * @return DomainRouter if successfully started, false otherwise
     */
	DomainRouter startRouter(long routerId, long startEventId) throws InternalErrorException;
	long startRouterAsync(long routerId);
	
	/**
	 * Stops a Domain Router
	 * 
	 * @param routerId
	 * @return true if successfully stopped, false otherwise
	 */
	boolean stopRouter(long routerId, long startEventId);
	long stopRouterAsync(long routerId);
	
	/**
	 * Reboots a Domain Router
	 * 
	 * @param routerId
	 * @return true if successfully rebooted, false otherwise
	 */
	boolean rebootRouter(long routerId, long startEventId) throws InternalErrorException;
	long rebootRouterAsync(long routerId);
	
	/**
	 * Destroys a Domain Router
	 * 
	 * @param routerId
	 * @return true if successfully destroyed, false otherwise
	 */
	boolean destroyRouter(long routerId);
    
    /**
     * Finds a domain router by user and data center
     * @param userId
     * @param dataCenterId
     * @return a list of DomainRouters
     */
	DomainRouterVO findDomainRouterBy(long accountId, long dataCenterId);
	
	/**
     * Finds a domain router by id
     * @param router id
     * @return a domainRouter
     */
	DomainRouterVO findDomainRouterById(long domainRouterId);
	
    
    /**
     * Retrieves a data center by id
     * 
     * @param dataCenterId
     * @return DataCenter
     */
    DataCenterVO getDataCenterBy(long dataCenterId);
    
    /**
     * Retrieves a pod by id
     * 
     * @param podId
     * @return Pod
     */
    HostPodVO getPodBy(long podId);
    
    /**
     * Retrieves the list of all data centers
     * @return a list of DataCenters
     */
    List<DataCenterVO> listDataCenters();
    
    /**
     * Retrieves a list of data centers that contain domain routers
     * that the specified user owns.
     * 
     * @param userId
     * @return a list of DataCenters
     */
    List<DataCenterVO> listDataCentersBy(long userId);
    
    /**
     * Retrieves a host by id
     * @param hostId
     * @return Host
     */
    HostVO getHostBy(long hostId);
    
    /**
     * Updates a host
     * @param hostId
     * @param guestOSCategoryId
     */
    void updateHost(long hostId, long guestOSCategoryId) throws InvalidParameterValueException;
    
    /**
     * Deletes a host
     * 
     * @param hostId
     * @param true if deleted, false otherwise
     */
    boolean deleteHost(long hostId);
    
    /**
     * Retrieves all Events between the start and end date specified
     * 
     * @param userId unique id of the user, pass in -1 to retrieve events for all users
     * @param accountId unique id of the account (which could be shared by many users), pass in -1 to retrieve events for all accounts
     * @param domainId the id of the domain in which to search for users (useful when -1 is passed in for userId)
     * @param type the type of the event.
     * @param level INFO, WARN, or ERROR
     * @param startDate inclusive.
     * @param endDate inclusive.  If date specified is greater than the current time, the
     *                system will use the current time.
     * @return List of events
     */
    List<EventVO> getEvents(long userId, long accountId, Long domainId, String type, String level, Date startDate, Date endDate);
    
    /**
     * returns the a map of the names/values in the configuraton table
     * @return map of configuration name/values
     */
    List<ConfigurationVO> searchForConfigurations(Criteria c, boolean showHidden);
    
    /**
     * returns the instance id of this management server.
     * @return id of the management server
     */
    long getId();
    
    /** revisit
     * Searches for users by the specified search criteria
     * Can search by: "id", "username", "account", "domainId", "type"
     * @param c
     * @return List of UserAccounts
     */
    List<UserAccountVO> searchForUsers(Criteria c);
    
    /**
     * Searches for Service Offerings by the specified search criteria
     * Can search by: "name"
     * @param c
     * @return List of ServiceOfferings
     */
    List<ServiceOfferingVO> searchForServiceOfferings(Criteria c);
    
    /**
     * Searches for Clusters by the specified search criteria
     * @param c
     * @return
     */
    List<ClusterVO> searchForClusters(Criteria c);
    
    /**
     * Searches for Pods by the specified search criteria
     * Can search by: pod name and/or zone name
     * @param c
     * @return List of Pods
     */
    List<HostPodVO> searchForPods(Criteria c);
    
    /**
     * Searches for Zones by the specified search criteria
     * Can search by: zone name
     * @param c
     * @return List of Zones
     */
    List<DataCenterVO> searchForZones(Criteria c);
    
    /**
     * Searches for servers by the specified search criteria
     * Can search by: "name", "type", "state", "dataCenterId", "podId"
     * @param c
     * @return List of Hosts
     */
    List<HostVO> searchForServers(Criteria c);
    
    /**
     * Searches for servers that are either Down or in Alert state
     * @param c
     * @return List of Hosts
     */
    List<HostVO> searchForAlertServers(Criteria c);
    
    /**
     * Search for templates by the specified search criteria
     * Can search by: "name", "ready", "isPublic"
     * @param c
     * @return List of VMTemplates
     */
    List<VMTemplateVO> searchForTemplates(Criteria c);

    /**
     * Lists the template host records by template Id
     * 
     * @param templateId
     * @param zoneId
     * @return List of VMTemplateHostVO
     */
    List<VMTemplateHostVO> listTemplateHostBy(long templateId, Long zoneId);
    
    /**
     * Locates a Pricing object by the query parameters
     * 
     * @param type
     * @param id
     * @return Pricing object
     */
    PricingVO findPricingByTypeAndId(String type, Long id);
    
    /**
     * Obtains pods that match the data center ID
     * @param dataCenterId
     * @return List of Pods
     */
    List<HostPodVO> listPods(long dataCenterId);
    
    /**
     * Creates a new service offering
     * @param userId
     * @param name
     * @param cpu
     * @param ramSize
     * @param speed
     * @param diskSpace
     * @param displayText
     * @param localStorageRequired
     * @param offerHA
     * @param useVirtualNetwork
     * @return the new ServiceOfferingVO
     */
    ServiceOfferingVO createServiceOffering(long userId, String name, int cpu, int ramSize, int speed, String displayText, boolean localStorageRequired, boolean offerHA, boolean useVirtualNetwork, String tags);
    
    /**
     * Persists a pricing object
     * @param id
     * @param price
     * @param priceUnit
     * @param type
     * @param typeId
     * @param created
     * @return ID of the new pricing object
     */
    Long createPricing(Long id, float price, String priceUnit, String type, Long typeId, Date created);
    
    /**
     * Updates a service offering
     * @param userId
     * @param serviceOfferingId
     * @param name
     * @param displayText
     * @param offerHA
     * @param useVirtualNetwork
     * @param tags tags for the service offering. if null, no change will be made. if empty string, all tags will be removed.
     * @return the updated ServiceOfferingVO
     */
    ServiceOfferingVO updateServiceOffering(long userId, long serviceOfferingId, String name, String displayText, Boolean offerHA, Boolean useVirtualNetwork, String tags);
    
    /**
     * Updates a pricing object
     * @param id
     * @param price
     * @param priceUnit
     * @pram type
     * @param typeId
     * @param created
     */
    // void updatePricing(Long id, float price, String priceUnit, String type, Long typeId, Date created);
    
    /**
     * Deletes a service offering
     * @param userId
     * @param serviceOfferingId
     * @return success/failure
     */
    boolean deleteServiceOffering(long userId, long serviceOfferingId);
    
    /**
     * Adds a new pod to the database
     * @param userId
     * @param podName
     * @param zoneId
     * @param gateway
     * @param cidr
     * @param startIp
     * @param endIp
     * @return Pod
     */
    HostPodVO createPod(long userId, String podName, Long zoneId, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException;
    
    /**
     * Edits a pod in the database
     * @param userId
     * @param podId
     * @param newPodName
     * @param gateway
     * @param cidr
     * @param startIp
     * @param endIp
     * @return Pod
     */
    HostPodVO editPod(long userId, long podId, String newPodName, String gateway, String cidr, String startIp, String endIp) throws InvalidParameterValueException, InternalErrorException;
    
    /**
     * Deletes a pod from the database
     * @param userId
     * @param podId
     */
    void deletePod(long userId, long podId) throws InvalidParameterValueException, InternalErrorException;
    
    /**
     * Adds a new zone to the database
     * @param userId
     * @param zoneName
     * @param dns1
     * @param dns2
     * @param dns3
     * @param dns4
     * @param "-" separated range for network virtualization.
     * @param guestNetworkCidr
     * @return Zone
     */
    DataCenterVO createZone(long userId, String zoneName, String dns1, String dns2, String dns3, String dns4, String vnetRange, String guestCidr) throws InvalidParameterValueException, InternalErrorException;
    
    /**
     * Edits a zone in the database
     * @param userId
     * @param zoneId
     * @param newZoneName
     * @param dns1
     * @param dns2
     * @param dns3
     * @param dns4
     * @param vnetRange range of the vnet to add to the zone.
     * @param guestNetworkCidr
     * @return Zone
     */
    DataCenterVO editZone(long userId, Long zoneId, String newZoneName, String dns1, String dns2, String dns3, String dns4, String vnetRange, String guestCidr) throws InvalidParameterValueException, InternalErrorException;
    
    /**
     * Deletes a zone from the database
     * @param userId
     * @param zoneId
     */
    void deleteZone(long userId, Long zoneId) throws InvalidParameterValueException, InternalErrorException;
    
    /**
     * Change a pod's private IP range
     * @param op
     * @param podId
     * @param startIP
     * @param endIP
     * @return Message to display to user
     * @throws InvalidParameterValueException if unable to add private ip range
     */
    String changePrivateIPRange(boolean add, Long podId, String startIP, String endIP) throws InvalidParameterValueException;
    
    // List<UserVO> searchUsers(String name);
    
    /**
     * Finds users with usernames similar to the parameter
     * @param username
     * @return list of Users
     */
    // List<? extends User> findUsersLike(String username);
    
    /**
     * Finds a user by their user ID.
     * @param ownerId
     * @return User
     */
    User findUserById(Long userId);
    
    /**
     * Gets user by id.
     * 
     * @param userId
     * @param active
     * @return
     */
    User getUser(long userId, boolean active);
    
    /**
     * Obtains a list of user statistics for the specified user ID.
     * @param userId
     * @return List of UserStatistics
     */
    List<UserStatisticsVO> listUserStatsBy(Long userId);
    
    /**
     * Obtains a list of virtual machines that are similar to the VM with the specified name.
     * @param vmInstanceName
     * @return List of VMInstances
     */
    List<VMInstanceVO> findVMInstancesLike(String vmInstanceName);
    
    /**
     * Finds a virtual machine instance with the specified Volume ID.
     * @param volumeId
     * @return VMInstance
     */
    VMInstanceVO findVMInstanceById(long vmId);

    /**
     * Finds a guest virtual machine instance with the specified ID.
     * @param userVmId
     * @return UserVmVO
     */
    UserVmVO findUserVMInstanceById(long userVmId);

    /**
     * Finds a service offering with the specified ID.
     * @param offeringId
     * @return ServiceOffering
     */
    ServiceOfferingVO findServiceOfferingById(long offeringId);
    
    /**
     * Obtains a list of all service offerings.
     * @return List of ServiceOfferings
     */
    List<ServiceOfferingVO> listAllServiceOfferings();
    
    /**
     * Obtains a list of all active hosts.
     * @return List of Hosts.
     */
    List<HostVO> listAllActiveHosts();
    
    /**
     * Finds a data center with the specified ID.
     * @param dataCenterId
     * @return DataCenter
     */
    DataCenterVO findDataCenterById(long dataCenterId);
    
    /**
     * Finds a VLAN with the specified ID.
     * @param vlanDbId
     * @return VLAN
     */
    VlanVO findVlanById(long vlanDbId);
    
    /**
     * Creates a new template with the specified parameters
     * @param id
     * @param name
     * @param displayText
     * @param String format
     * @param Long guestOsId
     * @param Boolean passwordEnabled
     * @param Boolean bootable
     * @return success/failure
     */
    boolean updateTemplate(Long id, String name, String displayText, String format, Long guestOsId, Boolean passwordEnabled, Boolean bootable) throws InvalidParameterValueException;
    
    /**
     * Creates a template by downloading to all zones
     * @param createdBy userId of the template creater
     * @param zoneId optional zoneId. if null, assumed to be all zones
     * @param name - user specified name for the template
     * @param displayText user readable name.
     * @param isis it public
     * @param featured is it featured
     * @param format format of the template (VHD, ISO, QCOW2, etc)
     * @param diskType filesystem such as ext2, ntfs etc
     * @param url url to download from
     * @param chksum checksum to be verified
     * @param requiresHvm
     * @param bits - 32 or 64 bit template
     * @param enablePassword should password generation be enabled
     * @param guestOSId guestOS id
     * @param bootable is the disk bootable
     * @return template id of created template
     * @throws IllegalArgumentException
     * @throws ResourceAllocationException
     * @throws InvalidParameterValueException
     */
    Long createTemplate(long createdBy, Long zoneId, String name, String displayText, boolean isPublic, boolean featured, String format, String diskType, String url, String chksum, boolean requiresHvm, int bits, boolean enablePassword, long guestOSId, boolean bootable) throws IllegalArgumentException, ResourceAllocationException, InvalidParameterValueException;
    
    /**
     * Deletes a template from all secondary storage servers
     * @param userId
     * @param templateId
     * @param zoneId
     * @return true if success
     */
    boolean deleteTemplate(long userId, long templateId, Long zoneId, long startEventId) throws InternalErrorException;
    long deleteTemplateAsync(long userId, long templateId, Long zoneId) throws InvalidParameterValueException;
    
    /**
     * Copies a template from one secondary storage server to another
     * @param userId
     * @param templateId
     * @param sourceZoneId - the source zone
     * @param destZoneId - the destination zone
     * @return true if success
     * @throws InternalErrorException
     */
    boolean copyTemplate(long userId, long templateId, long sourceZoneId, long destZoneId, long startEventId) throws InternalErrorException;
    long copyTemplateAsync(long userId, long templateId, long sourceZoneId, long destZoneId) throws InvalidParameterValueException;
    
    /**
     * Deletes an ISO from all secondary storage servers
     * @param userId
     * @param isoId
     * @param zoneId
     * @return true if success
     */
    long deleteIsoAsync(long userId, long isoId, Long zoneId) throws InvalidParameterValueException;
    
    /**
     * Finds a template by the specified ID.
     * @param templateId
     * @return A VMTemplate
     */
    VMTemplateVO findTemplateById(long templateId);
    
    /**
     * Finds a template-host reference by the specified template and zone IDs
     * @param templateId
     * @param zoneId
     * @return template-host reference
     */
    VMTemplateHostVO findTemplateHostRef(long templateId, long zoneId);
    
    /**
     * Obtains a list of virtual machines that match the specified host ID.
     * @param hostId
     * @return List of UserVMs.
     */
    List<UserVmVO> listUserVMsByHostId(long hostId);
    
    /**
     * Obtains a list of virtual machines by the specified search criteria.
     * Can search by: "userId", "name", "state", "dataCenterId", "podId", "hostId"
     * @param c
     * @return List of UserVMs.
     */
    List<UserVmVO> searchForUserVMs(Criteria c);
    
    /**
     * Obtains a list of firewall rules by the specified IP address and forwarding flag.
     * @param publicIPAddress
     * @param forwarding
     * @return
     */
    List<FirewallRuleVO> listIPForwarding(String publicIPAddress, boolean forwarding);

    /**
     * Create a single port forwarding rule from the given ip address and port to the vm's guest IP address and private port with the given protocol.
     * @param userId the id of the user performing the action (could be an admin's ID if performing on behalf of a user)
     * @param ipAddressVO
     * @param userVM
     * @param publicPort
     * @param privatePort
     * @param protocol
     * @return
     */
    FirewallRuleVO createPortForwardingRule(long userId, IPAddressVO ipAddressVO, UserVmVO userVM, String publicPort, String privatePort, String protocol) throws NetworkRuleConflictException;

    /**
     * Update an existing port forwarding rule on the given public IP / public port for the given protocol
     * @param userId id of the user performing the action
     * @param publicIp ip address of the forwarding rule to update
     * @param privateIp ip address to forward to
     * @param publicPort public port of the forwarding rule to update
     * @param privatePort private port to forward to
     * @param protocol protocol of the rule to update
     * @return the new firewall rule if updated, null if no rule on public IP / public port of that protocol could be found
     */
    FirewallRuleVO updatePortForwardingRule(long userId, String publicIp, String privateIp, String publicPort, String privatePort, String protocol);
    long updatePortForwardingRuleAsync(long userId, long accountId, String publicIp, String privateIp, String publicPort, String privatePort, String protocol);

    /**
     * Find a firewall rule by rule id
     * @param ruleId
     * @return
     */
    FirewallRuleVO findForwardingRuleById(Long ruleId);

    /**
     * Find an IP Address VO object by ip address string
     * @param ipAddress
     * @return IP Address VO object corresponding to the given address string, null if not found
     */
    IPAddressVO findIPAddressById(String ipAddress);

    /**
     * Search for network rules given the search criteria.  For now only group id (security group id) is supported.
     * @param c the search criteria including order by and max rows
     * @return list of rules for the security group id specified in the search criteria
     */
    List<NetworkRuleConfigVO> searchForNetworkRules(Criteria c);

    /**
     * Saves an event with the specified parameters.
     * @param userId
     * @param accountId
     * @param type
     * @param description
     * @return ID of the saved event.
     */
    // Long saveEvent(Long userId, long accountId, String level, String type, String description, String params);
    
    /**
     * Obtains a list of events by the specified search criteria.
     * Can search by: "username", "type", "level", "startDate", "endDate"
     * @param c
     * @return List of Events.
     */
    List<EventVO> searchForEvents(Criteria c);
    
    List<EventVO> listPendingEvents(int entryTime, int duration);
    
    /**
     * Obtains a list of routers by the specified host ID.
     * @param hostId
     * @return List of DomainRouters.
     */
    List<DomainRouterVO> listRoutersByHostId(long hostId);
    
    /**
     * Obtains a list of all active routers.
     * @return List of DomainRouters
     */
    List<DomainRouterVO> listAllActiveRouters();
    
    /**
     * Obtains a list of routers by the specified search criteria.
     * Can search by: "userId", "name", "state", "dataCenterId", "podId", "hostId"
     * @param c
     * @return List of DomainRouters.
     */
    List<DomainRouterVO> searchForRouters(Criteria c);
    
    List<ConsoleProxyVO> searchForConsoleProxy(Criteria c);
    
    /**
     * Finds a volume which is not destroyed or removed.
     */
    VolumeVO findVolumeById(long id);
    
    /**
     * Return the volume with the given id even if its destroyed or removed.
     */
    VolumeVO findAnyVolumeById(long volumeId);
    
    /** revisit
     * Obtains a list of storage volumes by the specified search criteria.
     * Can search by: "userId", "vType", "instanceId", "dataCenterId", "podId", "hostId"
     * @param c
     * @return List of Volumes.
     */
    List<VolumeVO> searchForVolumes(Criteria c);
    
    /**
	 * Checks that the volume is stored on a shared storage pool.
	 * @param volumeId
	 * @return true if the volume is on a shared storage pool, false otherwise
	 */
    boolean volumeIsOnSharedStorage(long volumeId) throws InvalidParameterValueException;
    
    /**
     * Finds a pod by the specified ID.
     * @param podId
     * @return HostPod
     */
    HostPodVO findHostPodById(long podId);
    
    /**
     * Finds a secondary storage host in the specified zone
     * @param zoneId
     * @return Host
     */
    HostVO findSecondaryStorageHosT(long zoneId);
    
    /**
     * Obtains a list of IP Addresses by the specified search criteria.
     * Can search by: "userId", "dataCenterId", "address"
     * @param sc
     * @return List of IPAddresses
     */
    List<IPAddressVO> searchForIPAddresses(Criteria c);
    
    /**
     * Obtains a list of billing records by the specified search criteria.
     * Can search by: "userId", "startDate", "endDate"
     * @param c
     * @return List of Billings.
    List<UsageVO> searchForUsage(Criteria c);
     */
    
    /**
     * Obtains a list of all active DiskTemplates.
     * @return list of DiskTemplates
     */
    List<DiskTemplateVO> listAllActiveDiskTemplates();
    
    /**
     * Obtains a list of all templates.
     * @return list of VMTemplates
     */
    List<VMTemplateVO> listAllTemplates();
    
    /**
     * Obtains a list of all guest OS.
     * @return list of GuestOS
     */
    List<GuestOSVO> listAllGuestOS();
    
    /**
     * Obtains a list of all guest OS categories.
     * @return list of GuestOSCategories
     */
    List<GuestOSCategoryVO> listAllGuestOSCategories();
        
    /**
     * Logs out a user
     * @param userId
     */
    void logoutUser(Long userId);
    
    /**
     * Updates a template pricing.
     * @param userId
     * @param id
     * @param price
     * @return if the update was successful, this method will return an empty string. if the method was not successful,
     * the method will return a descriptive error message.
     */
    String updateTemplatePricing(long userId, Long id, float price);
    
    /**
     * Updates a configuration value.
     * @param userId
     * @param name
     * @param value
  	 * @return
     */
    void updateConfiguration(long userId, String name, String value) throws InvalidParameterValueException, InternalErrorException;
	
	/**
	 * Creates or updates an IP forwarding or load balancer rule.
	 * @param isForwarding if true, an IP forwarding rule will be created/updated, else a load balancer rule will be created/updated
	 * @param address
	 * @param port
	 * @param privateIPAddress
	 * @param privatePort
	 * @param protocol
	 * @return the rule if it was successfully created
     * @throws InvalidParameterValueException
     * @throws PermissionDeniedException
     * @throws NetworkRuleConflictException
     * @throws InternalErrorException
	 */
	NetworkRuleConfigVO createOrUpdateRule(long userId, long securityGroupId, String address, String port, String privateIpAddress, String privatePort, String protocol, String algorithm)
	throws InvalidParameterValueException, PermissionDeniedException, NetworkRuleConflictException, InternalErrorException;
	long createOrUpdateRuleAsync(boolean isForwarding, long userId, long accountId, Long domainId, long securityGroupId, String address,
			String port, String privateIpAddress, String privatePort, String protocol, String algorithm);
	
	/**
	 * Deletes an IP forwarding or load balancer rule
	 * @param ruleId
	 * @param userId
     * @param accountId
	 * @throws InvalidParameterValueException
	 * @throws PermissionDeniedException
	 * @throws InternalErrorException
	 */
	void deleteRule(long id, long userId, long accountId) throws InvalidParameterValueException, PermissionDeniedException, InternalErrorException;
	long deleteRuleAsync(long id, long userId, long accountId);
	
	ConsoleProxyInfo getConsoleProxy(long dataCenterId, long userVmId);
	ConsoleProxyVO startConsoleProxy(long instanceId, long startEventId) throws InternalErrorException;
	long startConsoleProxyAsync(long instanceId);
	boolean stopConsoleProxy(long instanceId, long startEventId);
	long stopConsoleProxyAsync(long instanceId);
	boolean rebootConsoleProxy(long instanceId, long startEventId);
	long rebootConsoleProxyAsync(long instanceId);
	boolean destroyConsoleProxy(long instanceId, long startEventId);
	long destroyConsoleProxyAsync(long instanceId);
	String getConsoleAccessUrlRoot(long vmId);
	ConsoleProxyVO findConsoleProxyById(long instanceId);
	VMInstanceVO findSystemVMById(long instanceId);
	boolean stopSystemVM(long instanceId, long startEventId);
	VMInstanceVO startSystemVM(long instanceId, long startEventId) throws InternalErrorException;
	long startSystemVmAsync(long instanceId);
	long stopSystemVmAsync(long instanceId);
	long rebootSystemVmAsync(long longValue);
	boolean rebootSystemVM(long instanceId, long startEventId);



	
	/**
	 * Returns a configuration value with the specified name
	 * @param name
	 * @return configuration value
	 */
	String getConfigurationValue(String name);
	
	/**
	 * Returns the vnc port of the vm.
	 * 
	 * @param VirtualMachine vm
	 * @return the vnc port if found; -1 if unable to find.
	 */
	int getVncPort(VirtualMachine vm);
	

	/**
	 * Search for domains owned by the given domainId/domainName (those parameters are wrapped
	 * in a Criteria object.
	 * @return list of domains owned by the given user
	 */
	List<DomainVO> searchForDomains(Criteria c);
	
	List<DomainVO> searchForDomainChildren(Criteria c);

	/**
	 * create a new domain
	 * @param id
	 * @param domain name
	 * @param ownerId
	 * @param parentId
	 * 
	 */
	DomainVO createDomain(String name, Long ownerId, Long parentId);

	/**
     * delete a domain with the given domainId
     * @param domainId
     * @param ownerId
     * @param cleanup - whether or not to delete all accounts/VMs/sub-domains when deleting the domain
     */
	String deleteDomain(Long domainId, Long ownerId, Boolean cleanup);
	long deleteDomainAsync(Long domainId, Long ownerId, Boolean cleanup);
    /**
     * update an existing domain
     * @param domainId the id of the domain to be updated
     * @param domainName the new name of the domain
     */
    void updateDomain(Long domainId, String domainName);

    /**
     * find the domain Id associated with the given account
     * @param accountId the id of the account to use to look up the domain
     */
    Long findDomainIdByAccountId(Long accountId);
    
    /**
     * find the domain by id
     * @param domainId the id of the domainId
     */
    DomainVO findDomainIdById(Long domainId);

    /**
     * find the domain by its path
     * @param domainPath the path to use to lookup a domain
     * @return domainVO the domain with the matching path, or null if no domain with the given path exists
     */
    DomainVO findDomainByPath(String domainPath);

    /**
     * Finds accounts with account identifiers similar to the parameter
     * @param accountName
     * @return list of Accounts
     */
    List<AccountVO> findAccountsLike(String accountName);
    
    /**
     * Finds accounts with account identifier
     * @param accountName
     * @return an account that is active (not deleted)
     */
    Account findActiveAccountByName(String accountName);
    
    /**
     * Finds accounts with account identifier
     * @param accountName, domainId
     * @return an account that is active (not deleted)
     */
    
    Account findActiveAccount(String accountName, Long domainId);
    
    /**
     * Finds accounts with account identifier
     * @param accountName
     * @param domainId
     * @return an account that may or may not have been deleted
     */
    Account findAccountByName(String accountName, Long domainId);
    
    /**
     * Finds an account by the ID.
     * @param accountId
     * @return Account
     */
    Account findAccountById(Long accountId);

    /**
     * Finds a GuestOS by the ID.
     * @param id
     * @return GuestOS
     */
    GuestOS findGuestOSById(Long id);
    
    /**
     * Searches for accounts by the specified search criteria
     * Can search by: "id", "name", "domainid", "type"
     * @param c
     * @return List of Accounts
     */
    List<AccountVO> searchForAccounts(Criteria c);
    
    
    /**
     * Find the owning account of an IP Address
     * @param ipAddress
     * @return owning account if ip address is allocated, null otherwise
     */
    Account findAccountByIpAddress(String ipAddress);

    /**
	 * Updates an existing resource limit with the specified details. If a limit doesn't exist, will create one.
	 * @param domainId
	 * @param accountId
	 * @param type
	 * @param max
	 * @return
	 * @throws InvalidParameterValueException
	 */
    ResourceLimitVO updateResourceLimit(Long domainId, Long accountId, ResourceType type, Long max) throws InvalidParameterValueException;
    
    /**
     * Deletes a Limit
     * @param limitId - the database ID of the Limit
     * @return true if successful, false if not
     */
    boolean deleteLimit(Long limitId);
    
    /**
     * Finds limit by id
     * @param limitId - the database ID of the Limit
     * @return LimitVO object
     */
    ResourceLimitVO findLimitById(long limitId);
    
    /**
     * Searches for Limits.
     * @param domainId
     * @param accountId
     * @param type
     * @return a list of Limits
     */
    List<ResourceLimitVO> searchForLimits(Criteria c);
    
    /**
	 * Finds the correct limit for an account. I.e. if an account's limit is not present, it will check the account's domain, and as a last resort use the global limit.
	 * @param type
	 * @param accountId
	 */
	long findCorrectResourceLimit(ResourceType type, long accountId);
	
	/**
	 * Gets the count of resources for a resource type and account
	 * @param Type
	 * @param accountId
	 * @return count of resources
	 */
	long getResourceCount(ResourceType type, long accountId);

    /**
     * Lists ISOs that are available for the specified account ID.
     * @param accountId
     * @param accountType
     * @return a list of ISOs (VMTemplateVO objects)
     */
    List<VMTemplateVO> listIsos(Criteria c);
    
    /**
     * Searches for alerts
     * @param c
     * @return List of Alerts
     */
    List<AlertVO> searchForAlerts(Criteria c);

    /**
     * list all the capacity rows in capacity operations table
     * @param c
     * @return List of capacities
     */
    List<CapacityVO> listCapacities(Criteria c);

    public long getMemoryUsagebyHost(Long hostId);
    
    /**
     * Create a snapshot of a volume
     * @param userId the user for whom this snapshot is being created
     * @param volumeId the id of the volume
     * @return the Snapshot that was created
     * @throws InternalErrorException
     */
    long createSnapshotAsync(long userId, long volumeId)
    throws InvalidParameterValueException,
           ResourceAllocationException,
           InternalErrorException;

    
    long deleteSnapshotAsync(long userId, long snapshotId);

    long createVolumeFromSnapshotAsync(long userId, long accountId, long snapshotId, String volumeName) throws InternalErrorException, ResourceAllocationException;
    
    /**
     * List all snapshots of a disk volume. Optionaly lists snapshots created by specified interval
     * @param c the search criteria (order by, limit, etc.)
     * @return list of snapshots
     * @throws InvalidParameterValueException
     */
    List<SnapshotVO> listSnapshots(Criteria c, String interval) throws InvalidParameterValueException;

    /**
     * find a single snapshot by id
     * @param snapshotId
     * @return the snapshot if found, null otherwise
     */
    Snapshot findSnapshotById(long snapshotId);

    /**
     * Create a private template from a given snapshot
     * @param snapshotId the id of the snapshot to use as the basis of the template
     * @param name user provided string to use to name the template
     * @param description the display text to show when listing the template as given by the user
     * @param guestOSId the OS of the template
     * @param requiresHvm whether the new template will require HVM
     * @param bits number of bits (32-bit or 64-bit)
     * @param passwordEnabled whether or not the template is password enabled
     * @param iswhether or not the template is public
     * @return valid template if success, null otherwise
     * @throws InvalidParameterValueException, ResourceAllocationException
     */
    VMTemplateVO createPrivateTemplate(VMTemplateVO template, Long userId, long snapshotId, String name, String description) throws InvalidParameterValueException;
    long createPrivateTemplateAsync(Long userId, long vmId, String name, String description, long guestOSId, Boolean requiresHvm, Integer bits, Boolean passwordEnabled, boolean isPublic, boolean featured, Long snapshotId) throws InvalidParameterValueException, ResourceAllocationException, InternalErrorException;
    
    
    /**
     * Finds a diskOffering by the specified ID.
     * @param diskOfferingId
     * @return A DiskOffering
     */
    DiskOfferingVO findDiskOfferingById(long diskOffering);

    /**
     * Update the permissions on a template.  A private template can be made public, or individual accounts can be granted permission to launch instances from the template.
     * @param templateId
     * @param operation
     * @param isPublic
     * @param isFeatured
     * @param accountNames
     * @return
     * @throws InvalidParameterValueException
     * @throws PermissionDeniedException
     * @throws InternalErrorException
     */
    boolean updateTemplatePermissions(long templateId, String operation, Boolean isPublic, Boolean isFeatured, List<String> accountNames) throws InvalidParameterValueException, PermissionDeniedException, InternalErrorException;

    /**
     * List the permissions on a template.  This will return a list of account names that have been granted permission to launch instances from the template.
     * @param templateId
     * @return list of account names that have been granted permission to launch instances from the template
     */
    List<String> listTemplatePermissions(long templateId);

    /**
     * List private templates for which the given account/domain has been granted permission to launch instances
     * @param accountId
     * @return
     */
    List<VMTemplateVO> listPermittedTemplates(long accountId);

    /**
     * Lists templates that match the specified criteria
     * @param templateId - (optional) id of the template to return template host references for
     * @param name a name (possibly partial) to search for
     * @param keyword a keyword (using partial match) to search for, currently only searches name
     * @param templateFilter - the category of template to return
     * @param isIso whether this is an ISO search or non-ISO search
     * @param bootable if null will return both bootable and non-bootable ISOs, else will return only one or the other, depending on the boolean value
     * @param accountId parameter to use when searching for owner of template
     * @param pageSize size of search results
     * @param startIndex index in to search results to use
     * @param zoneId optional zoneid to limit search
     * @return list of templates
     */
    List<VMTemplateVO> listTemplates(Long templateId, String name, String keyword, TemplateFilter templateFilter, boolean isIso, Boolean bootable, Long accountId, Integer pageSize, Long startIndex, Long zoneId) throws InvalidParameterValueException;

    /**
     * Search for disk offerings based on search criteria
     * @param c the criteria to use for searching for disk offerings
     * @return a list of disk offerings that match the given criteria
     */
    List<DiskOfferingVO> searchForDiskOfferings(Criteria c);

    /**
     * Create a disk offering
     * @param domainId the id of the domain in which the disk offering is valid
     * @param name the name of the disk offering
     * @param description a string description of the disk offering
     * @param numGibibytes the number of gibibytes in the disk offering (1 gibibyte = 1024 MB)
     * @param mirrored boolean value of whether or not the offering provides disk mirroring
     * @param tags Comma separated string to indicate special tags for the disk offering.
     * @return the created disk offering, null if failed to create
     */
    DiskOfferingVO createDiskOffering(long domainId, String name, String description, int numGibibytes, boolean mirrored, String tags) throws InvalidParameterValueException;

    /**
     * Delete a disk offering
     * @param id id of the disk offering to delete
     * @return true if deleted, false otherwise
     */
    boolean deleteDiskOffering(long id);
    
    /**
     * Update a disk offering
     * @param userId
     * @param disk offering id
     * @param name the name of the disk offering to be updated
     * @param description a string description of the disk offering to be updated
     * @param tags for the disk offering. if null, no change will be made. if empty string, all tags will be removed.
     * @return updated disk offering
     */
    DiskOfferingVO updateDiskOffering(long userId, long diskOfferingId, String name, String description, String tags);
    
    /**
     * 
     * @param jobId async-call job id
     * @return async-call result object
     */
    AsyncJobResult queryAsyncJobResult(long jobId) throws PermissionDeniedException;
    AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId);
    AsyncJobVO findAsyncJobById(long jobId);
    
    List<AsyncJobVO> searchForAsyncJobs(Criteria c);
    

    /**
     * Assign a security group to a VM
     * @param userId id of the user assigning the security group
     * @param securityGroupId the id of the security group to apply (single add)
     * @param securityGroupIdList the list of ids of the security groups that should be assigned to the vm (will add missing groups and remove existing groups to reconcile with the given list)
     * @param publicIp ip address used for creating forwarding rules from the network rules in the group
     * @param vmId vm id to use from getting the private ip address used for creating forwarding rules from the network rules in the group
     */
    void assignSecurityGroup(Long userId, Long securityGroupId, List<Long> securityGroupIdList, String publicIp, Long vmId, long startEventId) throws PermissionDeniedException, NetworkRuleConflictException, InvalidParameterValueException, InternalErrorException;

    /**
     * remove a security group from a publicIp/vmId combination where it had been previously applied
     * @param userId id of the user performing the action (for events)
     * @param securityGroupId the id of the security group to remove
     * @param publicIp
     * @param vmId
     */
    void removeSecurityGroup(long userId, long securityGroupId, String publicIp, long vmId, long startEventId) throws InvalidParameterValueException, PermissionDeniedException;

    long assignSecurityGroupAsync(Long userId, Long securityGroupId, List<Long> securityGroupIdList, String publicIp, Long vmId);

    long removeSecurityGroupAsync(Long userId, long securityGroupId, String publicIp, long vmId);

    /**
     * validate that the list of security groups can be applied to the instance
     * @param securityGroupIds
     * @param instanceId
     * @return accountId that owns the instance if the security groups can be applied to the instance, null otherwise
     */
    Long validateSecurityGroupsAndInstance(List<Long> securityGroupIds, Long instanceId);

    /**
     * returns a list of security groups that can be applied to virtual machines for the given
     * account/domain
     * @param accountId the id of the account used for looking up groups
     * @param domainId the domain of the given account, or if the account is null the domain
     *                 to use for searching for groups
     * @return a list of security groups
     */
    List<SecurityGroupVO> listSecurityGroups(Long accountId, Long domainId);

    /**
     * returns a list of security groups
     * @param c
     * @return a list of security groups
     */
    List<SecurityGroupVO> searchForSecurityGroups(Criteria c);

    /**
     * returns a list of security groups from a given ip and vm id
     * @param c
     * @return a list of security groups
     */
    Map<String, List<SecurityGroupVO>> searchForSecurityGroupsByVM(Criteria c);

    /**
     * Create a security group, a group of network rules (port, private port, protocol, algorithm) that can be applied in mass to a VM
     * @param name name of the group, must be unique for the domain
     * @param description brief description of the group, can be null
     * @param domainId domain where the security group is valid
     * @param accountId owner of the security group, can be null for domain level security groups
     * @return
     */
    SecurityGroupVO createSecurityGroup(String name, String description, Long domainId, Long accountId);

    /**
     * Delete a security group.  If the group is being actively used, it cannot be deleted.
     * @param userId the id of the user performing the action
     * @param securityGroupId the id of the group to delete
     * @param eventId
     * @return true if the security group is deleted, exception is thrown otherwise
     */
    boolean deleteSecurityGroup(long userId, long securityGroupId, long eventId)  throws InvalidParameterValueException, PermissionDeniedException;
    long deleteSecurityGroupAsync(long userId, Long accountId, long securityGroupId);

    /**
     * check if a security group name in the given account/domain is in use
     *      - if accountId is specified, look only for the account
     *      - otherwise look for the name in domain-level security groups (accountId is null)
     * @param domainId id of the domain in which to search for security groups
     * @param accountId id of the account in which to search for security groups
     * @param name name of the security group to look for
     * @return true if the security group name is found, false otherwise
     */
    boolean isSecurityGroupNameInUse(Long domainId, Long accountId, String name);
    SecurityGroupVO findSecurityGroupById(Long groupId);

    boolean deleteNetworkRuleConfig(long userId, long networkRuleId);
    long deleteNetworkRuleConfigAsync(long userId, Account account, Long networkRuleId) throws PermissionDeniedException;

    LoadBalancerVO findLoadBalancer(Long accountId, String name);
    LoadBalancerVO findLoadBalancerById(long loadBalancerId);
    List<UserVmVO> listLoadBalancerInstances(long loadBalancerId, boolean applied);
    List<LoadBalancerVO> searchForLoadBalancers(Criteria c);
    LoadBalancerVO createLoadBalancer(Long userId, Long accountId, String name, String description, String ipAddress, String publicPort, String privatePort, String algorithm) throws InvalidParameterValueException, PermissionDeniedException;
    boolean deleteLoadBalancer(long userId, long loadBalancerId);
    long deleteLoadBalancerAsync(long userId, long loadBalancerId);

    /**
     * Update a load balancer rule from the existing private port to a new private port.  The load balancer is found by publicIp, public port, and algorithm.
     * The individual rule for update is matched by privateIp.
     * @param userId the id of the user performing the action
     * @param loadBalancer the load balancer rule being updated
     * @param privatePort the target private port for the load balancer rule (the rule will be updated from the existing port to this port)
     * @param algorithm the target algorithm of the load balancer rule (the rule will be updated from the existing algorithm to this algorithm)
     * @return the updated load balancer rule
     */
    LoadBalancerVO updateLoadBalancerRule(long userId, LoadBalancerVO loadBalancer, String privatePort, String algorithm);

    /**
     * Update the name and/or description of a load balancer rule
     * @param loadBalancer the load balancer rule to update
     * @param name the new name, null if not changing the name
     * @param description the new description, null if not changing the description
     * @return the updated load balancer rule
     */
    LoadBalancerVO updateLoadBalancerRule(LoadBalancerVO loadBalancer, String name, String description) throws InvalidParameterValueException;

    /**
     * Update the name, description, private port, and/or algorithm of a load balancer rule
     * @param userId the id of the user performing the action
     * @param accountId the id of the account that owns the load balancer rule
     * @param loadBalancerId the id of the load balancer rule being updated
     * @param name the new name, null if not changing the name
     * @param description the new description, null if not changing the description
     * @param privatePort the target private port for the load balancer rule (the rule will be updated from the existing port to this port)
     * @param algorithm the target algorithm of the load balancer rule (the rule will be updated from the existing algorithm to this algorithm)
     * @return the updated load balancer rule
     */
    long updateLoadBalancerRuleAsync(long userId, long accountId, long loadBalancerId, String name, String description, String privatePort, String algorithm);

    void assignToLoadBalancer(long userId, long loadBalancerId, List<Long> instanceIds) throws NetworkRuleConflictException, InternalErrorException, PermissionDeniedException, InvalidParameterValueException;
    long assignToLoadBalancerAsync(long userId, long loadBalancerId, List<Long> instanceIds);
    boolean removeFromLoadBalancer(long userId, long loadBalancerId, List<Long> instanceIds) throws InvalidParameterValueException;
    long removeFromLoadBalancerAsync(long userId, long loadBalancerId, List<Long> instanceIds);

    String[] getApiConfig();
    StoragePoolVO findPoolById(Long id);
	StoragePoolVO addPool(Long zoneId, Long podId, Long clusterId, String poolName, String storageUri, String tags, Map<String, String> details) throws ResourceInUseException, URISyntaxException, IllegalArgumentException, UnknownHostException, ResourceAllocationException;
	List<? extends StoragePoolVO> searchForStoragePools(Criteria c);
	
	/**
	 * Creates a policy with specified schedule to create snapshot for a volume . maxSnaps specifies the number of most recent snapshots that are to be retained.
	 * @param volumeId
	 * @param schedule MM[:HH][:DD] format. DD is day of week for weekly[1-7] and day of month for monthly
	 * @param interval hourly/daily/weekly/monthly
	 * @param maxSnaps If the number of snapshots go beyond maxSnaps the oldest snapshot is deleted
	 * @param timezone The timezone in which the above time format is specified
	 * @return
	 * @throws InvalidParameterValueException
	 */
	SnapshotPolicyVO createSnapshotPolicy(long userId, long accountId, long volumeId, String schedule, String intervalType,
			int maxSnaps, String timezone) throws InvalidParameterValueException;
	
	/**
	 * List all snapshot policies which are created for the specified volume
	 * @param volumeId
	 * @return
	 */
	List<SnapshotPolicyVO> listSnapshotPolicies(long volumeId);
	SnapshotPolicyVO findSnapshotPolicyById(Long policyId);
	
	/**
	 * Deletes snapshot scheduling policies
	 */
	boolean deleteSnapshotPolicies(long userId, List<Long> policyIds) throws InvalidParameterValueException;

	/**
	 * Get the recurring snapshots scheduled for this volume currently along with the time at which they are scheduled
	 * @param volumeId The volume for which the snapshots are required.
	 * @param policyId Show snapshots for only this policy.
	 * @return The list of snapshot schedules.
	 */
    List<SnapshotScheduleVO> findRecurringSnapshotSchedule(Long volumeId, Long policyId);
    
	/**
	 * Return whether a domain is a child domain of a given domain.
	 * @param parentId
	 * @param childId
	 * @return True if the domainIds are equal, or if the second domain is a child of the first domain.  False otherwise.
	 */
    boolean isChildDomain(Long parentId, Long childId);
	
    
    /**
     * List interval types the specified snapshot belongs to
     * @param snapshotId
     * @return
     */
    String getSnapshotIntervalTypes(long snapshotId);
    
    
	List<SecondaryStorageVmVO> searchForSecondaryStorageVm(Criteria c);
	
	/**
	 * Deletes a pool based on the pool id
	 * @param id -- pool id
	 * @return -- status of the operation
	 */
	boolean deletePool(Long id);
    
	/**
	 * Returns back a SHA1 signed response
	 * @param userId -- id for the user
	 * @return -- ArrayList of <CloudId+Signature>
	 */
    ArrayList<String> getCloudIdentifierResponse(long userId);
    
    /**
     * check if a network security group name in the given account/domain is in use
     *      - if accountId is specified, look only for the account
     *      - otherwise look for the name in domain-level security groups (accountId is null)
     * @param domainId id of the domain in which to search for security groups
     * @param accountId id of the account in which to search for security groups
     * @param name name of the security group to look for
     * @return true if the security group name is found, false otherwise
     */
    boolean isNetworkSecurityGroupNameInUse(Long domainId, Long accountId, String name);
    NetworkGroupVO findNetworkGroupByName(Long accountId, String groupName);

    /**
     * Find a network group by id
     * @param networkGroupId id of group to lookup
     * @return the network group if found, null otherwise
     */
    NetworkGroupVO findNetworkGroupById(long networkGroupId);

    /**
     * Authorize access to a network group.  Access can be granted to a set of IP ranges, or to network groups belonging to other accounts.
     * @param accountId the account id of the owner of the given network group
     * @param groupName the name of the network group from which access is being granted
     * @param protocol scopes the network protocol to which access is being granted
     * @param startPort scopes the start of a network port range to which access is being granted (or icmp type if the protocol is icmp)
     * @param endPort scopes the end of a network port range to which access is being granted (or icmp code if the protocol is icmp)
     * @param cidrList the IP range to which access is being granted
     * @param authorizedGroups the network groups (looked up by group name/account) to which access is being granted
     * @return the job id if scheduled, 0 if the job was not scheduled
     */
    long authorizeNetworkGroupIngressAsync(Long accountId, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups);
    List<IngressRuleVO> authorizeNetworkGroupIngress(AccountVO account, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups);

    /**
	 * Revoke access to a network group.  Access could have been granted to a set of IP ranges, or to network groups belonging to other accounts.  Access
	 * can be revoked in a similar manner (either from a set of IP ranges or from network groups belonging to other accounts).
	 * @param accountId the account id of the owner of the given network group
	 * @param groupName the name of the network group from which access is being revoked
	 * @param protocol access had been granted on a port range (start port, end port) and network protocol, this protocol scopes the network protocol from which access is being revoked
	 * @param startPort access had been granted on a port range (start port, end port) and network protocol, this start port scopes the start of a network port range from which access is being revoked
	 * @param endPort access had been granted on a port range (start port, end port) and network protocol, this end port scopes the end of a network port range from which access is being revoked
	 * @param cidrList the IP range from which access is being revoked
	 * @param authorizedGroups the network groups (looked up by group name/account) from which access is being revoked
	 * @return the job id if scheduled, 0 if the job was not scheduled
	 */
	long revokeNetworkGroupIngressAsync(Long accountId, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups);
	boolean revokeNetworkGroupIngress(AccountVO account, String groupName, String protocol, int startPort, int endPort, String [] cidrList, List<NetworkGroupVO> authorizedGroups);

	NetworkGroupVO createNetworkGroup(String name, String description, Long domainId, Long accountId, String accountName);

	/**
	 * Delete an empty network group.  If the group is not empty an error is returned.
	 * @param groupId
	 * @param accountId
	 * @throws PermissionDeniedException
	 */
	void deleteNetworkGroup(Long groupId, Long accountId) throws ResourceInUseException, PermissionDeniedException;

    /**
     * Search for network groups and associated ingress rules for the given account, domain, group name, and/or keyword.
     * The search terms are specified in the search criteria.
     * @return the list of network groups and associated ingress rules
     */
    public List<NetworkGroupRulesVO> searchForNetworkGroupRules(Criteria c);

	HostStats getHostStatistics(long hostId);
	
	/**
	 * Is the hypervisor snapshot capable.
	 * @return True if the hypervisor.type is XenServer
	 */
	boolean isHypervisorSnapshotCapable();
	List<String> searchForStoragePoolDetails(long poolId, String value);
	
	/**
	 * Returns a comma separated list of tags for the specified storage pool
	 * @param poolId
	 * @return comma separated list of tags
	 */
	String getStoragePoolTags(long poolId);
	
	/**
	 * Checks if a host has running VMs that are using its local storage pool.
	 * @return true if local storage is active on the host
	 */
	boolean isLocalStorageActiveOnHost(HostVO host);
	
	public List<PreallocatedLunVO> getPreAllocatedLuns(Criteria c);
	
	public String getNetworkGroupsNamesForVm(long vmId);
}
