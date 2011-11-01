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
package com.cloud.server;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.cloud.api.commands.ListGuestOsCmd;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.configuration.ResourceLimitVO;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.domain.DomainVO;
import com.cloud.event.EventVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.info.ConsoleProxyInfo;
import com.cloud.network.IPAddressVO;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeStats;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.utils.Pair;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;

/**
 * ManagementServer is the interface to talk to the Managment Server. This will be the line drawn between the UI and MS. If we
 * need to build a wire protocol, it will be built on top of this java interface.
 */
public interface ManagementServer extends ManagementService {

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
     * 
     * @param username
     *            required username for authentication
     * @param password
     *            password to use for authentication, can be null for single sign-on case
     * @param domainId
     *            id of domain where user with username resides
     * @param requestParameters
     *            the request parameters of the login request, which should contain timestamp of when the request signature is
     *            made, and the signature itself in the single sign-on case
     * @return a user object, null if the user failed to authenticate
     */
    UserAccount authenticateUser(String username, String password, Long domainId, Map<String, Object[]> requestParameters);

    String updateAdminPassword(long userId, String oldPassword, String newPassword);

    /**
     * Locate a user by their apiKey
     * 
     * @param apiKey
     *            that was created for a particular user
     * @return the user/account pair if one exact match was found, null otherwise
     */
    Pair<User, Account> findUserByApiKey(String apiKey);

    /**
     * Get an account by the accountId
     * 
     * @param accountId
     * @return the account, or null if not found
     */
    Account getAccount(long accountId);

    /**
     * Gets Volume statistics. The array returned will contain VolumeStats in the same order as the array of volumes requested.
     * 
     * @param volId
     * @return array of VolumeStats
     */
    VolumeStats[] getVolumeStatistics(long[] volId);

    /**
     * If the specified VLAN is associated with the pod, returns the pod ID. Else, returns null.
     * 
     * @param vlanDbId
     * @return pod ID, or null
     */
    Long getPodIdForVlan(long vlanDbId);

    /**
     * Return a list of IP addresses
     * 
     * @param accountId
     * @param allocatedOnly
     *            - if true, will only list IPs that are allocated to the specified account
     * @param zoneId
     *            - if specified, will list IPs in this zone
     * @param vlanDbId
     *            - if specified, will list IPs in this VLAN
     * @return list of IP addresses
     */
    List<IPAddressVO> listPublicIpAddressesBy(Long accountId, boolean allocatedOnly, Long zoneId, Long vlanDbId);

    /**
     * Return a list of private IP addresses that have been allocated to the given pod and zone
     * 
     * @param podId
     * @param zoneId
     * @return list of private IP addresses
     */
    List<DataCenterIpAddressVO> listPrivateIpAddressesBy(Long podId, Long zoneId);

    /**
     * Attaches an ISO to the virtual CDROM device of the specified VM. Will fail if the VM already has an ISO mounted.
     * 
     * @param vmId
     * @param userId
     * @param isoId
     * @param attach
     *            whether to attach or detach the iso from the instance
     * @return
     */
    boolean attachISOToVM(long vmId, long userId, long isoId, boolean attach);

    /**
     * Retrieves a host by id
     * 
     * @param hostId
     * @return Host
     */
    HostVO getHostBy(long hostId);

    /**
     * Retrieves all Events between the start and end date specified
     * 
     * @param userId
     *            unique id of the user, pass in -1 to retrieve events for all users
     * @param accountId
     *            unique id of the account (which could be shared by many users), pass in -1 to retrieve events for all accounts
     * @param domainId
     *            the id of the domain in which to search for users (useful when -1 is passed in for userId)
     * @param type
     *            the type of the event.
     * @param level
     *            INFO, WARN, or ERROR
     * @param startDate
     *            inclusive.
     * @param endDate
     *            inclusive. If date specified is greater than the current time, the system will use the current time.
     * @return List of events
     */
    List<EventVO> getEvents(long userId, long accountId, Long domainId, String type, String level, Date startDate, Date endDate);

    /**
     * returns the instance id of this management server.
     * 
     * @return id of the management server
     */
    long getId();

    /**
     * Searches for Zones by the specified search criteria Can search by: zone name
     * 
     * @param c
     * @return List of Zones
     */
    List<DataCenterVO> searchForZones(Criteria c);

