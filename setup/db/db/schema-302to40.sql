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
-- Schema upgrade from 3.0.2 to 4.0.0;
--;




DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.cpu.mhz';
DELETE FROM `cloud`.`configuration` WHERE name='secstorage.vm.cpu.mhz';
DELETE FROM `cloud`.`configuration` WHERE name='consoleproxy.ram.size';
DELETE FROM `cloud`.`configuration` WHERE name='secstorage.vm.ram.size';
DELETE FROM `cloud`.`configuration` WHERE name='open.vswitch.vlan.network';
DELETE FROM `cloud`.`configuration` WHERE name='open.vswitch.tunnel.network';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'consoleproxy.service.offering', NULL, 'Service offering used by console proxy; if NULL - system offering will be used');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'secstorage.service.offering', NULL, 'Service offering used by secondary storage; if NULL - system offering will be used');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'sdn.ovs.controller', NULL, 'Enable/Disable Open vSwitch SDN controller for L2-in-L3 overlay networks');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'sdn.ovs.controller.default.label', NULL, 'Default network label to be used when fetching interface for GRE endpoints');

ALTER TABLE `cloud`.`user_vm` ADD COLUMN `update_parameters` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Defines if the parameters need to be set for the vm';
UPDATE `cloud`.`user_vm` SET update_parameters=0 where id>0;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'ha.tag', NULL, 'HA tag defining that the host marked with this tag can be used for HA purposes only');

# Changes for Upload Volume
CREATE TABLE  `cloud`.`volume_host_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL,
  `volume_id` bigint unsigned NOT NULL,
  `zone_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `size` bigint unsigned,
  `physical_size` bigint unsigned DEFAULT 0,
  `download_state` varchar(255),
  `checksum` varchar(255) COMMENT 'checksum for the data disk',
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `url` varchar(255),
  `format` varchar(32) NOT NULL COMMENT 'format for the volume', 
  `destroyed` tinyint(1) COMMENT 'indicates whether the volume_host entry was destroyed by the user or not',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_volume_host_ref__host_id` FOREIGN KEY `fk_volume_host_ref__host_id` (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
  INDEX `i_volume_host_ref__host_id`(`host_id`),
  CONSTRAINT `fk_volume_host_ref__volume_id` FOREIGN KEY `fk_volume_host_ref__volume_id` (`volume_id`) REFERENCES `volumes` (`id`),
  INDEX `i_volume_host_ref__volume_id`(`volume_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`disk_offering` (name, display_text, customized, unique_name, disk_size, system_use, type) VALUES ( 'Custom', 'Custom Disk', 1, 'Cloud.com-Custom', 0, 0, 'Disk');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Storage', 'DEFAULT', 'management-server', 'storage.max.volume.upload.size', 500, 'The maximum size for a uploaded volume(in GB).');
# Changes for OVS tunnel manager

# The Following tables are not used anymore
DROP TABLE IF EXISTS `cloud`.`ovs_host_vlan_alloc`;
DROP TABLE IF EXISTS `cloud`.`ovs_tunnel`;
DROP TABLE IF EXISTS `cloud`.`ovs_tunnel_alloc`;
DROP TABLE IF EXISTS `cloud`.`ovs_vlan_mapping_dirty`;
DROP TABLE IF EXISTS `cloud`.`ovs_vm_flow_log`;
DROP TABLE IF EXISTS `cloud`.`ovs_work`;

CREATE TABLE `cloud`.`ovs_tunnel_interface` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `ip` varchar(16) DEFAULT NULL,
  `netmask` varchar(16) DEFAULT NULL,
  `mac` varchar(18) DEFAULT NULL,
  `host_id` bigint(20) DEFAULT NULL,
  `label` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ovs_tunnel_network`(
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `from` bigint unsigned COMMENT 'from host id',
  `to` bigint unsigned COMMENT 'to host id',
  `network_id` bigint unsigned COMMENT 'network identifier',
  `key` int unsigned COMMENT 'gre key',
  `port_name` varchar(32) COMMENT 'in port on open vswitch',
  `state` varchar(16) default 'FAILED' COMMENT 'result of tunnel creatation',
  PRIMARY KEY(`from`, `to`, `network_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`ovs_tunnel_interface` (`ip`, `netmask`, `mac`, `host_id`, `label`) VALUES ('0', '0', '0', 0, 'lock');

