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
-- Schema upgrade from 4.20.1.0 to 4.21.0.0
--;

-- Add console_endpoint_creator_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'console_endpoint_creator_address', 'VARCHAR(45)');

-- Add client_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'client_address', 'VARCHAR(45)');

-- Allow default roles to use quotaCreditsList
INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaCreditsList', permission, sort_order
FROM `cloud`.`role_permissions` rp
WHERE rp.rule = 'quotaStatement'
AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaCreditsList');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host', 'last_mgmt_server_id', 'bigint unsigned DEFAULT NULL COMMENT "last management server this host is connected to" AFTER `mgmt_server_id`');

-- Add column max_backup to backup_schedule table
ALTER TABLE `cloud`.`backup_schedule` ADD COLUMN `max_backups` int(8) default NULL COMMENT 'maximum number of backups to maintain';

-- Add columns name, description and backup_interval_type to backup table
ALTER TABLE `cloud`.`backups` ADD COLUMN `backup_interval_type` int(5) COMMENT 'type of backup, e.g. manual, recurring - hourly, daily, weekly or monthly';
ALTER TABLE `cloud`.`backups` ADD COLUMN `name` varchar(255) NOT NULL COMMENT 'name of the backup';
ALTER TABLE `cloud`.`backups` ADD COLUMN `vm_name` varchar(255) COMMENT 'name of the vm for which backup is taken, only set for orphaned backups';
ALTER TABLE `cloud`.`backups` ADD COLUMN `description` varchar(1024) COMMENT 'description for the backup';
UPDATE `cloud`.`backups` JOIN `cloud`.`vm_instance` ON `backups`.`vm_id` = `vm_instance`.`id` SET `backups`.`name` = `vm_instance`.`name`;

-- Make the column vm_id in backups table nullable to handle orphan backups
ALTER TABLE `cloud`.`backups` MODIFY COLUMN `vm_id` BIGINT UNSIGNED NULL;

-- Create backup details table
CREATE TABLE `cloud`.`backup_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `backup_id` bigint unsigned NOT NULL COMMENT 'backup id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup_details__backup_id` FOREIGN KEY `fk_backup_details__backup_id`(`backup_id`) REFERENCES `backups`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add column allocated_size to object_store table. Rename column 'used_bytes' to 'used_size'
ALTER TABLE `cloud`.`object_store` ADD COLUMN `allocated_size` bigint unsigned COMMENT 'allocated size in bytes';
ALTER TABLE `cloud`.`object_store` CHANGE COLUMN `used_bytes` `used_size` BIGINT UNSIGNED COMMENT 'used size in bytes';
ALTER TABLE `cloud`.`object_store` MODIFY COLUMN `total_size` bigint unsigned COMMENT 'total size in bytes';
UPDATE `cloud`.`object_store`
JOIN (
    SELECT object_store_id, SUM(quota) AS total_quota
    FROM `cloud`.`bucket`
    WHERE removed IS NULL
    GROUP BY object_store_id
) buckets_quota_sum_view ON `object_store`.id = buckets_quota_sum_view.object_store_id
SET `object_store`.allocated_size = buckets_quota_sum_view.total_quota;
