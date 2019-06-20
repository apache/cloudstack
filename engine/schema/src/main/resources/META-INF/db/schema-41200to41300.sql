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
-- Schema upgrade from 4.12.0.0 to 4.13.0.0
--;

-- DPDK client and server mode support
ALTER TABLE `cloud`.`service_offering_details` CHANGE COLUMN `value` `value` TEXT NOT NULL;

-- Add `sort_key` column to data_center
ALTER TABLE `cloud`.`data_center` ADD COLUMN `sort_key` INT(32) NOT NULL DEFAULT 0;

-- Recreate data_center_view
DROP VIEW IF EXISTS `cloud`.`data_center_view`;
CREATE VIEW `cloud`.`data_center_view` AS
    select
        data_center.id,
        data_center.uuid,
        data_center.name,
        data_center.is_security_group_enabled,
        data_center.is_local_storage_enabled,
        data_center.description,
        data_center.dns1,
        data_center.dns2,
        data_center.ip6_dns1,
        data_center.ip6_dns2,
        data_center.internal_dns1,
        data_center.internal_dns2,
        data_center.guest_network_cidr,
        data_center.domain,
        data_center.networktype,
        data_center.allocation_state,
        data_center.zone_token,
        data_center.dhcp_provider,
        data_center.removed,
        data_center.sort_key,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        dedicated_resources.affinity_group_id,
        dedicated_resources.account_id,
        affinity_group.uuid affinity_group_uuid
    from
        `cloud`.`data_center`
            left join
        `cloud`.`domain` ON data_center.domain_id = domain.id
            left join
        `cloud`.`dedicated_resources` ON data_center.id = dedicated_resources.data_center_id
            left join
        `cloud`.`affinity_group` ON dedicated_resources.affinity_group_id = affinity_group.id;

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
  `volumes` text,
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