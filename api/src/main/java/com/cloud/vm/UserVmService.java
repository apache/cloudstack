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
package com.cloud.vm;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.BaseCmd.HTTPMethod;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.admin.vm.RecoverVMCmd;
import org.apache.cloudstack.api.command.user.vm.AddNicToVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DestroyVMCmd;
import org.apache.cloudstack.api.command.user.vm.RebootVMCmd;
import org.apache.cloudstack.api.command.user.vm.RemoveNicFromVMCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMPasswordCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMSSHKeyCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMUserDataCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;
import org.apache.cloudstack.api.command.user.vm.StartVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateDefaultNicForVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVmNicIpCmd;
import org.apache.cloudstack.api.command.user.vm.UpgradeVMCmd;
import org.apache.cloudstack.api.command.user.vmgroup.CreateVMGroupCmd;
import org.apache.cloudstack.api.command.user.vmgroup.DeleteVMGroupCmd;

import com.cloud.dc.DataCenter;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network.IpAddresses;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.StoragePool;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.ExecutionException;

public interface UserVmService {


    /**
     * Destroys one virtual machine
     *
     * @param userId
     *            the id of the user performing the action
     * @param vmId
     *            the id of the virtual machine.
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    UserVm destroyVm(DestroyVMCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException;

    /**
     * Destroys one virtual machine
     *
     * @param vmId
     *            the id of the virtual machine.
     * @param expunge
     *            indicates if vm should be expunged
     * @throws ConcurrentOperationException
     * @throws ResourceUnavailableException
     */
    UserVm destroyVm(long vmId, boolean expunge) throws ResourceUnavailableException, ConcurrentOperationException;

    /**
     * Resets the password of a virtual machine.
     *
     * @param cmd
     *            - the command specifying vmId, password
     * @return the VM if reset worked successfully, null otherwise
     */
    UserVm resetVMPassword(ResetVMPasswordCmd cmd, String password) throws ResourceUnavailableException, InsufficientCapacityException;

