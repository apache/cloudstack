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


-- Create user token_keypairs table for apikey/secretkey tokens
CREATE TABLE IF NOT EXISTS `cloud`.`api_keypair` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(40) UNIQUE NOT NULL,
    `name` varchar(255) NOT NULL,
    `domain_id` bigint(20) unsigned NOT NULL,
    `account_id` bigint(20) unsigned NOT NULL,
    `user_id` bigint(20) unsigned NOT NULL,
    `start_date` datetime,
    `end_date` datetime,
    `description` varchar(100),
    `api_key` varchar(255) NOT NULL,
    `secret_key` varchar(255) NOT NULL,
    `created` datetime NOT NULL,
    `removed` datetime,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_api_keypair__user_id` FOREIGN KEY(`user_id`) REFERENCES `cloud`.`user`(`id`),
    CONSTRAINT `fk_api_keypair__account_id` FOREIGN KEY(`account_id`) REFERENCES `cloud`.`account`(`id`),
    CONSTRAINT `fk_api_keypair__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `cloud`.`domain`(`id`)
    );

CREATE TABLE IF NOT EXISTS `cloud`.`api_keypair_permissions` (
                                                             `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(40) UNIQUE,
    `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0,
    `rule` varchar(255) NOT NULL,
    `api_keypair_id` bigint(20) unsigned NOT NULL,
    `permission` varchar(255) NOT NULL,
    `description` varchar(255),
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_keypair_permissions__api_keypair_id` FOREIGN KEY(`api_keypair_id`) REFERENCES `cloud`.`api_keypair`(`id`)
    );

INSERT INTO `cloud`.`api_keypair` (uuid, user_id, domain_id, account_id, api_key, secret_key, created, name)
SELECT  uuid(), user.id, account.domain_id, account.id, user.api_key, user.secret_key, now(), 'Active key pair'
FROM    `cloud`.`user` AS user
JOIN    `cloud`.`account` AS account ON user.account_id = account.id
WHERE   user.api_key IS NOT NULL
  AND     user.secret_key IS NOT NULL;

ALTER TABLE `cloud`.`user` DROP COLUMN api_key, DROP COLUMN secret_key;
