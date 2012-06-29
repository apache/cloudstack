-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
-- 
--   http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--;
-- Schema upgrade from 2.1 to 2.2;
--;
ALTER TABLE `cloud`.`cluster` ADD COLUMN `guid` varchar(255) UNIQUE DEFAULT NULL;
ALTER TABLE `cloud`.`cluster` ADD COLUMN `cluster_type` varchar(64) DEFAULT 'CloudManaged';
ALTER TABLE `cloud`.`vm_template` ADD COLUMN `hypervisor_type` varchar(32) COMMENT 'hypervisor that the template is belonged to';
ALTER TABLE `cloud`.`vm_template` ADD COLUMN `extractable` int(1) unsigned NOT NULL default 0 COMMENT 'Is this template extractable';
ALTER TABLE `cloud`.`template_spool_ref` ADD CONSTRAINT `fk_template_spool_ref__template_id` FOREIGN KEY (`template_id`) REFERENCES `vm_template`(`id`);    

ALTER TABLE `cloud`.`guest_os` modify `name` varchar(255) ;

-- NOTE for tables below;
-- these 2 tables were used in 2.1, but are not in 2.2;
-- we will need a migration script for these tables when the migration is written;
-- furthermore we have renamed the following in 2.2;
-- network_group table --> security_group table;
-- network_group_vm_map table --> security_group_vm_map table;
DROP TABLE `cloud`.`security_group`;
DROP TABLE `cloud`.`security_group_vm_map`;
-- needs a migration step;
#ALTER TABLE `cloud`.`account` DROP COLUMN `network_domain`;

-- New migration from 2.1 to 2.2;

-- Easy stuff first.  All new tables.;