    /**
     * Resets the SSH Key of a virtual machine.
     *
     * @param cmd
     *            - the command specifying vmId, Keypair name
     * @return the VM if reset worked successfully, null otherwise
     */
    UserVm resetVMSSHKey(ResetVMSSHKeyCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException;

    UserVm resetVMUserData(ResetVMUserDataCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException;

    UserVm startVirtualMachine(StartVMCmd cmd) throws StorageUnavailableException, ExecutionException, ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException, ResourceAllocationException;

    UserVm rebootVirtualMachine(RebootVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException;

    UserVm updateVirtualMachine(UpdateVMCmd cmd) throws ResourceUnavailableException, InsufficientCapacityException;

    /**
     * Adds a NIC on the given network to the virtual machine
     * @param cmd the command object that defines the vm and the given network
     * @return the vm object if successful, null otherwise
     */
    UserVm addNicToVirtualMachine(AddNicToVMCmd cmd);

    /**
     * Removes a NIC on the given network from the virtual machine
     * @param cmd the command object that defines the vm and the given network
     * @return the vm object if successful, null otherwise
     */
    UserVm removeNicFromVirtualMachine(RemoveNicFromVMCmd cmd);

    /**
     * Updates default Nic to the given network for given virtual machine
     * @param cmd the command object that defines the vm and the given network
     * @return the vm object if successful, null otherwise
     */
    UserVm updateDefaultNicForVirtualMachine(UpdateDefaultNicForVMCmd cmd);

    /**
     * Updated the ip address on the given NIC to the virtual machine
     * @param cmd the command object that defines the ip address and the given nic
     * @return the vm object if successful, null otherwise
     */
    UserVm updateNicIpForVirtualMachine(UpdateVmNicIpCmd cmd);

    UserVm recoverVirtualMachine(RecoverVMCmd cmd) throws ResourceAllocationException;

    /**
     * Creates a Basic Zone User VM in the database and returns the VM to the
     * caller.
     *
     *
     *
     * @param sshKeyPair
     *            - name of the ssh key pair used to login to the virtual
     *            machine
     * @param cpuSpeed
     * @param memory
     * @param cpuNumber
     * @param zone
     *            - availability zone for the virtual machine
     * @param serviceOffering
     *            - the service offering for the virtual machine
     * @param template
     *            - the template for the virtual machine
     * @param securityGroupIdList
     *            - comma separated list of security groups id that going to be
     *            applied to the virtual machine
     * @param hostName
     *            - host name for the virtual machine
     * @param displayName
     *            - an optional user generated name for the virtual machine
     * @param diskOfferingId
     *            - the ID of the disk offering for the virtual machine. If the
     *            template is of ISO format, the diskOfferingId is for the root
     *            disk volume. Otherwise this parameter is used to indicate the
     *            offering for the data disk volume. If the templateId parameter
     *            passed is from a Template object, the diskOfferingId refers to
     *            a DATA Disk Volume created. If the templateId parameter passed
     *            is from an ISO object, the diskOfferingId refers to a ROOT
     *            Disk Volume created
     * @param diskSize
     *            - the arbitrary size for the DATADISK volume. Mutually
     *            exclusive with diskOfferingId
     * @param group
     *            - an optional group for the virtual machine
     * @param hypervisor
     *            - the hypervisor on which to deploy the virtual machine
     * @param userData
     *            - an optional binary data that can be sent to the virtual
     *            machine upon a successful deployment. This binary data must be
     *            base64 encoded before adding it to the request. Currently only
     *            HTTP GET is supported. Using HTTP GET (via querystring), you
     *            can send up to 2KB of data after base64 encoding
     * @param userDataId
     * @param userDataDetails
     * @param requestedIps
     *            TODO
     * @param defaultIp
     *            TODO
     * @param displayVm
     *            - Boolean flag whether to the display the vm to the end user or not
     * @param affinityGroupIdList
     * @param customId
     * @param dhcpOptionMap
     *           - Maps the dhcp option code and the dhcp value to the network uuid
     * @param dataDiskTemplateToDiskOfferingMap
     *            - Datadisk template to Disk offering Map
     *             an optional parameter that creates additional data disks for the virtual machine
     *             For each of the templates in the map, a data disk will be created from the corresponding
     *             disk offering obtained from the map
     *
     * @return UserVm object if successful.
     * @throws InsufficientCapacityException
     *             if there is insufficient capacity to deploy the VM.
     * @throws ConcurrentOperationException
     *             if there are multiple users working on the same VM or in the
     *             same environment.
     * @throws ResourceUnavailableException
     *             if the resources required to deploy the VM is not currently
     *             available.
     */
    UserVm createBasicSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> securityGroupIdList,
                                                  Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, HTTPMethod httpmethod,
                                                  String userData, Long userDataId, String userDataDetails, List<String> sshKeyPairs, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIp, Boolean displayVm, String keyboard,
                                                  List<Long> affinityGroupIdList, Map<String, String> customParameter, String customId, Map<String, Map<Integer, String>> dhcpOptionMap,
                                                  Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap,
                                                  Map<String, String> userVmOVFProperties, boolean dynamicScalingEnabled, Long overrideDiskOfferingId) throws InsufficientCapacityException,
        ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException;

    /**
     * Creates a User VM in Advanced Zone (Security Group feature is enabled) in
     * the database and returns the VM to the caller.
     *
     *
     *
     * @param type
     * @param zone
     *            - availability zone for the virtual machine
     * @param serviceOffering
     *            - the service offering for the virtual machine
     * @param template
     *            - the template for the virtual machine
     * @param networkIdList
     *            - list of network ids used by virtual machine
     * @param securityGroupIdList
     *            - comma separated list of security groups id that going to be
     *            applied to the virtual machine
     * @param hostName
     *            - host name for the virtual machine
     * @param displayName
     *            - an optional user generated name for the virtual machine
     * @param diskOfferingId
     *            - the ID of the disk offering for the virtual machine. If the
     *            template is of ISO format, the diskOfferingId is for the root
     *            disk volume. Otherwise this parameter is used to indicate the
     *            offering for the data disk volume. If the templateId parameter
     *            passed is from a Template object, the diskOfferingId refers to
     *            a DATA Disk Volume created. If the templateId parameter passed
     *            is from an ISO object, the diskOfferingId refers to a ROOT
     *            Disk Volume created
     * @param diskSize
     *            - the arbitrary size for the DATADISK volume. Mutually
     *            exclusive with diskOfferingId
     * @param group
     *            - an optional group for the virtual machine
     * @param hypervisor
     *            - the hypervisor on which to deploy the virtual machine
     * @param userData
     *            - an optional binary data that can be sent to the virtual
     *            machine upon a successful deployment. This binary data must be
     *            base64 encoded before adding it to the request. Currently only
     *            HTTP GET is supported. Using HTTP GET (via querystring), you
     *            can send up to 2KB of data after base64 encoding
     * @param userDataId
     * @param userDataDetails
     * @param requestedIps
     *            TODO
     * @param defaultIps
     *            TODO
     * @param displayVm
     *            - Boolean flag whether to the display the vm to the end user or not
     * @param affinityGroupIdList
     * @param customId
     * @param dhcpOptionMap
     *             - Maps the dhcp option code and the dhcp value to the network uuid
     * @param dataDiskTemplateToDiskOfferingMap
     *            - Datadisk template to Disk offering Map
     *             an optional parameter that creates additional data disks for the virtual machine
     *             For each of the templates in the map, a data disk will be created from the corresponding
     *             disk offering obtained from the map
     * @return UserVm object if successful.
     *
     * @throws InsufficientCapacityException
     *             if there is insufficient capacity to deploy the VM.
     * @throws ConcurrentOperationException
     *             if there are multiple users working on the same VM or in the
     *             same environment.
     * @throws ResourceUnavailableException
     *             if the resources required to deploy the VM is not currently
     *             available.
     */
    UserVm createAdvancedSecurityGroupVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList,
                                                     List<Long> securityGroupIdList, Account owner, String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor,
                                                     HTTPMethod httpmethod, String userData, Long userDataId, String userDataDetails, List<String> sshKeyPairs, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean displayVm, String keyboard,
                                                     List<Long> affinityGroupIdList, Map<String, String> customParameters, String customId, Map<String, Map<Integer, String>> dhcpOptionMap,
                                                     Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap, Map<String, String> userVmOVFProperties, boolean dynamicScalingEnabled, Long overrideDiskOfferingId, String vmType) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException;

    /**
     * Creates a User VM in Advanced Zone (Security Group feature is disabled)
     * in the database and returns the VM to the caller.
     *
     *
     *
     * @param sshKeyPair
     *            - name of the ssh key pair used to login to the virtual
     *            machine
     * @param cpuSpeed
     * @param memory
     * @param cpuNumber
     * @param zone
     *            - availability zone for the virtual machine
     * @param serviceOffering
     *            - the service offering for the virtual machine
     * @param template
     *            - the template for the virtual machine
     * @param networkIdList
     *            - list of network ids used by virtual machine
     * @param hostName
     *            - host name for the virtual machine
     * @param displayName
     *            - an optional user generated name for the virtual machine
     * @param diskOfferingId
     *            - the ID of the disk offering for the virtual machine. If the
     *            template is of ISO format, the diskOfferingId is for the root
     *            disk volume. Otherwise this parameter is used to indicate the
     *            offering for the data disk volume. If the templateId parameter
     *            passed is from a Template object, the diskOfferingId refers to
     *            a DATA Disk Volume created. If the templateId parameter passed
     *            is from an ISO object, the diskOfferingId refers to a ROOT
     *            Disk Volume created
     * @param diskSize
     *            - the arbitrary size for the DATADISK volume. Mutually
     *            exclusive with diskOfferingId
     * @param group
     *            - an optional group for the virtual machine
     * @param hypervisor
     *            - the hypervisor on which to deploy the virtual machine
     * @param userData
     *            - an optional binary data that can be sent to the virtual
     *            machine upon a successful deployment. This binary data must be
     *            base64 encoded before adding it to the request. Currently only
     *            HTTP GET is supported. Using HTTP GET (via querystring), you
     *            can send up to 2KB of data after base64 encoding
     * @param userDataId
     * @param userDataDetails
     * @param requestedIps
     *            TODO
     * @param defaultIps
     *            TODO
     * @param displayVm
     *            - Boolean flag whether to the display the vm to the end user or not
     * @param affinityGroupIdList
     * @param customId
     * @param dhcpOptionMap
     *             - Map that maps the DhcpOption code and their value on the Network uuid
     * @param dataDiskTemplateToDiskOfferingMap
     *            - Datadisk template to Disk offering Map
     *             an optional parameter that creates additional data disks for the virtual machine
     *             For each of the templates in the map, a data disk will be created from the corresponding
     *             disk offering obtained from the map
     * @return UserVm object if successful.
     *
     * @throws InsufficientCapacityException
     *             if there is insufficient capacity to deploy the VM.
     * @throws ConcurrentOperationException
     *             if there are multiple users working on the same VM or in the
     *             same environment.
     * @throws ResourceUnavailableException
     *             if the resources required to deploy the VM is not currently
     *             available.
     */
    UserVm createAdvancedVirtualMachine(DataCenter zone, ServiceOffering serviceOffering, VirtualMachineTemplate template, List<Long> networkIdList, Account owner,
                                        String hostName, String displayName, Long diskOfferingId, Long diskSize, String group, HypervisorType hypervisor, HTTPMethod httpmethod, String userData,
                                        Long userDataId, String userDataDetails, List<String> sshKeyPairs, Map<Long, IpAddresses> requestedIps, IpAddresses defaultIps, Boolean displayVm, String keyboard, List<Long> affinityGroupIdList,
                                        Map<String, String> customParameters, String customId, Map<String, Map<Integer, String>> dhcpOptionMap, Map<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap,
                                        Map<String, String> templateOvfPropertiesMap, boolean dynamicScalingEnabled, String vmType, Long overrideDiskOfferingId)

        throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, StorageUnavailableException, ResourceAllocationException;

    /**
     * Starts the virtual machine created from createVirtualMachine.
     *
     * @param cmd
     *            Command to deploy.
     * @return UserVm object if successful.
     * @throws InsufficientCapacityException
     *             if there is insufficient capacity to deploy the VM.
     * @throws ConcurrentOperationException
     *             if there are multiple users working on the same VM.
     * @throws ResourceUnavailableException
     *             if the resources required the deploy the VM is not currently available.
     */
    UserVm startVirtualMachine(DeployVMCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, ResourceAllocationException;

    /**
     * Creates a vm group.
     *
     * @param cmd The command specifying domain ID, account name, group name, and project ID
     */
    InstanceGroup createVmGroup(CreateVMGroupCmd cmd);

    boolean deleteVmGroup(DeleteVMGroupCmd cmd);

    /**
     * upgrade the service offering of the virtual machine
     *
     * @param cmd
     *            - the command specifying vmId and new serviceOfferingId
     * @return the vm
     * @throws ResourceAllocationException
     */
    UserVm upgradeVirtualMachine(UpgradeVMCmd cmd) throws ResourceAllocationException;

    UserVm stopVirtualMachine(long vmId, boolean forced) throws ConcurrentOperationException;

    void deletePrivateTemplateRecord(Long templateId);

    HypervisorType getHypervisorTypeOfUserVM(long vmid);

    UserVm createVirtualMachine(DeployVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException,
        StorageUnavailableException, ResourceAllocationException;

    UserVm getUserVm(long vmId);

    VirtualMachine getVm(long vmId);

    /**
     * Migrate the given VM to the destination host provided. The API returns the migrated VM if migration succeeds.
     * Only Root Admin can migrate a VM.
     *
     * @param vmId The ID of the VM to be migrated
     * @param destinationHost The destination host to where the VM will be migrated
     *
     * @return VirtualMachine migrated VM
     * @throws ManagementServerException
     *             in case we get error finding the VM or host or access errors or other internal errors.
     * @throws ConcurrentOperationException
     *             if there are multiple users working on the same VM.
     * @throws ResourceUnavailableException
     *             if the destination host to migrate the VM is not currently available.
     * @throws VirtualMachineMigrationException
     *             if the VM to be migrated is not in Running state
     */
    VirtualMachine migrateVirtualMachine(Long vmId, Host destinationHost) throws ResourceUnavailableException, ConcurrentOperationException, ManagementServerException,
        VirtualMachineMigrationException;

    /**
     * Migrate the given VM with its volumes to the destination host. The API returns the migrated VM if it succeeds.
     * Only root admin can migrate a VM.
     *
     * @param vmId The ID of the VM to be migrated
     * @param destinationHost The destination host to where the VM will be migrated
     * @param volumeToPool A map of volume to which pool it should be migrated
     *
     * @return VirtualMachine migrated VM
     * @throws ManagementServerException
     *             in case we get error finding the VM or host or access errors or other internal errors.
     * @throws ConcurrentOperationException
     *             if there are multiple users working on the same VM.
     * @throws ResourceUnavailableException
     *             if the destination host to migrate the VM is not currently available.
     * @throws VirtualMachineMigrationException
     *             if the VM to be migrated is not in Running state
     */
    VirtualMachine migrateVirtualMachineWithVolume(Long vmId, Host destinationHost, Map<String, String> volumeToPool) throws ResourceUnavailableException,
        ConcurrentOperationException, ManagementServerException, VirtualMachineMigrationException;

    UserVm moveVMToUser(AssignVMCmd moveUserVMCmd) throws ResourceAllocationException, ConcurrentOperationException, ResourceUnavailableException,
        InsufficientCapacityException;

    VirtualMachine vmStorageMigration(Long vmId, StoragePool destPool);

    VirtualMachine vmStorageMigration(Long vmId, Map<String, String> volumeToPool);

    UserVm restoreVM(RestoreVMCmd cmd) throws InsufficientCapacityException, ResourceUnavailableException;

    UserVm restoreVirtualMachine(Account caller, long vmId, Long newTemplateId) throws InsufficientCapacityException, ResourceUnavailableException;

    UserVm upgradeVirtualMachine(ScaleVMCmd cmd) throws ResourceUnavailableException, ConcurrentOperationException, ManagementServerException,
        VirtualMachineMigrationException;

    UserVm expungeVm(long vmId) throws ResourceUnavailableException, ConcurrentOperationException;

    /**
     * Finds and returns an encrypted password for a VM.
     *
     * @param  vmId
     * @return Base64 encoded userdata
     */
    String getVmUserData(long vmId);

    /**
     * determine whether the uservm should be visible to the end user
     * @return  value of the display flag
     */
    public boolean isDisplayResourceEnabled(Long vmId);

    void collectVmDiskStatistics(UserVm userVm);

    void collectVmNetworkStatistics (UserVm userVm);

    UserVm importVM(final DataCenter zone, final Host host, final VirtualMachineTemplate template, final String instanceName, final String displayName, final Account owner, final String userData, final Account caller, final Boolean isDisplayVm, final String keyboard,
                    final long accountId, final long userId, final ServiceOffering serviceOffering, final String sshPublicKey,
                    final String hostName, final HypervisorType hypervisorType, final Map<String, String> customParameters, final VirtualMachine.PowerState powerState) throws InsufficientCapacityException;

    /**
     * Unmanage a guest VM from CloudStack
     * @return true if the VM is successfully unmanaged, false if not.
     */
    boolean unmanageUserVM(Long vmId);
}
