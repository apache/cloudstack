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
-- Schema upgrade from 4.22.1.0 to 4.23.0.0
--;


-- Update value to random for the config 'vm.allocation.algorithm' or 'volume.allocation.algorithm' if configured as userconcentratedpod_random
-- Update value to firstfit for the config 'vm.allocation.algorithm' or 'volume.allocation.algorithm' if configured as userconcentratedpod_firstfit
UPDATE `cloud`.`configuration` SET value='random' WHERE name IN ('vm.allocation.algorithm', 'volume.allocation.algorithm') AND value='userconcentratedpod_random';
UPDATE `cloud`.`configuration` SET value='firstfit' WHERE name IN ('vm.allocation.algorithm', 'volume.allocation.algorithm') AND value='userconcentratedpod_firstfit';

-- Add management_server_details table to allow ManagementServer scope configs
CREATE TABLE IF NOT EXISTS `cloud`.`management_server_details` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
    `management_server_id` bigint unsigned NOT NULL COMMENT 'management server the detail is related to',
    `name` varchar(255) NOT NULL COMMENT 'name of the detail',
    `value` varchar(255) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_management_server_details__management_server_id` FOREIGN KEY `fk_management_server_details__management_server_id`(`management_server_id`) REFERENCES `mshost`(`id`) ON DELETE CASCADE,
    KEY `i_management_server_details__name__value` (`name`(128),`value`(128))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create table for Logs Web Session
CREATE TABLE IF NOT EXISTS `cloud`.`logs_web_session` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the session',
    `uuid` varchar(40) NOT NULL COMMENT 'UUID generated for the session',
    `filters` varchar(128) DEFAULT NULL COMMENT 'Filter keywords for the session',
    `created` datetime NOT NULL COMMENT 'When the session was created',
    `domain_id` bigint(20) unsigned NOT NULL COMMENT 'Domain of the account who generated the session',
    `account_id` bigint(20) unsigned NOT NULL COMMENT 'Account who generated the session',
    `creator_address` VARCHAR(45) DEFAULT NULL COMMENT 'Address of the creator of the session',
    `connections` int unsigned NOT NULL DEFAULT 0 COMMENT 'Number of connections for the session',
    `connected_time` datetime DEFAULT NULL COMMENT 'When the session was connected',
    `client_address` VARCHAR(45) DEFAULT NULL COMMENT 'Address of the client that connected to the session',
    `removed` datetime COMMENT 'When the session was removed/used',
    PRIMARY KEY(`id`),
    CONSTRAINT `uc_logs_web_session__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