INSERT INTO `cloud`.`ovs_tunnel_network` (`from`, `to`, `network_id`, `key`, `port_name`, `state`) VALUES (0, 0, 0, 0, 'lock', 'SUCCESS');

UPDATE `cloud`.`configuration` set component='NetworkManager' where name='external.network.stats.interval';
UPDATE `cloud`.`configuration` set category='Advanced' where name='guest.domain.suffix';
UPDATE `cloud`.`configuration` set component='NetworkManager' where name='network.guest.cidr.limit';
UPDATE `cloud`.`configuration` set component='NetworkManager' where name='router.cpu.mhz';
UPDATE `cloud`.`configuration` set component='NetworkManager' where name='router.ram.size';
UPDATE `cloud`.`configuration` set component='NetworkManager' where name='router.stats.interval';
UPDATE `cloud`.`configuration` set component='NetworkManager' where name='router.template.id';
UPDATE `cloud`.`configuration` set category='Advanced' where name='capacity.skipcounting.hours';
UPDATE `cloud`.`configuration` set category='Advanced' where name='use.local.storage';
UPDATE `cloud`.`configuration` set description = 'Percentage (as a value between 0 and 1) of local storage utilization above which alerts will be sent about low local storage available.' where name = 'cluster.localStorage.capacity.notificationthreshold';

