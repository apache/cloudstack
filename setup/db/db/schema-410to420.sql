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
-- Schema upgrade from 4.1.0 to 4.2.0;
--;

-- Disable foreign key checking
SET foreign_key_checks = 0;

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `max_hosts_per_cluster` int unsigned DEFAULT NULL COMMENT 'Max. hosts in cluster supported by hypervisor';
ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `storage_motion_supported` int(1) unsigned DEFAULT 0 COMMENT 'Is storage motion supported';
UPDATE `cloud`.`hypervisor_capabilities` SET `max_hosts_per_cluster`=32 WHERE `hypervisor_type`='VMware';
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, storage_motion_supported) VALUES ('XenServer', '6.1.0', 50, 1, 13, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_hosts_per_cluster) VALUES ('VMware', '5.1', 128, 0, 32);
DELETE FROM `cloud`.`configuration` where name='vmware.percluster.host.max';
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'xen.nics.max', '7', 'Maximum allowed nics for Vms created on Xen');
ALTER TABLE `cloud`.`load_balancer_vm_map` ADD state VARCHAR(40) NULL COMMENT 'service status updated by LB healthcheck manager';

alter table storage_pool change storage_provider_id storage_provider_name varchar(255);
alter table template_host_ref add state varchar(255);
alter table template_host_ref add update_count bigint unsigned;
alter table template_host_ref add updated datetime;
alter table volume_host_ref add state varchar(255);
alter table volume_host_ref add update_count bigint unsigned;
alter table volume_host_ref add updated datetime;
alter table template_spool_ref add updated datetime;
CREATE TABLE  `cloud`.`object_datastore_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `datastore_uuid`  varchar(255) NOT NULL,
  `datastore_role` varchar(255) NOT NULL,
  `object_uuid` varchar(255) NOT NULL,
  `object_type` varchar(255) NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `download_state` varchar(255),
  `url` varchar(255),
  `format` varchar(255),
  `checksum` varchar(255),
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `size` bigint unsigned COMMENT 'the size of the template on the pool',
  `state` varchar(255) NOT NULL,
  `update_count` bigint unsigned NOT NULL,
  `updated` DATETIME,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`data_store_provider` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of primary data store provider',
  `uuid` varchar(255) NOT NULL COMMENT 'uuid of primary data store provider',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`image_data_store` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of data store',
  `image_provider_name` varchar(255) NOT NULL COMMENT 'id of image_data_store_provider',
  `protocol` varchar(255) NOT NULL COMMENT 'protocol of data store',
  `data_center_id` bigint unsigned  COMMENT 'datacenter id of data store',
  `scope` varchar(255) COMMENT 'scope of data store',
  `uuid` varchar(255) COMMENT 'uuid of data store',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `image_data_store_id` bigint unsigned;

ALTER TABLE `cloud`.`service_offering` ADD COLUMN `is_volatile` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'true if the vm needs to be volatile, i.e., on every reboot of vm from API root disk is discarded and creates a new root disk';

ALTER TABLE `cloud`.`networks` ADD COLUMN `network_cidr` VARCHAR(18) COMMENT 'The network cidr for the isolated guest network which uses IP Reservation facility.For networks not using IP reservation, network_cidr is always null.';
ALTER TABLE `cloud`.`networks` CHANGE `cidr` `cidr` varchar(18) COMMENT 'CloudStack managed vms get IP address from cidr.In general this cidr also serves as the network CIDR. But in case IP reservation feature is being used by a Guest network, networkcidr is the Effective network CIDR for that network';


CREATE TABLE  `vpc_service_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_id` bigint unsigned NOT NULL COMMENT 'vpc_id',
  `service` varchar(255) NOT NULL COMMENT 'service',
  `provider` varchar(255) COMMENT 'service provider',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_service_map__vpc_id` FOREIGN KEY(`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE,
  UNIQUE (`vpc_id`, `service`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`load_balancer_healthcheck_policies` (
 `id` bigint(20) NOT NULL auto_increment,
  `uuid` varchar(40),
  `load_balancer_id` bigint unsigned NOT NULL,
  `pingpath` varchar(225) NULL DEFAULT '/',
  `description` varchar(4096)  NULL,
  `response_time` int(11) DEFAULT 5,
  `healthcheck_interval` int(11) DEFAULT 5,
  `healthcheck_thresshold` int(11) DEFAULT 2,
  `unhealth_thresshold` int(11) DEFAULT 10,
  `revoke` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 is when rule is set for Revoke',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  CONSTRAINT `fk_load_balancer_healthcheck_policies_loadbalancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.instancename.flag', 'false', 'Append guest VM display Name (if set) to the internal name of the VM');

INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (165, UUID(), 6, 'Windows 8 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (166, UUID(), 6, 'Windows 8 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (167, UUID(), 6, 'Windows Server 2012 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (168, UUID(), 6, 'Windows Server 8 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows 8 (32-bit)', 165);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows 8 (64-bit)', 166);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows Server 2012 (64-bit)', 167);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Windows Server 8 (64-bit)', 168);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Windows 8 (32-bit)', 165);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Windows 8 (64-bit)', 166);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Windows Server 2012 (64-bit)', 167);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("XenServer", 'Windows Server 8 (64-bit)', 168);

INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (211, UUID(), 7, 'Apple Mac OS X 10.6 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (212, UUID(), 7, 'Apple Mac OS X 10.6 (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (213, UUID(), 7, 'Apple Mac OS X 10.7 (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (214, UUID(), 7, 'Apple Mac OS X 10.7 (64-bit)');
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Apple Mac OS X 10.6 (32-bit)', 211);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Apple Mac OS X 10.6 (64-bit)', 212);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Apple Mac OS X 10.7 (32-bit)', 213);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Apple Mac OS X 10.7 (64-bit)', 214);

CREATE TABLE `cloud`.`user_vm_clone_setting` (
  `vm_id` bigint unsigned NOT NULL COMMENT 'guest VM id',
  `clone_type` varchar(10) NOT NULL COMMENT 'Full or Linked Clone (applicable to VMs on ESX)',
  PRIMARY KEY (`vm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'UserVmManager', 'vmware.create.full.clone' , 'false', 'If set to true, creates VMs as full clones on ESX hypervisor');

CREATE TABLE `cloud`.`affinity_group` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `uuid` varchar(40),
  `description` varchar(4096) NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  UNIQUE (`name`, `account_id`),
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_affinity_group__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`),
  CONSTRAINT `fk_affinity_group__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`),
  CONSTRAINT `uc_affinity_group__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`affinity_group_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `affinity_group_id` bigint unsigned NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_agvm__group_id` FOREIGN KEY(`affinity_group_id`) REFERENCES `affinity_group`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_affinity_group_vm_map___instance_id` FOREIGN KEY(`instance_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE nic_secondary_ips (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid` varchar(40),
  `vmId` bigint unsigned COMMENT 'vm instance id',
  `nicId` bigint unsigned NOT NULL,
  `ip4_address` char(40) COMMENT 'ip4 address',
  `ip6_address` char(40) COMMENT 'ip6 address',
  `network_id` bigint unsigned NOT NULL COMMENT 'network configuration id',
  `created` datetime NOT NULL COMMENT 'date created',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner.  foreign key to   account table',
  `domain_id` bigint unsigned NOT NULL COMMENT 'the domain that the owner belongs to',
   PRIMARY KEY (`id`),
   CONSTRAINT `fk_nic_secondary_ip__vmId` FOREIGN KEY `fk_nic_secondary_ip__vmId`(`vmId`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
   CONSTRAINT `fk_nic_secondary_ip__networks_id` FOREIGN KEY `fk_nic_secondary_ip__networks_id`(`network_id`) REFERENCES `networks`(`id`),
   CONSTRAINT `uc_nic_secondary_ip__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`nics` ADD COLUMN secondary_ip SMALLINT DEFAULT '0' COMMENT 'secondary ips configured for the nic';
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN dnat_vmip VARCHAR(40);

ALTER TABLE `cloud`.`alert` ADD COLUMN `archived` tinyint(1) unsigned NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`event` ADD COLUMN `archived` tinyint(1) unsigned NOT NULL DEFAULT 0;
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'alert.purge.interval', '86400', 'The interval (in seconds) to wait before running the alert purge thread');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'alert.purge.delay', '0', 'Alerts older than specified number days will be purged. Set this value to 0 to never delete alerts');

DROP VIEW IF EXISTS `cloud`.`event_view`;
CREATE VIEW `cloud`.`event_view` AS
    select
        event.id,
        event.uuid,
        event.type,
        event.state,
        event.description,
        event.created,
        event.level,
        event.parameters,
        event.start_id,
        eve.uuid start_uuid,
        event.user_id,
        event.archived,
        user.username user_name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name
    from
        `cloud`.`event`
            inner join
        `cloud`.`account` ON event.account_id = account.id
            inner join
        `cloud`.`domain` ON event.domain_id = domain.id
            inner join
        `cloud`.`user` ON event.user_id = user.id
            left join
        `cloud`.`projects` ON projects.project_account_id = event.account_id
            left join
        `cloud`.`event` eve ON event.start_id = eve.id;

ALTER TABLE `cloud`.`region` ADD COLUMN `gslb_service_enabled` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'Is GSLB service enalbed in the Region';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `is_gslb_provider` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if load balancer appliance is acting as gslb service provider in the zone';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `gslb_site_publicip` varchar(255)  DEFAULT NULL COMMENT 'GSLB service Provider site public ip';

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `gslb_site_privateip` varchar(255) DEFAULT NULL COMMENT 'GSLB service Provider site private ip';

CREATE TABLE `cloud`.`global_load_balancing_rules` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `account_id` bigint unsigned NOT NULL COMMENT 'account id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `region_id`  int unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) NULL COMMENT 'description',
  `state` char(32) NOT NULL COMMENT 'current state of this rule',
  `algorithm` varchar(255) NOT NULL COMMENT 'load balancing algorithm used to distribbute traffic across zones',
  `persistence` varchar(255) NOT NULL COMMENT 'session persistence used across the zone',
  `service_type` varchar(255) NOT NULL COMMENT 'GSLB service type (tcp/udp)',
  `gslb_domain_name` varchar(255) NOT NULL COMMENT 'DNS name for the GSLB service that is used to provide a FQDN for the GSLB service',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_global_load_balancing_rules_account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_global_load_balancing_rules_region_id` FOREIGN KEY(`region_id`) REFERENCES `region`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`global_load_balancer_lb_rule_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `gslb_rule_id` bigint unsigned NOT NULL,
  `lb_rule_id` bigint unsigned NOT NULL,
  `revoke` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 is when rule is set for Revoke',
  PRIMARY KEY  (`id`),
  UNIQUE KEY (`gslb_rule_id`, `lb_rule_id`),
  CONSTRAINT `fk_gslb_rule_id` FOREIGN KEY(`gslb_rule_id`) REFERENCES `global_load_balancing_rules`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lb_rule_id` FOREIGN KEY(`lb_rule_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.cpus', '40', 'The default maximum number of cpu cores that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.memory', '40960', 'The default maximum memory (in MiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.primary.storage', '200', 'The default maximum primary storage space (in GiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.secondary.storage', '400', 'The default maximum secondary storage space (in GiB) that can be used for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.cpus', '40', 'The default maximum number of cpu cores that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.memory', '40960', 'The default maximum memory (in MiB) that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.primary.storage', '200', 'The default maximum primary storage space (in GiB) that can be used for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.secondary.storage', '400', 'The default maximum secondary storage space (in GiB) that can be used for a project');



ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id';
ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `uuid` varchar(40) UNIQUE;

-- START: support for LXC
 
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('LXC', 'default', 50, 1);
ALTER TABLE `cloud`.`physical_network_traffic_types` ADD COLUMN `lxc_network_label` varchar(255) DEFAULT 'cloudbr0' COMMENT 'The network name label of the physical device dedicated to this traffic on a LXC host';
 
UPDATE configuration SET value='KVM,XenServer,VMware,BareMetal,Ovm,LXC' WHERE name='hypervisor.list';
 
INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones, hypervisor_type)
     VALUES (10, 'routing-10', 'SystemVM Template (LXC)', 0, now(), 'SYSTEM', 0, 64, 1, 'http://download.cloud.com/templates/acton/acton-systemvm-02062012.qcow2.bz2', '2755de1f9ef2ce4d6f2bee2efbb4da92', 0, 'SystemVM Template (LXC)', 'QCOW2', 15, 0, 1, 'LXC');

ALTER TABLE `cloud`.`user_vm` MODIFY user_data TEXT(32768);

-- END: support for LXC

CREATE TABLE `cloud`.`vm_snapshots` (
  `id` bigint(20) unsigned NOT NULL auto_increment COMMENT 'Primary Key',
  `uuid` varchar(40) NOT NULL,
  `name` varchar(255) NOT NULL,
  `display_name` varchar(255) default NULL,
  `description` varchar(255) default NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `vm_snapshot_type` varchar(32) default NULL,
  `state` varchar(32) NOT NULL,
  `parent` bigint unsigned default NULL,
  `current` int(1) unsigned default NULL,
  `update_count` bigint unsigned NOT NULL DEFAULT 0,
  `updated` datetime default NULL,
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT UNIQUE KEY `uc_vm_snapshots_uuid` (`uuid`),
  INDEX `vm_snapshots_name` (`name`),
  INDEX `vm_snapshots_vm_id` (`vm_id`),
  INDEX `vm_snapshots_account_id` (`account_id`),
  INDEX `vm_snapshots_display_name` (`display_name`),
  INDEX `vm_snapshots_removed` (`removed`),
  INDEX `vm_snapshots_parent` (`parent`),
  CONSTRAINT `fk_vm_snapshots_vm_id__vm_instance_id` FOREIGN KEY `fk_vm_snapshots_vm_id__vm_instance_id` (`vm_id`) REFERENCES `vm_instance` (`id`),
  CONSTRAINT `fk_vm_snapshots_account_id__account_id` FOREIGN KEY `fk_vm_snapshots_account_id__account_id` (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_vm_snapshots_domain_id__domain_id` FOREIGN KEY `fk_vm_snapshots_domain_id__domain_id` (`domain_id`) REFERENCES `domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `vm_snapshot_enabled` tinyint(1) DEFAULT 0 NOT NULL COMMENT 'Whether VM snapshot is supported by hypervisor';
UPDATE `cloud`.`hypervisor_capabilities` SET `vm_snapshot_enabled`=1 WHERE `hypervisor_type` in ('VMware', 'XenServer');

			
DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE VIEW `cloud`.`user_vm_view` AS
    select 
        vm_instance.id id,
        vm_instance.name name,
        user_vm.display_name display_name,
        user_vm.user_data user_data,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        instance_group.id instance_group_id,
        instance_group.uuid instance_group_uuid,
        instance_group.name instance_group_name,
        vm_instance.uuid uuid,
        vm_instance.last_host_id last_host_id,
        vm_instance.vm_type type,
        vm_instance.vnc_password vnc_password,
        vm_instance.limit_cpu_use limit_cpu_use,
        vm_instance.created created,
        vm_instance.state state,
        vm_instance.removed removed,
        vm_instance.ha_enabled ha_enabled,
        vm_instance.hypervisor_type hypervisor_type,
        vm_instance.instance_name instance_name,
        vm_instance.guest_os_id guest_os_id,
        guest_os.uuid guest_os_uuid,
        vm_instance.pod_id pod_id,
        host_pod_ref.uuid pod_uuid,
        vm_instance.private_ip_address private_ip_address,
        vm_instance.private_mac_address private_mac_address,
        vm_instance.vm_type vm_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        data_center.is_security_group_enabled security_group_enabled,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.name template_name,
        vm_template.display_text template_display_text,
        vm_template.enable_password password_enabled,
        iso.id iso_id,
        iso.uuid iso_uuid,
        iso.name iso_name,
        iso.display_text iso_display_text,
        service_offering.id service_offering_id,
        disk_offering.uuid service_offering_uuid,
        service_offering.cpu cpu,
        service_offering.speed speed,
        service_offering.ram_size ram_size,
        disk_offering.name service_offering_name,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.pool_type pool_type,
        volumes.id volume_id,
        volumes.uuid volume_uuid,
        volumes.device_id volume_device_id,
        volumes.volume_type volume_type,
        security_group.id security_group_id,
        security_group.uuid security_group_uuid,
        security_group.name security_group_name,
        security_group.description security_group_description,
        nics.id nic_id,
        nics.uuid nic_uuid,
        nics.network_id network_id,
        nics.ip4_address ip_address,
        nics.ip6_address ip6_address,
        nics.ip6_gateway ip6_gateway,
        nics.ip6_cidr ip6_cidr,
        nics.default_nic is_default_nic,
        nics.gateway gateway,
        nics.netmask netmask,
        nics.mac_address mac_address,
        nics.broadcast_uri broadcast_uri,
        nics.isolation_uri isolation_uri,
        vpc.id vpc_id,
        vpc.uuid vpc_uuid,
        networks.uuid network_uuid,
        networks.name network_name,
        networks.traffic_type traffic_type,
        networks.guest_type guest_type,
        user_ip_address.id public_ip_id,
        user_ip_address.uuid public_ip_uuid,
        user_ip_address.public_ip_address public_ip_address,
        ssh_keypairs.keypair_name keypair_name,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
		affinity_group.id affinity_group_id,
        affinity_group.uuid affinity_group_uuid,
        affinity_group.name affinity_group_name,
        affinity_group.description affinity_group_description
    from
        `cloud`.`user_vm`
            inner join
        `cloud`.`vm_instance` ON vm_instance.id = user_vm.id
            and vm_instance.removed is NULL
            inner join
        `cloud`.`account` ON vm_instance.account_id = account.id
            inner join
        `cloud`.`domain` ON vm_instance.domain_id = domain.id
            left join
        `cloud`.`guest_os` ON vm_instance.guest_os_id = guest_os.id
            left join
        `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`instance_group_vm_map` ON vm_instance.id = instance_group_vm_map.instance_id
            left join
        `cloud`.`instance_group` ON instance_group_vm_map.group_id = instance_group.id
            left join
        `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
            left join
        `cloud`.`host` ON vm_instance.host_id = host.id
            left join
        `cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
            left join
        `cloud`.`vm_template` iso ON iso.id = user_vm.iso_id
            left join
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
            left join
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
            left join
        `cloud`.`volumes` ON vm_instance.id = volumes.instance_id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`security_group_vm_map` ON vm_instance.id = security_group_vm_map.instance_id
            left join
        `cloud`.`security_group` ON security_group_vm_map.security_group_id = security_group.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON networks.vpc_id = vpc.id
            left join
        `cloud`.`user_ip_address` ON user_ip_address.vm_id = vm_instance.id
            left join
        `cloud`.`user_vm_details` ON user_vm_details.vm_id = vm_instance.id
            and user_vm_details.name = 'SSH.PublicKey'
            left join
        `cloud`.`ssh_keypairs` ON ssh_keypairs.public_key = user_vm_details.value
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = vm_instance.id
            and resource_tags.resource_type = 'UserVm'
            left join
        `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
            and async_job.instance_type = 'VirtualMachine'
            and async_job.job_status = 0
            left join
        `cloud`.`affinity_group_vm_map` ON vm_instance.id = affinity_group_vm_map.instance_id
			left join
        `cloud`.`affinity_group` ON affinity_group_vm_map.affinity_group_id = affinity_group.id;

DROP VIEW IF EXISTS `cloud`.`affinity_group_view`;
CREATE VIEW `cloud`.`affinity_group_view` AS
    select 
        affinity_group.id id,
        affinity_group.name name,
        affinity_group.type type,
        affinity_group.description description,
        affinity_group.uuid uuid,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        vm_instance.id vm_id,
        vm_instance.uuid vm_uuid,
        vm_instance.name vm_name,
        vm_instance.state vm_state,
        user_vm.display_name vm_display_name
    from
        `cloud`.`affinity_group`
            inner join
        `cloud`.`account` ON affinity_group.account_id = account.id
            inner join
        `cloud`.`domain` ON affinity_group.domain_id = domain.id
            left join
        `cloud`.`affinity_group_vm_map` ON affinity_group.id = affinity_group_vm_map.affinity_group_id
            left join
        `cloud`.`vm_instance` ON vm_instance.id = affinity_group_vm_map.instance_id
            left join
        `cloud`.`user_vm` ON user_vm.id = vm_instance.id;

DROP VIEW IF EXISTS `cloud`.`host_view`;
CREATE VIEW `cloud`.`host_view` AS
    select 
        host.id,
        host.uuid,
        host.name,
        host.status,
        host.disconnected,
        host.type,
        host.private_ip_address,
        host.version,
        host.hypervisor_type,
        host.hypervisor_version,
        host.capabilities,
        host.last_ping,
        host.created,
        host.removed,
        host.resource_state,
        host.mgmt_server_id,
        host.cpus,
        host.speed,
        host.ram,
        cluster.id cluster_id,
        cluster.uuid cluster_uuid,
        cluster.name cluster_name,
        cluster.cluster_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        host_tags.tag,
        guest_os_category.id guest_os_category_id,
        guest_os_category.uuid guest_os_category_uuid,
        guest_os_category.name guest_os_category_name,
        mem_caps.used_capacity memory_used_capacity,
        mem_caps.reserved_capacity memory_reserved_capacity,
        cpu_caps.used_capacity cpu_used_capacity,
        cpu_caps.reserved_capacity cpu_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`host`
            left join
        `cloud`.`cluster` ON host.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON host.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
            left join
        `cloud`.`host_details` ON host.id = host_details.id
            and host_details.name = 'guest.os.category.id'
            left join
        `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT( host_details.value , UNSIGNED)
            left join
        `cloud`.`host_tags` ON host_tags.host_id = host.id
            left join
        `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
            and mem_caps.capacity_type = 0
            left join
        `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
            and cpu_caps.capacity_type = 1
            left join
        `cloud`.`async_job` ON async_job.instance_id = host.id
            and async_job.instance_type = 'Host'
            and async_job.job_status = 0;
        
DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE VIEW `cloud`.`volume_view` AS
    select 
        volumes.id,
        volumes.uuid,
        volumes.name,
        volumes.device_id,
        volumes.volume_type,
        volumes.size,
        volumes.created,
        volumes.state,
        volumes.attached,
        volumes.removed,
        volumes.pod_id,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        vm_instance.id vm_id,
        vm_instance.uuid vm_uuid,
        vm_instance.name vm_name,
        vm_instance.state vm_state,
        vm_instance.vm_type,
        user_vm.display_name vm_display_name,
        volume_host_ref.size volume_host_size,
        volume_host_ref.created volume_host_created,
        volume_host_ref.format,
        volume_host_ref.download_pct,
        volume_host_ref.download_state,
        volume_host_ref.error_str,
        disk_offering.id disk_offering_id,
        disk_offering.uuid disk_offering_uuid,
        disk_offering.name disk_offering_name,
        disk_offering.display_text disk_offering_display_text,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.name pool_name,
        cluster.hypervisor_type,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.extractable,
        vm_template.type template_type,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`volumes`
            inner join
        `cloud`.`account` ON volumes.account_id = account.id
            inner join
        `cloud`.`domain` ON volumes.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`data_center` ON volumes.data_center_id = data_center.id
            left join
        `cloud`.`vm_instance` ON volumes.instance_id = vm_instance.id
            left join
        `cloud`.`user_vm` ON user_vm.id = vm_instance.id
            left join
        `cloud`.`volume_host_ref` ON volumes.id = volume_host_ref.volume_id
            and volumes.data_center_id = volume_host_ref.zone_id
            left join
        `cloud`.`disk_offering` ON volumes.disk_offering_id = disk_offering.id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`vm_template` ON volumes.template_id = vm_template.id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = volumes.id
            and resource_tags.resource_type = 'Volume'
            left join
        `cloud`.`async_job` ON async_job.instance_id = volumes.id
            and async_job.instance_type = 'Volume'
            and async_job.job_status = 0;                       
 
DROP VIEW IF EXISTS `cloud`.`storage_pool_view`;
CREATE VIEW `cloud`.`storage_pool_view` AS
    select 
        storage_pool.id,
        storage_pool.uuid,
        storage_pool.name,
        storage_pool.status,
        storage_pool.path,
        storage_pool.pool_type,
        storage_pool.host_address,
        storage_pool.created,
        storage_pool.removed,
        storage_pool.capacity_bytes,
        storage_pool.scope,
        cluster.id cluster_id,
        cluster.uuid cluster_uuid,
        cluster.name cluster_name,
        cluster.cluster_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,      
        data_center.networktype data_center_type,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        storage_pool_details.name tag,
        op_host_capacity.used_capacity disk_used_capacity,
        op_host_capacity.reserved_capacity disk_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`storage_pool`
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON storage_pool.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON storage_pool.pod_id = host_pod_ref.id
            left join
        `cloud`.`storage_pool_details` ON storage_pool_details.pool_id = storage_pool.id
            and storage_pool_details.value = 'true'
            left join
        `cloud`.`op_host_capacity` ON storage_pool.id = op_host_capacity.host_id
            and op_host_capacity.capacity_type = 3
            left join
        `cloud`.`async_job` ON async_job.instance_id = storage_pool.id
            and async_job.instance_type = 'StoragePool'
            and async_job.job_status = 0;
            

DROP VIEW IF EXISTS `cloud`.`domain_router_view`;
CREATE VIEW `cloud`.`domain_router_view` AS
    select 
        vm_instance.id id,
        vm_instance.name name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        vm_instance.uuid uuid,
        vm_instance.created created,
        vm_instance.state state,
        vm_instance.removed removed,
        vm_instance.pod_id pod_id,
        vm_instance.instance_name instance_name,
        host_pod_ref.uuid pod_uuid,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.networktype data_center_type,
        data_center.dns1 dns1,
        data_center.dns2 dns2,
        data_center.ip6_dns1 ip6_dns1,
        data_center.ip6_dns2 ip6_dns2,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        service_offering.id service_offering_id,
        disk_offering.uuid service_offering_uuid,
        disk_offering.name service_offering_name,
        nics.id nic_id,
        nics.uuid nic_uuid,
        nics.network_id network_id,
        nics.ip4_address ip_address,
        nics.ip6_address ip6_address,
        nics.ip6_gateway ip6_gateway,
        nics.ip6_cidr ip6_cidr,
        nics.default_nic is_default_nic,
        nics.gateway gateway,
        nics.netmask netmask,
        nics.mac_address mac_address,
        nics.broadcast_uri broadcast_uri,
        nics.isolation_uri isolation_uri,
        vpc.id vpc_id,
        vpc.uuid vpc_uuid,
        networks.uuid network_uuid,
        networks.name network_name,
        networks.network_domain network_domain,
        networks.traffic_type traffic_type,
        networks.guest_type guest_type,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        domain_router.template_version template_version,
        domain_router.scripts_version scripts_version,
        domain_router.is_redundant_router is_redundant_router,
        domain_router.redundant_state redundant_state,
        domain_router.stop_pending stop_pending
    from
        `cloud`.`domain_router`
            inner join
        `cloud`.`vm_instance` ON vm_instance.id = domain_router.id
            inner join
        `cloud`.`account` ON vm_instance.account_id = account.id
            inner join
        `cloud`.`domain` ON vm_instance.domain_id = domain.id
            left join
        `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
            left join
        `cloud`.`host` ON vm_instance.host_id = host.id
            left join
        `cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
            left join
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
            left join
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
            left join
        `cloud`.`volumes` ON vm_instance.id = volumes.instance_id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON domain_router.vpc_id = vpc.id
            left join
        `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
            and async_job.instance_type = 'DomainRouter'
            and async_job.job_status = 0;
            
CREATE TABLE `cloud`.`external_cisco_vnmc_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which cisco vnmc device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this cisco vnmc device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the cisco vnmc device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external cisco vnmc device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_cisco_vnmc_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_cisco_vnmc_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_cisco_asa1000v_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which cisco asa1kv device is added',
  `management_ip` varchar(255) UNIQUE NOT NULL COMMENT 'mgmt. ip of cisco asa1kv device',
  `in_port_profile` varchar(255) NOT NULL COMMENT 'inside port profile name of cisco asa1kv device',
  `cluster_id` bigint unsigned NOT NULL COMMENT 'id of the Vmware cluster to which cisco asa1kv device is attached (cisco n1kv switch)',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_cisco_asa1000v_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_cisco_asa1000v_devices__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_asa1000v_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `network_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of guest network',
  `asa1000v_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of asa1000v device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_asa1000v_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_network_asa1000v_map__asa1000v_id` FOREIGN KEY (`asa1000v_id`) REFERENCES `external_cisco_asa1000v_devices`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `eip_associate_public_ip` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if public IP is associated with user VM creation by default when EIP service is enabled.' AFTER `elastic_ip_service`;

-- Re-enable foreign key checking, at the end of the upgrade path
SET foreign_key_checks = 1;			


-- Add "default" field to account/user tables
ALTER TABLE `cloud`.`account` ADD COLUMN `default` int(1) unsigned NOT NULL DEFAULT '0' COMMENT '1 if account is default';
ALTER TABLE `cloud`.`user` ADD COLUMN `default` int(1) unsigned NOT NULL DEFAULT '0' COMMENT '1 if user is default';
UPDATE `cloud`.`account` SET `cloud`.`account`.`default`=1 WHERE id IN (1,2);
UPDATE `cloud`.`user` SET `cloud`.`user`.`default`=1 WHERE id IN (1,2);

ALTER VIEW `cloud`.`user_view` AS
    select 
        user.id,
        user.uuid,
        user.username,
        user.password,
        user.firstname,
        user.lastname,
        user.email,
        user.state,
        user.api_key,
        user.secret_key,
        user.created,
        user.removed,
        user.timezone,
        user.registration_token,
        user.is_registered,
        user.incorrect_login_attempts,
        user.default,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`user`
            inner join
        `cloud`.`account` ON user.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`async_job` ON async_job.instance_id = user.id
            and async_job.instance_type = 'User'
            and async_job.job_status = 0;
       

DROP VIEW IF EXISTS `cloud`.`account_view`;
CREATE VIEW `cloud`.`account_view` AS
    select
        account.id,
        account.uuid,
        account.account_name,
        account.type,
        account.state,
        account.removed,
        account.cleanup_needed,
        account.network_domain,
        account.default,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        account_netstats_view.bytesReceived,
        account_netstats_view.bytesSent,
        vmlimit.max vmLimit,
        vmcount.count vmTotal,
        runningvm.vmcount runningVms,
        stoppedvm.vmcount stoppedVms,
        iplimit.max ipLimit,
        ipcount.count ipTotal,
        free_ip_view.free_ip ipFree,
        volumelimit.max volumeLimit,
        volumecount.count volumeTotal,
        snapshotlimit.max snapshotLimit,
        snapshotcount.count snapshotTotal,
        templatelimit.max templateLimit,
        templatecount.count templateTotal,
        vpclimit.max vpcLimit,
        vpccount.count vpcTotal,
        projectlimit.max projectLimit,
        projectcount.count projectTotal,
        networklimit.max networkLimit,
        networkcount.count networkTotal,
        cpulimit.max cpuLimit,
        cpucount.count cpuTotal,
        memorylimit.max memoryLimit,
        memorycount.count memoryTotal,
        primary_storage_limit.max primaryStorageLimit,
        primary_storage_count.count primaryStorageTotal,
        secondary_storage_limit.max secondaryStorageLimit,
        secondary_storage_count.count secondaryStorageTotal,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`free_ip_view`,
        `cloud`.`account`
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`data_center` ON account.default_zone_id = data_center.id
            left join
        `cloud`.`account_netstats_view` ON account.id = account_netstats_view.account_id
            left join
        `cloud`.`resource_limit` vmlimit ON account.id = vmlimit.account_id
            and vmlimit.type = 'user_vm'
            left join
        `cloud`.`resource_count` vmcount ON account.id = vmcount.account_id
            and vmcount.type = 'user_vm'
            left join
        `cloud`.`account_vmstats_view` runningvm ON account.id = runningvm.account_id
            and runningvm.state = 'Running'
            left join
        `cloud`.`account_vmstats_view` stoppedvm ON account.id = stoppedvm.account_id
           and stoppedvm.state = 'Stopped'
            left join
        `cloud`.`resource_limit` iplimit ON account.id = iplimit.account_id
            and iplimit.type = 'public_ip'
            left join
        `cloud`.`resource_count` ipcount ON account.id = ipcount.account_id
            and ipcount.type = 'public_ip'
            left join
        `cloud`.`resource_limit` volumelimit ON account.id = volumelimit.account_id
            and volumelimit.type = 'volume'
            left join
        `cloud`.`resource_count` volumecount ON account.id = volumecount.account_id
            and volumecount.type = 'volume'
            left join
        `cloud`.`resource_limit` snapshotlimit ON account.id = snapshotlimit.account_id
            and snapshotlimit.type = 'snapshot'
            left join
        `cloud`.`resource_count` snapshotcount ON account.id = snapshotcount.account_id
            and snapshotcount.type = 'snapshot'
            left join
        `cloud`.`resource_limit` templatelimit ON account.id = templatelimit.account_id
            and templatelimit.type = 'template'
            left join
        `cloud`.`resource_count` templatecount ON account.id = templatecount.account_id
            and templatecount.type = 'template'
            left join
        `cloud`.`resource_limit` vpclimit ON account.id = vpclimit.account_id
            and vpclimit.type = 'vpc'
            left join
        `cloud`.`resource_count` vpccount ON account.id = vpccount.account_id
            and vpccount.type = 'vpc'
            left join
        `cloud`.`resource_limit` projectlimit ON account.id = projectlimit.account_id
            and projectlimit.type = 'project'
            left join
        `cloud`.`resource_count` projectcount ON account.id = projectcount.account_id
            and projectcount.type = 'project'
            left join
        `cloud`.`resource_limit` networklimit ON account.id = networklimit.account_id
            and networklimit.type = 'network'
            left join
        `cloud`.`resource_count` networkcount ON account.id = networkcount.account_id
            and networkcount.type = 'network'
            left join
        `cloud`.`resource_limit` cpulimit ON account.id = cpulimit.account_id
            and cpulimit.type = 'cpu'
            left join
        `cloud`.`resource_count` cpucount ON account.id = cpucount.account_id
            and cpucount.type = 'cpu'
            left join
        `cloud`.`resource_limit` memorylimit ON account.id = memorylimit.account_id
            and memorylimit.type = 'memory'
            left join
        `cloud`.`resource_count` memorycount ON account.id = memorycount.account_id
            and memorycount.type = 'memory'
            left join
        `cloud`.`resource_limit` primary_storage_limit ON account.id = primary_storage_limit.account_id
            and primary_storage_limit.type = 'primary_storage'
            left join
        `cloud`.`resource_count` primary_storage_count ON account.id = primary_storage_count.account_id
            and primary_storage_count.type = 'primary_storage'
            left join
        `cloud`.`resource_limit` secondary_storage_limit ON account.id = secondary_storage_limit.account_id
            and secondary_storage_limit.type = 'secondary_storage'
            left join
        `cloud`.`resource_count` secondary_storage_count ON account.id = secondary_storage_count.account_id
            and secondary_storage_count.type = 'secondary_storage'
            left join
        `cloud`.`async_job` ON async_job.instance_id = account.id
            and async_job.instance_type = 'Account'
            and async_job.job_status = 0;


alter table `cloud_usage`.`usage_network_offering` add column nic_id bigint(20) unsigned NOT NULL;
ALTER TABLE `cloud`.`data_center_details` MODIFY value varchar(1024);
ALTER TABLE `cloud`.`cluster_details` MODIFY value varchar(255);
ALTER TABLE `cloud`.`storage_pool_details` MODIFY value varchar(255);
ALTER TABLE `cloud`.`account_details` MODIFY value varchar(255);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'midonet.apiserver.address', 'http://localhost:8081', 'Specify the address at which the Midonet API server can be contacted (if using Midonet)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'midonet.providerrouter.id', 'd7c5e6a3-e2f4-426b-b728-b7ce6a0448e5', 'Specifies the UUID of the Midonet provider router (if using Midonet)');

alter table cloud.vpc_gateways add column source_nat boolean default false;
alter table cloud.private_ip_address add column source_nat boolean default false;

CREATE TABLE `cloud`.`account_vnet_map` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `vnet_range` varchar(255) NOT NULL COMMENT 'dedicated guest vlan range',
  `account_id` bigint unsigned NOT NULL COMMENT 'account id. foreign key to account table',
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'physical network id. foreign key to the the physical network table',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_account_vnet_map__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network` (`id`) ON DELETE CASCADE,
  INDEX `i_account_vnet_map__physical_network_id`(`physical_network_id`),
  CONSTRAINT `fk_account_vnet_map__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  INDEX `i_account_vnet_map__account_id`(`account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD COLUMN account_vnet_map_id bigint unsigned;
ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD CONSTRAINT `fk_op_dc_vnet_alloc__account_vnet_map_id` FOREIGN KEY `fk_op_dc_vnet_alloc__account_vnet_map_id` (`account_vnet_map_id`) REFERENCES `account_vnet_map` (`id`);
