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


-- ======================================================================
-- DNS Framework Schema
-- ======================================================================

-- DNS Server Table (Stores DNS Server Configurations)
CREATE TABLE `cloud`.`dns_server` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the dns server',
    `uuid` varchar(40) COMMENT 'uuid of the dns server',
    `name` varchar(255) NOT NULL COMMENT 'display name of the dns server',
    `provider_type` varchar(255) NOT NULL COMMENT 'Provider type such as PowerDns',
    `url` varchar(1024) NOT NULL COMMENT 'dns server url',
    `dns_username` varchar(255) COMMENT 'username or email for dns server credentials',
    `api_key` varchar(255) NOT NULL COMMENT 'dns server api_key',
    `external_server_id` varchar(255) COMMENT 'dns server id e.g. localhost for powerdns',
    `port` int(11) DEFAULT NULL COMMENT 'optional dns server port',
    `name_servers` varchar(1024) DEFAULT NULL COMMENT 'Comma separated list of name servers',
    `is_public` tinyint(1) NOT NULL DEFAULT '0',
    `public_domain_suffix` VARCHAR(255),
    `state` ENUM('Enabled', 'Disabled') NOT NULL DEFAULT 'Disabled',
    `domain_id` bigint unsigned COMMENT 'for domain-specific ownership',
    `account_id` bigint(20) unsigned NOT NULL,
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime DEFAULT NULL COMMENT 'Date removed (soft delete)',
    PRIMARY KEY (`id`),
    KEY `i_dns_server__account_id` (`account_id`),
    CONSTRAINT `fk_dns_server__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- DNS Zone Table (Stores DNS Zone Metadata)
CREATE TABLE `cloud`.`dns_zone` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the dns zone',
    `uuid` varchar(40) COMMENT 'uuid of the dns zone',
    `name` varchar(255) NOT NULL COMMENT 'dns zone name (e.g. example.com)',
    `dns_server_id` bigint unsigned NOT NULL COMMENT 'fk to dns_server.id',
    `external_reference` VARCHAR(255) COMMENT 'id of external provider resource',
    `domain_id` bigint unsigned COMMENT 'for domain-specific ownership',
    `account_id` bigint unsigned COMMENT 'account id. foreign key to account table',
    `description` varchar(1024) DEFAULT NULL,
    `type` ENUM('Private', 'Public') NOT NULL DEFAULT 'Public',
    `state` ENUM('Active', 'Inactive') NOT NULL DEFAULT 'Inactive',
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime DEFAULT NULL COMMENT 'Date removed (soft delete)',
    PRIMARY KEY (`id`),
    CONSTRAINT `uc_dns_zone__uuid` UNIQUE (`uuid`),
    KEY `i_dns_zone__dns_server` (`dns_server_id`),
    KEY `i_dns_zone__account_id` (`account_id`),
    CONSTRAINT `fk_dns_zone__dns_server_id` FOREIGN KEY (`dns_server_id`) REFERENCES `dns_server` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dns_zone__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_dns_zone__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- DNS Zone Network Map (One-to-Many Link)
CREATE TABLE `cloud`.`dns_zone_network_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id of the dns zone to network mapping',
  `uuid` varchar(40),
  `dns_zone_id` bigint(20) unsigned NOT NULL,
  `network_id` bigint(20) unsigned NOT NULL COMMENT 'network to which dns zone is associated to',
  `sub_domain` varchar(255) DEFAULT NULL COMMENT 'Subdomain for auto-registration',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime DEFAULT NULL COMMENT 'Date removed (soft delete)',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_dns_zone__uuid` UNIQUE (`uuid`),
  KEY `fk_dns_map__zone_id` (`dns_zone_id`),
  KEY `fk_dns_map__network_id` (`network_id`),
  CONSTRAINT `fk_dns_map__zone_id` FOREIGN KEY (`dns_zone_id`) REFERENCES `dns_zone` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dns_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Set default limit to 10 DNS zones for standard Accounts
INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`, `default_value`)
VALUES ('Advanced', 'DEFAULT', 'ResourceLimitManager', 'max.account.dns_zones', '10', 'The default maximum number of DNS zones that can be created by an Account', '10');
