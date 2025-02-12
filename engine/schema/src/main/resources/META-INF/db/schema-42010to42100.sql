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

-- Add columns max_backup and backup_interval_type to backup table
ALTER TABLE `cloud`.`backup_schedule` ADD COLUMN `max_backups` int(8) default NULL COMMENT 'maximum number of backups to maintain';
ALTER TABLE `cloud`.`backups` ADD COLUMN `backup_interval_type` int(5) COMMENT 'type of backup, e.g. manual, recurring - hourly, daily, weekly or monthly';

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

CREATE TABLE IF NOT EXISTS `cloud`.`logs_web_session` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `uuid` varchar(40) NOT NULL COMMENT 'UUID generated for the session',
    `filter` varchar(64) DEFAULT NULL COMMENT 'Filter keyword for the session',
    `created` datetime NOT NULL COMMENT 'When the session was created',
    `domain_id` bigint(20) unsigned NOT NULL COMMENT 'Domain of the account who generated the session',
    `account_id` bigint(20) unsigned NOT NULL COMMENT 'Account who generated the session',
    `creator_address` VARCHAR(45) DEFAULT NULL COMMENT 'Address of the creator of the session',
    `connections` int unsigned NOT NULL DEFAULT 0 COMMENT 'Number of connections for the session',
    `connected_time` datetime DEFAULT NULL COMMENT 'When the session was connected',
    `client_address` VARCHAR(45) DEFAULT NULL COMMENT 'Address of the client that connected to the session',
    `removed` datetime COMMENT 'When the session was removed/used',
    CONSTRAINT `uc_logs_web_session__uuid` UNIQUE (`uuid`)
);
