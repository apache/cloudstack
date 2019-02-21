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
-- Schema upgrade from 4.8.1 to 4.9.0;
--;

ALTER TABLE `event` ADD INDEX `archived` (`archived`);
ALTER TABLE `event` ADD INDEX `state` (`state`);



-- Add cluster.storage.operations.exclude property
INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `description`, `default_value`, `updated`, `scope`, `is_dynamic`) VALUES ('Advanced', 'DEFAULT', 'CapacityManager', 'cluster.storage.operations.exclude', 'Exclude cluster from storage operations', 'false', now(), 'Cluster', '1');

-- 3) Missing indexes (Add indexes to avoid full table scans)
ALTER TABLE `cloud`.`op_it_work` ADD INDEX `i_type_and_updated` (`type` ASC, `updated_at` ASC);
ALTER TABLE `cloud`.`vm_root_disk_tags` ADD INDEX `i_vm_id` (`vm_id` ASC);
ALTER TABLE `cloud`.`vm_compute_tags` ADD INDEX `i_vm_id` (`vm_id` ASC);
ALTER TABLE `cloud`.`vm_network_map` ADD INDEX `i_vm_id` (`vm_id` ASC);
ALTER TABLE `cloud`.`ssh_keypairs` ADD INDEX `i_public_key` (`public_key` (64) ASC);
ALTER TABLE `cloud`.`user_vm_details` ADD INDEX `i_name_vm_id` (`vm_id` ASC, `name` ASC);
ALTER TABLE `cloud`.`instance_group` ADD INDEX `i_name` (`name` ASC);

-- 4) Some views query (Change view to improve account retrieval speed)
CREATE OR REPLACE
VIEW `account_vmstats_view` AS
    SELECT
        `vm_instance`.`account_id` AS `account_id`,
        `vm_instance`.`state` AS `state`,
        COUNT(0) AS `vmcount`
    FROM
        `vm_instance`
    WHERE
        (`vm_instance`.`vm_type` = 'User' and `vm_instance`.`removed` is NULL)
    GROUP BY `vm_instance`.`account_id` , `vm_instance`.`state`;
-- End CLOUDSTACK-9340

-- Dynamic roles
CREATE TABLE IF NOT EXISTS `cloud`.`roles` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) COMMENT 'unique name of the dynamic role',
  `role_type` varchar(255) NOT NULL COMMENT 'the type of the role',
  `removed` datetime COMMENT 'date removed',
  `description` text COMMENT 'description of the role',
  PRIMARY KEY (`id`),
  KEY `i_roles__name` (`name`),
  KEY `i_roles__role_type` (`role_type`),
  UNIQUE KEY (`name`, `role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`role_permissions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `role_id` bigint(20) unsigned NOT NULL COMMENT 'id of the role',
  `rule` varchar(255) NOT NULL COMMENT 'rule for the role, api name or wildcard',
  `permission` varchar(255) NOT NULL COMMENT 'access authority, allow or deny',
  `description` text COMMENT 'description of the rule',
  `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'permission sort order',
  PRIMARY KEY (`id`),
  KEY `fk_role_permissions__role_id` (`role_id`),
  KEY `i_role_permissions__sort_order` (`sort_order`),
  UNIQUE KEY (`role_id`, `rule`),
  CONSTRAINT `fk_role_permissions__role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Default CloudStack roles
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (1, UUID(), 'Root Admin', 'Admin', 'Default root admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (2, UUID(), 'Resource Admin', 'ResourceAdmin', 'Default resource admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (3, UUID(), 'Domain Admin', 'DomainAdmin', 'Default domain admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (4, UUID(), 'User', 'User', 'Default Root Admin role') ON DUPLICATE KEY UPDATE name=name;

-- Out-of-band management
CREATE TABLE IF NOT EXISTS `cloud`.`oobm` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `host_id` bigint(20) unsigned DEFAULT NULL COMMENT 'foreign key to host',
  `enabled` int(1) unsigned DEFAULT '0' COMMENT 'is out-of-band management enabled for host',
  `power_state` varchar(32) DEFAULT 'Disabled' COMMENT 'out-of-band management power status',
  `driver` varchar(32) DEFAULT NULL COMMENT 'out-of-band management driver',
  `address` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface address',
  `port` int(10) unsigned DEFAULT NULL COMMENT 'out-of-band management interface port',
  `username` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface username',
  `password` varchar(255) DEFAULT NULL COMMENT 'out-of-band management interface password',
  `update_count` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'atomic increase count making status update operation atomical',
  `update_time` datetime COMMENT 'last power state update datetime',
  `mgmt_server_id` bigint(20) unsigned DEFAULT NULL COMMENT 'management server id which owns out-of-band management for the host',
  PRIMARY KEY (`id`),
  KEY `fk_oobm__host_id` (`host_id`),
  KEY `i_oobm__enabled` (`enabled`),
  KEY `i_oobm__power_state` (`power_state`),
  KEY `i_oobm__update_time` (`update_time`),
  KEY `i_oobm__mgmt_server_id` (`mgmt_server_id`),
  CONSTRAINT `fk_oobm__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 171, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 172, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 177, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 178, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 179, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 180, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 181, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 182, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centosGuest', 227, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.0', 'centos64Guest', 228, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '4.1', 'centos64Guest', 228, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.0', 'centos64Guest', 228, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.1', 'centos64Guest', 228, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 228, now(), 0);