CREATE TABLE IF NOT EXISTS `cloud`.`version` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `version` char(40) NOT NULL UNIQUE COMMENT 'version',
  `updated` datetime NOT NULL COMMENT 'Date this version table was updated',
  `step` char(32) NOT NULL COMMENT 'Step in the upgrade to this version',
  PRIMARY KEY (`id`),
  INDEX `i_version__version`(`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`mshost` DROP KEY `msid`;
ALTER TABLE `cloud`.`mshost` MODIFY COLUMN `msid` bigint unsigned NOT NULL UNiQUE;

CREATE TABLE `cloud`.`op_it_work` (
  `id` char(40) COMMENT 'reservation id',
  `mgmt_server_id` bigint unsigned COMMENT 'management server id',
  `created_at` bigint unsigned NOT NULL COMMENT 'when was this work detail created',
  `thread` varchar(255) NOT NULL COMMENT 'thread name',
  `type` char(32) NOT NULL COMMENT 'type of work',
  `step` char(32) NOT NULL COMMENT 'state',
  `updated_at` bigint unsigned NOT NULL COMMENT 'time it was taken over',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance',
  `resource_type` char(32) COMMENT 'type of resource being worked on',
  `resource_id` bigint unsigned COMMENT 'resource id being worked on',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_op_it_work__mgmt_server_id` FOREIGN KEY (`mgmt_server_id`) REFERENCES `mshost`(`msid`),
  CONSTRAINT `fk_op_it_work__instance_id` FOREIGN KEY (`instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
  INDEX `i_op_it_work__step`(`step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_offerings` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `name` varchar(64) NOT NULL unique COMMENT 'network offering',
  `display_text` varchar(255) NOT NULL COMMENT 'text to display to users',
  `nw_rate` smallint unsigned COMMENT 'network rate throttle mbits/s',
  `mc_rate` smallint unsigned COMMENT 'mcast rate throttle mbits/s',
  `concurrent_connections` int(10) unsigned COMMENT 'concurrent connections supported on this network',
  `traffic_type` varchar(32) NOT NULL COMMENT 'traffic type carried on this network',
  `tags` varchar(4096) COMMENT 'tags supported by this offering',
  `system_only` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is this network offering for system use only',
  `specify_vlan` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Should the user specify vlan',
  `service_offering_id` bigint unsigned UNIQUE COMMENT 'service offering id that this network offering is tied to',
  `created` datetime NOT NULL COMMENT 'time the entry was created',
  `removed` datetime DEFAULT NULL COMMENT 'time the entry was removed',
  `default` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if network offering is default',
  `availability` varchar(255) NOT NULL COMMENT 'availability of the network',
  `dns_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if network offering provides dns service',
  `gateway_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if network offering provides gateway service',
  `firewall_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if network offering provides firewall service',
  `lb_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if network offering provides lb service',
  `userdata_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if network offering provides user data service',
  `vpn_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if network offering provides vpn service',
  `dhcp_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if network offering provides dhcp service',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`networks` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) COMMENT 'name for this network',
  `display_text` varchar(255) COMMENT 'display text for this network',
  `traffic_type` varchar(32) NOT NULL COMMENT 'type of traffic going through this network',
  `broadcast_domain_type` varchar(32) NOT NULL COMMENT 'type of broadcast domain used',
  `broadcast_uri` varchar(255) COMMENT 'broadcast domain specifier',
  `gateway` varchar(15) COMMENT 'gateway for this network configuration',
  `cidr` varchar(18) COMMENT 'network cidr', 
  `mode` varchar(32) COMMENT 'How to retrieve ip address in this network',
  `network_offering_id` bigint unsigned NOT NULL COMMENT 'network offering id that this configuration is created from',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center id that this configuration is used in',
  `guru_name` varchar(255) NOT NULL COMMENT 'who is responsible for this type of network configuration',
  `state` varchar(32) NOT NULL COMMENT 'what state is this configuration in',
  `related` bigint unsigned NOT NULL COMMENT 'related to what other network configuration',
  `domain_id` bigint unsigned NOT NULL COMMENT 'foreign key to domain id',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this network',
  `dns1` varchar(255) COMMENT 'comma separated DNS list',
  `dns2` varchar(255) COMMENT 'comma separated DNS list',
  `guru_data` varchar(1024) COMMENT 'data stored by the network guru that setup this network',
  `set_fields` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'which fields are set already',
  `guest_type` char(32) COMMENT 'type of guest network',
  `shared` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '0 if network is shared, 1 if network dedicated',
  `network_domain` varchar(255) COMMENT 'domain',
  `reservation_id` char(40) COMMENT 'reservation id',
  `is_default` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if network is default',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_networks`(
  `id` bigint unsigned NOT NULL UNIQUE KEY,
  `mac_address_seq` bigint unsigned NOT NULL DEFAULT 1 COMMENT 'mac address',
  `nics_count` int unsigned NOT NULL DEFAULT 0 COMMENT '# of nics',
  `gc` tinyint unsigned NOT NULL DEFAULT 1 COMMENT 'gc this network or not',
  `check_for_gc` tinyint unsigned NOT NULL DEFAULT 1 COMMENT 'check this network for gc or not',
  PRIMARY KEY(`id`),
  CONSTRAINT `fk_op_networks__id` FOREIGN KEY (`id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`account_network_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `account_id` bigint unsigned NOT NULL COMMENT 'account id',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  `is_owner` smallint(1) NOT NULL COMMENT 'is the owner of the network',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_account_network_ref__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_account_network_ref__networks_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`certificate` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `certificate` text COMMENT 'the actual custom certificate being stored in the db',
  `updated` varchar(1) COMMENT 'status of the certificate',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`certificate` (id,certificate,updated) VALUES ('1',null,'N');

CREATE TABLE `cloud`.`nics` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned COMMENT 'vm instance id',
  `mac_address` varchar(17) COMMENT 'mac address',
  `ip4_address` varchar(15) COMMENT 'ip4 address',
  `netmask` varchar(15) COMMENT 'netmask for ip4 address',
  `gateway` varchar(15) COMMENT 'gateway',
  `ip_type` varchar(32) COMMENT 'type of ip',
  `broadcast_uri` varchar(255) COMMENT 'broadcast uri',
  `network_id` bigint unsigned NOT NULL COMMENT 'network configuration id',
  `mode` varchar(32) COMMENT 'mode of getting ip address',  
  `state` varchar(32) NOT NULL COMMENT 'state of the creation',
  `strategy` varchar(32) NOT NULL COMMENT 'reservation strategy',
  `reserver_name` varchar(255) COMMENT 'Name of the component that reserved the ip address',
  `reservation_id` varchar(64) COMMENT 'id for the reservation',
  `device_id` int(10) COMMENT 'device id for the network when plugged into the virtual machine',
  `update_time` timestamp NOT NULL COMMENT 'time the state was changed',
  `isolation_uri` varchar(255) COMMENT 'id for isolation',
  `ip6_address` char(40) COMMENT 'ip6 address',
  `default_nic` tinyint NOT NULL COMMENT "None", 
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_nics__instance_id` FOREIGN KEY `fk_nics__instance_id`(`instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nics__networks_id` FOREIGN KEY `fk_nics__networks_id`(`network_id`) REFERENCES `networks`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `network_offerings` VALUES (1,'System-Public-Network','System Offering for System-Public-Network',NULL,NULL,NULL,'Public',NULL,1,0,NULL,now(),NULL,0,'Required',0,0,0,0,0,0,0),(2,'System-Management-Network','System Offering for System-Management-Network',NULL,NULL,NULL,'Management',NULL,1,0,NULL,now(),NULL,0,'Required',0,0,0,0,0,0,0),(3,'System-Control-Network','System Offering for System-Control-Network',NULL,NULL,NULL,'Control',NULL,1,0,NULL,now(),NULL,0,'Required',0,0,0,0,0,0,0),(4,'System-Storage-Network','System Offering for System-Storage-Network',NULL,NULL,NULL,'Storage',NULL,1,0,NULL,now(),NULL,0,'Required',0,0,0,0,0,0,0),(5,'System-Guest-Network','System-Guest-Network',NULL,NULL,NULL,'Guest',NULL,1,0,NULL,now(),NULL,1,'Required',1,0,0,0,1,0,1),(6,'DefaultVirtualizedNetworkOffering','Virtual Vlan',NULL,NULL,NULL,'Guest',NULL,0,0,NULL,now(),NULL,1,'Required',1,1,1,1,1,1,1),(7,'DefaultDirectNetworkOffering','Direct',NULL,NULL,NULL,'Public',NULL,0,0,NULL,now(),NULL,1,'Required',1,0,0,0,1,0,1);

CREATE TABLE `cloud`.`cluster_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `cluster_id` bigint unsigned NOT NULL COMMENT 'cluster id',
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_cluster_details__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `id` bigint unsigned NOT NULL UNIQUE auto_increment;
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `one_to_one_nat` int(1) unsigned NOT NULL default '0';
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `vm_id` bigint unsigned DEFAULT NULL;
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `state` char(32) NOT NULL DEFAULT 'Free';
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `mac_address` bigint unsigned NOT NULL;
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `source_network_id` bigint unsigned NOT NULL;
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `network_id` bigint unsigned;

UPDATE `cloud`.`user_ip_address` set state='Allocated' WHERE allocated IS NOT NULL;

CREATE TABLE `cloud`.`firewall_rules` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `ip_address_id` bigint unsigned NOT NULL COMMENT 'id of the corresponding ip address',
  `start_port` int(10) NOT NULL COMMENT 'starting port of a port range',
  `end_port` int(10) NOT NULL COMMENT 'end port of a port range',
  `state` char(32) NOT NULL COMMENT 'current state of this rule',
  `protocol` char(16) NOT NULL default 'TCP' COMMENT 'protocol to open these ports for',
  `purpose` char(32) NOT NULL COMMENT 'why are these ports opened?',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  `xid` char(40) NOT NULL COMMENT 'external id',
  `is_static_nat` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if firewall rule is one to one nat rule',
  `created` datetime COMMENT 'Date created',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_firewall_rules__ip_address_id` FOREIGN KEY(`ip_address_id`) REFERENCES `user_ip_address`(`id`),
  CONSTRAINT `fk_firewall_rules__network_id` FOREIGN KEY(`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_firewall_rules__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_firewall_rules__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`port_forwarding_rules` (
  `id` bigint unsigned NOT NULL COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance id',
  `dest_ip_address` char(40) NOT NULL COMMENT 'id_address',
  `dest_port_start` int(10) NOT NULL COMMENT 'starting port of the port range to map to',
  `dest_port_end` int(10) NOT NULL COMMENT 'end port of the the port range to map to',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_port_forwarding_rules__id` FOREIGN KEY(`id`) REFERENCES `firewall_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`load_balancing_rules` (
  `id` bigint unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) NULL COMMENT 'description',
  `default_port_start` int(10) NOT NULL COMMENT 'default private port range start',
  `default_port_end` int(10) NOT NULL COMMENT 'default destination port range end',
  `algorithm` varchar(255) NOT NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_load_balancing_rules__id` FOREIGN KEY(`id`) REFERENCES `firewall_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`load_balancer_vm_map` ADD COLUMN `revoke` tinyint(1) unsigned NOT NULL DEFAULT 0;

CREATE TABLE `cloud`.`op_host` (
  `id` bigint unsigned NOT NULL UNIQUE COMMENT 'host id',
  `sequence` bigint unsigned DEFAULT 1 NOT NULL COMMENT 'sequence for the host communication',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_op_host__id` FOREIGN KEY (`id`) REFERENCES `host`(`id`) ON DELETE CASCADE 
) ENGINE = InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`guest_os_hypervisor` (
  `id` bigint unsigned NOT NULL auto_increment,
  `hypervisor_type` varchar(32) NOT NULL,
  `guest_os_name` varchar(255) NOT NULL,
  `guest_os_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`) 
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

INSERT INTO op_host(id, sequence) select id, sequence from host;

-- Alter Tables to add Columns;

ALTER TABLE `cloud`.`cluster` ADD COLUMN `hypervisor_type` varchar(32);
UPDATE `cloud`.`cluster` SET hypervisor_type=(SELECT DISTINCT host.hypervisor_type from host where host.cluster_id = cluster.id GROUP BY host.hypervisor_type); 

ALTER TABLE `cloud`.`volumes` ADD COLUMN `attached` datetime;
UPDATE `cloud`.`volumes` SET attached=now() WHERE removed IS NULL AND instance_id IS NOT NULL;

ALTER TABLE `cloud`.`volumes` ADD COLUMN `chain_info` text;

ALTER TABLE `cloud`.`volumes` MODIFY COLUMN `volume_type` VARCHAR(64) NOT NULL;

ALTER TABLE `cloud`.`volumes` ADD COLUMN `state` VARCHAR(32);
UPDATE `cloud`.`volumes` SET state='Destroy' WHERE removed IS NOT NULL OR destroyed=1 OR status='Creating' OR status='Corrupted' OR status='Failed';
UPDATE `cloud`.`volumes` SET state='Ready' WHERE removed IS NULL AND (destroyed=0 OR destroyed is NULL) AND status='Created';

ALTER TABLE `cloud`.`vlan` ADD COLUMN `network_id` bigint unsigned NOT NULL;

ALTER TABLE `cloud`.`data_center` ADD COLUMN `domain` varchar(100);
ALTER TABLE `cloud`.`data_center` ADD COLUMN `domain_id` bigint unsigned;
ALTER TABLE `cloud`.`data_center` ADD COLUMN `networktype` varchar(255) NOT NULL DEFAULT 'Basic'; 
ALTER TABLE `cloud`.`data_center` ADD COLUMN `dns_provider` char(64) DEFAULT 'VirtualRouter';
ALTER TABLE `cloud`.`data_center` ADD COLUMN `gateway_provider` char(64) DEFAULT 'VirtualRouter';
ALTER TABLE `cloud`.`data_center` ADD COLUMN `firewall_provider` char(64) DEFAULT 'VirtualRouter';
ALTER TABLE `cloud`.`data_center` ADD COLUMN `dhcp_provider` char(64) DEFAULT 'VirtualRouter';
ALTER TABLE `cloud`.`data_center` ADD COLUMN `lb_provider` char(64) DEFAULT 'VirtualRouter';
ALTER TABLE `cloud`.`data_center` ADD COLUMN `vpn_provider` char(64) DEFAULT 'VirtualRouter';
ALTER TABLE `cloud`.`data_center` ADD COLUMN `userdata_provider` char(64) DEFAULT 'VirtualRouter';
ALTER TABLE `cloud`.`data_center` ADD COLUMN `enable` tinyint NOT NULL DEFAULT 1;

ALTER TABLE `cloud`.`op_dc_ip_address_alloc` ADD COLUMN `reservation_id` char(40) NULL;
ALTER TABLE `cloud`.`op_dc_ip_address_alloc` ADD COLUMN `mac_address` bigint unsigned NOT NULL;
UPDATE `cloud`.`op_dc_ip_address_alloc` SET reservation_id=concat(cast(instance_id as CHAR), ip_address) WHERE taken is NOT NULL;

ALTER TABLE `cloud`.`op_dc_link_local_ip_address_alloc` ADD COLUMN `reservation_id` char(40) NULL;
UPDATE `cloud`.`op_dc_link_local_ip_address_alloc` SET reservation_id=concat(cast(instance_id as CHAR),ip_address) WHERE taken is NOT NULL;

ALTER TABLE `cloud`.`host_pod_ref` ADD COLUMN `enabled` tinyint NOT NULL DEFAULT 1;

ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD COLUMN `reservation_id` char(40) NULL;
UPDATE op_dc_vnet_alloc set reservation_id=concat(cast(data_center_id as CHAR), concat("-", vnet)) WHERE taken is NOT NULL; 

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `service_offering_id` bigint unsigned NOT NULL;
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `reservation_id` char(40);
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `hypervisor_type` char(32);
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `account_id` bigint unsigned NOT NULL;
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `domain_id` bigint unsigned NOT NULL;

UPDATE vm_instance inner join user_vm on user_vm.id=vm_instance.id set vm_instance.service_offering_id=user_vm.service_offering_id, vm_instance.account_id=user_vm.account_id, vm_instance.domain_id=user_vm.domain_id where vm_instance.id = user_vm.id and vm_instance.type='User';
UPDATE vm_instance inner join domain_router on vm_instance.id=domain_router.id set vm_instance.account_id=domain_router.account_id, vm_instance.domain_id=domain_router.domain_id where vm_instance.type='DomainRouter';
UPDATE vm_instance SET service_offering_id=(SELECT id FROM disk_offering WHERE unique_name='Cloud.Com-SoftwareRouter') WHERE type='DomainRouter';
UPDATE vm_instance SET service_offering_id=(SELECT id FROM disk_offering WHERE unique_name='Cloud.Com-ConsoleProxy') WHERE type='ConsoleProxy';
UPDATE vm_instance SET service_offering_id=(SELECT id FROM disk_offering WHERE unique_name='Cloud.Com-SecondaryStorage') WHERE type='SecondaryStorageVm';

ALTER TABLE `cloud`.`user_vm` ADD COLUMN `iso_id` bigint unsigned;
ALTER TABLE `cloud`.`user_vm` ADD COLUMN `display_name` varchar(255);

UPDATE user_vm inner join vm_instance on user_vm.id=vm_instance.id set user_vm.iso_id=vm_instance.iso_id, user_vm.display_name=vm_instance.display_name where vm_instance.type='User';

ALTER TABLE `cloud`.`template_host_ref` ADD COLUMN `physical_size` bigint unsigned DEFAULT 0;
UPDATE template_host_ref INNER JOIN template_spool_ref ON template_host_ref.template_id=template_spool_ref.template_id SET template_host_ref.physical_size=template_spool_ref.template_size;  


CREATE TABLE `cloud`.`user_vm_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_vm_details__vm_id` FOREIGN KEY `fk_user_vm_details__vm_id`(`vm_id`) REFERENCES `user_vm`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`domain_router` MODIFY COLUMN `guest_netmask` varchar(15);
ALTER TABLE `cloud`.`domain_router` MODIFY COLUMN `guest_ip_address` varchar(15);
ALTER TABLE `cloud`.`domain_router` ADD COLUMN `network_id` bigint unsigned NOT NULL;

CREATE TABLE  `cloud`.`upload` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL,
  `type_id` bigint unsigned NOT NULL,
  `type` varchar(255),
  `mode` varchar(255),
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `upload_pct` int(10) unsigned,
  `upload_state` varchar(255),
  `error_str` varchar(255),
  `url` varchar(255),
  `install_path` varchar(255),
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_upload__host_id` FOREIGN KEY `fk_upload__host_id` (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
  INDEX `i_upload__host_id`(`host_id`),
  INDEX `i_upload__type_id`(`type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`console_proxy` MODIFY COLUMN `public_mac_address` varchar(17);

ALTER TABLE `cloud`.`secondary_storage_vm` MODIFY COLUMN `public_mac_address` varchar(17);
ALTER TABLE `cloud`.`secondary_storage_vm` MODIFY COLUMN `nfs_share` varchar(255);

ALTER TABLE `cloud`.`resource_count` ADD COLUMN `domain_id` bigint unsigned;
ALTER TABLE `cloud`.`resource_count` MODIFY COLUMN `account_id` bigint unsigned;

ALTER TABLE `cloud`.`op_host_capacity` ADD COLUMN `reserved_capacity` bigint unsigned NOT NULL;

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `system_use` tinyint(1) unsigned NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `customized` tinyint(1) unsigned NOT NULL DEFAULT 0;

update `cloud`.`disk_offering` set system_use=1, removed=null WHERE unique_name like 'Cloud.Com-%';
update `cloud`.`disk_offering` set name='System Offering For Console Proxy' where name='Fake Offering For DomP' and system_use=1;
update `cloud`.`service_offering` set ram_size=1024 where id=(SELECT id from disk_offering where name='System Offering For Console Proxy' and system_use=1);
update `cloud`.`disk_offering` set name='System Offering For Software Router' where name='Fake Offering For DomR' and system_use=1;
update `cloud`.`disk_offering` set name='System Offering For Secondary Storage VM' where name='Fake Offering For Secondary Storage VM' and system_use=1;

ALTER TABLE `cloud`.`user_statistics` ADD COLUMN `public_ip_address` varchar(15) DEFAULT NULL;
ALTER TABLE `cloud`.`user_statistics` ADD COLUMN `device_id` bigint unsigned NOT NULL default 0;
ALTER TABLE `cloud`.`user_statistics` ADD COLUMN `device_type` varchar(32) NOT NULL default 'DomainRouter';

CREATE TABLE `cloud`.`remote_access_vpn` (
  `vpn_server_addr_id` bigint unsigned UNIQUE NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `network_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `local_ip` varchar(15) NOT NULL,
  `ip_range` varchar(32) NOT NULL,
  `ipsec_psk` varchar(256) NOT NULL,
  `state` char(32) NOT NULL,
  PRIMARY KEY  (`vpn_server_addr_id`),
  CONSTRAINT `fk_remote_access_vpn__account_id` FOREIGN KEY `fk_remote_access_vpn__account_id`(`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_remote_access_vpn__domain_id` FOREIGN KEY `fk_remote_access_vpn__domain_id`(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_remote_access_vpn__network_id` FOREIGN KEY `fk_remote_access_vpn__network_id` (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_remote_access_vpn__vpn_server_addr_id` FOREIGN KEY `fk_remote_access_vpn__vpn_server_addr_id` (`vpn_server_addr_id`) REFERENCES `user_ip_address` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vpn_users` (
  `id` bigint unsigned NOT NULL UNIQUE auto_increment,
  `owner_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `state` char(32) NOT NULL COMMENT 'What state is this vpn user in',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_vpn_users__owner_id` FOREIGN KEY (`owner_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vpn_users__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  INDEX `i_vpn_users_username`(`username`),
  UNIQUE `i_vpn_users__account_id__username`(`owner_id`, `username`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`storage_pool` ADD COLUMN `status` varchar(32);

--drop network group related constraints/indexes;
ALTER TABLE `cloud`.`network_group` DROP foreign key `fk_network_group__domain_id`;
ALTER TABLE `cloud`.`network_group` DROP INDEX `fk_network_group__domain_id`;
ALTER TABLE `cloud`.`network_group` DROP foreign key `fk_network_group___account_id`;

ALTER TABLE `cloud`.`network_group` drop index `i_network_group_name`;

ALTER TABLE `cloud`.`network_ingress_rule` drop foreign key `fk_network_ingress_rule___network_group_id`;
ALTER TABLE `cloud`.`network_ingress_rule` drop foreign key `fk_network_ingress_rule___allowed_network_id`;
ALTER TABLE `cloud`.`network_ingress_rule` drop index `i_network_ingress_rule_network_id`;
ALTER TABLE `cloud`.`network_ingress_rule` drop index `i_network_ingress_rule_allowed_network`;

ALTER TABLE `cloud`.`network_group_vm_map` drop foreign key `fk_network_group_vm_map___network_group_id`;
ALTER TABLE `cloud`.`network_group_vm_map` drop foreign key `fk_network_group_vm_map___instance_id`;

ALTER TABLE `cloud`.`network_group` RENAME TO `security_group`;
ALTER TABLE `cloud`.`network_ingress_rule` RENAME TO `security_ingress_rule`;
ALTER TABLE `cloud`.`security_ingress_rule` CHANGE COLUMN `network_group_id` `security_group_id` bigint unsigned NOT NULL;
ALTER TABLE `cloud`.`security_ingress_rule` CHANGE COLUMN `allowed_network_group` `allowed_security_group` varchar(255);
ALTER TABLE `cloud`.`security_ingress_rule` CHANGE COLUMN `allowed_net_grp_acct` `allowed_sec_grp_acct` varchar(100);
ALTER TABLE `cloud`.`network_group_vm_map` RENAME TO `security_group_vm_map`;
ALTER TABLE `cloud`.`security_group_vm_map` CHANGE COLUMN `network_group_id` `security_group_id` bigint unsigned NOT NULL;

ALTER TABLE `cloud`.`security_group` ADD CONSTRAINT `fk_security_group__domain_id` FOREIGN KEY `fk_security_group__domain_id` (`domain_id`) REFERENCES `domain` (`id`);
ALTER TABLE `cloud`.`security_group` ADD INDEX `i_security_group_name`(`name`);

ALTER TABLE `cloud`.`security_ingress_rule` ADD CONSTRAINT `fk_security_ingress_rule___security_group_id` FOREIGN KEY `fk_security_ingress_rule__security_group_id` (`security_group_id`) REFERENCES `security_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`security_ingress_rule` ADD CONSTRAINT `fk_security_ingress_rule___allowed_network_id` FOREIGN KEY `fk_security_ingress_rule__allowed_network_id` (`allowed_network_id`) REFERENCES `security_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`security_ingress_rule` ADD INDEX `i_security_ingress_rule_network_id`(`security_group_id`);
ALTER TABLE `cloud`.`security_ingress_rule` ADD INDEX `i_security_ingress_rule_allowed_network`(`allowed_network_id`);

ALTER TABLE `cloud`.`security_group_vm_map` DROP KEY `fk_network_group_vm_map___network_group_id`;
ALTER TABLE `cloud`.`security_group_vm_map` DROP KEY `fk_network_group_vm_map___instance_id`;
ALTER TABLE `cloud`.`security_group_vm_map` ADD CONSTRAINT `fk_security_group_vm_map___security_group_id` FOREIGN KEY `fk_security_group_vm_map___security_group_id` (`security_group_id`) REFERENCES `security_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`security_group_vm_map` ADD CONSTRAINT `fk_security_group_vm_map___instance_id` FOREIGN KEY `fk_security_group_vm_map___instance_id` (`instance_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE;
--n/w to sec grps ends --;

CREATE TABLE `cloud`.`instance_group` (
  `id` bigint unsigned NOT NULL UNIQUE auto_increment,
  `account_id` bigint unsigned NOT NULL COMMENT 'owner.  foreign key to account table',
  `name` varchar(255) NOT NULL,
  `removed` datetime COMMENT 'date the group was removed',
  `created` datetime COMMENT 'date the group was created',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`instance_group_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `group_id` bigint unsigned NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ssh_keypairs` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner, foreign key to account table',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain, foreign key to domain table',
  `keypair_name` varchar(256) NOT NULL COMMENT 'name of the key pair',
  `fingerprint` varchar(128) NOT NULL COMMENT 'fingerprint for the ssh public key',
  `public_key` varchar(5120) NOT NULL COMMENT 'public key of the ssh key pair',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`usage_event` (
  `id` bigint unsigned NOT NULL auto_increment,
  `type` varchar(32) NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `created` datetime NOT NULL,
  `zone_id` bigint unsigned NOT NULL,
  `resource_id` bigint unsigned,
  `resource_name` varchar(255),
  `offering_id` bigint unsigned,
  `template_id` bigint unsigned,
  `size` bigint unsigned,  
  `processed` tinyint NOT NULL default '0',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_host_vlan_alloc`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `host_id` bigint unsigned COMMENT 'host id',
  `account_id` bigint unsigned COMMENT 'account id',
  `vlan` bigint unsigned COMMENT 'vlan id under account #account_id on host #host_id',
  `ref` int unsigned NOT NULL DEFAULT 0 COMMENT 'reference count',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_tunnel_alloc`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `from` bigint unsigned COMMENT 'from host id',
  `to` bigint unsigned COMMENT 'to host id',
  `in_port` int unsigned COMMENT 'in port on open vswitch',
  PRIMARY KEY(`from`, `to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_tunnel`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `from` bigint unsigned COMMENT 'from host id',
  `to` bigint unsigned COMMENT 'to host id',
  `key` int unsigned default '0' COMMENT 'current gre key can be used',
  PRIMARY KEY(`from`, `to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_tunnel_account`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `from` bigint unsigned COMMENT 'from host id',
  `to` bigint unsigned COMMENT 'to host id',
  `account` bigint unsigned COMMENT 'account',
  `key` int unsigned COMMENT 'gre key',
  `port_name` varchar(32) COMMENT 'in port on open vswitch',
  `state` varchar(16) default 'FAILED' COMMENT 'result of tunnel creatation',
  PRIMARY KEY(`from`, `to`, `account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`ovs_tunnel_account` (`from`, `to`, `account`, `key`, `port_name`, `state`) VALUES (0, 0, 0, 0, 'lock', 'SUCCESS');

CREATE TABLE `cloud`.`ovs_vlan_mapping_dirty`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `account_id` bigint unsigned COMMENT 'account id',
  `dirty` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 means vlan mapping of this account was changed',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_vm_flow_log` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs flows to be synced.',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `logsequence` bigint unsigned  COMMENT 'seq number to be sent to agent, uniquely identifies flow update',
  `vm_name` varchar(255) NOT NULL COMMENT 'vm name',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_work` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs rules to be synced.',
  `mgmt_server_id` bigint unsigned COMMENT 'management server that has taken up the work of doing rule sync',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `taken` datetime COMMENT 'time it was taken by the management server',
  `step` varchar(32) NOT NULL COMMENT 'Step in the work',
  `seq_no` bigint unsigned  COMMENT 'seq number to be sent to agent, uniquely identifies ruleset update',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`storage_pool_work` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `pool_id` bigint unsigned NOT NULL COMMENT 'storage pool associated with the vm',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm identifier',
  `stopped_for_maintenance` tinyint unsigned NOT NULL DEFAULT 0 COMMENT 'this flag denoted whether the vm was stopped during maintenance',
  `started_after_maintenance` tinyint unsigned NOT NULL DEFAULT 0 COMMENT 'this flag denoted whether the vm was started after maintenance',
  `mgmt_server_id` bigint unsigned NOT NULL COMMENT 'management server id',
  PRIMARY KEY (`id`),
 UNIQUE (pool_id,vm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE vm_instance set hypervisor_type=(SELECT value FROM configuration WHERE name='hypervisor.type');

-- Insert stuff?;
INSERT INTO `cloud`.`sequence` (name, value) VALUES ('volume_seq', (SELECT max(id) + 1 or 200 from volumes));
INSERT INTO `cloud`.`sequence` (name, value) VALUES ('networks_seq', 200);

DELETE FROM `cloud`.`sync_queue`;
DELETE FROM `cloud`.`sync_queue_item`;
DELETE FROM `cloud`.`async_job`;
UPDATE `cloud`.`vm_template` SET type='SYSTEM' WHERE name='systemvm-xenserver-2.2.4';
UPDATE `cloud`.`vm_template` SET type='BUILTIN' WHERE unique_name='centos53-x86_64';
UPDATE `cloud`.`vm_template` SET type='PERHOST' WHERE unique_name='xs-tools.iso';
DELETE FROM template_host_ref WHERE install_path LIKE '%xs-tools%';
DELETE FROM configuration where name='upgrade.url';
DELETE FROM configuration where name='router.template.id';
INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.client.iprange', '10.1.2.1-10.1.2.8', 'The range of ips to be allocated to remote access vpn clients. The first ip in the range is used by the VPN server');
INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.psk.length', '48', 'The length of the ipsec preshared key (minimum 8, maximum 256)');
INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Network', 'DEFAULT', 'AgentManager', 'remote.access.vpn.user.limit', '8', 'The maximum number of VPN users that can be created per account');
INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Advanced', 'DEFAULT', 'management-server', 'management-server', NULL, 'The cidr of management server network');
Update configuration set name='storage.max.volume.size' where name='max.volume.size.mb';
INSERT INTO sequence (name, value)
    VALUES ('snapshots_seq', '1');
UPDATE cloud.sequence SET value=IF((SELECT COUNT(*) FROM cloud.snapshots)  > 0, (SELECT max(id) FROM cloud.snapshots) + 1, 1) WHERE name='snapshots_seq';
UPDATE configuration set name='direct.attach.security.groups.enabled' where name='direct.attach.network.groups.enabled';
UPDATE configuration set name='guest.domain.suffix' where name='domain.suffix';
UPDATE vm_template set type='SYSTEM'  where id=1;
UPDATE vm_template set type='SYSTEM' WHERE unique_name LIKE 'routing%';
UPDATE `cloud`.`vm_template` SET type='USER' WHERE type!='BUILTIN' AND type!='PERHOST' AND type!='SYSTEM';

INSERT INTO `cloud`.`guest_os_category` (id, name) VALUES (8, 'Novel');
INSERT INTO `cloud`.`guest_os_category` (id, name) VALUES (9, 'Unix');
INSERT INTO `cloud`.`guest_os_category` (id, name) VALUES (10, 'Ubuntu');

update `cloud`.`guest_os` set display_name = 'Debian GNU/Linux 5.0 (32-bit)' where id=15;
update `cloud`.`guest_os` set display_name = 'Windows Server 2003 (32-bit)' where id=50;
update `cloud`.`guest_os` set display_name = 'Windows Server 2003 (64-bit)' where id=51;
update `cloud`.`guest_os` set display_name = 'Windows Server 2008 (32-bit)' where id=52;
update `cloud`.`guest_os` set display_name = 'Windows Server 2008 (64-bit)' where id=53;
update `cloud`.`guest_os` set display_name = 'Other Ubuntu (32-bit)' where id=59;
update `cloud`.`guest_os` set display_name = 'Other (32-bit)' where id=60;
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (61, 6, 'Windows 2000 Server');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (62, 6, 'Windows 98');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (63, 6, 'Windows 95');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (64, 6, 'Windows NT 4');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (65, 6, 'Windows 3.1');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (66, 4, 'Red Hat Enterprise Linux 3(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (67, 4, 'Red Hat Enterprise Linux 3(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (68, 7, 'Open Enterprise Server');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (69, 7, 'Asianux 3(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (70, 7, 'Asianux 3(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (72, 2, 'Debian GNU/Linux 5(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (73, 2, 'Debian GNU/Linux 4(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (74, 2, 'Debian GNU/Linux 4(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (75, 7, 'Other 2.6x Linux (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (76, 7, 'Other 2.6x Linux (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (77, 8, 'Novell Netware 6.x');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (78, 8, 'Novell Netware 5.1');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (79, 9, 'Sun Solaris 10(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (80, 9, 'Sun Solaris 10(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (81, 9, 'Sun Solaris 9(Experimental)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (82, 9, 'Sun Solaris 8(Experimental)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (83, 9, 'FreeBSD (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (84, 9, 'FreeBSD (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (85, 9, 'SCO OpenServer 5');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (86, 9, 'SCO UnixWare 7');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (87, 6, 'Windows Server 2003 DataCenter Edition(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (88, 6, 'Windows Server 2003 DataCenter Edition(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (89, 6, 'Windows Server 2003 Standard Edition(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (90, 6, 'Windows Server 2003 Standard Edition(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (91, 6, 'Windows Server 2003 Web Edition');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (92, 6, 'Microsoft Small Bussiness Server 2003');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (93, 6, 'Windows XP (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (94, 6, 'Windows XP (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (95, 6, 'Windows 2000 Advanced Server');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (96, 5, 'SUSE Linux Enterprise 8(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (97, 5, 'SUSE Linux Enterprise 8(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (98, 7, 'Other Linux (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (99, 7, 'Other Linux (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (100, 10, 'Other Ubuntu (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (101, 6, 'Windows Vista (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (102, 6, 'DOS');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (103, 7, 'Other (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (104, 7, 'OS/2');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (105, 6, 'Windows 2000 Professional');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (106, 4, 'Red Hat Enterprise Linux 4(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (107, 5, 'SUSE Linux Enterprise 9(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (108, 5, 'SUSE Linux Enterprise 9(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (109, 5, 'SUSE Linux Enterprise 10(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (110, 5, 'SUSE Linux Enterprise 10(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (111, 1, 'CentOS 5.5 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (112, 1, 'CentOS 5.5 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (113, 4, 'Red Hat Enterprise Linux 5.5 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (114, 4, 'Red Hat Enterprise Linux 5.5 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (115, 4, 'Fedora 13');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (116, 4, 'Fedora 12');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (117, 4, 'Fedora 11');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (118, 4, 'Fedora 10');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (119, 4, 'Fedora 9');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (120, 4, 'Fedora 8');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (121, 10, 'Ubuntu 10.04 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (122, 10, 'Ubuntu 9.10 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (123, 10, 'Ubuntu 9.04 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (124, 10, 'Ubuntu 8.10 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (125, 10, 'Ubuntu 8.04 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (126, 10, 'Ubuntu 10.04 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (127, 10, 'Ubuntu 9.10 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (128, 10, 'Ubuntu 9.04 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (129, 10, 'Ubuntu 8.10 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (130, 10, 'Ubuntu 8.04 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (131, 4, 'Red Hat Enterprise Linux 2');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (132, 2, 'Debian GNU/Linux 6(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (133, 2, 'Debian GNU/Linux 6(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (134, 3, 'Oracle Enterprise Linux 5.5 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (135, 3, 'Oracle Enterprise Linux 5.5 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (136, 4, 'Red Hat Enterprise Linux 6.0 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (137, 4, 'Red Hat Enterprise Linux 6.0 (64-bit)');

INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 4.5 (32-bit)', 1);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 4.6 (32-bit)', 2);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 4.7 (32-bit)', 3);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 4.8 (32-bit)', 4);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.0 (32-bit)', 5);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.0 (64-bit)', 6);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.1 (32-bit)', 7);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.1 (32-bit)', 8);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.2 (32-bit)', 9);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.2 (64-bit)', 10);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.3 (32-bit)', 11);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.3 (64-bit)', 12);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.4 (32-bit)', 13);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'CentOS 5.4 (64-bit)', 14);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Debian Lenny 5.0 (32-bit)', 15);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.0 (32-bit)', 16);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.0 (64-bit)', 17);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.1 (32-bit)', 18);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.1 (64-bit)', 19);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.2 (32-bit)', 20);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.2 (64-bit)', 21);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.3 (32-bit)', 22);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.3 (64-bit)', 23);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.4 (32-bit)', 24);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Oracle Enterprise Linux 5.4 (64-bit)', 25);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 4.5 (32-bit)', 26);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 4.6 (32-bit)', 27);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 4.7 (32-bit)', 28);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 4.8 (32-bit)', 29);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.0 (32-bit)', 30);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.0 (64-bit)', 31);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.1 (32-bit)', 32);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.1 (64-bit)', 33);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.2 (32-bit)', 34);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.2 (64-bit)', 35);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.3 (32-bit)', 36);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.3 (64-bit)', 37);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.4 (32-bit)', 38);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Red Hat Enterprise Linux 5.4 (64-bit)', 39);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'SUSE Linux Enterprise Server 9 SP4 (32-bit)', 40);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'SUSE Linux Enterprise Server 10 SP1 (32-bit)', 41);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'SUSE Linux Enterprise Server 10 SP1 (64-bit)', 42);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'SUSE Linux Enterprise Server 10 SP2 (32-bit)', 43);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'SUSE Linux Enterprise Server 10 SP2 (64-bit)', 44);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'SUSE Linux Enterprise Server 10 SP3 (64-bit)', 45);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'SUSE Linux Enterprise Server 11 (32-bit)', 46);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'SUSE Linux Enterprise Server 11 (64-bit)', 47);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows 7 (32-bit)', 48);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows 7 (64-bit)', 49);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows Server 2003 (32-bit)', 50);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows Server 2003 (64-bit)', 51);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows Server 2008 (32-bit)', 52);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows Server 2008 (64-bit)', 53);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows Server 2008 R2 (64-bit)', 54);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows 2000 SP4 (32-bit)', 55);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows Vista (32-bit)', 56);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows XP SP2 (32-bit)', 57);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows XP SP3 (32-bit)', 58);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Other install media', 59);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Other install media', 100);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Other install media', 60);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Other install media', 103);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 121);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 126);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 122);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 127);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 123);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 128);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 124);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 129);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 125);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('XenServer', 'Other install media', 130);


INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 7(32-bit)', 48);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 7(64-bit)', 49);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2008 R2(64-bit)', 54);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2008(32-bit)', 52);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2008(64-bit)', 53);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2003, Enterprise Edition (32-bit)', 50);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2003, Enterprise Edition (64-bit)', 51);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2003, Datacenter Edition (32-bit)', 87);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2003, Datacenter Edition (64-bit)', 88);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2003, Standard Edition (32-bit)', 89);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2003, Standard Edition (64-bit)', 90);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Server 2003, Web Edition', 91);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Small Bussiness Server 2003', 92);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Vista (32-bit)', 56);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows Vista (64-bit)', 101);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows XP Professional (32-bit)', 93);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows XP Professional (32-bit)', 57);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows XP Professional (32-bit)', 58);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows XP Professional (64-bit)', 94);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 2000 Advanced Server', 95);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 2000 Server', 61);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 2000 Professional', 105);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 2000 Server', 55);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 98', 62);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 95', 63);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows NT 4', 64);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Microsoft Windows 3.1', 65);

INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(32-bit)', 30);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(32-bit)', 32);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(32-bit)', 34);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(32-bit)', 36);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(32-bit)', 38);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(64-bit)', 31);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(64-bit)', 33);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(64-bit)', 35);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(64-bit)', 37);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 5(64-bit)', 39);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 4(32-bit)', 26);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 4(32-bit)', 27);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 4(32-bit)', 28);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 4(32-bit)', 29);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 4(64-bit)', 106);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 3(32-bit)', 66);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 3(64-bit)', 67);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 2', 131);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 11(32-bit)', 46);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 11(64-bit)', 47);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 10(32-bit)', 41);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 10(32-bit)', 43);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 10(64-bit)', 42);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 10(64-bit)', 44);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 10(64-bit)', 45);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 10(32-bit)', 109);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 10(64-bit)', 110);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 8/9(32-bit)', 40);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 8/9(32-bit)', 96);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 8/9(64-bit)', 97);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 8/9(32-bit)', 107);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Suse Linux Enterprise 8/9(64-bit)', 108);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Open Enterprise Server', 68);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Asianux 3(32-bit)', 69);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Asianux 3(64-bit)', 70);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Debian GNU/Linux 5(32-bit)', 15);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Debian GNU/Linux 5(64-bit)', 72);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Debian GNU/Linux 4(32-bit)', 73);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Debian GNU/Linux 4(64-bit)', 74);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (32-bit)', 59);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (32-bit)', 121);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (32-bit)', 122);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (32-bit)', 123);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (32-bit)', 124);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (32-bit)', 125);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (64-bit)', 100);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (64-bit)', 126);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (64-bit)', 127);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (64-bit)', 128);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (64-bit)', 129);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu Linux (64-bit)', 130);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Other 2.6x Linux (32-bit)', 75);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Other 2.6x Linux (64-bit)', 76);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Other Linux (32-bit)', 98);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Other Linux (64-bit)', 99);

INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Novell Netware 6.x', 77);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Novell Netware 5.1', 78);

INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Sun Solaris 10(32-bit)', 79);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Sun Solaris 10(64-bit)', 80);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Sun Solaris 9(Experimental)', 81);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Sun Solaris 8(Experimental)', 82);

INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'FreeBSD (32-bit)', 83);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'FreeBSD (64-bit)', 84);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'OS/2', 104);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'SCO OpenServer 5', 85);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'SCO UnixWare 7', 86);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'DOS', 102);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Other (32-bit)', 60);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Other (64-bit)', 103);



INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 4.5', 1);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 4.6', 2);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 4.7', 3);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 4.8', 4);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.0', 5);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.0', 6);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.1', 7);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.1', 8);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.2', 9);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.2', 10);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.3', 11);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.3', 12);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.4', 13);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.4', 14);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.5', 111);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'CentOS 5.5', 112);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 4.5', 26);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 4.6', 27);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 4.7', 28);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 4.8', 29);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.0', 30);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.0', 31);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.1', 32);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.1', 33);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.2', 34);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.2', 35);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.3', 36);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.3', 37);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.4', 38);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.4', 39);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.5', 113);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 5.5', 114);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 4', 106);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 3', 66);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 3', 67);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Red Hat Enterprise Linux 2', 131);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Fedora 13', 115);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Fedora 12', 116);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Fedora 11', 117);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Fedora 10', 118);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Fedora 9', 119);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Fedora 8', 120);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 10.04', 121);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 10.04', 126);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 9.10', 122);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 9.10', 127);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 9.04', 123);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 9.04', 128);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 8.10', 124);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 8.10', 129);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 8.04', 125);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Ubuntu 8.04', 130);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Debian GNU/Linux 5', 15);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Debian GNU/Linux 5', 72);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Debian GNU/Linux 4', 73);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Debian GNU/Linux 4', 74);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Other Linux 2.6x', 75);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Other Linux 2.6x', 76);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Other Ubuntu', 59);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Other Ubuntu', 100);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Other Linux', 98);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Other Linux', 99);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows 7', 48);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows 7', 49);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2003', 50);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2003', 51);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2003', 87);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2003', 88);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2003', 89);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2003', 90);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2003', 91);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2003', 92);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2008', 52);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Server 2008', 53);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows 2000', 55);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows 2000', 61);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows 2000', 95);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows 98', 62);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Vista', 56);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows Vista', 101);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows XP SP2', 57);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows XP SP3', 58);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows XP ', 93);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Windows XP ', 94);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'DOS', 102);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Other', 60);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('KVM', 'Other', 103);
UPDATE `cloud`.`guest_os` SET name = display_name;

INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones, hypervisor_type)
    VALUES (3, 'routing-3', 'SystemVM Template (KVM)', 0, now(), 'SYSTEM', 0, 64, 1, 'http://download.cloud.com/releases/2.2.0/systemvm.qcow2.bz2', 'ec463e677054f280f152fcc264255d2f', 0, 'SystemVM Template (KVM)', 'QCOW2', 15, 0, 1, 'KVM');
INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones, hypervisor_type)
    VALUES (8, 'routing-8', 'SystemVM Template (vSphere)', 0, now(), 'SYSTEM', 0, 32, 1, 'http://download.cloud.com/releases/2.2.0/systemvm.ova', '3c9d4c704af44ebd1736e1bc78cec1fa', 0, 'SystemVM Template (vSphere)', 'OVA', 15, 0, 1, 'VMware');

INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, display_text, enable_password, format, guest_os_id, featured, cross_zones, hypervisor_type, extractable)
    VALUES (4, 'centos55-x86_64', 'CentOS 5.5(64-bit) no GUI (KVM)', 1, now(), 'BUILTIN', 0, 64, 1, 'http://download.cloud.com/releases/2.2.0/eec2209b-9875-3c8d-92be-c001bd8a0faf.qcow2.bz2', 'ed0e788280ff2912ea40f7f91ca7a249', 'CentOS 5.5(64-bit) no GUI (KVM)', 0, 'QCOW2', 112, 1, 1, 'KVM', 1);

INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text,  format, guest_os_id, featured, cross_zones, hypervisor_type, extractable)
    VALUES (7, 'centos53-x64', 'CentOS 5.3(64-bit) no GUI (vSphere)', 1, now(), 'BUILTIN', 0, 64, 1, 'http://download.cloud.com/releases/2.2.0/CentOS5.3-x86_64.ova', 'f6f881b7f2292948d8494db837fe0f47', 0, 'CentOS 5.3(64-bit) no GUI (vSphere)', 'OVA', 12, 1, 1, 'VMware', 1);
UPDATE vm_instance SET guest_os_id=15 where vm_template_id=1;
UPDATE vm_instance SET vm_template_id=(SELECT id FROM vm_template WHERE name='systemvm-xenserver-2.2.4' AND removed IS NULL) where vm_template_id=1;


ALTER TABLE `cloud`.`instance_group` ADD CONSTRAINT `fk_instance_group__account_id` FOREIGN KEY `fk_instance_group__account_id` (`account_id`) REFERENCES `account` (`id`);

ALTER TABLE `cloud`.`instance_group_vm_map` ADD CONSTRAINT `fk_instance_group_vm_map___group_id` FOREIGN KEY `fk_instance_group_vm_map___group_id` (`group_id`) REFERENCES `instance_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`instance_group_vm_map` ADD CONSTRAINT `fk_instance_group_vm_map___instance_id` FOREIGN KEY `fk_instance_group_vm_map___instance_id` (`instance_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`secondary_storage_vm` MODIFY COLUMN `guid` varchar(255);

ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT `fk_ssh_keypairs__account_id` FOREIGN KEY `fk_ssh_keypair__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT `fk_ssh_keypairs__domain_id` FOREIGN KEY `fk_ssh_keypair__domain_id` (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE;
UPDATE vm_template SET hypervisor_type='XenServer' WHERE hypervisor_type IS NULL AND format='VHD';
UPDATE vm_template SET hypervisor_type='KVM' WHERE hypervisor_type IS NULL AND format='QCOW2';
UPDATE vm_template SET hypervisor_type='VmWare' WHERE hypervisor_type IS NULL AND format='OVA';
UPDATE vm_template SET hypervisor_type='None' WHERE hypervisor_type IS NULL AND format='ISO';

ALTER TABLE `cloud`.`volumes` ADD COLUMN `source_id` bigint unsigned  COMMENT 'id for the source';
ALTER TABLE `cloud`.`volumes` ADD COLUMN `source_type` varchar(32) COMMENT 'source from which the volume is created -- snapshot, diskoffering, template, blank';

ALTER TABLE `cloud`.`user_statistics` ADD UNIQUE KEY `account_id` (`account_id`,`data_center_id`);
#UPDATE vm_instance SET instance_name=concat("r-", concat(cast(id as CHAR), concat("-", (SELECT value FROM configuration WHERE name='instance.name')))) WHERE type='DomainRouter';
#UPDATE vm_instance SET instance_name=concat("s-", concat(cast(id as CHAR), concat("-", (SELECT value FROM configuration WHERE name='instance.name')))) WHERE type='SecondaryStorageVm';
#UPDATE vm_instance SET instance_name=concat("v-", concat(cast(id as CHAR), concat("-", (SELECT value FROM configuration WHERE name='instance.name')))) WHERE type='ConsoleProxy';
#UPDATE vm_instance SET instance_name=concat("i-", concat(cast(account_id as CHAR), concat("-", concat(cast(id as CHAR), concat("-", (SELECT value FROM configuration WHERE name='instance.name')))))) WHERE type='User';

ALTER TABLE `cloud`.`data_center` MODIFY COLUMN `guest_network_cidr` varchar(18);

ALTER TABLE `cloud`.`op_ha_work` ADD CONSTRAINT `fk_op_ha_work__instance_id` FOREIGN KEY `fk_op_ha_work__instance_id` (`instance_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`op_ha_work` ADD CONSTRAINT `fk_op_ha_work__mgmt_server_id` FOREIGN KEY `fk_op_ha_work__mgmt_server_id`(`mgmt_server_id`) REFERENCES `mshost`(`msid`);

ALTER TABLE `cloud`.`secondary_storage_vm` ADD CONSTRAINT `fk_secondary_storage_vm__id` FOREIGN KEY `fk_secondary_storage_vm__id`(`id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`snapshots` MODIFY COLUMN `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'Primary Key';

ALTER TABLE `cloud`.`storage_pool` DROP KEY `uuid`;

ALTER TABLE `cloud`.`template_host_ref` DROP FOREIGN KEY `fk_template_host_ref__template_id`;
ALTER TABLE `cloud`.`template_host_ref` ADD CONSTRAINT `fk_template_host_ref__template_id` FOREIGN KEY `fk_template_host_ref__template_id` (`template_id`) REFERENCES `vm_template` (`id`);

ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE;

INSERT INTO configuration (category, instance, component, name, value, description)
    VALUES ('Advanced', 'DEFAULT', 'management-server', 'hypervisor.list', 'KVM,XenServer,VMware', 'The list of hypervisors that this deployment will use.');

DELETE FROM load_balancer_vm_map WHERE load_balancer_id NOT IN (SELECT id FROM load_balancer);
DELETE FROM vm_instance WHERE type='User' AND id NOT IN (SELECT id FROM user_vm);

