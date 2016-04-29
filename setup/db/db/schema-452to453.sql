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
-- Schema upgrade from 4.5.2 to 4.5.3;
--;

-- Dynamic roles
CREATE TABLE IF NOT EXISTS `cloud`.`roles` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) COMMENT 'unique name of the dynamic role',
  `role_type` varchar(255) NOT NULL COMMENT 'the type of the role',
  `removed` datetime COMMENT 'date removed',
  `description` text COMMENT 'description of the role',
  PRIMARY KEY (`id`),
  KEY `i_roles__name` (`name`),
  KEY `i_roles__role_type` (`role_type`),
  UNIQUE KEY (`name`, `role_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`role_permissions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `role_id` bigint(20) unsigned NOT NULL COMMENT 'id of the role',
  `rule` varchar(255) NOT NULL COMMENT 'rule for the role, api name or wildcard',
  `permission` varchar(255) NOT NULL COMMENT 'access authority, allow or deny',
  `description` text COMMENT 'description of the rule',
  `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'permission sort order',
  PRIMARY KEY (`id`),
  KEY `fk_role_permissions__role_id` (`role_id`),
  KEY `i_role_permissions__sort_order` (`sort_order`),
  UNIQUE KEY (`role_id`, `rule`),
  CONSTRAINT `fk_role_permissions__role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Default CloudStack roles
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (1, UUID(), 'Root Admin', 'Admin', 'Default root admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (2, UUID(), 'Resource Admin', 'ResourceAdmin', 'Default resource admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (3, UUID(), 'Domain Admin', 'DomainAdmin', 'Default domain admin role') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (4, UUID(), 'User', 'User', 'Default Root Admin role') ON DUPLICATE KEY UPDATE name=name;
