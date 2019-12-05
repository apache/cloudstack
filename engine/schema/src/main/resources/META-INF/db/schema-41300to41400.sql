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
-- Schema upgrade from 4.13.0.0 to 4.14.0.0
--;

-- KVM: enable storage data motion on KVM hypervisor_capabilities
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported` = 1 WHERE `hypervisor_capabilities`.`hypervisor_type` = 'KVM';

-- Backup and Recovery

CREATE TABLE IF NOT EXISTS `cloud`.`backup_offering` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL COMMENT 'backup offering name',
  `description` varchar(255) NOT NULL COMMENT 'backup offering description',
  `external_id` varchar(255) NOT NULL COMMENT 'external ID on provider side',
  `zone_id` bigint(20) unsigned NOT NULL COMMENT 'zone id',
  `provider` varchar(255) NOT NULL COMMENT 'backup provider',
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fk_backup_offering__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `backup_offering_id` bigint unsigned DEFAULT NULL COMMENT 'ID of backup offering';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `backup_external_id` varchar(255) DEFAULT NULL COMMENT 'ID of external backup job or container if any';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `backup_volumes` text DEFAULT NULL COMMENT 'details of backedup volumes';

CREATE TABLE IF NOT EXISTS `cloud`.`backups` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL UNIQUE,
  `vm_id` bigint(20) unsigned NOT NULL,
  `external_id` varchar(255) NOT NULL COMMENT 'external ID',
  `type` varchar(255) NOT NULL COMMENT 'backup type',
  `date` varchar(255) NOT NULL COMMENT 'backup date',
  `size` bigint(20) DEFAULT 0 COMMENT 'size of the backup',
  `protected_size` bigint(20) DEFAULT 0,
  `status` varchar(32) DEFAULT NULL COMMENT 'backup status',
  `backup_offering_id` bigint(20) unsigned NOT NULL COMMENT,
  `account_id` bigint(20) unsigned NOT NULL COMMENT,
  `domain_id` bigint(20) unsigned NOT NULL COMMENT,
  `zone_id` bigint(20) unsigned NOT NULL,
  `removed` datetime DEFAULT NULL COMMENT 'Date removed.  not null if removed',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_backup__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`backup_schedule` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vm_id` bigint(20) unsigned NOT NULL UNIQUE,
  `schedule_type` int(4) DEFAULT NULL COMMENT 'backup schedulet type e.g. hourly, daily, etc.',
  `schedule` varchar(100) DEFAULT NULL COMMENT 'schedule time of execution',
  `timezone` varchar(100) DEFAULT NULL COMMENT 'the timezone in which the schedule time is specified',
  `scheduled_timestamp` datetime DEFAULT NULL COMMENT 'Time at which the backup was scheduled for execution',
  `async_job_id` bigint(20) unsigned DEFAULT NULL COMMENT 'If this schedule is being executed, it is the id of the create aysnc_job. Before that it is null',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_backup` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `zone_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `size` bigint(20) DEFAULT 0,
  `protected_size` bigint(20) DEFAULT 0,
  `created` datetime NOT NULL,
  `removed` datetime,
  PRIMARY KEY (`id`),
  INDEX `i_usage_backup` (`zone_id`,`account_id`,`vm_id`,`created`)
) ENGINE=InnoDB CHARSET=utf8;
