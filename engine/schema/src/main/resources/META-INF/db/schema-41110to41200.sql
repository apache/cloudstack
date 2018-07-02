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
-- Schema upgrade from 4.11.1.0 to 4.12.0.0
--;

-- [CLOUDSTACK-10314] Add reason column to ACL rule table
ALTER TABLE `cloud`.`network_acl_item` ADD COLUMN `reason` VARCHAR(2500) AFTER `display`;

-- [CLOUDSTACK-9846] Make provision to store content and subject for Alerts in separate columns.
ALTER TABLE `cloud`.`alert` ADD COLUMN `content` VARCHAR(5000);

-- Fix the name of the column used to hold IPv4 range in 'vlan' table.
ALTER TABLE `vlan` CHANGE `description` `ip4_range` varchar(255);

-- [CLOUDSTACK-10344] bug when moving ACL rules (change order with drag and drop)
-- We are only adding the permission to the default rules. Any custom rule must be configured by the root admin.
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'moveNetworkAclItem', 'ALLOW', 100) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'moveNetworkAclItem', 'ALLOW', 302) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'moveNetworkAclItem', 'ALLOW', 260) ON DUPLICATE KEY UPDATE rule=rule;

UPDATE `cloud`.`async_job` SET `removed` = now() WHERE `removed` IS NULL;

-- Backup and Recovery

CREATE TABLE IF NOT EXISTS `cloud`.`backup_policy` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL,
  `name` varchar(255) NOT NULL COMMENT 'backup policy name',
  `description` varchar(255) NOT NULL COMMENT 'backup policy description',
  `external_id` varchar(80) NOT NULL COMMENT 'backup policy ID on provider side',
  `zone_id` bigint(20) unsigned NOT NULL COMMENT 'zone id',
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fk_backup_policy__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`vm_backup` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL,
  `name` varchar(255) NOT NULL COMMENT 'backup name',
  `description` varchar(255) COMMENT 'backup description',
  `external_id` varchar(80) COMMENT 'backup ID on provider side',
  `policy_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `volumes` varchar(5100),
  `status` varchar(20) NOT NULL,
  `size` bigint(20) DEFAULT 0,
  `protected_size` bigint(20) DEFAULT 0,
  `account_id` bigint(20) unsigned NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_backup__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_backup__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_vm_backup` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `zone_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `backup_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `size` bigint(20) DEFAULT 0,
  `protected_size` bigint(20) DEFAULT 0,
  `created` datetime NOT NULL,
  `removed` datetime,
  PRIMARY KEY (`id`),
  INDEX `i_usage_vmbackup` (`zone_id`,`account_id`,`backup_id`,`vm_id`,`created`)
) ENGINE=InnoDB CHARSET=utf8;
