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
-- Schema upgrade from 4.2.0 to 4.3.0;
--;

-- Disable foreign key checking
SET foreign_key_checks = 0;

ALTER TABLE `cloud`.`async_job` ADD COLUMN `related` CHAR(40) NOT NULL;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `session_key`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `job_cmd_originator`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `callback_type`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `callback_address`;

ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_type` VARCHAR(32);
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_dispatcher` VARCHAR(64);
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_executing_msid` bigint;
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_pending_signals` int(10) NOT NULL DEFAULT 0;

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state` VARCHAR(74) DEFAULT 'PowerUnknown';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state_update_time` DATETIME;
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state_update_count` INT DEFAULT 0;
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_host` bigint unsigned;
ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__power_host` FOREIGN KEY (`power_host`) REFERENCES `cloud`.`host`(`id`);

CREATE TABLE `cloud`.`vm_work_job` (
  `id` bigint unsigned UNIQUE NOT NULL,
  `step` char(32) NOT NULL COMMENT 'state',
  `vm_type` char(32) NOT NULL COMMENT 'type of vm',
  `vm_instance_id` bigint unsigned NOT NULL COMMENT 'vm instance',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vm_work_job__instance_id` FOREIGN KEY (`vm_instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
  INDEX `i_vm_work_job__vm`(`vm_type`, `vm_instance_id`),
  INDEX `i_vm_work_job__step`(`step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`async_job_journal` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `job_id` bigint unsigned NOT NULL,
  `journal_type` varchar(32),
  `journal_text` varchar(1024) COMMENT 'journal descriptive informaton',
  `journal_obj` varchar(1024) COMMENT 'journal strutural information, JSON encoded object',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_async_job_journal__job_id` FOREIGN KEY (`job_id`) REFERENCES `async_job`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`async_job_join_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `job_id` bigint unsigned NOT NULL,
  `join_job_id` bigint unsigned NOT NULL,
  `join_status` int NOT NULL,
  `join_result` varchar(1024),
  `join_msid` bigint,
  `complete_msid` bigint,
  `sync_source_id` bigint COMMENT 'upper-level job sync source info before join',
  `wakeup_handler` varchar(64),
  `wakeup_dispatcher` varchar(64),
  `wakeup_interval` bigint NOT NULL DEFAULT 3000 COMMENT 'wakeup interval in seconds',
  `created` datetime NOT NULL,
  `last_updated` datetime,
  `next_wakeup` datetime,
  `expiration` datetime,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_async_job_join_map__job_id` FOREIGN KEY (`job_id`) REFERENCES `async_job`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_async_job_join_map__join_job_id` FOREIGN KEY (`join_job_id`) REFERENCES `async_job`(`id`),
  CONSTRAINT `fk_async_job_join_map__join` UNIQUE (`job_id`, `join_job_id`),
  INDEX `i_async_job_join_map__join_job_id`(`join_job_id`),
  INDEX `i_async_job_join_map__created`(`created`),
  INDEX `i_async_job_join_map__last_updated`(`last_updated`),
  INDEX `i_async_job_join_map__next_wakeup`(`next_wakeup`),
  INDEX `i_async_job_join_map__expiration`(`expiration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`configuration` ADD COLUMN `default_value` VARCHAR(4095) COMMENT 'Default value for a configuration parameter';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `updated` datetime COMMENT 'Time this was updated by the server. null means this row is obsolete.';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'Can this parameter be scoped';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `is_dynamic` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Can the parameter be change dynamically without restarting the server';

UPDATE `cloud`.`configuration` SET `default_value` = `value`;
