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


#Schema upgrade from 3.0.2 to 3.0.3;

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
ALTER TABLE `cloud_usage`.`account` ADD COLUMN `default_zone_id` bigint unsigned;

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

DELETE FROM `cloud`.`storage_pool_host_ref` WHERE pool_id IN (SELECT id FROM storage_pool WHERE removed IS NOT NULL);

ALTER TABLE `cloud`.`service_offering` MODIFY `nw_rate` smallint(5) unsigned DEFAULT '200' COMMENT 'network rate throttle mbits/s';


INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (141, 1, 'CentOS 5.6 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (142, 1, 'CentOS 5.6 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (143, 1, 'CentOS 6.0 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (144, 1, 'CentOS 6.0 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (145, 3, 'Oracle Enterprise Linux 5.6 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (146, 3, 'Oracle Enterprise Linux 5.6 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (147, 3, 'Oracle Enterprise Linux 6.0 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (148, 3, 'Oracle Enterprise Linux 6.0 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (149, 4, 'Red Hat Enterprise Linux 5.6 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (150, 4, 'Red Hat Enterprise Linux 5.6 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (151, 5, 'SUSE Linux Enterprise Server 10 SP3 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (152, 5, 'SUSE Linux Enterprise Server 10 SP4 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (153, 5, 'SUSE Linux Enterprise Server 10 SP4 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (154, 5, 'SUSE Linux Enterprise Server 11 SP1 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (155, 5, 'SUSE Linux Enterprise Server 11 SP1 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (156, 10, 'Ubuntu 10.10 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (157, 10, 'Ubuntu 10.10 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (161, 1, 'CentOS 5.7 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (162, 1, 'CentOS 5.7 (64-bit)');
