SET foreign_key_checks = 0;
DROP TABLE IF EXISTS `cloud`.`configuration`;
DROP TABLE IF EXISTS `cloud`.`ip_forwarding`;
DROP TABLE IF EXISTS `cloud`.`management_agent`;
DROP TABLE IF EXISTS `cloud`.`host`;
DROP TABLE IF EXISTS `cloud`.`mshost`;
DROP TABLE IF EXISTS `cloud`.`service_offering`;
DROP TABLE IF EXISTS `cloud`.`user`;
DROP TABLE IF EXISTS `cloud`.`user_ip_address`;
DROP TABLE IF EXISTS `cloud`.`user_statistics`;
DROP TABLE IF EXISTS `cloud`.`vm_template`;
DROP TABLE IF EXISTS `cloud`.`vm_instance`;
DROP TABLE IF EXISTS `cloud`.`domain_router`;
DROP TABLE IF EXISTS `cloud`.`event`;
DROP TABLE IF EXISTS `cloud`.`host_details`;
DROP TABLE IF EXISTS `cloud`.`host_pod_ref`;
DROP TABLE IF EXISTS `cloud`.`host_zone_ref`;
DROP TABLE IF EXISTS `cloud`.`data_ceneter`;
DROP TABLE IF EXISTS `cloud`.`volumes`;
DROP TABLE IF EXISTS `cloud`.`storage`;
DROP TABLE IF EXISTS `cloud`.`disk_template_ref`;
DROP TABLE IF EXISTS `cloud`.`data_center`;
DROP TABLE IF EXISTS `cloud`.`pricing`;
DROP TABLE IF EXISTS `cloud`.`sequence`;
DROP TABLE IF EXISTS `cloud`.`user_vm`;
DROP TABLE IF EXISTS `cloud`.`template_host_ref`;
DROP TABLE IF EXISTS `cloud`.`template_zone_ref`;
DROP TABLE IF EXISTS `cloud`.`ha_work`;
DROP TABLE IF EXISTS `cloud`.`dc_vnet_alloc`;
DROP TABLE IF EXISTS `cloud`.`dc_ip_address_alloc`;
DROP TABLE IF EXISTS `cloud`.`vlan`;
DROP TABLE IF EXISTS `cloud`.`host_vlan_map`;
DROP TABLE IF EXISTS `cloud`.`pod_vlan_map`;
DROP TABLE IF EXISTS `cloud`.`vm_host`;
DROP TABLE IF EXISTS `cloud`.`op_ha_work`;
DROP TABLE IF EXISTS `cloud`.`op_dc_vnet_alloc`;
DROP TABLE IF EXISTS `cloud`.`op_dc_ip_address_alloc`;
DROP TABLE IF EXISTS `cloud`.`op_vm_host`;
DROP TABLE IF EXISTS `cloud`.`op_host_queue`;
DROP TABLE IF EXISTS `cloud`.`console_proxy`;
DROP TABLE IF EXISTS `cloud`.`secondary_storage_vm`;
DROP TABLE IF EXISTS `cloud`.`domain`;
DROP TABLE IF EXISTS `cloud`.`account`;
DROP TABLE IF EXISTS `cloud`.`limit`;
DROP TABLE IF EXISTS `cloud`.`op_host_capacity`;
DROP TABLE IF EXISTS `cloud`.`alert`;
DROP TABLE IF EXISTS `cloud`.`op_lock`;
DROP TABLE IF EXISTS `cloud`.`op_host_upgrade`;
DROP TABLE IF EXISTS `cloud`.`snapshots`;
DROP TABLE IF EXISTS `cloud`.`scheduled_volume_backups`;
DROP TABLE IF EXISTS `cloud`.`vm_disk`;
DROP TABLE IF EXISTS `cloud`.`disk_offering`;
DROP TABLE IF EXISTS `cloud`.`security_group`;
DROP TABLE IF EXISTS `cloud`.`network_rule_config`;
DROP TABLE IF EXISTS `cloud`.`host_details`;
DROP TABLE IF EXISTS `cloud`.`launch_permission`;
DROP TABLE IF EXISTS `cloud`.`resource_limit`;
DROP TABLE IF EXISTS `cloud`.`async_job`;
DROP TABLE IF EXISTS `cloud`.`sync_queue`;
DROP TABLE IF EXISTS `cloud`.`sync_queue_item`;
DROP TABLE IF EXISTS `cloud`.`security_group_vm_map`;
DROP TABLE IF EXISTS `cloud`.`load_balancer_vm_map`;
DROP TABLE IF EXISTS `cloud`.`load_balancer`;
DROP TABLE IF EXISTS `cloud`.`storage_pool`;
DROP TABLE IF EXISTS `cloud`.`storage_pool_host_ref`;
DROP TABLE IF EXISTS `cloud`.`template_spool_ref`;
DROP TABLE IF EXISTS `cloud`.`guest_os`;
DROP TABLE IF EXISTS `cloud`.`snapshot_policy`;
DROP TABLE IF EXISTS `cloud`.`snapshot_policy_ref`;
DROP TABLE IF EXISTS `cloud`.`snapshot_schedule`;
DROP TABLE IF EXISTS `cloud`.`op_pod_vlan_alloc`;
DROP TABLE IF EXISTS `cloud`.`ext_lun_alloc`;
DROP TABLE IF EXISTS `cloud`.`storage_pool_details`;
DROP TABLE IF EXISTS `cloud`.`ext_lun_details`;
DROP TABLE IF EXISTS `cloud`.`cluster`;
/*DROP TABLE IF EXISTS `cloud`.`netapp_storage_pool`;*/

