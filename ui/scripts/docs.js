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
cloudStack.docs = {

    //Dedicate Resource

    helpDedicateResource: {

        desc: 'Check this box to dedicate the resources to specific domain/account',
        externalLink: ''

    },

    helpAccountForDedication: {

        desc: 'Please enter an account name which belongs to the above selected domain in order to dedicate this resource to this account',
        externalLink: ''

    },


    //Delete/archive events
    helpEventsDeleteType: {

        desc: 'Delete all the events by specifying its TYPE (e.g., USER.LOGIN)',
        externalLink: ''

    },

    helpEventsDeleteDate: {

        desc: 'Delete all the events which have been created in date range (specify start and end date)',
        externalLink: ''
    },

    helpEventsArchiveType: {

        desc: 'Archive all the events by specifying its TYPE (i.e., USER.LOGIN)',
        externalLink: ''
    },

    helpEventsArchiveDate: {

        desc: 'Archive all the events which have been created in date range (specify start and end date)',
        externalLink: ''
    },

    //Delete/archive Alerts
    helpAlertsDeleteType: {

        desc: 'Delete all the alerts by specifying its TYPE (integer number)',
        externalLink: ''

    },

    helpAlertsDeleteDate: {

        desc: 'Delete all the alerts which have been created in date range (specify start and end date)',
        externalLink: ''
    },

    helpAlertsArchiveType: {

        desc: 'Archive all the alerts by specifying its TYPE (integer number)',
        externalLink: ''
    },

    helpAlertsArchiveDate: {

        desc: 'Archive all the alerts which have been created in date range (specify start and end date)',
        externalLink: ''
    },

    helpManaged: {

        desc: 'True if CloudStack manages the storage; else, false (check with storage provider if unknown)',
        externalLink: ''
    },

    helpCapacityBytes: {

        desc: 'Number of bytes for the primary storage to have',
        externalLink: ''
    },

    helpCapacityIops: {

        desc: 'Number of IOPS for the primary storage to have',
        externalLink: ''
    },

    helpUrl: {

        desc: 'Arbitrary data to be sent to the storage provider for configuration purposes',
        externalLink: ''
    },

    //Ldap
    helpLdapQueryFilter: {

        desc: 'Query filter is used to find a mapped user in the external LDAP server.Cloudstack provides some wildchars to represent the unique attributes in its database . Example - If Cloudstack account-name is same as the LDAP uid, then following will be a valid filter: Queryfilter :  (&(uid=%u) ,  Queryfilter: .incase of Active Directory , Email _ID :(&(mail=%e)) , displayName :(&(displayName=%u)',

        externalLink: ''
    },


    //IP Reservation tooltips
    helpIPReservationCidr: {
        desc: 'Edit CIDR when you want to configure IP Reservation in isolated guest Network',
        externalLink: ''

    },

    helpIPReservationNetworkCidr: {
        desc: 'The CIDR of the entire network when IP reservation is configured',
        externalLink: ' '

    },

    helpReservedIPRange: {
        desc: 'The IP Range which is not used by CloudStack to allocate to Guest VMs.Can be used for Non Cloudstack purposes.',
        externalLink: ''
    },

    // Add account
    helpAccountUsername: {
        desc: 'Any desired login ID. Must be unique in the current domain. The same username can exist in other domains, including sub-domains.',
        externalLink: ''
    },

    helpOverridePublicNetwork: {
        desc: 'Choose to override zone wide traffic label for guest traffic for this cluster',
        externalLink: ''

    },

    helpOverrideGuestNetwork: {

        desc: 'Choose to override zone wide traffic label for guest traffic for this cluster',
        externalLink: ''

    },

    helpAccountPassword: {
        desc: 'Any desired password',
        externalLink: ''
    },
    helpAccountConfirmPassword: {
        desc: 'Type the same password again',
        externalLink: ''
    },
    helpAccountEmail: {
        desc: 'The account\'s email address',
        externalLink: ''
    },
    helpAccountFirstName: {
        desc: 'The first name, also known as the given name, of a person; or the first part of the entity represented by the account, such as a customer or department',
        externalLink: ''
    },
    helpAccountLastName: {
        desc: 'The last name, also known as the family name, of a person; or the second part of the name of a customer or department',
        externalLink: ''
    },
    helpAccountDomain: {
        desc: 'Domain in which the account is to be created',
        externalLink: ''
    },
    helpAccountAccount: {
        desc: 'Multiple users can exist in an account. Set the parent account name here.',
        externalLink: ''
    },
    helpAccountType: {
        desc: 'Root admin can access all resources. Domain admin can access the domain\'s users, but not physical servers. User sees only their own resources, such as VMs.',
        externalLink: ''
    },
    helpAccountTimezone: {
        desc: 'Set the time zone that corresponds to the account\'s locale',
        externalLink: ''
    },
    helpAccountNetworkDomain: {
        desc: 'If you want to assign a special domain name to the account\'s guest VM network, specify the DNS suffix',
        externalLink: ''
    },
    // Add cluster
    helpClusterZone: {
        desc: 'The zone in which you want to create the cluster',
        externalLink: ''
    },
    helpClusterHypervisor: {
        desc: 'The type of hypervisor software that runs on the hosts in this cluster. All hosts in a cluster run the same hypervisor.',
        externalLink: ''
    },
    helpClusterPod: {
        desc: 'The pod in which you want to create the cluster',
        externalLink: ''
    },
    helpClusterName: {
        desc: 'Cluster name. Used for display only. For VMware hypervisor, this must be a predefined name in VCenter.',
        externalLink: ''
    },
    helpClustervCenterHost: {
        desc: 'The hostname or IP address of the vCenter server',
        externalLink: ''
    },
    helpClustervCenterUsername: {
        desc: 'ID of a user with all administrative privileges on vCenter',
        externalLink: ''
    },
    helpClustervCenterPassword: {
        desc: 'Password of the user in Username',
        externalLink: ''
    },
    helpClustervCenterDatacenter: {
        desc: 'The vCenter datacenter that the cluster is in. For example, cloud.dc.VM',
        externalLink: ''
    },
    // Add compute offering
    helpComputeOfferingName: {
        desc: 'Any desired name for the offering',
        externalLink: ''
    },
    helpComputeOfferingDescription: {
        desc: 'A short description of the offering that can be displayed to users',
        externalLink: ''
    },
    helpComputeOfferingStorageType: {
        desc: 'Type of disk for the VM. Local storage is attached to the hypervisor host where the VM is running. Shared storage is accessible via NFS.',
        externalLink: ''
    },
    helpComputeOfferingProvisioningType: {
        desc: 'Provisioning type to create a volume. Thin and sparse is lazy allocation. fat is eager allocation.',
        externalLink: ''
    },
    helpComputeOfferingCPUCores: {
        desc: 'The number of cores which should be allocated to a VM with this offering',
        externalLink: ''
    },
    helpComputeOfferingCPUMHz: {
        desc: 'The CPU speed of the cores that the VM is allocated. For example, 2000 provides a 2 GHz clock.',
        externalLink: ''
    },
    helpComputeOfferingMemory: {
        desc: 'The amount of memory in megabytes to allocate for the system VM. For example, 2048 provides 2 GB RAM.',
        externalLink: ''
    },
    helpComputeOfferingNetworkRate: {
        desc: 'Allowed data transfer rate in megabits(Mb) per second',
        externalLink: ''
    },
    helpComputeOfferingDiskBytesReadRate: {
        desc: 'Allowed disk read rate in bytes per second',
        externalLink: ''
    },
    helpComputeOfferingDiskBytesWriteRate: {
        desc: 'Allowed disk write rate in bytes per second',
        externalLink: ''
    },
    helpComputeOfferingDiskIopsReadRate: {
        desc: 'Allowed disk read rate in I/O requests per second',
        externalLink: ''
    },
    helpComputeOfferingDiskIopsWriteRate: {
        desc: 'Allowed disk write rate in I/O requests per second',
        externalLink: ''
    },
    helpComputeOfferingHA: {
        desc: 'If yes, the administrator can choose to have the VM be monitored and as highly available as possible',
        externalLink: ''
    },
    helpComputeOfferingStorageTags: {
        desc: 'Comma-separated list of attributes that should be associated with the primary storage used by the VM. For example "ssd,blue".',
        externalLink: ''
    },
    helpComputeOfferingHostTags: {
        desc: 'Any tags that you use to organize your hosts',
        externalLink: ''
    },
    helpComputeOfferingCPUCap: {
        desc: 'If yes, the system will limit the level of CPU usage even if spare capacity is available',
        externalLink: ''
    },
    helpComputeOfferingPublic: {
        desc: 'Yes makes the offering available to all domains. No limits the scope to a subdomain; you will be prompted for the subdomain\'s name.',
        externalLink: ''
    },
    helpComputeOfferingDomain: {
        desc: 'The domain to associate this compute offering with'
    },
    // Create Instance Snapshot
    helpCreateInstanceSnapshotName: {
        desc: 'Give the snapshot a name. A unique name will be automatically generated if you leave this blank'
    },
    helpCreateInstanceSnapshotDescription: {
        desc: 'A short description of for the snapshot'
    },
    helpCreateInstanceSnapshotMemory: {
        desc: 'Check this to include CPU/memory state. Does not quiesce the VM. If not checked, the snapshot contain only volumes.'
    },
    // Create instance storage snapshot
    helpCreateInstanceStorageSnapshotVolume: {
        desc: 'Choose a volume that you want to take a snapshot of'
    },
    helpCreateInstanceStorageSnapshotName: {
        desc: 'Give the snapshot a name. A unique name will be automatically generated if you leave this blank'
    },
    // Add disk offering
    helpDiskOfferingName: {
        desc: 'Any desired name for the offering',
        externalLink: ''
    },
    helpDiskOfferingDescription: {
        desc: 'A short description of the offering that can be displayed to users',
        externalLink: ''
    },
    helpDiskOfferingProvisioningType: {
        desc: 'Provisioning type to create a volume. Thin and sparse is lazy allocation. fat is eager allocation.',
        externalLink: ''
    },
    helpDiskOfferingStorageType: {
        desc: 'Type of disk for the VM. Local is attached to the hypervisor host where the VM is running. Shared is storage accessible via NFS.',
        externalLink: ''
    },
    helpDiskOfferingQoSType: {
        desc: 'Type of Quality of Service desired, if any.',
        externalLink: ''
    },
    helpDiskOfferingCustomDiskSize: {
        desc: 'If checked, the user can set their own disk size. If not checked, the root administrator must define a value in Disk Size.',
        externalLink: ''
    },
    helpDiskOfferingDiskSize: {
        desc: 'Appears only if Custom Disk Size is not selected. Define the volume size in GB. (1GB = 1,073,741,824 bytes)',
        externalLink: ''
    },
    helpDiskOfferingDiskBytesReadRate: {
        desc: 'Allowed disk read rate in bytes per second',
        externalLink: ''
    },
    helpDiskOfferingDiskBytesWriteRate: {
        desc: 'Allowed disk write rate in bytes per second',
        externalLink: ''
    },
    helpDiskOfferingDiskIopsReadRate: {
        desc: 'Allowed disk read rate in I/O requests per second',
        externalLink: ''
    },
    helpDiskOfferingDiskIopsWriteRate: {
        desc: 'Allowed disk write rate in I/O requests per second',
        externalLink: ''
    },
    helpDiskOfferingCustomDiskIops: {
        desc: 'If checked, the user can set Min and Max IOPS. If not checked, the root administrator can define such values.',
        externalLink: ''
    },
    helpDiskOfferingDiskIopsMin: {
        desc: 'Appears only if Custom IOPS is not selected. Define the minimum volume IOPS.',
        externalLink: ''
    },
    helpDiskOfferingDiskIopsMax: {
        desc: 'Appears only if Custom IOPS is not selected. Define the maximum volume IOPS.',
        externalLink: ''
    },
    helpDiskOfferingHypervisorSnapshotReserve: {
        desc: 'Hypervisor snapshot reserve space as a percent of a volume (for managed storage using XenServer or VMware) (Ex. The value 25 means 25%.)).'
    },
    helpDiskOfferingCacheMode: {
        desc: 'The write caching mode to use for disks created with this disk offering. This can improve write performance.',
        externalLink: ''
    },
    helpDiskOfferingStorageTags: {
        desc: 'Comma-separated list of attributes that should be associated with the primary storage for this disk. For example "ssd,blue".',
        externalLink: ''
    },
    helpDiskOfferingPublic: {
        desc: 'Yes makes the offering available to all domains. No limits the scope to a subdomain; you will be prompted for the subdomain\'s name.',
        externalLink: ''
    },
    helpDiskOfferingDomain: {
        desc: 'Select the subdomain in which this offering is available',
        externalLink: ''
    },
    // Add domain
    helpDomainName: {
        desc: 'Any desired name for the new sub-domain. Must be unique within the current domain.',
        externalLink: ''
    },
    helpDomainNetworkDomain: {
        desc: 'If you want to assign a special domain name to this domain\'s guest VM network, specify the DNS suffix',
        externalLink: ''
    },
    helpDomainId: {
        desc: 'A valid domain id. CloudStack will generate one for you if empty.',
        externalLink: ''
    },
    // Add F5
    helpF5IPAddress: {
        desc: 'The IP address of the device',
        externalLink: ''
    },
    helpF5Username: {
        desc: 'A user ID with valid authentication credentials that provide to access the device',
        externalLink: ''
    },
    helpF5Password: {
        desc: 'The password for the user ID provided in Username',
        externalLink: ''
    },
    helpF5Type: {
        desc: 'The type of device that is being added',
        externalLink: ''
    },
    helpF5PublicInterface: {
        desc: 'Interface of device that is configured to be part of the public network',
        externalLink: ''
    },
    helpF5PrivateInterface: {
        desc: 'Interface of device that is configured to be part of the private network',
        externalLink: ''
    },
    helpF5Retries: {
        desc: 'Number of times to attempt a command on the device before considering the operation failed. Default is 2.',
        externalLink: ''
    },
    helpF5Mode: {
        desc: 'Side by side mode is supported for the F5.',
        externalLink: ''
    },
    helpF5Dedicated: {
        desc: 'Check this box to dedicate the device to a single account. The value in the Capacity field will be ignored.',
        externalLink: ''
    },
    helpF5Capacity: {
        desc: 'Number of guest networks/accounts that will share this device',
        externalLink: ''
    },
    // Add guest network
    helpGuestNetworkName: {
        desc: 'The name of the network. This will be user-visible.',
        externalLink: ''
    },
    helpGuestNetworkDisplayText: {
        desc: 'The description of the network. This will be user-visible.',
        externalLink: ''
    },
    helpGuestNetworkZone: {
        desc: 'The name of the zone this network applies to. The administrator must configure the IP range for the guest networks in each zone.',
        externalLink: ''
    },
    helpGuestNetworkNetworkOffering: {
        desc: 'If the administrator has configured multiple network offerings, select the one you want to use for this network',
        externalLink: ''
    },
    helpGuestNetworkGateway: {
        desc: 'The gateway that the guests should use.',
        externalLink: ''
    },
    helpGuestNetworkNetmask: {
        desc: 'The netmask in use on the subnet the guests will use.',
        externalLink: ''
    },
    // Add guest network from zone
    helpGuestNetworkZoneName: {
        desc: 'The name of the network. This will be user-visible.',
        externalLink: ''
    },
    helpGuestNetworkZoneDescription: {
        desc: 'The description of the network. This will be user-visible.',
        externalLink: ''
    },
    helpGuestNetworkZoneVLANID: {
        desc: 'The VLAN tag for this network',
        externalLink: ''
    },
    helpGuestNetworkZoneScope: {
        desc: 'Scope',
        externalLink: ''
    },
    helpGuestNetworkZoneNetworkOffering: {
        desc: 'If the administrator has configured multiple network offerings, select the one you want to use for this network',
        externalLink: ''
    },
    helpGuestNetworkZoneGateway: {
        desc: 'The gateway that the guests should use.',
        externalLink: ''
    },
    helpGuestNetworkZoneNetmask: {
        desc: 'The netmask in use on the subnet the guests will use.',
        externalLink: ''
    },
    helpGuestNetworkZoneStartIP: {
        desc: 'The first IP address to define a range that can be assigned to guests. We strongly recommend the use of multiple NICs.',
        externalLink: ''
    },
    helpGuestNetworkZoneEndIP: {
        desc: 'The final IP address to define a range that can be assigned to guests. We strongly recommend the use of multiple NICs.',
        externalLink: ''
    },
    helpGuestNetworkZoneNetworkDomain: {
        desc: 'If you want to assign a special domain name to this guest VM network, specify the DNS suffix',
        externalLink: ''
    },
    helpGuestNetworkHideIpAddressUsage: {
        desc: 'If you want the IP address usage records hidden for the network',
        externalLink: ''
    },
    // Add host
    helpHostZone: {
        desc: 'The zone where you want to add the host',
        externalLink: ''
    },
    helpHostPod: {
        desc: 'The pod where you want to add the host',
        externalLink: ''
    },
    helpHostCluster: {
        desc: 'The cluster where you want to add the host',
        externalLink: ''
    },
    helpHostName: {
        desc: 'The DNS name or IP address of the host',
        externalLink: ''
    },
    helpHostUsername: {
        desc: 'Usually root',
        externalLink: ''
    },
    helpHostPassword: {
        desc: 'The password for the user named in Username. The password was set during hypervisor installation on the host.',
        externalLink: ''
    },
    helpHostTags: {
        desc: 'Any labels that you use to categorize hosts for ease of maintenance or to enable HA.',
        externalLink: ''
    },
    // Add Netscaler
    helpNetScalerIPAddress: {
        desc: 'The IP address of the device',
        externalLink: ''
    },
    helpNetScalerUsername: {
        desc: 'A user ID with valid authentication credentials that provide to access the device',
        externalLink: ''
    },
    helpNetScalerPassword: {
        desc: 'The password for the user ID provided in Username',
        externalLink: ''
    },
    helpNetScalerType: {
        desc: 'The type of device that is being added',
        externalLink: ''
    },
    helpNetScalerPublicInterface: {
        desc: 'Interface of device that is configured to be part of the public network',
        externalLink: ''
    },
    helpNetScalerPrivateInterface: {
        desc: 'Interface of device that is configured to be part of the private network',
        externalLink: ''
    },
    helpNetScalerRetries: {
        desc: 'Number of times to attempt a command on the device before considering the operation failed. Default is 2.',
        externalLink: ''
    },
    helpNetScalerDedicated: {
        desc: 'Check this box to dedicate the device to a single account. The value in the Capacity field will be ignored.',
        externalLink: ''
    },
    helpNetScalerCapacity: {
        desc: 'Number of guest networks/accounts that will share this device',
        externalLink: ''
    },
    helpNetscalerServicePackages: {
        desc: 'Choose the Netscaler Service Package you want to use.',
        externalLink: ''
    },
    // Add network offering
    helpNetworkOfferingName: {
        desc: 'Any desired name for the network offering',
        externalLink: ''
    },
    helpNetworkOfferingDescription: {
        desc: 'A short description of the offering that can be displayed to users',
        externalLink: ''
    },
    helpNetworkOfferingNetworkRate: {
        desc: 'Allowed data transfer rate in megabits(Mb) per second',
        externalLink: ''
    },
    helpNetworkOfferingTrafficType: {
        desc: 'The type of network traffic that will be carried on the network',
        externalLink: ''
    },
    helpNetworkOfferingGuestType: {
        desc: 'Choose whether the guest network is isolated or shared.',
        externalLink: ''
    },
    helpNetworkOfferingSpecifyVLAN: {
        desc: '(Isolated guest networks only) Indicate whether a VLAN should be specified when this offering is used',
        externalLink: ''
    },
    helpNetworkOfferingVPC: {
        desc: 'Specify whether this offering is for a virtual private cloud',
        externalLink: ''
    },
    helpNetworkOfferingSystemOffering: {
        desc: 'Choose the system service offering that you want the virtual routers to use in this network',
        externalLink: ''
    },
    helpNetworkOfferingLBIsolation: {
        desc: 'Specify what type of load balancer isolation you want for the network: Shared or Dedicated',
        externalLink: ''
    },
    helpNetworkOfferingMode: {
        desc: 'Choose Inline or Side by Side to specify whether a firewall is placed in front of the load balancing device (inline) or in parallel with it (side-by-side)',
        externalLink: ''
    },
    helpNetworkOfferingAssociatePublicIP: {
        desc: 'Select this option if you want to assign a public IP address to the VMs deployed in the guest network',
        externalLink: ''
    },
    helpNetworkOfferingRedundantRouterCapability: {
        desc: 'Select this option if you want to use two virtual routers in the network for uninterrupted connection: one operating as the master virtual router and the other as the backup',
        externalLink: ''
    },
    helpNetworkOfferingConserveMode: {
        desc: 'Check this box to use conserve mode, where network resources are allocated only when the first VM starts. You can define more than one service on the same public IP only when conserve mode is on.',
        externalLink: ''
    },
    helpNetworkOfferingTags: {
        desc: 'Network tag to specify which physical network to use',
        externalLink: ''
    },
    // Add pod
    helpPodZone: {
        desc: 'The zone where you want to add the pod',
        externalLink: ''
    },
    helpPodName: {
        desc: 'Set a name for the pod',
        externalLink: ''
    },
    helpPodGateway: {
        desc: 'The gateway for the hosts in the pod',
        externalLink: ''
    },
    helpPodNetmask: {
        desc: 'The network prefix that defines the pod\'s subnet.',
        externalLink: ''
    },
    helpPodStartIP: {
        desc: 'The first IP address to define a range in the management network that is used to manage various system VMs',
        externalLink: ''
    },
    helpPodEndIP: {
        desc: 'The last IP address to define a range in the management network that is used to manage various system VMs',
        externalLink: ''
    },
    // Add primary storage
    helpPrimaryStorageZone: {
        desc: 'The zone in which you want to create the primary storage',
        externalLink: ''
    },
    helpPrimaryStoragePod: {
        desc: 'The pod in which you want to create the primary storage',
        externalLink: ''
    },
    helpPrimaryStorageCluster: {
        desc: 'The cluster in which you want to create the primary storage',
        externalLink: ''
    },
    helpPrimaryStorageName: {
        desc: 'The name of the storage device',
        externalLink: ''
    },
    helpPrimaryStorageProtocol: {
        desc: 'For XenServer, choose NFS, iSCSI, or PreSetup. For KVM, choose NFS, SharedMountPoint, RDB, CLVM or Gluster. For vSphere, choose VMFS (iSCSI or FiberChannel) or NFS. For Hyper-V, choose SMB/CIFS. For LXC, choose NFS or SharedMountPoint. For OVM, choose NFS or ocfs2.',
        externalLink: ''
    },
    helpPrimaryStorageServer: {
        desc: 'NFS, iSCSI, or PreSetup: IP address or DNS name of the storage device. VMFS: IP address or DNS name of the vCenter server.',
        externalLink: ''
    },
    helpPrimaryStoragePath: {
        desc: 'NFS: exported path from the server. VMFS: /datacenter name/datastore name. SharedMountPoint: path where primary storage is mounted, such as "/mnt/primary"',
        externalLink: ''
    },
    helpPrimaryStorageSRNameLabel: {
        desc: 'The name-label of the SR that has been set up independently of the cloud management system',
        externalLink: ''
    },
    helpPrimaryStorageTargetIQN: {
        desc: 'In iSCSI, this is the IQN of the target. For example, iqn.1986-03.com.sun:02:01ec9bb549-1271378984',
        externalLink: ''
    },
    helpPrimaryStorageLun: {
        desc: 'In iSCSI, this is the LUN number. For example, 3.',
        externalLink: ''
    },
    helpPrimaryStorageRBDMonitor: {
        desc: 'The address of a Ceph monitor. Can also be a Round Robin DNS record',
        externalLink: ''
    },
    helpPrimaryStorageRBDPool: {
        desc: 'The pool to use on the Ceph cluster. This pool should already exist',
        externalLink: ''
    },
    helpPrimaryStorageRBDId: {
        desc: 'The cephx user to use without the client. prefix. For example: admin',
        externalLink: ''
    },
    helpPrimaryStorageRBDSecret: {
        desc: 'The base64 encoded secret of the cephx user.',
        externalLink: ''
    },
    helpPrimaryStorageTags: {
        desc: 'Comma-separated list of tags for this storage device. Must be the same set or a superset of the tags on your disk offerings.',
        externalLink: ''
    },
    // Add secondary storage
    helpSecondaryStorageZone: {
        desc: 'The zone in which you want to create the secondary storage',
        externalLink: ''
    },
    helpSecondaryStorageNFSServer: {
        desc: 'The IP address of the server',
        externalLink: ''
    },
    helpSecondaryStoragePath: {
        desc: 'The exported path from the server',
        externalLink: ''
    },
    // S3
    helpS3AccessKey: {
        desc: 'This identifies the S3 user account',
        externalLink: ''
    },
    helpS3SecretKey: {
        desc: 'The password matching the S3 Access Key',
        externalLink: ''
    },
    helpS3Bucket: {
        desc: 'The S3 bucket to store CloudStack data in. The bucket must be created and owned by the same user identified by the S3 Access Key',
        externalLink: ''
    },
    helpS3Endpoint: {
        desc: 'The S3 server to use. Non-standard ports can be appended to the hostname as per s3.amazonaws.com:8080',
        externalLink: ''
    },
    helpS3ConnectionTimeout: {
        desc: 'The number of milliseconds to wait when establishing an S3 connection. This is optional',
        externalLink: ''
    },
    helpS3MaxErrorRetry: {
        desc: 'The number of times to retry S3 requests before failing. This is optional',
        externalLink: ''
    },
    helpS3SocketTimeout: {
        desc: 'The number of milliseconds to wait while reading an S3 response. This is optional',
        externalLink: ''
    },
    helpNFSStagingServer: {
        desc: 'The name or IP address of the NFS secondary staging server',
        externalLink: ''
    },
    helpNFSStagingPath: {
        desc: 'The exported path to use on the NFS staging server',
        externalLink: ''
    },
    // Add SRX
    helpSRXIPAddress: {
        desc: 'The IP address of the device',
        externalLink: ''
    },
    helpSRXUsername: {
        desc: 'A user ID with valid authentication credentials that provide to access the device',
        externalLink: ''
    },
    helpSRXPassword: {
        desc: 'The password for the user ID provided in Username',
        externalLink: ''
    },
    helpSRXType: {
        desc: 'The type of device that is being added',
        externalLink: ''
    },
    helpSRXPublicInterface: {
        desc: 'Interface of device that is configured to be part of the public network. For example, ge-0/0/2',
        externalLink: ''
    },
    helpSRXPrivateInterface: {
        desc: 'Interface of device that is configured to be part of the private network. For example, ge-0/0/1',
        externalLink: ''
    },
    helpSRXUsageInterface: {
        desc: 'Interface used to meter traffic. If you don\'t want to use the public interface, specify a different interface name here.',
        externalLink: ''
    },
    helpSRXRetries: {
        desc: 'Number of times to attempt a command on the device before considering the operation failed. Default is 2.',
        externalLink: ''
    },
    helpSRXTimeout: {
        desc: 'The time to wait for a command on the SRX before considering it failed. Default is 300 seconds.',
        externalLink: ''
    },
    helpSRXMode: {
        desc: 'Side by side mode is supported for the SRX.',
        externalLink: ''
    },
    helpSRXPublicNetwork: {
        desc: 'The name of the public network on the SRX. For example, trust.',
        externalLink: ''
    },
    helpSRXPrivateNetwork: {
        desc: 'The name of the private network on the SRX. For example, untrust.',
        externalLink: ''
    },
    helpSRXDedicated: {
        desc: 'Check this box to dedicate the device to a single account. The value in the Capacity field will be ignored.',
        externalLink: ''
    },
    helpSRXCapacity: {
        desc: 'Number of guest networks/accounts that will share this device',
        externalLink: ''
    },
    // Add Palo Alto
    helpPaloAltoIPAddress: {
        desc: 'The IP address of the device',
        externalLink: ''
    },
    helpPaloAltoUsername: {
        desc: 'A user ID with valid authentication credentials that provide to access the device',
        externalLink: ''
    },
    helpPaloAltoPassword: {
        desc: 'The password for the user ID provided in Username',
        externalLink: ''
    },
    helpPaloAltoType: {
        desc: 'The type of device that is being added',
        externalLink: ''
    },
    helpPaloAltoPublicInterface: {
        desc: 'Interface of device that is configured to be part of the public network. For example, ge-0/0/2',
        externalLink: ''
    },
    helpPaloAltoPrivateInterface: {
        desc: 'Interface of device that is configured to be part of the private network. For example, ge-0/0/1',
        externalLink: ''
    },
    helpPaloAltoUsageInterface: {
        desc: 'Interface used to meter traffic. If you don\'t want to use the public interface, specify a different interface name here.',
        externalLink: ''
    },
    helpPaloAltoRetries: {
        desc: 'Number of times to attempt a command on the device before considering the operation failed. Default is 2.',
        externalLink: ''
    },
    helpPaloAltoTimeout: {
        desc: 'The time to wait for a command on the Palo Alto before considering it failed. Default is 300 seconds.',
        externalLink: ''
    },
    helpPaloAltoMode: {
        desc: 'Side by side mode is supported for the Palo Alto.',
        externalLink: ''
    },
    helpPaloAltoPublicNetwork: {
        desc: 'The name of the public network on the Palo Alto. For example, trust.',
        externalLink: ''
    },
    helpPaloAltoPrivateNetwork: {
        desc: 'The name of the private network on the Palo Alto. For example, untrust.',
        externalLink: ''
    },
    helpPaloAltoVirtualRouter: {
        desc: 'The name of the virtual router on the Palo Alto.',
        externalLink: ''
    },
    helpPaloAltoThreatProfile: {
        desc: 'The threat profile name/group to associate with allow firewall policies.',
        externalLink: ''
    },
    helpPaloAltoLogProfile: {
        desc: 'The log profile name/group to associate with allow firewall policies.',
        externalLink: ''
    },
    helpPaloAltoDedicated: {
        desc: 'Check this box to dedicate the device to a single account. The value in the Capacity field will be ignored.',
        externalLink: ''
    },
    helpPaloAltoCapacity: {
        desc: 'Number of guest networks/accounts that will share this device',
        externalLink: ''
    },
    // Add system service offering
    helpSystemOfferingName: {
        desc: 'Any desired name for the offering',
        externalLink: ''
    },
    helpSystemOfferingDescription: {
        desc: 'A short description of the offering that can be displayed to the root administrator',
        externalLink: ''
    },
    helpSystemOfferingVMType: {
        desc: 'The type of system VM that is being offered',
        externalLink: ''
    },
    helpSystemOfferingStorageType: {
        desc: 'Type of disk for the system VM. Local storage is attached to the host where the system VM is running. Shared storage is accessible via NFS.',
        externalLink: ''
    },
    helpSystemOfferingCPUCores: {
        desc: 'The number of cores which should be allocated to a system VM with this offering',
        externalLink: ''
    },
    helpSystemOfferingCPUMHz: {
        desc: 'The CPU speed of the cores that the system VM is allocated. For example, 2000 would provide for a 2 GHz clock.',
        externalLink: ''
    },
    helpSystemOfferingMemory: {
        desc: 'The amount of memory in megabytes to allocate for the system VM. For example, 2048 provides 2 GB RAM.',
        externalLink: ''
    },
    helpSystemOfferingNetworkRate: {
        desc: 'Allowed data transfer rate in megabits(Mb) per second',
        externalLink: ''
    },
    helpSystemOfferingDiskBytesReadRate: {
        desc: 'Allowed disk read rate in bytes per second',
        externalLink: ''
    },
    helpSystemOfferingDiskBytesWriteRate: {
        desc: 'Allowed disk write rate in bytes per second',
        externalLink: ''
    },
    helpSystemOfferingDiskIopsReadRate: {
        desc: 'Allowed disk read rate in I/O requests per second',
        externalLink: ''
    },
    helpSystemOfferingDiskIopsWriteRate: {
        desc: 'Allowed disk write rate in I/O requests per second',
        externalLink: ''
    },
    helpSystemOfferingHA: {
        desc: 'If yes, the administrator can choose to have the system VM be monitored and as highly available as possible',
        externalLink: ''
    },
    helpSystemOfferingStorageTags: {
        desc: 'Comma-separated list of attributes that should be associated with the primary storage used by the system VM. For example "ssd,blue".',
        externalLink: ''
    },
    helpSystemOfferingHostTags: {
        desc: 'Any tags that you use to organize your hosts',
        externalLink: ''
    },
    helpSystemOfferingCPUCap: {
        desc: 'If yes, the system will limit the level of CPU usage even if spare capacity is available',
        externalLink: ''
    },
    helpSystemOfferingPublic: {
        desc: 'Yes makes the offering available to all domains. No limits the scope to a subdomain; you will be prompted for the subdomain\'s name.',
        externalLink: ''
    },
    helpSystemOfferingDomain: {
        desc: 'Select the subdomain in which this offering is available',
        externalLink: ''
    },
    // Add tier
    helpTierName: {
        desc: 'A unique name for the tier',
        externalLink: ''
    },
    helpTierNetworkOffering: {
        desc: 'If the administrator has configured multiple network offerings, select the one you want to use for this tier',
        externalLink: ''
    },
    helpTierGateway: {
        desc: 'The gateway for the tier. Must be in the Super CIDR range of the VPC and not overlapping the CIDR of any other tier in this VPC.',
        externalLink: ''
    },
    helpTierNetmask: {
        desc: 'Netmask for the tier. For example, with VPC CIDR of 10.0.0.0/16 and network tier CIDR of 10.0.1.0/24, gateway is 10.0.1.1 and netmask is 255.255.255.0',
        externalLink: ''
    },
    // Add user
    helpUserUsername: {
        desc: 'Any desired user ID. Must be unique in the current domain. The same username can exist in other domains, including sub-domains.',
        externalLink: ''
    },
    helpUserPassword: {
        desc: 'Any desired user password',
        externalLink: ''
    },
    helpUserConfirmPassword: {
        desc: 'Type the same password again',
        externalLink: ''
    },
    helpUserEmail: {
        desc: 'The user\'s email address',
        externalLink: ''
    },
    helpUserFirstName: {
        desc: 'The user\'s first name, also known as the given name',
        externalLink: ''
    },
    helpUserLastName: {
        desc: 'The user\'s last name, also known as the family name',
        externalLink: ''
    },
    helpUserTimezone: {
        desc: 'Set the time zone that corresponds to the user\'s locale',
        externalLink: ''
    },
    // Add volume
    helpVolumeName: {
        desc: 'Give a unique volume name. If it is not provided, a name will be generated randomly.',
        externalLink: ''
    },
    helpVolumeAvailabilityZone: {
        desc: 'Where do you want the storage to reside? This should be close to the VM that will use the volume.',
        externalLink: ''
    },
    helpVolumeDiskOffering: {
        desc: 'Choose the characteristics of the storage.',
        externalLink: ''
    },
    helpVolumeSizeGb: {
        desc: 'Volume size in GB (1GB = 1,073,741,824 bytes)',
        externalLink: ''
    },
    // Add VPC
    helpVPCName: {
        desc: 'A name for the new VPC',
        externalLink: ''
    },
    helpVPCDescription: {
        desc: 'Display text about the VPC',
        externalLink: ''
    },
    helpVPCZone: {
        desc: 'Zone where you want the VPC to be available',
        externalLink: ''
    },
    helpVPCSuperCIDR: {
        desc: 'CIDR range for all the tiers within a VPC. Each tier\'s CIDR must be within the Super CIDR.',
        externalLink: ''
    },
    helpVPCDomain: {
        desc: 'If you want to assign a special domain name to this VPC\'s guest VM network, specify the DNS suffix',
        externalLink: ''
    },
    // Add VPC gateway
    helpVPCGatewayPhysicalNetwork: {
        desc: 'Name of a physical network that has been created in the zone',
        externalLink: ''
    },
    helpVPCGatewayVLAN: {
        desc: 'The VLAN associated with the VPC gateway',
        externalLink: ''
    },
    helpVPCGatewayIP: {
        desc: 'The IP address associated with the VPC gateway',
        externalLink: ''
    },
    helpVPCGatewayGateway: {
        desc: 'The gateway through which the traffic is routed to and from the VPC',
        externalLink: ''
    },
    helpVPCGatewayNetmask: {
        desc: 'The netmask associated with the VPC gateway',
        externalLink: ''
    },
    // Add VPN customer gateway
    helpVPNGatewayName: {
        desc: 'A unique name for the VPN customer gateway',
        externalLink: ''
    },
    helpVPNGatewayGateway: {
        desc: 'The IP address for the remote gateway',
        externalLink: ''
    },
    helpVPNGatewayCIDRList: {
        desc: 'The guest CIDR list of the remote subnets. Enter a CIDR or a comma-separated list. Do not overlap the VPC\'s CIDR or another guest CIDR.',
        externalLink: ''
    },
    helpVPNGatewayIPsecPresharedKey: {
        desc: 'Enter a secret key value. The endpoints of the VPN share a secret key. This is used to authenticate the customer gateway and the VPC VPN gateway to each other.',
        externalLink: ''
    },
    helpVPNGatewayIKEEncryption: {
        desc: 'Enter AES128, AES192, AES256, or 3DES to specify the Internet Key Exchange (IKE) policy for phase-1. Authentication is accomplished with Preshared Keys.',
        externalLink: ''
    },
    helpVPNGatewayIKEHash: {
        desc: 'Enter SHA1 or MD5 to specify the IKE hash algorithm for phase-1',
        externalLink: ''
    },
    helpVPNGatewayIKEDH: {
        desc: 'Enter Group-5 (1536-bit), Group-2 (1024-bit), or None to specify the public-key cryptography protocol to use. The 1536-bit Diffie-Hellman group is used within IKE to establish session keys.',
        externalLink: ''
    },
    helpVPNGatewayESPEncryption: {
        desc: 'Enter AES128, AES192, AES256, or 3DES to specify the Encapsulating Security Payload (ESP) algorithm within phase-2',
        externalLink: ''
    },
    helpVPNGatewayESPHash: {
        desc: 'Enter SHA1 or MD5 to specify the Encapsulating Security Payload (ESP) hash for phase-2',
        externalLink: ''
    },
    helpVPNGatewayPerfectForwardSecrecy: {
        desc: 'Choose Group-5 (1536-bit), Group-2 (1024-bit), or None to specify whether to enforce a new Diffie-Hellman key exchange and, if so, what size of DH group to use',
        externalLink: ''
    },
    helpVPNGatewayIKELifetime: {
        desc: 'The phase-1 lifetime of the security association in seconds. Whenever the time expires, a new phase-1 exchange is performed.',
        externalLink: ''
    },
    helpVPNGatewayESPLifetime: {
        desc: 'The phase-2 lifetime of the security association in seconds. Whenever the time expires, a re-key is initiated to provide a new IPsec encryption and authentication session keys.',
        externalLink: ''
    },
    helpVPNGatewayDeadPeerDetection: {
        desc: 'Check this to make the virtual router query its IKE peer at regular intervals to ensure continued availability. It is recommended to have the same DPD setting on both sides of the VPN connection.',
        externalLink: ''
    },
    helpVPNGatewayForceEncapsulation: {
        desc: 'Force UDP encapsulation for ESP packets even if no NAT situation is detected. This may help to surmount restrictive firewalls. In order to force the peer to encapsulate packets, NAT detection payloads are faked',
        externalLink: ''
    },
    // Copy template
    helpCopyTemplateDestination: {
        desc: 'The zone to which you want to copy the template',
        externalLink: ''
    },
    // Enter token
    helpEnterTokenProjectID: {
        desc: 'Unique identifying number for the project. Use the number you received in the invitation email',
        externalLink: ''
    },
    helpEnterTokenToken: {
        desc: 'Unique security code that authorizes you to accept the project invitation. Use the token you received in the invitation email',
        externalLink: ''
    },
    // Register template
    helpRegisterTemplate: {
        desc: '',
        externalLink: ''
    },
    // Register template
    helpRegisterTemplate: {
        desc: '',
        externalLink: ''
    },
    // Register ISO
    helpRegisterISOName: {
        desc: 'A unique name for the ISO. This will be visible to users, so choose something descriptive.',
        externalLink: ''
    },
    helpRegisterISODescription: {
        desc: 'Display text describing the ISO. This will be visible to users, so choose something descriptive.',
        externalLink: ''
    },
    helpRegisterISOURL: {
        desc: 'The Management Server will download the file from the specified URL, such as http://my.web.server/filename.iso',
        externalLink: ''
    },
    helpRegisterISODirectDownload: {
        desc: 'KVM Only: Secondary Storage is bypassed and ISO is downloaded to Primary Storage on deployment',
        externalLink: ''
    },
    helpRegisterISOZone: {
        desc: 'Choose the zone where you want the ISO to be available, or All Zones to make it available throughout the cloud',
        externalLink: ''
    },
    helpRegisterISOBootable: {
        desc: 'Indicate whether the machine can be booted using this ISO',
        externalLink: ''
    },
    helpRegisterISOOSType: {
        desc: 'Operating system of the VM represented by the ISO. If the OS type of the ISO is not listed, choose Other.',
        externalLink: ''
    },
    helpRegisterISOExtractable: {
        desc: 'Whether the ISO is downloadable by users or not',
        externalLink: ''
    },
    helpRegisterISOPublic: {
        desc: 'Check this to make the ISO accessible to all users. The ISO will appear in the Community ISOs list.',
        externalLink: ''
    },
    helpRegisterISOFeatured: {
        desc: 'Check this to make the ISO more prominent for users. The ISO will appear in the Featured ISOs list.',
        externalLink: ''
    },
    // Register template
    helpRegisterTemplateName: {
        desc: 'A unique name for the template. This will be visible to users, so choose something descriptive.',
        externalLink: ''
    },
    helpRegisterTemplateDescription: {
        desc: 'Display text describing the template. This will be visible to users, so choose something descriptive.',
        externalLink: ''
    },
    helpRegisterTemplateURL: {
        desc: 'The Management Server will download the file from the specified URL, such as http://my.web.server/filename.vhd.gz',
        externalLink: ''
    },
    helpRegisterTemplateDirectDownload: {
        desc: 'KVM Only: Secondary Storage is bypassed and template/ISO is downloaded to Primary Storage on deployment',
        externalLink: ''
    },
    helpRegisterTemplateZone: {
        desc: 'Choose one or more zones where you want the template to be available, or All Zones to make it available throughout the cloud. (Tip: Use Ctrl to choose multiple zones)',
        externalLink: ''
    },
    helpRegisterTemplateHypervisor: {
        desc: 'The hypervisor software from which this template is being imported; this determines the value of Format',
        externalLink: ''
    },
    helpRegisterTemplateFormat: {
        desc: 'The data format of the template upload file',
        externalLink: ''
    },
    helpRegisterTemplateOSType: {
        desc: 'Operating system of the VM represented by the template. If the OS type of the template is not listed, choose Other.',
        externalLink: ''
    },
    helpRegisterTemplateExtractable: {
        desc: 'Whether the template is downloadable by users or not',
        externalLink: ''
    },
    helpRegisterTemplateDynamicallyScalable: {
        desc: 'Check this if the template contains XS/VMWare tools to support dynamic scaling of VM cpu/memory.',
        externalLink: ''
    },
    helpRegisterTemplatePasswordEnabled: {
        desc: 'Check this if the template has the password change script installed.',
        externalLink: ''
    },
    helpRegisterTemplatePublic: {
        desc: 'Check this to make the template accessible to all users. The template will appear in the Community Templates list.',
        externalLink: ''
    },
    helpRegisterTemplateFeatured: {
        desc: 'Check this to make the template more prominent for users. The template will appear in the Featured Templates list.',
        externalLink: ''
    },
    helpRegisterTemplateRouting: {
        desc: 'Check this if the template is used for deploying router.',
        externalLink: ''
    },
    helpRegisterTemplateHvm: {
        desc: 'Check this if the template requires hvm.',
        externalLink: ''
    },
    // Upload volume
    helpUploadVolumeName: {
        desc: 'A unique name for the volume. This will be visible to users, so choose something descriptive.',
        externalLink: ''
    },
    helpUploadVolumeZone: {
        desc: 'Choose the zone where you want to store the volume. VMs running on hosts in this zone can attach the volume.',
        externalLink: ''
    },
    helpUploadVolumeFormat: {
        desc: 'The disk image format of the volume. XenServer is VHD, VMware is OVA, and KVM is QCOW2. Hyper-V is VHD or VHDX. OVM is RAW.',
        externalLink: ''
    },
    helpUploadVolumeURL: {
        desc: 'Secure HTTP or HTTPS URL that can be used to get the disk. File type must match Format. For example, if Format is VHD, http://yourFileServerIP/userdata/myDataDisk.vhd',
        externalLink: ''
    },
    helpUploadVolumeChecksum: {
        desc: 'Use the hash that you created at the start of the volume upload procedure',
        externalLink: ''
    },
    helpLdapGroupName: {
        desc: 'The group name from which you want to import LDAP users',
        externalLink: ''
    },
    helpSamlEnable: {
        desc: 'Enable SAML Single Sign On for the user(s)',
        externalLink: ''
    },
    helpSamlEntity: {
        desc: 'Choose the SAML Identity Provider Entity ID with which you want to enable the Single Sign On for the user(s)',
        externalLink: ''
    },
    helpVpcOfferingName: {
        desc: 'Any desired name for the VPC offering',
        externalLink: ''
    },
    helpVpcOfferingDescription: {
        desc: 'A short description of the offering that can be displayed to users',
        externalLink: ''
    },
    helpOvm3pool: {
        desc: 'Pool the Ovm3 nodes in this cluster, required for vm node migrations',
        externalLink: ''
    },
    helpOvm3cluster: {
        desc: 'Use the native Ovm3 OCFS2 clustering, required for native HA and requires pooling',
        externalLink: ''
    },
    helpOvm3Vip: {
        desc: 'The VIP used by the pool and cluster',
        externalLink: ''
    },
    helpLdapGroupName: {
        desc: 'Fully qualified name of OU/GROUP in LDAP',
        externalLink: ''
    },
    helpLdapGroupType: {
        desc: 'Type of LDAP name provided. Can be either GROUP/OU',
        externalLink: ''
    },
    helpLdapLinkDomainAdmin: {
        desc: 'domain admin of the linked domain. Specify a username in GROUP/OU of LDAP'
    },
    helpSetReservationSystemVms: {
        desc: 'If enabled, IP range reservation is set for SSVM & CPVM. Global setting "system.vm.public.ip.reservation.mode.strictness" is used to control whether reservation is strict or not (preferred)'
    },
    helpL2UserData: {
        desc: 'Pass user and meta data to VMs (via ConfigDrive)',
        externalLink: ''
    },

    helpComputeOfferingMinCPUCores: {
        desc: 'This will be used for the setting the range (min-max) of the number of cpu cores that should be allowed for VMs using this custom offering.',
        externalLink: ''
    },

    helpComputeOfferingMaxCPUCores: {
        desc: 'This will be used for the setting the range (min-max) of the number of cpu cores that should be allowed for VMs using this custom offering.',
        externalLink: ''
    },

    helpComputeOfferingMinMemory: {
        desc: 'This will be used for the setting the range (min-max) amount of memory that should be allowed for VMs using this custom offering.',
        externalLink: ''
    },

    helpComputeOfferingMaxMemory: {
        desc: 'This will be used for the setting the range (min-max) amount of memory that should be allowed for VMs using this custom offering.',
        externalLink: ''
    },

    helpComputeOfferingType: {
        desc: 'This will be used for setting the type of compute offering - whether it is fixed, custom constrained or custom unconstrained.',
        externalLink: ''
    }
};
