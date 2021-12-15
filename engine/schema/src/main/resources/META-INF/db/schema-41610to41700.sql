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
-- Schema upgrade from 4.16.1.0 to 4.17.0.0
--;

-- PR#5668 Change the type of the 'ipsec_psk' field to allow large PSK.
ALTER TABLE cloud.remote_access_vpn MODIFY ipsec_psk text NOT NULL;

-- For IPv6 guest prefixes.
CREATE TABLE `cloud`.`dc_ip6_guest_prefix` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) DEFAULT NULL,
  `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'zone it belongs to',
  `pod_id` bigint(20) unsigned DEFAULT NULL COMMENT 'pod it belongs to',
  `prefix` varchar(255) NOT NULL COMMENT 'prefix of the ipv6 network',
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_dc_ip6_guest_prefix__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`),
  CONSTRAINT `fk_dc_ip6_guest_prefix__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref`(`id`),
  CONSTRAINT `uc_dc_ip6_guest_prefix__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ip6_guest_prefix_subnet_network_map` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) DEFAULT NULL,
  `prefix_id` bigint(20) unsigned NOT NULL COMMENT 'ip6 guest prefix to which subnet belongs to',
  `subnet` varchar(255) NOT NULL COMMENT 'subnet of the ipv6 network',
  `network_id` bigint(20) unsigned DEFAULT NULL COMMENT 'network to which subnet is associated to',
  `state` varchar(255) NOT NULL COMMENT 'state of the subnet network',
  `updated` datetime default NULL,
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ip6_guest_prefix_subnet_network_map__prefix_id` FOREIGN KEY (`prefix_id`) REFERENCES `dc_ip6_guest_prefix`(`id`),
  CONSTRAINT `fk_ip6_guest_prefix_subnet_network_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `uc_ip6_guest_prefix_subnet_network_map__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`public_ip6_address_network_map` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `uuid` varchar(40) DEFAULT NULL,
  `vlan_db_id` bigint(20) unsigned NOT NULL COMMENT 'id of the vlan to which this address belong to',
  `public_ip_address` varchar(255) NOT NULL COMMENT 'ipv6 address',
  `network_id` bigint(20) unsigned DEFAULT NULL COMMENT 'network to which this address is associated to',
  `state` varchar(255) NOT NULL COMMENT 'state of the address',
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_public_ip6_address_network_map__prefix_id` FOREIGN KEY (`vlan_db_id`) REFERENCES `vlan`(`id`),
  CONSTRAINT `fk_public_ip6_address_network_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `uc_public_ip6_address_network_map__uuid` UNIQUE (`uuid`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