/*
CREATE TABLE `cloud`.`netapp_storage_pool` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `ip_address` varchar(15) NOT NULL COMMENT 'ip address of the pool/volume',
  `aggregate_name` varchar(255) NOT NULL COMMENT 'name for the aggregate',
  `pool_name` varchar(255) NOT NULL COMMENT 'name for the pool/volume',
  `snapshot_policy` varchar(255) NOT NULL COMMENT 'snapshot policy',  
  `pool_size` bigint unsigned COMMENT 'size of the pool',
  `round_robin_marker` bigint unsigned COMMENT 'this is set to 1 to indicate the vol/pool to use for allocation',  
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
*/

CREATE TABLE `cloud`.`cluster` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name for the cluster',
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod id',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ext_lun_alloc` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `size` bigint unsigned NOT NULL COMMENT 'virtual size',
  `portal` varchar(255) NOT NULL COMMENT 'ip or host name to the storage server',
  `target_iqn` varchar(255) NOT NULL COMMENT 'target iqn',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center id this belongs to',
  `lun` int NOT NULL COMMENT 'lun',
  `taken` datetime COMMENT 'time occupied',
  `volume_id` bigint unsigned COMMENT 'vm taking this lun',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ext_lun_details` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `ext_lun_id` bigint unsigned NOT NULL COMMENT 'lun id',
  `tag` varchar(255) COMMENT 'tags associated with this vm',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_host_upgrade` (
  `host_id` bigint unsigned NOT NULL UNIQUE COMMENT 'host id',
  `version` varchar(20) NOT NULL COMMENT 'version',
  `state` varchar(20) NOT NULL COMMENT 'state',
  PRIMARY KEY (`host_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_lock` (
  `key` varchar(128) NOT NULL UNIQUE COMMENT 'primary key of the table',
  `mac` varchar(17) NOT NULL COMMENT 'mac address of who acquired this lock',
  `ip` varchar(15) NOT NULL COMMENT 'ip address of who acquired this lock',
  `thread` varchar(255) NOT NULL COMMENT 'Thread that acquired this lock',
  `acquired_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Time acquired',
  `waiters` int NOT NULL DEFAULT 0 COMMENT 'How many have waited for this',
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`configuration` (
  `category` varchar(255) NOT NULL DEFAULT 'Advanced',
  `instance` varchar(255) NOT NULL,
  `component` varchar(255) NOT NULL DEFAULT 'management-server',
  `name` varchar(255) NOT NULL,
  `value` varchar(4095),
  `description` varchar(1024),
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_ha_work` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs to be ha.',
  `type` varchar(32) NOT NULL COMMENT 'type of work',
  `vm_type` varchar(32) NOT NULL COMMENT 'VM type',
  `state` varchar(32) NOT NULL COMMENT 'state of the vm instance when this happened.',
  `mgmt_server_id` bigint unsigned COMMENT 'management server that has taken up the work of doing ha',
  `host_id` bigint unsigned COMMENT 'host that the vm is suppose to be on',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `tried` int(10) unsigned COMMENT '# of times tried',
  `taken` datetime COMMENT 'time it was taken by the management server',
  `step` varchar(32) NOT NULL COMMENT 'Step in the work',
  `time_to_try` bigint COMMENT 'time to try do this work',
  `updated` bigint unsigned NOT NULL COMMENT 'time the VM state was updated when it was stored into work queue',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`sequence` (
  `name` varchar(64) UNIQUE NOT NULL COMMENT 'name of the sequence',
  `value` bigint unsigned NOT NULL COMMENT 'sequence value',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`sequence` (name, value) VALUES ('vm_instance_seq', 1);
INSERT INTO `cloud`.`sequence` (name, value) VALUES ('vm_template_seq', 200);
INSERT INTO `cloud`.`sequence` (name, value) VALUES ('public_mac_address_seq', 1);
INSERT INTO `cloud`.`sequence` (name, value) VALUES ('private_mac_address_seq', 1);
INSERT INTO `cloud`.`sequence` (name, value) VALUES ('storage_pool_seq', 200);

CREATE TABLE  `cloud`.`disk_template_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `description` varchar(255) NOT NULL,
  `host` varchar(255) NOT NULL COMMENT 'host on which the server exists',
  `parent` varchar(255) NOT NULL COMMENT 'parent path',
  `path` varchar(255) NOT NULL,
  `size` int(10) unsigned NOT NULL COMMENT 'size of the disk',
  `type` varchar(255) NOT NULL COMMENT 'file system type',
  `created` datetime NOT NULL COMMENT 'Date created',
  `removed` datetime COMMENT 'Date removed if not null',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`volumes` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner.  foreign key to account table',
  `domain_id` bigint unsigned NOT NULL COMMENT 'the domain that the owner belongs to',
  `pool_id` bigint unsigned  COMMENT 'pool it belongs to. foreign key to storage_pool table',
  `instance_id` bigint unsigned NULL COMMENT 'vm instance it belongs to. foreign key to vm_instance table',
  `device_id` bigint unsigned NULL COMMENT 'which device inside vm instance it is ',
  `name` varchar(255) COMMENT 'A user specified name for the volume',
  `size` bigint unsigned NOT NULL COMMENT 'total size',
  `folder` varchar(255)  COMMENT 'The folder where the volume is saved',
  `path` varchar(255) COMMENT 'Path',
  `pod_id` bigint unsigned COMMENT 'pod this volume belongs to',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center this volume belongs to',
  `iscsi_name` varchar(255) COMMENT 'iscsi target name',
  `host_ip` varchar(15)  COMMENT 'host ip address for convenience',
  `volume_type` varchar(64) COMMENT 'root, swap or data',
  `resource_type` varchar(64) COMMENT 'pool-based or host-based',
  `pool_type` varchar(64) COMMENT 'type of the pool',
  `mirror_state` varchar(64) COMMENT 'not_mirrored, active or defunct',
  `mirror_vol` bigint unsigned COMMENT 'the other half of the mirrored set if mirrored',
  `disk_offering_id` bigint unsigned NOT NULL COMMENT 'can be null for system VMs',
  `template_id` bigint unsigned COMMENT 'fk to vm_template.id',
  `first_snapshot_backup_uuid` varchar (255) COMMENT 'The first snapshot that was ever taken for this volume',
  `recreatable` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is this volume recreatable?',
  `destroyed` tinyint(1) COMMENT 'indicates whether the volume was destroyed by the user or not',
  `created` datetime COMMENT 'Date Created',
  `updated` datetime COMMENT 'Date updated for attach/detach',
  `removed` datetime COMMENT 'Date removed.  not null if removed',
  `status` varchar(32) COMMENT 'Async API volume creation status',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`snapshots` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner.  foreign key to account table',
  `volume_id` bigint unsigned NOT NULL COMMENT 'volume it belongs to. foreign key to volume table',
  `status` varchar(32) COMMENT 'snapshot creation status',
  `path` varchar(255) COMMENT 'Path',
  `name` varchar(255) NOT NULL COMMENT 'snapshot name',
  `snapshot_type` int(4) NOT NULL COMMENT 'type of snapshot, e.g. manual, recurring',
  `type_description` varchar(25) COMMENT 'description of the type of snapshot, e.g. manual, recurring',
  `created` datetime COMMENT 'Date Created',
  `removed` datetime COMMENT 'Date removed.  not null if removed',
  `backup_snap_id` varchar(255) COMMENT 'Back up uuid of the snapshot',
  `prev_snap_id` bigint unsigned COMMENT 'Id of the most recent snapshot',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vlan` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `vlan_id` varchar(255),
  `vlan_gateway` varchar(255),
  `vlan_netmask` varchar(255),
  `description` varchar(255),
  `vlan_type` varchar(255),
  `data_center_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`pod_vlan_map` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod id. foreign key to pod table',
  `vlan_db_id` bigint unsigned NOT NULL COMMENT 'database id of vlan. foreign key to vlan table',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`account_vlan_map` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `account_id` bigint unsigned NOT NULL COMMENT 'account id. foreign key to account table',
  `vlan_db_id` bigint unsigned NOT NULL COMMENT 'database id of vlan. foreign key to vlan table',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`data_center` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `name` varchar(255),
  `description` varchar(255),
  `dns1` varchar(255) NOT NULL,
  `dns2` varchar(255),
  `internal_dns1` varchar(255) NOT NULL,
  `internal_dns2` varchar(255),
  `gateway` varchar(15),
  `netmask` varchar(15),
  `vnet` varchar(255),
  `router_mac_address` varchar(17) NOT NULL DEFAULT '02:00:00:00:00:01' COMMENT 'mac address for the router within the domain',
  `mac_address` bigint unsigned NOT NULL DEFAULT '1' COMMENT 'Next available mac address for the ethernet card interacting with public internet',
  `guest_network_cidr` varchar(15),
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_dc_ip_address_alloc` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `ip_address` varchar(15) NOT NULL COMMENT 'ip address',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center it belongs to',
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod it belongs to',
  `instance_id` bigint unsigned NULL COMMENT 'instance id',
  `taken` datetime COMMENT 'Date taken',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_dc_link_local_ip_address_alloc` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `ip_address` varchar(15) NOT NULL COMMENT 'ip address',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center it belongs to',
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod it belongs to',
  `instance_id` bigint unsigned NULL COMMENT 'instance id',
  `taken` datetime COMMENT 'Date taken',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`host_pod_ref` (
  `id` bigint unsigned NOT NULL UNIQUE auto_increment,
  `name` varchar(255) NOT NULL,
  `data_center_id` bigint unsigned NOT NULL,
  `gateway` varchar(255) NOT NULL COMMENT 'gateway for the pod',
  `cidr_address` varchar(15) NOT NULL COMMENT 'CIDR address for the pod',
  `cidr_size` bigint unsigned NOT NULL COMMENT 'CIDR size for the pod',
  `description` varchar(255) COMMENT 'store private ip range in startIP-endIP format',
  PRIMARY KEY  (`id`),
  UNIQUE KEY (`name`, `data_center_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_dc_vnet_alloc` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary id',
    `vnet` varchar(18) NOT NULL COMMENT 'vnet',
    `data_center_id` bigint unsigned NOT NULL COMMENT 'data center the vnet belongs to',
    `account_id` bigint unsigned NULL COMMENT 'account the vnet belongs to right now',
    `taken` datetime COMMENT 'Date taken',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`ip_forwarding` (
  `id` bigint unsigned NOT NULL auto_increment,
  `group_id` bigint unsigned default NULL,
  `public_ip_address` varchar(15) NOT NULL,
  `public_port` varchar(10) default NULL,
  `private_ip_address` varchar(15) NOT NULL,
  `private_port` varchar(10) default NULL,
  `enabled` tinyint(1) NOT NULL default '1',
  `protocol` varchar(16) NOT NULL default 'TCP',
  `forwarding` tinyint(1) NOT NULL default '1',
  `algorithm` varchar(255) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`host` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `status` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `private_ip_address` varchar(15) NOT NULL,
  `private_netmask` varchar(15),
  `private_mac_address` varchar(17),
  `storage_ip_address` varchar(15) NOT NULL,
  `storage_netmask` varchar(15),
  `storage_mac_address` varchar(17),
  `storage_ip_address_2` varchar(15),
  `storage_mac_address_2` varchar(17),
  `storage_netmask_2` varchar(15),
  `cluster_id` bigint unsigned COMMENT 'foreign key to cluster',
  `public_ip_address` varchar(15),
  `public_netmask` varchar(15),
  `public_mac_address` varchar(17),
  `proxy_port` int(10) unsigned,
  `data_center_id` bigint unsigned NOT NULL,
  `pod_id` bigint unsigned,
  `cpus` int(10) unsigned,
  `speed` int(10) unsigned,
  `url` varchar(255) COMMENT 'iqn for the servers',
  `fs_type` varchar(32),
  `hypervisor_type` varchar(32) COMMENT 'hypervisor type, can be NONE for storage',
  `ram` bigint unsigned,
  `resource` varchar(255) DEFAULT NULL COMMENT 'If it is a local resource, this is the class name',
  `version` varchar(40) NOT NULL,
  `sequence` bigint unsigned NOT NULL DEFAULT 1,
  `parent` varchar(255) COMMENT 'parent path for the storage server',
  `total_size` bigint unsigned COMMENT 'TotalSize',
  `capabilities` varchar(255) COMMENT 'host capabilities in comma separated list',
  `guid` varchar(255) UNIQUE,
  `available` int(1) unsigned NOT NULL DEFAULT 1 COMMENT 'Is this host ready for more resources?',
  `setup` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is this host already setup?',
  `dom0_memory` bigint unsigned NOT NULL COMMENT 'memory used by dom0 for computing and routing servers',
  `last_ping` int(10) unsigned NOT NULL COMMENT 'time in seconds from the start of machine of the last ping',
  `mgmt_server_id` bigint unsigned COMMENT 'ManagementServer this host is connected to.',
  `disconnected` datetime COMMENT 'Time this was disconnected',
  `created` datetime COMMENT 'date the host first signed on',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`host_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL COMMENT 'host id',
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`mshost` (
  `id` bigint unsigned NOT NULL auto_increment,
  `msid` bigint  NOT NULL UNIQUE COMMENT 'management server id derived from MAC address',
  `name` varchar(255),
  `version` varchar(255),
  `service_ip` varchar(15) NOT NULL,
  `service_port` integer NOT NULL,
  `last_update` DATETIME NULL COMMENT 'Last record update time',
  `removed` datetime COMMENT 'date removed if not null',
  `alert_count` integer NOT NULL DEFAULT 0,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_vm_host` (
  `id` bigint unsigned NOT NULL UNIQUE COMMENT 'foreign key to host_id',
  `vnc_ports` bigint unsigned NOT NULL DEFAULT '0' COMMENT 'vnc ports open on the host',
  `start_at` int(5) unsigned  NOT NULL DEFAULT '0' COMMENT 'Start the vnc port look up at this bit',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`user` (
  `id` bigint unsigned NOT NULL auto_increment,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `firstname` varchar(255) default NULL,
  `lastname` varchar(255) default NULL,
  `email` varchar(255) default NULL,
  `state` varchar(10) NOT NULL default 'enabled',
  `api_key` varchar(255) default NULL,
  `secret_key` varchar(255) default NULL,
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed',
  `timezone` varchar(30) default NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`event` (
  `id` bigint unsigned NOT NULL auto_increment,
  `type` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL DEFAULT 'Completed',
  `description` varchar(1024) NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `created` datetime NOT NULL,
  `level` varchar(16) NOT NULL,
  `start_id` bigint unsigned NOT NULL DEFAULT 0,
  `parameters` varchar(1024) NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`user_ip_address` (
  `account_id` bigint unsigned NULL,
  `domain_id` bigint unsigned NULL,
  `public_ip_address` varchar(15) unique NOT NULL,
  `data_center_id` bigint unsigned NOT NULL COMMENT 'zone that it belongs to',
  `source_nat` int(1) unsigned NOT NULL default '0',
  `allocated` datetime NULL COMMENT 'Date this ip was allocated to someone',
  `vlan_db_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`public_ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`user_statistics` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT,
  `data_center_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `net_bytes_received` bigint unsigned NOT NULL default '0',
  `net_bytes_sent` bigint unsigned NOT NULL default '0',
  `current_bytes_received` bigint unsigned NOT NULL default '0',
  `current_bytes_sent` bigint unsigned NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`vm_template` (
  `id` bigint unsigned NOT NULL auto_increment,
  `unique_name` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `public` int(1) unsigned NOT NULL,
  `featured` int(1) unsigned NOT NULL,
  `type` varchar(32) NULL,
  `hvm`  int(1) unsigned NOT NULL COMMENT 'requires HVM',
  `bits` int(6) unsigned NOT NULL COMMENT '32 bit or 64 bit',
  `url` varchar(255) NULL COMMENT 'the url where the template exists externally',
  `format` varchar(32) NOT NULL COMMENT 'format for the template', 
  `created` datetime NOT NULL COMMENT 'Date created',
  `removed` datetime COMMENT 'Date removed if not null',
  `account_id` bigint unsigned NOT NULL COMMENT 'id of the account that created this template',
  `checksum` varchar(255) COMMENT 'checksum for the template root disk',
  `display_text` varchar(4096) NULL COMMENT 'Description text set by the admin for display purpose only',
  `enable_password` int(1) unsigned NOT NULL default 1 COMMENT 'true if this template supports password reset',
  `guest_os_id` bigint unsigned NOT NULL COMMENT 'the OS of the template',
  `bootable` int(1) unsigned NOT NULL default 1 COMMENT 'true if this template represents a bootable ISO',
  `prepopulate` int(1) unsigned NOT NULL default 0 COMMENT 'prepopulate this template to primary storage',
  `cross_zones` int(1) unsigned NOT NULL default 0 COMMENT 'Make this template available in all zones',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`vm_instance` (
  `id` bigint unsigned UNIQUE NOT NULL,
  `name` varchar(255) NOT NULL,
  `display_name` varchar(255),
  `group` varchar(255),
  `instance_name` varchar(255) NOT NULL COMMENT 'name of the vm instance running on the hosts',
  `state` varchar(32) NOT NULL,
  `vm_template_id` bigint unsigned,
  `iso_id` bigint unsigned,
  `guest_os_id` bigint unsigned NOT NULL,
  `private_mac_address` varchar(17),
  `private_ip_address` varchar(15),
  `private_netmask` varchar(15),
  `pod_id` bigint unsigned NOT NULL,
  `storage_ip` varchar(15),
  `data_center_id` bigint unsigned NOT NULL COMMENT 'Data Center the instance belongs to',
  `host_id` bigint unsigned,
  `last_host_id` bigint unsigned COMMENT 'tentative host for first run or last host that it has been running on',
  `proxy_id` bigint unsigned NULL COMMENT 'console proxy allocated in previous session',
  `proxy_assign_time` DATETIME NULL COMMENT 'time when console proxy was assigned',
  `vnc_password` varchar(255) NOT NULL COMMENT 'vnc password',
  `ha_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Should HA be enabled for this VM',
  `mirrored_vols` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'Are the volumes mirrored',
  `update_count` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'date state was updated',
  `update_time` datetime COMMENT 'date the destroy was requested',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  `type` varchar(32) NOT NULL COMMENT 'type of vm it is',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`pricing` (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `price` FLOAT UNSIGNED NOT NULL,
  `price_unit` VARCHAR(45) NOT NULL,
  `type` VARCHAR(255) NOT NULL,
  `type_id` INTEGER UNSIGNED,
  `created` DATETIME NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`user_vm` (
  `id` bigint unsigned UNIQUE NOT NULL,
  `domain_router_id` bigint unsigned COMMENT 'router id',
  `service_offering_id` bigint unsigned NOT NULL COMMENT 'service offering id',
  `vnet` varchar(18) COMMENT 'vnet',
  `dc_vlan` varchar(18) COMMENT 'zone vlan',
  `account_id` bigint unsigned NOT NULL COMMENT 'user id of owner',
  `domain_id` bigint unsigned NOT NULL,
  `guest_ip_address` varchar(15) COMMENT 'ip address within the guest network',
  `guest_mac_address` varchar(17) COMMENT 'mac address within the guest network',
  `guest_netmask` varchar(15) COMMENT 'netmask within the guest network',
  `external_ip_address` varchar(15)  COMMENT 'ip address within the external network',
  `external_mac_address` varchar(17)  COMMENT 'mac address within the external network',
  `external_vlan_db_id` bigint unsigned  COMMENT 'foreign key into vlan table',
  `user_data` varchar(2048),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`domain_router` (
  `id` bigint unsigned UNIQUE NOT NULL COMMENT 'Primary Key',
  `gateway` varchar(15)  NOT NULL COMMENT 'ip address of the gateway to this domR',
  `ram_size` int(10) unsigned NOT NULL DEFAULT 128 COMMENT 'memory to use in mb',
  `dns1` varchar(15) COMMENT 'dns1',
  `dns2` varchar(15) COMMENT 'dns2',
  `domain` varchar(255) COMMENT 'domain',
  `public_mac_address` varchar(17)   COMMENT 'mac address of the public facing network card',
  `public_ip_address` varchar(15)  COMMENT 'public ip address used for source net',
  `public_netmask` varchar(15)  COMMENT 'netmask used for the domR',
  `guest_mac_address` varchar(17) NOT NULL COMMENT 'mac address of the pod facing network card',
  `guest_dc_mac_address` varchar(17) COMMENT 'mac address of the data center facing network card',
  `guest_netmask` varchar(15) NOT NULL COMMENT 'netmask used for the guest network',
  `guest_ip_address` varchar(15) NOT NULL COMMENT ' ip address in the guest network',   
  `vnet` varchar(18) COMMENT 'vnet',
  `dc_vlan` varchar(18) COMMENT 'vnet',
  `vlan_db_id` bigint unsigned COMMENT 'Foreign key into vlan id table',
  `vlan_id` varchar(255) COMMENT 'optional VLAN ID for DomainRouter that can be used in rundomr.sh',
  `account_id` bigint unsigned NOT NULL COMMENT 'account id of owner',
  `domain_id` bigint unsigned NOT NULL,
  `dhcp_ip_address` bigint unsigned NOT NULL DEFAULT 2 COMMENT 'next ip address for dhcp for this domR',
  `role` varchar(64) NOT NULL COMMENT 'type of role played by this router',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET=utf8 COMMENT = 'information about the domR instance';

CREATE TABLE  `cloud`.`template_host_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL,
  `pool_id` bigint unsigned,
  `template_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `size` bigint unsigned,
  `download_state` varchar(255),
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `url` varchar(255),
  `destroyed` tinyint(1) COMMENT 'indicates whether the template_host entry was destroyed by the user or not',
  `is_copy` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'indicates whether this was copied ',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`template_zone_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `zone_id` bigint unsigned NOT NULL,
  `template_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`console_proxy` (
  `id` bigint unsigned NOT NULL auto_increment,
  `gateway` varchar(15)  COMMENT 'gateway info for this console proxy towards public network interface',
  `dns1` varchar(15) COMMENT 'dns1',
  `dns2` varchar(15) COMMENT 'dns2',
  `domain` varchar(255) COMMENT 'domain',
  `public_mac_address` varchar(17) NOT NULL unique COMMENT 'mac address of the public facing network card',
  `public_ip_address` varchar(15) UNIQUE COMMENT 'public ip address for the console proxy',
  `public_netmask` varchar(15)  COMMENT 'public netmask used for the console proxy',
  `guest_mac_address` varchar(17) NOT NULL unique COMMENT 'mac address of the guest facing network card',
  `guest_ip_address`  varchar(15) UNIQUE COMMENT 'guest ip address for the console proxy',
  `guest_netmask` varchar(15)  COMMENT 'guest netmask used for the console proxy',
  `vlan_db_id` bigint unsigned COMMENT 'Foreign key into vlan id table',
  `vlan_id` varchar(255) COMMENT 'optional VLAN ID for console proxy that can be used',
  `ram_size` int(10) unsigned NOT NULL DEFAULT 512 COMMENT 'memory to use in mb',
  `active_session` int(10) NOT NULL DEFAULT 0 COMMENT 'active session number',
  `last_update` DATETIME NULL COMMENT 'Last session update time',
  `session_details` BLOB NULL COMMENT 'session detail info',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`secondary_storage_vm` (
  `id` bigint unsigned NOT NULL auto_increment,
  `gateway` varchar(15)  COMMENT 'gateway info for this sec storage vm towards public network interface',
  `dns1` varchar(15) COMMENT 'dns1',
  `dns2` varchar(15) COMMENT 'dns2',
  `domain` varchar(255) COMMENT 'domain',
  `public_mac_address` varchar(17) NOT NULL unique COMMENT 'mac address of the public facing network card',
  `public_ip_address` varchar(15) UNIQUE COMMENT 'public ip address for the sec storage vm',
  `public_netmask` varchar(15)  COMMENT 'public netmask used for the sec storage vm',
  `guest_mac_address` varchar(17) NOT NULL unique COMMENT 'mac address of the guest facing network card',
  `guest_ip_address`  varchar(15) UNIQUE COMMENT 'guest ip address for the console proxy',
  `guest_netmask` varchar(15)  COMMENT 'guest netmask used for the console proxy',
  `vlan_db_id` bigint unsigned COMMENT 'Foreign key into vlan id table',
  `vlan_id` varchar(255) COMMENT 'optional VLAN ID for sec storage vm that can be used',
  `ram_size` int(10) unsigned NOT NULL DEFAULT 512 COMMENT 'memory to use in mb',
  `guid` varchar(255) NOT NULL COMMENT 'copied from guid of secondary storage host',
  `nfs_share` varchar(255) NOT NULL COMMENT 'server and path exported by the nfs server ',
  `last_update` DATETIME NULL COMMENT 'Last session update time',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`domain` (
  `id` bigint unsigned NOT NULL auto_increment,
  `parent` bigint unsigned,
  `name` varchar(255),
  `owner` bigint unsigned NOT NULL,
  `path` varchar(255),
  `level` int(10) NOT NULL DEFAULT 0,
  `child_count` int(10) NOT NULL DEFAULT 0,
  `next_child_seq` bigint unsigned NOT NULL DEFAULT 1,
  `removed` datetime COMMENT 'date removed',
  PRIMARY KEY  (`id`),
  UNIQUE (parent, name, removed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`account` (
  `id` bigint unsigned NOT NULL auto_increment,
  `account_name` varchar(100) COMMENT 'an account name set by the creator of the account, defaults to username for single accounts',
  `type` int(1) unsigned NOT NULL,
  `domain_id` bigint unsigned,
  `state` varchar(10) NOT NULL default 'enabled',
  `removed` datetime COMMENT 'date removed',
  `cleanup_needed` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`resource_limit` (
  `id` bigint unsigned NOT NULL auto_increment,
  `domain_id` bigint unsigned,
  `account_id` bigint unsigned,
  `type` varchar(255),
  `max` bigint NOT NULL default '-1',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`resource_count` (
  `id` bigint unsigned NOT NULL auto_increment,
  `account_id` bigint unsigned NOT NULL,
  `type` varchar(255),
  `count` bigint NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_host_capacity` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned,
  `data_center_id` bigint unsigned NOT NULL,
  `pod_id` bigint unsigned,
  `used_capacity` bigint unsigned NOT NULL,
  `total_capacity` bigint unsigned NOT NULL,
  `capacity_type` int(1) unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`alert` (
  `id` bigint unsigned NOT NULL auto_increment,
  `type` int(1) unsigned NOT NULL,
  `pod_id` bigint unsigned,
  `data_center_id` bigint unsigned NOT NULL,
  `subject` varchar(999) COMMENT 'according to SMTP spec, max subject length is 1000 including the CRLF character, so allow enough space to fit long pod/zone/host names',
  `sent_count` int(3) unsigned NOT NULL,
  `created` DATETIME NULL COMMENT 'when this alert type was created',
  `last_sent` DATETIME NULL COMMENT 'Last time the alert was sent',
  `resolved` DATETIME NULL COMMENT 'when the alert status was resolved (available memory no longer at critical level, etc.)',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`async_job` (
  `id` bigint unsigned NOT NULL auto_increment,
  `user_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `session_key` varchar(64) COMMENT 'all async-job manage to apply session based security enforcement',
  `instance_type` varchar(64) COMMENT 'instance_type and instance_id work together to allow attaching an instance object to a job',			
  `instance_id` bigint unsigned,
  `job_cmd` varchar(64) NOT NULL COMMENT 'command name',
  `job_cmd_originator` varchar(64) COMMENT 'command originator',
  `job_cmd_info` text COMMENT 'command parameter info',
  `job_cmd_ver` int(1) COMMENT 'command version',
  `callback_type` int(1) COMMENT 'call back type, 0 : polling, 1 : email',
  `callback_address` varchar(128) COMMENT 'call back address by callback_type',
  `job_status` int(1) COMMENT 'general job execution status',
  `job_process_status` int(1) COMMENT 'job specific process status for asynchronize progress update',
  `job_result_code` int(1) COMMENT 'job result code, specify error code corresponding to result message',
  `job_result` text COMMENT 'job result info',
  `job_init_msid` bigint COMMENT 'the initiating msid',
  `job_complete_msid` bigint  COMMENT 'completing msid',
  `created` datetime COMMENT 'date created',
  `last_updated` datetime COMMENT 'date created',
  `last_polled` datetime COMMENT 'date polled',
  `removed` datetime COMMENT 'date removed',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`sync_queue` (
  `id` bigint unsigned NOT NULL auto_increment,
  `sync_objtype` varchar(64) NOT NULL, 
  `sync_objid` bigint unsigned NOT NULL,
  `queue_proc_msid` bigint,
  `queue_proc_number` bigint COMMENT 'process number, increase 1 for each iteration',
  `queue_proc_time` datetime COMMENT 'last time to process the queue',
  `created` datetime COMMENT 'date created',
  `last_updated` datetime COMMENT 'date created',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`stack_maid` (
  `id` bigint unsigned NOT NULL auto_increment,
  `msid` bigint unsigned NOT NULL,
  `thread_id` bigint unsigned NOT NULL,
  `seq` int unsigned NOT NULL,
  `cleanup_delegate` varchar(128),
  `cleanup_context` text,
  `created` datetime,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`sync_queue_item` (
  `id` bigint unsigned NOT NULL auto_increment,
  `queue_id` bigint unsigned NOT NULL,
  `content_type` varchar(64),
  `content_id` bigint,
  `queue_proc_msid` bigint COMMENT 'owner msid when the queue item is being processed',
  `queue_proc_number` bigint COMMENT 'used to distinguish raw items and items being in process',
  `created` datetime COMMENT 'time created',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vm_disk` (
  `id` bigint unsigned NOT NULL auto_increment,
  `instance_id` bigint unsigned NOT NULL,
  `disk_offering_id` bigint unsigned NOT NULL,
  `removed` datetime COMMENT 'date removed',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`disk_offering` (
  `id` bigint unsigned NOT NULL auto_increment,
  `domain_id` bigint unsigned,
  `name` varchar(255) NOT NULL,
  `display_text` varchar(4096) NULL COMMENT 'Description text set by the admin for display purpose only',
  `disk_size` bigint unsigned NOT NULL COMMENT 'disk space in mbs',
  `mirrored` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'Enable mirroring?',
  `type` varchar(32) COMMENT 'inheritted by who?',
  `tags` varchar(4096) COMMENT 'comma separated tags about the disk_offering',
  `recreatable` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'The root disk is always recreatable',
  `use_local_storage` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Indicates whether local storage pools should be used',
  `unique_name` varchar(32) UNIQUE COMMENT 'unique name',
  `removed` datetime COMMENT 'date removed',
  `created` datetime COMMENT 'date the disk offering was created',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`service_offering` (
  `id` bigint unsigned NOT NULL,
  `cpu` int(10) unsigned NOT NULL COMMENT '# of cores',
  `speed` int(10) unsigned NOT NULL COMMENT 'speed per core in mhz',
  `ram_size` bigint unsigned NOT NULL,
  `nw_rate` smallint unsigned default 200 COMMENT 'network rate throttle mbits/s',
  `mc_rate` smallint unsigned default 10 COMMENT 'mcast rate throttle mbits/s',
  `ha_enabled` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Enable HA',
  `guest_ip_type` varchar(255) NOT NULL DEFAULT 'Virtualized' COMMENT 'Type of guest network -- direct or virtualized',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`security_group` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) NULL,
  `domain_id` bigint unsigned NULL,
  `account_id` bigint unsigned NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_rule_config` (
  `id` bigint unsigned NOT NULL auto_increment,
  `security_group_id` bigint unsigned NOT NULL,
  `public_port` varchar(10) default NULL,
  `private_port` varchar(10) default NULL,
  `protocol` varchar(16) NOT NULL default 'TCP',
  `create_status` varchar(32) COMMENT 'rule creation status',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`security_group_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `security_group_id` bigint unsigned NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`load_balancer_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `load_balancer_id` bigint unsigned NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  `pending` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'whether the vm is being applied to the load balancer (pending=1) or has already been applied (pending=0)',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`load_balancer` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) NULL,
  `account_id` bigint unsigned NOT NULL,
  `ip_address` varchar(15) NOT NULL,
  `public_port` varchar(10) NOT NULL,
  `private_port` varchar(10) NOT NULL,
  `algorithm` varchar(255) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`storage_pool` (
  `id` bigint unsigned UNIQUE NOT NULL,
  `name` varchar(255) COMMENT 'should be NOT NULL',
  `uuid` varchar(255) UNIQUE,
  `pool_type` varchar(32) NOT NULL,
  `port` int unsigned NOT NULL,
  `data_center_id` bigint unsigned NOT NULL,
  `pod_id` bigint unsigned,
  `cluster_id` bigint unsigned COMMENT 'foreign key to cluster',
  `available_bytes` bigint unsigned,
  `capacity_bytes` bigint unsigned,
  `host_address` varchar(255) NOT NULL COMMENT 'FQDN or IP of storage server',
  `path` varchar(255) NOT NULL COMMENT 'Filesystem path that is shared',
  `created` datetime COMMENT 'date the pool created',
  `removed` datetime COMMENT 'date removed if not null',
  `update_time` DATETIME,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`storage_pool_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `pool_id` bigint unsigned NOT NULL COMMENT 'pool the detail is related to',
  `name` varchar(255) NOT NULL COMMENT 'name of the detail',
  `value` varchar(255) NOT NULL COMMENT 'value of the detail',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`storage_pool_host_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL,
  `pool_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `local_path` varchar(255),
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`template_spool_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `pool_id` bigint unsigned NOT NULL,
  `template_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `download_state` varchar(255),
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `template_size` bigint unsigned NOT NULL COMMENT 'the size of the template on the pool',
  `marked_for_gc` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'if true, the garbage collector will evict the template from this pool.',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`guest_os` (
  `id` bigint unsigned NOT NULL auto_increment,
  `category_id` bigint unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `display_name` varchar(255) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`guest_os_category` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`launch_permission` (
  `id` bigint unsigned NOT NULL auto_increment,
  `template_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`snapshot_policy` (
  `id` bigint unsigned NOT NULL auto_increment,
  `volume_id` bigint unsigned NOT NULL,
  `schedule` varchar(100) NOT NULL COMMENT 'schedule time of execution',
  `timezone` varchar(100) NOT NULL COMMENT 'the timezone in which the schedule time is specified',
  `interval` int(4) NOT NULL default 4 COMMENT 'backup schedule, e.g. hourly, daily, etc.',
  `max_snaps` int(8) NOT NULL default 0 COMMENT 'maximum number of snapshots to maintain',
  `active` tinyint(1) unsigned NOT NULL COMMENT 'Is the policy active',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`snapshot_policy_ref` (
  `snap_id` bigint unsigned NOT NULL,
  `volume_id` bigint unsigned NOT NULL,
  `policy_id` bigint unsigned NOT NULL,
  UNIQUE (snap_id, policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`snapshot_schedule` (
  `id` bigint unsigned NOT NULL auto_increment,
  `volume_id` bigint unsigned NOT NULL COMMENT 'The volume for which this snapshot is being taken',
  `policy_id` bigint unsigned NOT NULL COMMENT 'One of the policyIds for which this snapshot was taken',
  `scheduled_timestamp` datetime NOT NULL COMMENT 'Time at which the snapshot was scheduled for execution',
  `async_job_id` bigint unsigned COMMENT 'If this schedule is being executed, it is the id of the create aysnc_job. Before that it is null',
  `snapshot_id` bigint unsigned COMMENT 'If this schedule is being executed, then the corresponding snapshot has this id. Before that it is null',
  UNIQUE (volume_id, policy_id),
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_pod_vlan_alloc` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary id',
    `vlan` varchar(18) NOT NULL COMMENT 'vlan id',
    `data_center_id` bigint unsigned NOT NULL COMMENT 'data center the pod belongs to',
    `pod_id` bigint unsigned NOT NULL COMMENT 'pod the vlan belongs to',
    `account_id` bigint unsigned NULL COMMENT 'account the vlan belongs to right now',
    `taken` datetime COMMENT 'Date taken',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_group` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `account_name` varchar(100) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_ingress_rule` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_group_id` bigint unsigned NOT NULL,
  `start_port` varchar(10) default NULL,
  `end_port` varchar(10) default NULL,
  `protocol` varchar(16) NOT NULL default 'TCP',
  `allowed_network_id` bigint unsigned,
  `allowed_network_group` varchar(255) COMMENT 'data duplicated from network_group table to avoid lots of joins when listing rules (the name of the group should be displayed rather than just id)',
  `allowed_net_grp_acct` varchar(100) COMMENT 'data duplicated from network_group table to avoid lots of joins when listing rules (the name of the group owner should be displayed)',
  `allowed_ip_cidr`  varchar(44),
  `create_status` varchar(32) COMMENT 'rule creation status',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_group_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_group_id` bigint unsigned NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_nwgrp_work` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs rules to be synced.',
  `mgmt_server_id` bigint unsigned COMMENT 'management server that has taken up the work of doing rule sync',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `taken` datetime COMMENT 'time it was taken by the management server',
  `step` varchar(32) NOT NULL COMMENT 'Step in the work',
  `seq_no` bigint unsigned  COMMENT 'seq number to be sent to agent, uniquely identifies ruleset update',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_vm_ruleset_log` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs rules to be synced.',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `logsequence` bigint unsigned  COMMENT 'seq number to be sent to agent, uniquely identifies ruleset update',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET foreign_key_checks = 1;