    /**
     * Searches for servers that are either Down or in Alert state
     * 
     * @param c
     * @return List of Hosts
     */
    List<HostVO> searchForAlertServers(Criteria c);

    /**
     * Search for templates by the specified search criteria Can search by: "name", "ready", "isPublic"
     * 
     * @param c
     * @return List of VMTemplates
     */
    List<VMTemplateVO> searchForTemplates(Criteria c);

    /**
     * Obtains pods that match the data center ID
     * 
     * @param dataCenterId
     * @return List of Pods
     */
    List<HostPodVO> listPods(long dataCenterId);

    /**
     * Change a pod's private IP range
     * 
     * @param op
     * @param podId
     * @param startIP
     * @param endIP
     * @return Message to display to user
     */
    String changePrivateIPRange(boolean add, Long podId, String startIP, String endIP);

    /**
     * Finds a user by their user ID.
     * 
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
     * Obtains a list of virtual machines that are similar to the VM with the specified name.
     * 
     * @param vmInstanceName
     * @return List of VMInstances
     */
    List<VMInstanceVO> findVMInstancesLike(String vmInstanceName);

    /**
     * Finds a virtual machine instance with the specified Volume ID.
     * 
     * @param volumeId
     * @return VMInstance
     */
    VMInstanceVO findVMInstanceById(long vmId);

    /**
     * Finds a guest virtual machine instance with the specified ID.
     * 
     * @param userVmId
     * @return UserVmVO
     */
    UserVmVO findUserVMInstanceById(long userVmId);

    /**
     * Finds a service offering with the specified ID.
     * 
     * @param offeringId
     * @return ServiceOffering
     */
    ServiceOfferingVO findServiceOfferingById(long offeringId);

    /**
     * Obtains a list of all service offerings.
     * 
     * @return List of ServiceOfferings
     */
    List<ServiceOfferingVO> listAllServiceOfferings();

    /**
     * Obtains a list of all active hosts.
     * 
     * @return List of Hosts.
     */
    List<HostVO> listAllActiveHosts();

    /**
     * Finds a data center with the specified ID.
     * 
     * @param dataCenterId
     * @return DataCenter
     */
    DataCenterVO findDataCenterById(long dataCenterId);

    /**
     * Finds a template by the specified ID.
     * 
     * @param templateId
     * @return A VMTemplate
     */
    VMTemplateVO findTemplateById(long templateId);

    List<EventVO> listPendingEvents(int entryTime, int duration);

    /**
     * Obtains a list of routers by the specified host ID.
     * 
     * @param hostId
     * @return List of DomainRouters.
     */
    List<DomainRouterVO> listRoutersByHostId(long hostId);

    /**
     * Obtains a list of all active routers.
     * 
     * @return List of DomainRouters
     */
    List<DomainRouterVO> listAllActiveRouters();

    /**
     * Finds a pod by the specified ID.
     * 
     * @param podId
     * @return HostPod
     */
    HostPodVO findHostPodById(long podId);

    /**
     * Finds a secondary storage host in the specified zone
     * 
     * @param zoneId
     * @return Host
     */
    HostVO findSecondaryStorageHosT(long zoneId);

    /**
     * Obtains a list of billing records by the specified search criteria. Can search by: "userId", "startDate", "endDate"
     * 
     * @param c
     * @return List of Billings. List<UsageVO> searchForUsage(Criteria c);
     */

    /**
     * Obtains a list of all templates.
     * 
     * @return list of VMTemplates
     */
    List<VMTemplateVO> listAllTemplates();

    /**
     * Logs out a user
     * 
     * @param userId
     */
    void logoutUser(Long userId);

    ConsoleProxyInfo getConsoleProxy(long dataCenterId, long userVmId);

    ConsoleProxyVO startConsoleProxy(long instanceId);

    ConsoleProxyVO stopConsoleProxy(VMInstanceVO systemVm, boolean isForced) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException;

    ConsoleProxyVO rebootConsoleProxy(long instanceId);

    String getConsoleAccessUrlRoot(long vmId);

    ConsoleProxyVO findConsoleProxyById(long instanceId);

    VMInstanceVO findSystemVMById(long instanceId);

    VirtualMachine startSystemVm(long vmId);

    /**
     * Returns a configuration value with the specified name
     * 
     * @param name
     * @return configuration value
     */
    String getConfigurationValue(String name);

