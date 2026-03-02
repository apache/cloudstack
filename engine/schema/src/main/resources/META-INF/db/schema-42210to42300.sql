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

CREATE TABLE `cloud`.`backup_offering_details` (
    `id` bigint unsigned NOT NULL auto_increment,
    `backup_offering_id` bigint unsigned NOT NULL COMMENT 'Backup offering id',
    `name` varchar(255) NOT NULL,
    `value` varchar(1024) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_offering_details__backup_offering_id` FOREIGN KEY `fk_offering_details__backup_offering_id`(`backup_offering_id`) REFERENCES `backup_offering`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Update value to random for the config 'vm.allocation.algorithm' or 'volume.allocation.algorithm' if configured as userconcentratedpod_random
-- Update value to firstfit for the config 'vm.allocation.algorithm' or 'volume.allocation.algorithm' if configured as userconcentratedpod_firstfit
UPDATE `cloud`.`configuration` SET value='random' WHERE name IN ('vm.allocation.algorithm', 'volume.allocation.algorithm') AND value='userconcentratedpod_random';
UPDATE `cloud`.`configuration` SET value='firstfit' WHERE name IN ('vm.allocation.algorithm', 'volume.allocation.algorithm') AND value='userconcentratedpod_firstfit';

-- Create webhook_filter table
DROP TABLE IF EXISTS `cloud`.`webhook_filter`;
CREATE TABLE IF NOT EXISTS `cloud`.`webhook_filter` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the webhook filter',
    `uuid` varchar(255) COMMENT 'uuid of the webhook filter',
    `webhook_id` bigint unsigned  NOT NULL COMMENT 'id of the webhook',
    `type` varchar(20) COMMENT 'type of the filter',
    `mode` varchar(20) COMMENT 'mode of the filter',
    `match_type` varchar(20) COMMENT 'match type of the filter',
    `value` varchar(256) NOT NULL COMMENT 'value of the filter used for matching',
    `created` datetime NOT NULL COMMENT 'date created',
    PRIMARY KEY (`id`),
    INDEX `i_webhook_filter__webhook_id`(`webhook_id`),
    CONSTRAINT `fk_webhook_filter__webhook_id` FOREIGN KEY(`webhook_id`) REFERENCES `webhook`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
