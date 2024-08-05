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

-- Whitelabel GUI
CREATE TABLE IF NOT EXISTS `cloud`.`gui_themes` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(255) UNIQUE,
    `name` varchar(2048) NOT NULL COMMENT 'A name to identify the theme.',
    `description` varchar(4096) DEFAULT NULL COMMENT 'A description for the theme.',
    `css` text DEFAULT NULL COMMENT 'The CSS to be retrieved and imported into the GUI when matching the theme access configurations.',
    `json_configuration` text DEFAULT NULL COMMENT 'The JSON with the configurations to be retrieved and imported into the GUI when matching the theme access configurations.',
    `recursive_domains` tinyint(1) DEFAULT 0 COMMENT 'Defines whether the subdomains of the informed domains are considered. Default value is false.',
    `is_public` tinyint(1) default 1 COMMENT 'Defines whether a theme can be retrieved by anyone when only the `internet_domains_names` is informed. If the `domain_uuids` or `account_uuids` is informed, it is considered as `false`.',
    `created` datetime NOT NULL,
    `removed` datetime DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `cloud`.`gui_themes_details` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `gui_theme_id` bigint(20) unsigned NOT NULL COMMENT 'Foreign key referencing the GUI theme on `gui_themes` table.',
    `type` varchar(100) DEFAULT NOT NULL COMMENT 'The type of GUI theme details. Valid options are: `account`, `domain` and `commonName`',
    `value` text NOT NULL COMMENT 'The value of the `type` details. Can be an UUID (account or domain) or internet common name.',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_gui_themes_details__gui_theme_id` FOREIGN KEY (`gui_theme_id`) REFERENCES `gui_themes`(`id`)
);