DELETE FROM `cloud`.`configuration` WHERE name='direct.agent.pool.size';
DELETE FROM `cloud`.`configuration` WHERE name='xen.max.product.version';
DELETE FROM `cloud`.`configuration` WHERE name='xen.max.version';
DELETE FROM `cloud`.`configuration` WHERE name='xen.max.xapi.version';
DELETE FROM `cloud`.`configuration` WHERE name='xen.min.product.version';
DELETE FROM `cloud`.`configuration` WHERE name='xen.min.version';
DELETE FROM `cloud`.`configuration` WHERE name='xen.min.xapi.version';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'enable.ec2.api', 'false', 'enable EC2 API on CloudStack');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'enable.s3.api', 'false', 'enable Amazon S3 API on CloudStack');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'vmware.use.nexus.vswitch', 'false', 'Enable/Disable Cisco Nexus 1000v vSwitch in VMware environment');
ALTER TABLE `cloud`.`account` ADD COLUMN `default_zone_id` bigint unsigned;
ALTER TABLE `cloud`.`account` ADD CONSTRAINT `fk_account__default_zone_id` FOREIGN KEY `fk_account__default_zone_id`(`default_zone_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE;


DELETE FROM `cloud`.`storage_pool_host_ref` WHERE pool_id IN (SELECT id FROM `cloud`.`storage_pool` WHERE removed IS NOT NULL);

DROP TABLE IF EXISTS `cloud`.`cluster_vsm_map`;
DROP TABLE IF EXISTS `cloud`.`virtual_supervisor_module`;
DROP TABLE IF EXISTS `cloud`.`port_profile`;

CREATE TABLE  `cloud`.`cluster_vsm_map` (
  `cluster_id` bigint unsigned NOT NULL,
  `vsm_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`cluster_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`virtual_supervisor_module` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `host_id` bigint NOT NULL,
  `vsm_name` varchar(255),
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `ipaddr` varchar(80) NOT NULL,
  `management_vlan` int(32),
  `control_vlan` int(32),
  `packet_vlan` int(32),
  `storage_vlan` int(32),
  `vsm_domain_id` bigint unsigned,
  `config_mode` varchar(20),
  `config_state` varchar(20),
  `vsm_device_state` varchar(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`port_profile` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `port_profile_name` varchar(255),
  `port_mode` varchar(10),
  `vsm_id` bigint unsigned NOT NULL,
  `trunk_low_vlan_id` int,
  `trunk_high_vlan_id` int,
  `access_vlan_id` int,
  `port_type` varchar(20) NOT NULL,
  `port_binding` varchar(20),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM `cloud`.`storage_pool_host_ref` WHERE pool_id IN (SELECT id FROM `cloud`.`storage_pool` WHERE removed IS NOT NULL);

ALTER TABLE `cloud`.`service_offering` MODIFY `nw_rate` smallint(5) unsigned DEFAULT '200' COMMENT 'network rate throttle mbits/s';



-- RBD Primary Storage pool support (commit: 406fd95d87bfcdbb282d65589ab1fb6e9fd0018a)
ALTER TABLE `cloud`.`storage_pool` ADD `user_info` VARCHAR( 255 ) NULL COMMENT 'Authorization information for the storage pool. Used by network filesystems' AFTER `host_address`;

-- Resource tags (commit: 62d45b9670520a1ee8b520509393d4258c689b50)
CREATE TABLE `cloud`.`resource_tags` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `key` varchar(255),
  `value` varchar(255),
  `resource_id` bigint unsigned NOT NULL,
  `resource_uuid` varchar(40),
  `resource_type` varchar(255),
  `customer` varchar(255),
  `domain_id` bigint unsigned NOT NULL COMMENT 'foreign key to domain id',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this network',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_tags__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`),
  CONSTRAINT `fk_tags__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`),
  UNIQUE `i_tags__resource_id__resource_type__key`(`resource_id`, `resource_type`, `key`),
  CONSTRAINT `uc_resource_tags__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Nicira Integration (commit: 79c7da07abd4294f150851aa0c2d06a28564c5a9)
CREATE TABLE `cloud`.`external_nicira_nvp_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which nicira nvp device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this nicira nvp device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the nicira nvp device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external nicira nvp device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_nicira_nvp_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_nicira_nvp_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`nicira_nvp_nic_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `logicalswitch` varchar(255) NOT NULL COMMENT 'nicira uuid of logical switch this port is provisioned on',
  `logicalswitchport` varchar(255) UNIQUE COMMENT 'nicira uuid of this logical switch port',
  `nic` varchar(255) UNIQUE COMMENT 'cloudstack uuid of the nic connected to this logical switch port',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- rrq 5839
-- Remove the unique constraint on physical_network_id, provider_name from physical_network_service_providers
-- Because the name of this contraint is not set we need this roundabout way
-- The key is also used by the foreign key constraint so drop and recreate that one
ALTER TABLE `cloud`.`physical_network_service_providers` DROP FOREIGN KEY fk_pnetwork_service_providers__physical_network_id;

SET @constraintname = (select CONCAT(CONCAT('DROP INDEX ', A.CONSTRAINT_NAME), ' ON cloud.physical_network_service_providers' )
from information_schema.key_column_usage A
JOIN information_schema.key_column_usage B ON B.table_name = 'physical_network_service_providers' AND B.COLUMN_NAME = 'provider_name' AND A.COLUMN_NAME ='physical_network_id' AND B.CONSTRAINT_NAME=A.CONSTRAINT_NAME
where A.table_name = 'physical_network_service_providers' LIMIT 1);

PREPARE stmt1 FROM @constraintname; 
EXECUTE stmt1; 
DEALLOCATE PREPARE stmt1; 

AlTER TABLE `cloud`.`physical_network_service_providers` ADD CONSTRAINT `fk_pnetwork_service_providers__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE;
UPDATE `cloud`.`configuration` SET description='In second, timeout for creating volume from snapshot' WHERE name='create.volume.from.snapshot.wait';

