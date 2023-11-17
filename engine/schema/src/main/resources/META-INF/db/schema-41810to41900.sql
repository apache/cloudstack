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
-- Schema upgrade from 4.18.1.0 to 4.19.0.0
--;

ALTER TABLE `cloud`.`mshost` MODIFY COLUMN `state` varchar(25);

-- Invalidate existing console_session records
UPDATE `cloud`.`console_session` SET removed=now();
-- Modify acquired column in console_session to datetime type
ALTER TABLE `cloud`.`console_session` DROP `acquired`, ADD `acquired` datetime COMMENT 'When the session was acquired' AFTER `host_id`;

-- IP quarantine PR#7378
CREATE TABLE IF NOT EXISTS `cloud`.`quarantined_ips` (
  `id` bigint(20) unsigned NOT NULL auto_increment,
  `uuid` varchar(255) UNIQUE,
  `public_ip_address_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the quarantined public IP address, foreign key to `user_ip_address` table',
  `previous_owner_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the previous owner of the public IP address, foreign key to `account` table',
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  `end_date` datetime NOT NULL,
  `removal_reason` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_quarantined_ips__public_ip_address_id` FOREIGN KEY(`public_ip_address_id`) REFERENCES `cloud`.`user_ip_address`(`id`),
  CONSTRAINT `fk_quarantined_ips__previous_owner_id` FOREIGN KEY(`previous_owner_id`) REFERENCES `cloud`.`account`(`id`)
);

-- create_public_parameter_on_roles. #6960
ALTER TABLE `cloud`.`roles` ADD COLUMN `public_role` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Indicates whether the role will be visible to all users (public) or only to root admins (private). If this parameter is not specified during the creation of the role its value will be defaulted to true (public).';

-- Add tables for VM Scheduler
DROP TABLE IF EXISTS `cloud`.`vm_schedule`;
CREATE TABLE `cloud`.`vm_schedule` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL,
  `uuid` varchar(40) NOT NULL COMMENT 'schedule uuid',
  `description` varchar(1024) COMMENT 'description of the vm schedule',
  `schedule` varchar(255) NOT NULL COMMENT 'schedule frequency in cron format',
  `timezone` varchar(100) NOT NULL COMMENT 'the timezone in which the schedule time is specified',
  `action` varchar(20) NOT NULL COMMENT 'action to perform',
  `enabled` int(1) NOT NULL COMMENT 'Enabled or disabled',
  `start_date` datetime NOT NULL COMMENT 'start time for this schedule',
  `end_date` datetime COMMENT 'end time for this schedule',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  INDEX `i_vm_schedule__vm_id`(`vm_id`),
  INDEX `i_vm_schedule__enabled_end_date`(`enabled`, `end_date`),
  CONSTRAINT `fk_vm_schedule__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`vm_scheduled_job`;
CREATE TABLE `cloud`.`vm_scheduled_job` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL,
  `vm_schedule_id` bigint unsigned NOT NULL,
  `uuid` varchar(40) NOT NULL COMMENT 'scheduled job uuid',
  `action` varchar(20) NOT NULL COMMENT 'action to perform',
  `scheduled_timestamp` datetime NOT NULL COMMENT 'Time at which the action is taken',
  `async_job_id` bigint unsigned DEFAULT NULL COMMENT 'If this schedule is being executed, it is the id of the create aysnc_job. Before that it is null',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`vm_schedule_id`, `scheduled_timestamp`),
  INDEX `i_vm_scheduled_job__scheduled_timestamp`(`scheduled_timestamp`),
  INDEX `i_vm_scheduled_job__vm_id`(`vm_id`),
  CONSTRAINT `fk_vm_scheduled_job__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vm_scheduled_job__vm_schedule_id` FOREIGN KEY (`vm_schedule_id`) REFERENCES `vm_schedule`(`id`) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add support for different cluster types for kubernetes
ALTER TABLE `cloud`.`kubernetes_cluster` ADD COLUMN `cluster_type` varchar(64) DEFAULT 'CloudManaged' COMMENT 'type of cluster';
ALTER TABLE `cloud`.`kubernetes_cluster` MODIFY COLUMN `kubernetes_version_id` bigint unsigned NULL COMMENT 'the ID of the Kubernetes version of this Kubernetes cluster';

-- Add indexes for data store browser
ALTER TABLE `cloud`.`template_spool_ref` ADD INDEX `i_template_spool_ref__install_path`(`install_path`);
ALTER TABLE `cloud`.`volumes` ADD INDEX `i_volumes__path`(`path`);
ALTER TABLE `cloud`.`snapshot_store_ref` ADD INDEX `i_snapshot_store_ref__install_path`(`install_path`);
ALTER TABLE `cloud`.`template_store_ref` ADD INDEX `i_template_store_ref__install_path`(`install_path`);

-- Add table for image store object download
DROP TABLE IF EXISTS `cloud`.`image_store_object_download`;
CREATE TABLE `cloud`.`image_store_object_download` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `store_id` bigint unsigned NOT NULL COMMENT 'image store id',
  `path` varchar(255) NOT NULL COMMENT 'path on store',
  `download_url` varchar(255) NOT NULL COMMENT 'download url',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  UNIQUE KEY (`store_id`, `path`),
  INDEX `i_image_store_object_download__created`(`created`),
  CONSTRAINT `fk_image_store_object_download__store_id` FOREIGN KEY (`store_id`) REFERENCES `image_store`(`id`) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Set removed state for all removed accounts
