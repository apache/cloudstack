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
  `uuid` varchar(40) NOT NULL,
  `name` varchar(255) NOT NULL COMMENT 'backup offering name',
  `description` varchar(255) NOT NULL COMMENT 'backup offering description',
  `external_id` varchar(80) NOT NULL COMMENT 'external ID on provider side',
  `zone_id` bigint(20) unsigned NOT NULL COMMENT 'zone id',
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fk_backup_offering__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`backups` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `offering_id` bigint(20) unsigned NOT NULL,
  `external_id` varchar(80) COMMENT 'backup ID on provider side',
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

CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_backup` (
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
  INDEX `i_usage_backup` (`zone_id`,`account_id`,`backup_id`,`vm_id`,`created`)
) ENGINE=InnoDB CHARSET=utf8;