ALTER TABLE `cloud`.`data_center` ADD COLUMN `is_local_storage_enabled` tinyint NOT NULL DEFAULT 0 COMMENT 'Is local storage offering enabled for this data center; 1: enabled, 0: not';
UPDATE `cloud`.`data_center` SET `is_local_storage_enabled` = IF ((SELECT `value` FROM `cloud`.`configuration` WHERE `name`='use.local.storage')='true', 1, 0) WHERE `removed` IS NULL;
DELETE FROM `cloud`.`configuration` where name='use.local.storage';

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `max_data_volumes_limit` int unsigned DEFAULT 6 COMMENT 'Max. data volumes per VM supported by hypervisor';
UPDATE `cloud`.`hypervisor_capabilities` SET `max_data_volumes_limit`=13 WHERE `hypervisor_type`='XenServer' AND (`hypervisor_version`='6.0' OR `hypervisor_version`='6.0.2');
INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`) VALUES ('Advanced', 'DEFAULT', 'management-server', 'event.purge.interval', '86400', 'The interval (in seconds) to wait before running the event purge thread');
UPDATE `cloud`.`configuration` SET description='Do URL encoding for the api response, false by default' WHERE name='encode.api.response';



CREATE TABLE `cloud`.`vpc_offerings` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) NOT NULL,
  `unique_name` varchar(64) UNIQUE COMMENT 'unique name of the vpc offering',
  `name` varchar(255) COMMENT 'vpc name',
  `display_text` varchar(255) COMMENT 'display text',
  `state` char(32) COMMENT 'state of the vpc offering that has Disabled value by default',
  `default` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if vpc offering is default',
  `removed` datetime COMMENT 'date removed if not null',
  `created` datetime NOT NULL COMMENT 'date created',
  `service_offering_id` bigint unsigned COMMENT 'service offering id that virtual router is tied to',
  PRIMARY KEY  (`id`),
  INDEX `i_vpc__removed`(`removed`),
  CONSTRAINT `fk_vpc_offerings__service_offering_id` FOREIGN KEY `fk_vpc_offerings__service_offering_id` (`service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`vpc_offering_service_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_offering_id` bigint unsigned NOT NULL COMMENT 'vpc_offering_id',
  `service` varchar(255) NOT NULL COMMENT 'service',
  `provider` varchar(255) COMMENT 'service provider',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_offering_service_map__vpc_offering_id` FOREIGN KEY(`vpc_offering_id`) REFERENCES `vpc_offerings`(`id`) ON DELETE CASCADE,
  UNIQUE (`vpc_offering_id`, `service`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vpc` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) NOT NULL,
  `name` varchar(255) COMMENT 'vpc name',
  `display_text` varchar(255) COMMENT 'vpc display text',
  `cidr` varchar(18) COMMENT 'vpc cidr',
  `vpc_offering_id` bigint unsigned NOT NULL COMMENT 'vpc offering id that this vpc is created from',
  `zone_id` bigint unsigned NOT NULL COMMENT 'the id of the zone this Vpc belongs to',
  `state` varchar(32) NOT NULL COMMENT 'state of the VP (can be Enabled and Disabled)',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain the vpc belongs to',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this vpc',
  `network_domain` varchar(255) COMMENT 'network domain',
  `removed` datetime COMMENT 'date removed if not null',
  `created` datetime NOT NULL COMMENT 'date created',
  `restart_required` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if restart is required for the VPC',
  PRIMARY KEY  (`id`),
  INDEX `i_vpc__removed`(`removed`),
  CONSTRAINT `fk_vpc__zone_id` FOREIGN KEY `fk_vpc__zone_id` (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vpc__vpc_offering_id` FOREIGN KEY (`vpc_offering_id`) REFERENCES `vpc_offerings`(`id`), 
  CONSTRAINT `fk_vpc__account_id` FOREIGN KEY `fk_vpc__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vpc__domain_id` FOREIGN KEY `fk_vpc__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`router_network_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `router_id` bigint unsigned NOT NULL COMMENT 'router id',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  `guest_type` char(32) COMMENT 'type of guest network that can be shared or isolated',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_router_network_ref__networks_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  UNIQUE `i_router_network_ref__router_id__network_id`(`router_id`, `network_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`vpc_gateways` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `ip4_address` char(40) COMMENT 'ip4 address of the gateway',
  `netmask` varchar(15) COMMENT 'netmask of the gateway',
  `gateway` varchar(15) COMMENT 'gateway',
  `vlan_tag` varchar(255),
  `type` varchar(32) COMMENT 'type of gateway; can be Public/Private/Vpn',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id vpc gateway belongs to',
  `vpc_id` bigint unsigned NOT NULL COMMENT 'id of the vpc the gateway belongs to',
  `zone_id` bigint unsigned NOT NULL COMMENT 'id of the zone the gateway belongs to',
  `created` datetime COMMENT 'date created',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `state` varchar(32) NOT NULL COMMENT 'what state the vpc gateway in',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_gateways__network_id` FOREIGN KEY `fk_vpc_gateways__network_id`(`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `fk_vpc_gateways__vpc_id` FOREIGN KEY `fk_vpc_gateways__vpc_id`(`vpc_id`) REFERENCES `vpc`(`id`),
  CONSTRAINT `fk_vpc_gateways__zone_id` FOREIGN KEY `fk_vpc_gateways__zone_id`(`zone_id`) REFERENCES `data_center`(`id`),
  CONSTRAINT `fk_vpc_gateways__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vpc_gateways__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_vpc_gateways__uuid` UNIQUE (`uuid`),
  INDEX `i_vpc_gateways__removed`(`removed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`private_ip_address` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `ip_address` char(40) NOT NULL COMMENT 'ip address',
  `network_id` bigint unsigned NOT NULL COMMENT 'id of the network ip belongs to',
  `reservation_id` char(40) COMMENT 'reservation id',
  `mac_address` varchar(17) COMMENT 'mac address',
  `vpc_id` bigint unsigned COMMENT 'vpc this ip belongs to',
  `taken` datetime COMMENT 'Date taken',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_private_ip_address__vpc_id` FOREIGN KEY `fk_private_ip_address__vpc_id`(`vpc_id`) REFERENCES `vpc`(`id`),
  CONSTRAINT `fk_private_ip_address__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`static_routes` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `vpc_gateway_id` bigint unsigned COMMENT 'id of the corresponding ip address',
  `cidr` varchar(18) COMMENT 'cidr for the static route', 
  `state` char(32) NOT NULL COMMENT 'current state of this rule',
  `vpc_id` bigint unsigned COMMENT 'vpc the firewall rule is associated with',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner id',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id',
  `created` datetime COMMENT 'Date created',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_static_routes__vpc_gateway_id` FOREIGN KEY(`vpc_gateway_id`) REFERENCES `vpc_gateways`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_static_routes__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_static_routes__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_static_routes__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_static_routes__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`networks` ADD COLUMN `vpc_id` bigint unsigned COMMENT 'vpc this network belongs to';
ALTER TABLE `cloud`.`networks`ADD CONSTRAINT `fk_networks__vpc_id` FOREIGN KEY(`vpc_id`) REFERENCES `vpc`(`id`);

ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `vpc_id` bigint unsigned COMMENT 'vpc the firewall rule is associated with';
ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `traffic_type` char(32) COMMENT 'the type of the rule, can be Ingress or Egress';
ALTER TABLE `cloud`.`firewall_rules` MODIFY `ip_address_id` bigint unsigned COMMENT 'id of the corresponding ip address';
ALTER TABLE `cloud`.`firewall_rules` ADD CONSTRAINT `fk_firewall_rules__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE;


ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `vpc_id` bigint unsigned COMMENT 'vpc the ip address is associated with';
ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`domain_router` ADD COLUMN `vpc_id` bigint unsigned COMMENT 'correlated virtual router vpc ID';
ALTER TABLE `cloud`.`domain_router` ADD CONSTRAINT `fk_domain_router__vpc_id` FOREIGN KEY `fk_domain_router__vpc_id`(`vpc_id`) REFERENCES `vpc`(`id`);


ALTER TABLE `cloud`.`physical_network_service_providers` ADD COLUMN `networkacl_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is Network ACL service provided';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vpc.cleanup.interval', '3600', 'The interval (in seconds) between cleanup for Inactive VPCs');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vpc.max.networks', '3', 'Maximum number of networks per vpc');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Account Defaults', 'DEFAULT', 'management-server', 'max.account.vpcs', '20', 'The default maximum number of vpcs that can be created for an account');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.vpcs', '20', 'The default maximum number of vpcs that can be created for a project');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'ha.workers', 5, 'Number of HA worker threads.');