UPDATE `cloud`.`account` SET state='removed' WHERE `removed` IS NOT NULL;


-- New tables for VNF
CREATE TABLE IF NOT EXISTS `cloud`.`vnf_template_nics` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `template_id` bigint unsigned NOT NULL COMMENT 'id of the VNF template',
    `device_id` bigint unsigned NOT NULL COMMENT 'Device id of the NIC when plugged into the VNF appliances',
    `device_name` varchar(1024) NOT NULL COMMENT 'Name of the NIC',
    `required` tinyint NOT NULL DEFAULT '1' COMMENT 'True if the NIC is required. False if optional',
    `management` tinyint NOT NULL DEFAULT '1' COMMENT 'True if the NIC is a management interface',
    `description` varchar(1024) COMMENT 'Description of the NIC',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_id_device_id` (`template_id`, `device_id`),
    KEY `fk_vnf_template_nics__template_id` (`template_id`),
    CONSTRAINT `fk_vnf_template_nics__template_id` FOREIGN KEY (`template_id`) REFERENCES `vm_template` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cloud`.`vnf_template_details` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `template_id` bigint unsigned NOT NULL COMMENT 'id of the VNF template',
    `name` varchar(255) NOT NULL,
    `value` varchar(1024) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
    PRIMARY KEY (`id`),
    KEY `fk_vnf_template_details__template_id` (`template_id`),
    CONSTRAINT `fk_vnf_template_details__template_id` FOREIGN KEY (`template_id`) REFERENCES `vm_template` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add tables for Cluster DRS
DROP TABLE IF EXISTS `cloud`.`cluster_drs_plan`;
CREATE TABLE `cloud`.`cluster_drs_plan` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `cluster_id` bigint unsigned NOT NULL,
  `event_id` bigint unsigned NOT NULL,
  `uuid` varchar(40) NOT NULL COMMENT 'schedule uuid',
  `type` varchar(20) NOT NULL COMMENT 'type of plan',
  `status` varchar(20) NOT NULL COMMENT 'status of plan',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  INDEX `i_cluster_drs_plan__cluster_id_status`(`cluster_id`, `status`),
  INDEX `i_cluster_drs_plan__status`(`status`),
  INDEX `i_cluster_drs_plan__created`(`created`),
  CONSTRAINT `fk_cluster_drs_plan__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster`(`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

DROP TABLE IF EXISTS `cloud`.`cluster_drs_plan_migration`;
CREATE TABLE `cloud`.`cluster_drs_plan_migration` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `plan_id` bigint unsigned NOT NULL,
  `vm_id` bigint unsigned NOT NULL,
  `src_host_id` bigint unsigned NOT NULL,
  `dest_host_id` bigint unsigned NOT NULL,
  `job_id` bigint unsigned NULL,
  `status` varchar(20) NULL COMMENT 'status of async job',
  PRIMARY KEY (`id`),
  INDEX `i_cluster_drs_plan_migration__plan_id_status`(`plan_id`, `status`),
  CONSTRAINT `fk_cluster_drs_plan_migration__plan_id` FOREIGN KEY (`plan_id`) REFERENCES `cluster_drs_plan`(`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

INSERT INTO `cloud`.`configuration_subgroup` (`name`, `keywords`, `precedence`, `group_id`) VALUES ('DRS', 'drs', 4, (SELECT id FROM `cloud`.`configuration_group` WHERE `name` = 'Miscellaneous'));

UPDATE `cloud`.`configuration`
    SET subgroup_id = (SELECT id FROM `cloud`.`configuration_subgroup` WHERE name = 'DRS')
    WHERE name IN ('drs.automatic.enable', 'drs.algorithm', 'drs.automatic.interval', 'drs.max.migrations', 'drs.imbalance', 'drs.metric', 'drs.plan.expire.interval');

-- Add table for snapshot zone reference
CREATE TABLE  `cloud`.`snapshot_zone_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `zone_id` bigint unsigned NOT NULL,
  `snapshot_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_snapshot_zone_ref__zone_id` FOREIGN KEY `fk_snapshot_zone_ref__zone_id` (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
  INDEX `i_snapshot_zone_ref__zone_id`(`zone_id`),
  CONSTRAINT `fk_snapshot_zone_ref__snapshot_id` FOREIGN KEY `fk_snapshot_zone_ref__snapshot_id` (`snapshot_id`) REFERENCES `snapshots` (`id`) ON DELETE CASCADE,
  INDEX `i_snapshot_zone_ref__snapshot_id`(`snapshot_id`),
  INDEX `i_snapshot_zone_ref__removed`(`removed`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- Alter snapshot_store_ref table to add download related fields
ALTER TABLE `cloud`.`snapshot_store_ref`
    ADD COLUMN `download_state` varchar(255) DEFAULT NULL COMMENT 'the state of the snapshot download' AFTER `volume_id`,
    ADD COLUMN `download_pct` int unsigned DEFAULT NULL COMMENT 'the percentage of the snapshot download completed' AFTER `download_state`,
    ADD COLUMN `error_str` varchar(255) DEFAULT NULL COMMENT 'the error message when the snapshot download occurs' AFTER `download_pct`,
    ADD COLUMN `local_path` varchar(255) DEFAULT NULL COMMENT 'the path of the snapshot download' AFTER `error_str`,
    ADD COLUMN `display` tinyint(1) unsigned NOT NULL DEFAULT 1  COMMENT '1 implies store reference is available for listing' AFTER `error_str`;
