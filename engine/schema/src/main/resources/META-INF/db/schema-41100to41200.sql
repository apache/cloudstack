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
-- Schema upgrade from 4.11.0.0 to 4.12.0.0
--;

DROP TABLE IF EXISTS `cloud`.`domain_vnet_map`;
CREATE TABLE `cloud`.`domain_vnet_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `vnet_range` varchar(255) NOT NULL COMMENT 'dedicated guest vlan range',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain id. foreign key to domain table',
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'physical network id. foreign key to the the physical network table',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_domain_vnet_map__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network` (`id`) ON DELETE CASCADE,
  INDEX `i_domain_vnet_map__physical_network_id` (`physical_network_id`),
  CONSTRAINT `fk_domain_vnet_map__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE,
  INDEX `i_domain_vnet_map__domain_id` (`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD COLUMN domain_vnet_map_id bigint unsigned DEFAULT NULL;
ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD CONSTRAINT `fk_op_dc_vnet_alloc__domain_vnet_map_id` FOREIGN KEY `fk_op_dc_vnet_alloc__domain_vnet_map_id` (`domain_vnet_map_id`) REFERENCES `domain_vnet_map` (`id`);

