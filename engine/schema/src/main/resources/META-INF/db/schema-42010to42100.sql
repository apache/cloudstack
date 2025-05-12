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

-- Add table for reconcile commands
CREATE TABLE IF NOT EXISTS `cloud`.`reconcile_commands` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `management_server_id` bigint unsigned NOT NULL COMMENT 'node id of the management server',
    `host_id` bigint unsigned NOT NULL COMMENT 'id of the host',
    `request_sequence` bigint unsigned NOT NULL COMMENT 'sequence of the request',
    `resource_id` bigint unsigned DEFAULT NULL COMMENT 'id of the resource',
    `resource_type` varchar(255) COMMENT 'type if the resource',
    `state_by_management` varchar(255) COMMENT 'state of the command updated by management server',
    `state_by_agent` varchar(255) COMMENT 'state of the command updated by cloudstack agent',
    `command_name` varchar(255) COMMENT 'name of the command',
    `command_info` MEDIUMTEXT COMMENT 'info of the command',
    `answer_name` varchar(255) COMMENT 'name of the answer',
    `answer_info` MEDIUMTEXT COMMENT 'info of the answer',
    `created` datetime COMMENT 'date the reconcile command was created',
    `removed` datetime COMMENT 'date the reconcile command was removed',
    `updated` datetime COMMENT 'date the reconcile command was updated',
    `retry_count` bigint unsigned DEFAULT 0 COMMENT 'The retry count of reconciliation',
    PRIMARY KEY(`id`),
    INDEX `i_reconcile_command__host_id`(`host_id`),
    CONSTRAINT `fk_reconcile_command__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add column max_backup to backup_schedule table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backup_schedule', 'max_backups', 'int(8) default NULL COMMENT "maximum number of backups to maintain"');

-- Add columns name, description and backup_interval_type to backup table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'name', 'VARCHAR(255) NULL COMMENT "name of the backup"');
UPDATE `cloud`.`backups` backup INNER JOIN `cloud`.`vm_instance` vm ON backup.vm_id = vm.id SET backup.name = vm.name;
ALTER TABLE `cloud`.`backups` MODIFY COLUMN `name` VARCHAR(255) NOT NULL;
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'description', 'VARCHAR(1024) COMMENT "description for the backup"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.backups', 'backup_interval_type', 'int(5) COMMENT "type of backup, e.g. manual, recurring - hourly, daily, weekly or monthly"');

-- Make the column vm_id in backups table nullable to handle orphan backups
ALTER TABLE `cloud`.`backups` MODIFY COLUMN `vm_id` BIGINT UNSIGNED NULL;

-- Create backup details table
CREATE TABLE IF NOT EXISTS `cloud`.`backup_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `backup_id` bigint unsigned NOT NULL COMMENT 'backup id',
  `name` varchar(255) NOT NULL,
  `value` TEXT NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup_details__backup_id` FOREIGN KEY `fk_backup_details__backup_id`(`backup_id`) REFERENCES `backups`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add diskOfferingId, deviceId, minIops and maxIops to backed_volumes in backups table
UPDATE `cloud`.`backups` b
INNER JOIN `cloud`.`vm_instance` vm ON b.vm_id = vm.id
SET b.backed_volumes = (
    SELECT CONCAT("[",
        GROUP_CONCAT(
            CONCAT(
                "{\"uuid\":\"", v.uuid, "\",",
                "\"type\":\"", v.volume_type, "\",",
                "\"size\":", v.`size`, ",",
                "\"path\":\"", IFNULL(v.path, 'null'), "\",",
                "\"deviceId\":", IFNULL(v.device_id, 'null'), ",",
                "\"diskOfferingId\":\"", doff.uuid, "\",",
                "\"minIops\":", IFNULL(v.min_iops, 'null'), ",",
                "\"maxIops\":", IFNULL(v.max_iops, 'null'),
                "}"
            )
            SEPARATOR ","
        ),
    "]")
    FROM `cloud`.`volumes` v
    LEFT JOIN `cloud`.`disk_offering` doff ON v.disk_offering_id = doff.id
    WHERE v.instance_id = vm.id
);

-- Add diskOfferingId, deviceId, minIops and maxIops to backup_volumes in vm_instance table
UPDATE `cloud`.`vm_instance` vm
SET vm.backup_volumes = (
    SELECT CONCAT("[", 
        GROUP_CONCAT(
            CONCAT(
                "{\"uuid\":\"", v.uuid, "\",",
                "\"type\":\"", v.volume_type, "\",",
                "\"size\":", v.`size`, ",",
                "\"path\":\"", IFNULL(v.path, 'null'), "\",",
                "\"deviceId\":", IFNULL(v.device_id, 'null'), ",",
                "\"diskOfferingId\":\"", doff.uuid, "\",",
                "\"minIops\":", IFNULL(v.min_iops, 'null'), ",",
                "\"maxIops\":", IFNULL(v.max_iops, 'null'),
                "}"
            )
            SEPARATOR ","
        ), 
    "]") 
    FROM `cloud`.`volumes` v 
    LEFT JOIN `cloud`.`disk_offering` doff ON v.disk_offering_id = doff.id 
    WHERE v.instance_id = vm.id
) 
WHERE vm.backup_offering_id IS NOT NULL;

-- Add column allocated_size to object_store table. Rename column 'used_bytes' to 'used_size'
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.object_store', 'allocated_size', 'bigint unsigned COMMENT "allocated size in bytes"');
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
