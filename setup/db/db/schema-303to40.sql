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
-- Schema upgrade from 3.0.3 to 4.0.0;
--;

-- RBD Primary Storage pool support (commit: 406fd95d87bfcdbb282d65589ab1fb6e9fd0018a)
ALTER TABLE `storage_pool` ADD `user_info` VARCHAR( 255 ) NULL COMMENT 'Authorization information for the storage pool. Used by network filesystems' AFTER `host_address`;

-- Resource tags (commit: 62d45b9670520a1ee8b520509393d4258c689b50)
CREATE TABLE `cloud`.`resource_tags` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40),
  `key` varchar(255),
  `value` varchar(255),
  `resource_id` bigint unsigned NOT NULL,
  `resource_uuid` varchar(40),
  `resource_type` varchar(255),
  `customer` varchar(255),
  `domain_id` bigint unsigned NOT NULL COMMENT 'foreign key to domain id',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this network',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_tags__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`),
  CONSTRAINT `fk_tags__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`),
  UNIQUE `i_tags__resource_id__resource_type__key`(`resource_id`, `resource_type`, `key`),
  CONSTRAINT `uc_resource_tags__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Nicira Integration (commit: 79c7da07abd4294f150851aa0c2d06a28564c5a9)
CREATE TABLE `cloud`.`external_nicira_nvp_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which nicira nvp device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this nicira nvp device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the nicira nvp device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external nicira nvp device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_nicira_nvp_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_nicira_nvp_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`nicira_nvp_nic_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `logicalswitch` varchar(255) NOT NULL COMMENT 'nicira uuid of logical switch this port is provisioned on',
  `logicalswitchport` varchar(255) UNIQUE COMMENT 'nicira uuid of this logical switch port',
  `nic` varchar(255) UNIQUE COMMENT 'cloudstack uuid of the nic connected to this logical switch port',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- rrq 5839
-- Remove the unique constraint on physical_network_id, provider_name from physical_network_service_providers
-- Because the name of this contraint is not set we need this roundabout way
-- The key is also used by the foreign key constraint so drop and recreate that one
ALTER TABLE physical_network_service_providers DROP FOREIGN KEY fk_pnetwork_service_providers__physical_network_id;

SET @constraintname = (select CONCAT(CONCAT('DROP INDEX ', A.CONSTRAINT_NAME), ' ON physical_network_service_providers' )
from information_schema.key_column_usage A
JOIN information_schema.key_column_usage B ON B.table_name = 'physical_network_service_providers' AND B.COLUMN_NAME = 'provider_name' AND A.COLUMN_NAME ='physical_network_id' AND B.CONSTRAINT_NAME=A.CONSTRAINT_NAME
where A.table_name = 'physical_network_service_providers');

PREPARE stmt1 FROM @constraintname; 
EXECUTE stmt1; 
DEALLOCATE PREPARE stmt1; 

AlTER TABLE physical_network_service_providers ADD CONSTRAINT `fk_pnetwork_service_providers__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE;
UPDATE `cloud`.`configuration` SET description='In second, timeout for creating volume from snapshot' WHERE name='create.volume.from.snapshot.wait';