    /**
     * Returns the vnc port of the vm.
     * 
     * @param VirtualMachine
     *            vm
     * @return the vnc port if found; -1 if unable to find.
     */
    Pair<String, Integer> getVncPort(VirtualMachine vm);

    /**
     * find the domain Id associated with the given account
     * 
     * @param accountId
     *            the id of the account to use to look up the domain
     */
    Long findDomainIdByAccountId(Long accountId);

    /**
     * find the domain by its path
     * 
     * @param domainPath
     *            the path to use to lookup a domain
     * @return domainVO the domain with the matching path, or null if no domain with the given path exists
     */
    DomainVO findDomainByPath(String domainPath);

    /**
     * Finds accounts with account identifiers similar to the parameter
     * 
     * @param accountName
     * @return list of Accounts
     */
    List<AccountVO> findAccountsLike(String accountName);

    /**
     * Finds accounts with account identifier
     * 
     * @param accountName
     * @return an account that is active (not deleted)
     */
    Account findActiveAccountByName(String accountName);

    /**
     * Finds accounts with account identifier
     * 
     * @param accountName
     *            , domainId
     * @return an account that is active (not deleted)
     */

    Account findActiveAccount(String accountName, Long domainId);

    /**
     * Finds accounts with account identifier
     * 
     * @param accountName
     * @param domainId
     * @return an account that may or may not have been deleted
     */
    Account findAccountByName(String accountName, Long domainId);

    /**
     * Finds an account by the ID.
     * 
     * @param accountId
     * @return Account
     */
    Account findAccountById(Long accountId);

    /**
     * Deletes a Limit
     * 
     * @param limitId
     *            - the database ID of the Limit
     * @return true if successful, false if not
     */
    boolean deleteLimit(Long limitId);

    /**
     * Finds limit by id
     * 
     * @param limitId
     *            - the database ID of the Limit
     * @return LimitVO object
     */
    ResourceLimitVO findLimitById(long limitId);

    /**
     * Lists ISOs that are available for the specified account ID.
     * 
     * @param accountId
     * @param accountType
     * @return a list of ISOs (VMTemplateVO objects)
     */
    List<VMTemplateVO> listIsos(Criteria c);

    public long getMemoryUsagebyHost(Long hostId);

    /**
     * List private templates for which the given account/domain has been granted permission to launch instances
     * 
     * @param accountId
     * @return
     */
    List<VMTemplateVO> listPermittedTemplates(long accountId);

    /**
     * 
     * @param jobId
     *            async-call job id
     * @return async-call result object
     */
    AsyncJobResult queryAsyncJobResult(long jobId);

    AsyncJobVO findAsyncJobById(long jobId);

    String[] getApiConfig();

    StoragePoolVO findPoolById(Long id);

    List<? extends StoragePoolVO> searchForStoragePools(Criteria c);

    /**
     * Return whether a domain is a child domain of a given domain.
     * 
     * @param parentId
     * @param childId
     * @return True if the domainIds are equal, or if the second domain is a child of the first domain. False otherwise.
     */
    boolean isChildDomain(Long parentId, Long childId);

    SecurityGroupVO findNetworkGroupByName(Long accountId, String groupName);

    /**
     * Find a network group by id
     * 
     * @param networkGroupId
     *            id of group to lookup
     * @return the network group if found, null otherwise
     */
    SecurityGroupVO findNetworkGroupById(long networkGroupId);

    List<String> searchForStoragePoolDetails(long poolId, String value);

    boolean checkLocalStorageConfigVal();

    VolumeVO findVolumeByInstanceAndDeviceId(long instanceId, long deviceId);

    InstanceGroupVO getGroupForVm(long vmId);

    List<VlanVO> searchForZoneWideVlans(long dcId, String vlanType, String vlanId);

    /*
     * Fetches the version of cloud stack
     */
    @Override
    String getVersion();

    GuestOSVO getGuestOs(Long guestOsId);

    VolumeVO getRootVolume(Long instanceId);

    long getPsMaintenanceCount(long podId);

    boolean isPoolUp(long instanceId);

    boolean checkIfMaintenable(long hostId);

    String getHashKey();

    List<GuestOSHypervisorVO> listGuestOSByHypervisor(ListGuestOsCmd cmd);
}
