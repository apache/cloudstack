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
-- Schema upgrade from 4.23.0.0 to 24.0.0
--;

-- Multi-VLAN trunk nics: a nic may be associated with additional networks beyond its primary nics.network_id.
CREATE TABLE IF NOT EXISTS `cloud`.`nic_network_map` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid` varchar(40),
  `nic_id` bigint unsigned NOT NULL COMMENT 'nic this association belongs to',
  `network_id` bigint unsigned NOT NULL COMMENT 'additional network this nic is associated with',
  `ip4_address` char(40) COMMENT 'ip4 address assigned to this nic from this network',
  `ip6_address` char(40) COMMENT 'ip6 address assigned to this nic from this network',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_nic_network_map__nic_id` FOREIGN KEY (`nic_id`) REFERENCES `nics`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nic_network_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `uc_nic_network_map__uuid` UNIQUE (`uuid`),
  UNIQUE KEY `uk_nic_network_map__nic_id_network_id` (`nic_id`, `network_id`),
  INDEX `i_nic_network_map__nic_id` (`nic_id`),
  INDEX `i_nic_network_map__network_id` (`network_id`),
  INDEX `i_nic_network_map__removed` (`removed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.nics', 'multi_network', 'tinyint(1) NOT NULL DEFAULT 0 COMMENT "true if this nic has additional network associations in nic_network_map"');
