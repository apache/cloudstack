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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.cloud.alert.AlertVO;
import com.cloud.api.commands.AssignPortForwardingServiceCmd;
import com.cloud.api.commands.CreateDomainCmd;
import com.cloud.api.commands.CreatePortForwardingServiceCmd;
import com.cloud.api.commands.CreatePortForwardingServiceRuleCmd;
import com.cloud.api.commands.CreateUserCmd;
import com.cloud.api.commands.DeletePortForwardingServiceCmd;
import com.cloud.api.commands.DeleteUserCmd;
import com.cloud.api.commands.DeployVMCmd;
import com.cloud.api.commands.EnableAccountCmd;
import com.cloud.api.commands.EnableUserCmd;
import com.cloud.api.commands.GetCloudIdentifierCmd;
import com.cloud.api.commands.ListAccountsCmd;
import com.cloud.api.commands.ListAlertsCmd;
import com.cloud.api.commands.ListAsyncJobsCmd;
import com.cloud.api.commands.ListCapacityCmd;
import com.cloud.api.commands.ListCfgsByCmd;
import com.cloud.api.commands.ListClustersCmd;
import com.cloud.api.commands.ListDiskOfferingsCmd;
import com.cloud.api.commands.ListDomainChildrenCmd;
import com.cloud.api.commands.ListDomainsCmd;
import com.cloud.api.commands.ListEventsCmd;
import com.cloud.api.commands.ListGuestOsCategoriesCmd;
import com.cloud.api.commands.ListGuestOsCmd;
import com.cloud.api.commands.ListHostsCmd;
import com.cloud.api.commands.ListIsosCmd;
import com.cloud.api.commands.ListLoadBalancerRuleInstancesCmd;
import com.cloud.api.commands.ListLoadBalancerRulesCmd;
import com.cloud.api.commands.ListPodsByCmd;
import com.cloud.api.commands.ListPortForwardingServiceRulesCmd;
import com.cloud.api.commands.ListPortForwardingServicesByVmCmd;
import com.cloud.api.commands.ListPortForwardingServicesCmd;
import com.cloud.api.commands.ListPreallocatedLunsCmd;
import com.cloud.api.commands.ListPublicIpAddressesCmd;
import com.cloud.api.commands.ListRoutersCmd;
import com.cloud.api.commands.ListServiceOfferingsCmd;
import com.cloud.api.commands.ListSnapshotsCmd;
import com.cloud.api.commands.ListStoragePoolsCmd;
import com.cloud.api.commands.ListSystemVMsCmd;
import com.cloud.api.commands.ListTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.ListTemplatesCmd;
import com.cloud.api.commands.ListUsersCmd;
import com.cloud.api.commands.ListVMsCmd;
import com.cloud.api.commands.ListVlanIpRangesCmd;
import com.cloud.api.commands.ListVolumesCmd;
import com.cloud.api.commands.ListZonesByCmd;
import com.cloud.api.commands.LockAccountCmd;
import com.cloud.api.commands.LockUserCmd;
import com.cloud.api.commands.QueryAsyncJobResultCmd;
import com.cloud.api.commands.RebootSystemVmCmd;
import com.cloud.api.commands.RegisterCmd;
import com.cloud.api.commands.RemovePortForwardingServiceCmd;
import com.cloud.api.commands.StartSystemVMCmd;
import com.cloud.api.commands.StopSystemVmCmd;
import com.cloud.api.commands.UpdateAccountCmd;
import com.cloud.api.commands.UpdateDomainCmd;
import com.cloud.api.commands.UpdateIPForwardingRuleCmd;
import com.cloud.api.commands.UpdateTemplateOrIsoCmd;
import com.cloud.api.commands.UpdateTemplateOrIsoPermissionsCmd;
import com.cloud.api.commands.UpdateUserCmd;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.capacity.CapacityVO;
import com.cloud.configuration.ConfigurationVO;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.domain.DomainVO;
import com.cloud.event.EventVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientStorageCapacityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostStats;
import com.cloud.host.HostVO;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.network.FirewallRuleVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.NetworkRuleConfigVO;
import com.cloud.network.SecurityGroupVO;
import com.cloud.network.security.NetworkGroupVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.DiskTemplateVO;
import com.cloud.storage.GuestOS;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.StorageStats;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.SecondaryStorageVmVO;
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
     * Creates a new user, stores the password as is so encrypted passwords are recommended.
     * @param cmd the create command that has the username, email, password, account name, domain, timezone, etc. for creating the user.
     * @return the user if created successfully, null otherwise
     */
    UserAccount createUser(CreateUserCmd cmd);

    ClusterVO findClusterById(long clusterId);
    List<ClusterVO> listClusterByPodId(long podId);

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
     * @param cmd - the delete command defining the id of the user to be deleted.
     * @return true if delete was successful, false otherwise
     */
    boolean deleteUser(DeleteUserCmd cmd);

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
     * @param cmd - the enableAccount command defining the accountId to be deleted.
     * @return true if enable was successful, false otherwise
     */
    boolean enableAccount(EnableAccountCmd cmd) throws InvalidParameterValueException;

    /**
     * Locks an account by accountId.  A locked account cannot access the API, but will still have running VMs/IP addresses allocated/etc.
     * @param cmd - the LockAccount command defining the accountId to be locked.
     * @return true if enable was successful, false otherwise
     */
    boolean lockAccount(LockAccountCmd cmd);

    /**
     * Updates an account name
     * @param cmd
     * @return true if update was successful, false otherwise
     */
    
    boolean updateAccount(UpdateAccountCmd cmd) throws InvalidParameterValueException;

    /**
     * Enables a user
     * @param cmd
     * @return true if enable was successful, false otherwise
     */
    boolean enableUser(EnableUserCmd cmd) throws InvalidParameterValueException;

    /**
     * Locks a user by userId.  A locked user cannot access the API, but will still have running VMs/IP addresses allocated/etc.
     * @param userId
     * @return true if enable was successful, false otherwise
     */
    boolean lockUser(LockUserCmd cmd);
    
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

    String updateAdminPassword(long userId, String oldPassword, String newPassword);

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
     * Searches for vlan by the specified search criteria
     * Can search by: "id", "vlan", "name", "zoneID"
     * @param cmd
     * @return List of Vlans
     */
    List<VlanVO> searchForVlans(ListVlanIpRangesCmd cmd) throws InvalidParameterValueException;
    
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
     * @param size - size of the volume
     * @return true if success, false if not
     */
    VolumeVO createVolume(long accountId, long userId, String name, long zoneId, long diskOfferingId, long startEventId, long size) throws InternalErrorException;
    long createVolumeAsync(long accountId, long userId, String name, long zoneId, long diskOfferingId, long size) throws InvalidParameterValueException, InternalErrorException, ResourceAllocationException;
    
    /**
     * Finds the root volume of the VM
     * @param vmId
     * @return Volume
     */
    VolumeVO findRootVolume(long vmId);

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
     * Generates a random password that will be used (initially) by newly created and started virtual machines
     * @return a random password
     */
    String generateRandomPassword();

    /**
     * Attaches an ISO to the virtual CDROM device of the specified VM. Will fail if the VM already has an ISO mounted.
     * @param vmId
     * @param userId
     * @param isoId
     * @param attach whether to attach or detach the iso from the instance
     * @return
     */
    boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach, long startEventId);

    /**
     * Creates and starts a new Virtual Machine.
     * 
     * @param cmd the command with the deployment parameters
     *   - userId
     *   - accountId
     *   - zoneId
     *   - serviceOfferingId
     *   - templateId:  the id of the template (or ISO) to use for creating the virtual machine
     *   - diskOfferingId:  ID of the disk offering to use when creating the root disk (if deploying from an ISO) or the data disk (if deploying from a template). If deploying from a template and a disk offering ID is not passed in, the VM will have only a root disk.
     *   - displayName:  user-supplied name to be shown in the UI or returned in the API
     *   - groupName:  user-supplied groupname to be shown in the UI or returned in the API
     *   - userData:  user-supplied base64-encoded data that can be retrieved by the instance from the virtual router
     *   - size:  size to be used for volume creation in case the disk offering is private (i.e. size=0)
     * @return VirtualMachine if successfully deployed, null otherwise
     * @throws InvalidParameterValueException if the parameter values are incorrect.
     * @throws ExecutionException
     * @throws StorageUnavailableException
     * @throws ConcurrentOperationException
     */
    UserVm deployVirtualMachine(DeployVMCmd cmd) throws ResourceAllocationException, InvalidParameterValueException, InternalErrorException, InsufficientStorageCapacityException, PermissionDeniedException, ExecutionException, StorageUnavailableException, ConcurrentOperationException;

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
     * Starts a Domain Router
     * 
     * @param routerId
     * @return DomainRouter if successfully started, false otherwise
     */
	DomainRouter startRouter(long routerId, long startEventId) throws InternalErrorException;
	
	/**
	 * Stops a Domain Router
	 * 
	 * @param routerId
	 * @return true if successfully stopped, false otherwise
	 */
	boolean stopRouter(long routerId, long startEventId);
	
	/**
	 * Reboots a Domain Router
	 * 
	 * @param routerId
	 * @return true if successfully rebooted, false otherwise
	 */
	boolean rebootRouter(long routerId, long startEventId) throws InternalErrorException;

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
     * Retrieves the list of data centers with search criteria.
     * Currently the only search criteria is "available" zones for the account that invokes the API.  By specifying
     * available=true all zones which the account can access.  By specifying available=false the zones where the
     * account has virtual machine instances will be returned.
     * @return a list of DataCenters
     */
    List<DataCenterVO> listDataCenters(ListZonesByCmd cmd);
    
    /**
     * Retrieves a host by id
     * @param hostId
     * @return Host
     */
    HostVO getHostBy(long hostId);
    
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
    List<ConfigurationVO> searchForConfigurations(ListCfgsByCmd c);
    
    /**
     * returns the instance id of this management server.
     * @return id of the management server
     */
    long getId();
    
    /** revisit
     * Searches for users by the specified search criteria
     * Can search by: "id", "username", "account", "domainId", "type"
     * @param cmd
     * @return List of UserAccounts
     */
    List<UserAccountVO> searchForUsers(ListUsersCmd cmd) throws PermissionDeniedException;
    
    /**
     * Searches for Service Offerings by the specified search criteria
     * Can search by: "name"
     * @param cmd
     * @return List of ServiceOfferings
     */
    List<ServiceOfferingVO> searchForServiceOfferings(ListServiceOfferingsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
    /**
     * Searches for Clusters by the specified search criteria
     * @param c
     * @return
     */
    List<ClusterVO> searchForClusters(ListClustersCmd c);
    
    /**
     * Searches for Pods by the specified search criteria
     * Can search by: pod name and/or zone name
     * @param cmd
     * @return List of Pods
     */
    List<HostPodVO> searchForPods(ListPodsByCmd cmd);
    
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
     * @param cmd
     * @return List of Hosts
     */
    List<HostVO> searchForServers(ListHostsCmd cmd);
    
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
     * Obtains pods that match the data center ID
     * @param dataCenterId
     * @return List of Pods
     */
    List<HostPodVO> listPods(long dataCenterId);

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
     * Creates a new template
     * @param cmd
     * @return success/failure
     */
    boolean updateTemplate(UpdateTemplateOrIsoCmd cmd) throws InvalidParameterValueException;
    
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
     * Obtains a list of virtual machines by the specified search criteria.
     * Can search by: "userId", "name", "state", "dataCenterId", "podId", "hostId"
     * @param cmd the API command that wraps the search criteria
     * @return List of UserVMs.
     */
    List<UserVmVO> searchForUserVMs(ListVMsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

    /**
     * Update an existing port forwarding rule on the given public IP / public port for the given protocol
     * @param cmd - the UpdateIPForwardingRuleCmd command that wraps publicIp, privateIp, publicPort, privatePort, protocol of the rule to update
     * @return the new firewall rule if updated, null if no rule on public IP / public port of that protocol could be found
     */
    FirewallRuleVO updatePortForwardingRule(UpdateIPForwardingRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

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
     * Search for network rules given the search criteria.
     * @param cmd the command containing the search criteria including port forwarding service id or rule id.
     * @return list of rules for the port forwarding service id specified in the search criteria
     */
    List<NetworkRuleConfigVO> searchForNetworkRules(ListPortForwardingServiceRulesCmd c) throws InvalidParameterValueException, PermissionDeniedException;

    /**
     * Obtains a list of events by the specified search criteria.
     * Can search by: "username", "type", "level", "startDate", "endDate"
     * @param c
     * @return List of Events.
     */
    List<EventVO> searchForEvents(ListEventsCmd c) throws PermissionDeniedException, InvalidParameterValueException;
    
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
     * @param cmd
     * @return List of DomainRouters.
     */
    List<DomainRouterVO> searchForRouters(ListRoutersCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
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
     * @param cmd
     * @return List of Volumes.
     */
    List<VolumeVO> searchForVolumes(ListVolumesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
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
     * @param cmd the command that wraps the search criteria
     * @return List of IPAddresses
     */
    List<IPAddressVO> searchForIPAddresses(ListPublicIpAddressesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
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
    List<GuestOSVO> listGuestOSByCriteria(ListGuestOsCmd cmd);
    
    /**
     * Obtains a list of all guest OS categories.
     * @return list of GuestOSCategories
     */
    List<GuestOSCategoryVO> listGuestOSCategoriesByCriteria(ListGuestOsCategoriesCmd cmd);
        
    /**
     * Logs out a user
     * @param userId
     */
    void logoutUser(Long userId);

	/**
	 * Creates a network rule as part of a port forwarding service.  If this port forwarding service has been applied to any virtual
	 * machines, the network rules will get sent to the router.
     * @param cmd the command describing the port forwarding service the rule belongs to, the public port, the private port, and the protocol
     * @return a new network rule that is part of the port forwarding service if successful, null otherwise
     * @throws InvalidParameterValueException
     * @throws PermissionDeniedException
     * @throws NetworkRuleConflictException
     * @throws InternalErrorException
     */
    NetworkRuleConfigVO createPortForwardingServiceRule(CreatePortForwardingServiceRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, NetworkRuleConflictException, InternalErrorException;

    /**
     * Apply a port forwarding service rule to all VMs that have the port forwarding service applied
     * @param ruleId the id of the created rule to apply
     * @return the updated rule if successful, null otherwise
     */
    NetworkRuleConfigVO applyPortForwardingServiceRule(Long ruleId) throws NetworkRuleConflictException;

	ConsoleProxyInfo getConsoleProxy(long dataCenterId, long userVmId);
	ConsoleProxyVO startConsoleProxy(long instanceId, long startEventId) throws InternalErrorException;
	long startConsoleProxyAsync(long instanceId);
	boolean stopConsoleProxy(long instanceId, long startEventId);
	long stopConsoleProxyAsync(long instanceId);
	boolean rebootConsoleProxy(long instanceId, long startEventId);
	long rebootConsoleProxyAsync(long instanceId);
	String getConsoleAccessUrlRoot(long vmId);
	ConsoleProxyVO findConsoleProxyById(long instanceId);
	VMInstanceVO findSystemVMById(long instanceId);
	boolean stopSystemVM(StopSystemVmCmd cmd);
	VMInstanceVO startSystemVM(StartSystemVMCmd cmd) throws InternalErrorException;
	boolean rebootSystemVM(RebootSystemVmCmd cmd);

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
	 * in a command object.
	 * @return list of domains owned by the given user
	 */
	List<DomainVO> searchForDomains(ListDomainsCmd c) throws PermissionDeniedException;
	
	List<DomainVO> searchForDomainChildren(ListDomainChildrenCmd cmd) throws PermissionDeniedException;

	/**
	 * create a new domain
	 * @param command - the create command defining the name to use and the id of the parent domain under which to create the new domain.
	 */
    DomainVO createDomain(CreateDomainCmd command) throws InvalidParameterValueException, PermissionDeniedException;

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
    void updateDomain(UpdateDomainCmd cmd) throws InvalidParameterValueException;

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
     * @param cmd
     * @return List of Accounts
     */
    List<AccountVO> searchForAccounts(ListAccountsCmd cmd);
    
    
    /**
     * Find the owning account of an IP Address
     * @param ipAddress
     * @return owning account if ip address is allocated, null otherwise
     */
    Account findAccountByIpAddress(String ipAddress);

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
    List<AlertVO> searchForAlerts(ListAlertsCmd cmd);

    /**
     * list all the capacity rows in capacity operations table
     * @param cmd
     * @return List of capacities
     */
    List<CapacityVO> listCapacities(ListCapacityCmd cmd);

    public long getMemoryUsagebyHost(Long hostId);

    /**
     * @param userId    The Id of the user who invoked this operation.
     * @param volumeId  The volume for which this snapshot is being taken
     * @return          The properties of the snapshot taken
     */
    SnapshotVO createTemplateSnapshot(Long userId, long volumeId);
    
    /**
     * Destroy a snapshot
     * @param snapshotId the id of the snapshot to destroy
     * @return true if snapshot successfully destroyed, false otherwise
     */
    boolean destroyTemplateSnapshot(Long userId, long snapshotId);

    long createVolumeFromSnapshotAsync(long userId, long accountId, long snapshotId, String volumeName) throws InternalErrorException, ResourceAllocationException;
    
    /**
     * List all snapshots of a disk volume. Optionaly lists snapshots created by specified interval
     * @param cmd the command containing the search criteria (order by, limit, etc.)
     * @return list of snapshots
     * @throws InvalidParameterValueException
     */
    List<SnapshotVO> listSnapshots(ListSnapshotsCmd cmd) throws InvalidParameterValueException;

    /**
     * find a single snapshot by id
     * @param snapshotId
     * @return the snapshot if found, null otherwise
     */
    Snapshot findSnapshotById(long snapshotId);

    /**
     * Finds a diskOffering by the specified ID.
     * @param diskOfferingId
     * @return A DiskOffering
     */
    DiskOfferingVO findDiskOfferingById(long diskOffering);

    /**
     * Finds the obj associated with the private disk offering 
     * @return -- vo obj for private disk offering
     */
    List<DiskOfferingVO> findPrivateDiskOffering();

    /**
     * List the permissions on a template.  This will return a list of account names that have been granted permission to launch instances from the template.
     * @param cmd the command wrapping the search criteria (template id)
     * @return list of account names that have been granted permission to launch instances from the template
     */
    List<String> listTemplatePermissions(ListTemplateOrIsoPermissionsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

    /**
     * List private templates for which the given account/domain has been granted permission to launch instances
     * @param accountId
     * @return
     */
    List<VMTemplateVO> listPermittedTemplates(long accountId);

    /**
     * List ISOs that match the specified criteria. 
     * @param cmd The command that wraps the (optional) templateId, name, keyword, templateFilter, bootable, account, and zoneId parameters.
     * @return list of ISOs
     */
    List<VMTemplateVO> listIsos(ListIsosCmd cmd) throws IllegalArgumentException, InvalidParameterValueException;

    /**
     * List templates that match the specified criteria. 
     * @param cmd The command that wraps the (optional) templateId, name, keyword, templateFilter, bootable, account, and zoneId parameters.
     * @return list of ISOs
     */
    List<VMTemplateVO> listTemplates(ListTemplatesCmd cmd) throws IllegalArgumentException, InvalidParameterValueException;

    /**
     * Search for disk offerings based on search criteria
     * @param cmd the command containing the criteria to use for searching for disk offerings
     * @return a list of disk offerings that match the given criteria
     */
    List<DiskOfferingVO> searchForDiskOfferings(ListDiskOfferingsCmd cmd);

    /**
     * 
     * @param jobId async-call job id
     * @return async-call result object
     */
    AsyncJobResult queryAsyncJobResult(long jobId) throws PermissionDeniedException;

    /**
     * Queries for the status or final result of an async job.
     * @param cmd the command that specifies the job id
     * @return an async-call result object
     * @throws PermissionDeniedException
     */
    AsyncJobResult queryAsyncJobResult(QueryAsyncJobResultCmd cmd) throws PermissionDeniedException;

    AsyncJobVO findInstancePendingAsyncJob(String instanceType, long instanceId);
    AsyncJobVO findAsyncJobById(long jobId);

    /**
     * Search for async jobs by account and/or startDate
     * @param cmd the command specifying the account and start date parameters
     * @return the list of async jobs that match the criteria
     */
    List<AsyncJobVO> searchForAsyncJobs(ListAsyncJobsCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

    /**
     * Assign a security group to a VM
     * @param cmd the command specifying secuirtyGroupId, securityGroupIdList, publicIp, vmId parameters
     */
    void assignSecurityGroup(AssignPortForwardingServiceCmd cmd) throws PermissionDeniedException, NetworkRuleConflictException, InvalidParameterValueException, InternalErrorException;

    /**
     * remove a security group from a publicIp/vmId combination where it had been previously applied
     * @param userId id of the user performing the action (for events)
     * @param securityGroupId the id of the security group to remove
     * @param publicIp
     * @param vmId
     */
    void removeSecurityGroup(long userId, long securityGroupId, String publicIp, long vmId, long startEventId) throws InvalidParameterValueException, PermissionDeniedException;
    void removeSecurityGroup(RemovePortForwardingServiceCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;
    
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
     * @param cmd
     * @return a list of security groups
     */
    List<SecurityGroupVO> searchForSecurityGroups(ListPortForwardingServicesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

    /**
     * returns a list of security groups from a given ip and vm id
     * @param c
     * @return a list of security groups
     */
    Map<String, List<SecurityGroupVO>> searchForSecurityGroupsByVM(ListPortForwardingServicesByVmCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

    /**
     * Create a security group, a group of network rules (port, private port, protocol, algorithm) that can be applied in mass to a VM
     * @param cmd the command specifying name, description, domainId, account parameters
     * @return
     */
    SecurityGroupVO createPortForwardingService(CreatePortForwardingServiceCmd cmd) throws InvalidParameterValueException;

    /**
     * Delete a security group.  If the group is being actively used, it cannot be deleted.
     * @param cmd - the command containing securityGroupId
     * @return true if the security group is deleted, exception is thrown otherwise
     */
    boolean deleteSecurityGroup(DeletePortForwardingServiceCmd cmd)  throws InvalidParameterValueException, PermissionDeniedException;

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

    LoadBalancerVO findLoadBalancer(Long accountId, String name);
    LoadBalancerVO findLoadBalancerById(long loadBalancerId);

    /**
     * List instances that have either been applied to a load balancer or are eligible to be assigned to a load balancer.
     * @param cmd
     * @return list of vm instances that have been or can be applied to a load balancer
     */
    List<UserVmVO> listLoadBalancerInstances(ListLoadBalancerRuleInstancesCmd cmd) throws PermissionDeniedException;

    /**
     * List load balancer rules based on the given criteria
     * @param cmd the command that specifies the criteria to use for listing load balancers.  Load balancers can be listed
     *            by id, name, public ip, and vm instance id
     * @return list of load balancers that match the criteria
     */
    List<LoadBalancerVO> searchForLoadBalancers(ListLoadBalancerRulesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException;

    String[] getApiConfig();
    StoragePoolVO findPoolById(Long id);
	List<? extends StoragePoolVO> searchForStoragePools(Criteria c);

	/**
	 * List storage pools that match the given criteria
	 * @param cmd the command that wraps the search criteria (zone, pod, name, IP address, path, and cluster id)
	 * @return a list of storage pools that match the given criteria
	 */
	List<? extends StoragePoolVO> searchForStoragePools(ListStoragePoolsCmd cmd);

	SnapshotPolicyVO findSnapshotPolicyById(Long policyId);

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
	 * List system VMs by the given search criteria
	 * @param cmd the command that wraps the search criteria (host, name, state, type, zone, pod, and/or id)
	 * @return the list of system vms that match the given criteria
	 */
    List<? extends VMInstanceVO> searchForSystemVm(ListSystemVMsCmd cmd);

    /**
	 * Returns back a SHA1 signed response
	 * @param userId -- id for the user
	 * @return -- ArrayList of <CloudId+Signature>
	 */
    ArrayList<String> getCloudIdentifierResponse(GetCloudIdentifierCmd cmd) throws InvalidParameterValueException;

    NetworkGroupVO findNetworkGroupByName(Long accountId, String groupName);

    /**
     * Find a network group by id
     * @param networkGroupId id of group to lookup
     * @return the network group if found, null otherwise
     */
    NetworkGroupVO findNetworkGroupById(long networkGroupId);


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
	
	public List<PreallocatedLunVO> getPreAllocatedLuns(ListPreallocatedLunsCmd cmd);
	
	public String getNetworkGroupsNamesForVm(long vmId);
	
	boolean checkLocalStorageConfigVal(); 

	boolean validateCustomVolumeSizeRange(long size) throws InvalidParameterValueException;
	boolean updateUser(UpdateUserCmd cmd) throws InvalidParameterValueException;
	boolean updateTemplatePermissions(UpdateTemplateOrIsoPermissionsCmd cmd)throws InvalidParameterValueException, PermissionDeniedException,InternalErrorException;
	String[] createApiKeyAndSecretKey(RegisterCmd cmd);
}