CREATE TABLE `cloud`.`s2s_vpn_gateway` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `addr_id` bigint unsigned NOT NULL,
  `vpc_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_s2s_vpn_gateway__addr_id` FOREIGN KEY (`addr_id`) REFERENCES `user_ip_address` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_s2s_vpn_gateway__vpc_id` FOREIGN KEY (`vpc_id`) REFERENCES `vpc` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_s2s_vpn_gateway__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_s2s_vpn_gateway__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_s2s_vpn_gateway__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`s2s_customer_gateway` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `name` varchar(255) NOT NULL,
  `gateway_ip` char(40) NOT NULL,
  `guest_cidr_list` varchar(200) NOT NULL,
  `ipsec_psk` varchar(256),
  `ike_policy` varchar(30) NOT NULL,
  `esp_policy` varchar(30) NOT NULL,
  `ike_lifetime` int NOT NULL DEFAULT 86400,
  `esp_lifetime` int NOT NULL DEFAULT 3600,
  `dpd` int(1) NOT NULL DEFAULT 0,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_s2s_customer_gateway__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_s2s_customer_gateway__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_s2s_customer_gateway__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`s2s_vpn_connection` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `vpn_gateway_id` bigint unsigned NULL,
  `customer_gateway_id` bigint unsigned NULL,
  `state` varchar(32) NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_s2s_vpn_connection__vpn_gateway_id` FOREIGN KEY (`vpn_gateway_id`) REFERENCES `s2s_vpn_gateway` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_s2s_vpn_connection__customer_gateway_id` FOREIGN KEY (`customer_gateway_id`) REFERENCES `s2s_customer_gateway` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_s2s_vpn_connection__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_s2s_vpn_connection__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_s2s_vpn_connection__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `cloud`.`configuration` SET category='Network' WHERE name='guest.domain.suffix';
UPDATE `cloud`.`configuration` SET component='management-server' WHERE name='agent.lb.enabled';
UPDATE `cloud`.`configuration` SET component='StorageManager' WHERE name='backup.snapshot.wait';
UPDATE `cloud`.`configuration` SET component='StorageManager' WHERE name='copy.volume.wait';
UPDATE `cloud`.`configuration` SET component='StorageManager' WHERE name='create.volume.from.snapshot.wait';
UPDATE `cloud`.`configuration` SET component='TemplateManager' WHERE name='primary.storage.download.wait';
UPDATE `cloud`.`configuration` SET component='StorageManager' WHERE name='storage.cleanup.enabled';
UPDATE `cloud`.`configuration` SET component='StorageManager' WHERE name='storage.cleanup.interval';
UPDATE `cloud`.`configuration` SET description='Comma separated list of cidrs internal to the datacenter that can host template download servers, please note 0.0.0.0 is not a valid site ' WHERE name='secstorage.allowed.internal.sites';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'site2site.vpn.vpngateway.connection.limit', '4', 'The maximum number of VPN connection per VPN gateway');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'site2site.vpn.customergateway.subnets.limit', '10', 'The maximum number of subnets per customer gateway');

INSERT IGNORE INTO `cloud`.`guest_os_category` VALUES ('11','None',NULL); 
ALTER TABLE `cloud`.`user` ADD COLUMN `incorrect_login_attempts` integer unsigned NOT NULL DEFAULT '0';
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'incorrect.login.attempts.allowed', '5', 'Incorrect login attempts allowed before the user is disabled');
UPDATE `cloud`.`configuration` set description ='Uuid of the service offering used by console proxy; if NULL - system offering will be used' where name ='consoleproxy.service.offering';

UPDATE `cloud`.`user` SET PASSWORD=RAND() WHERE id=1;
ALTER TABLE `cloud_usage`.`account` ADD COLUMN `default_zone_id` bigint unsigned;
