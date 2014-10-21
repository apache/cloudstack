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

SET foreign_key_checks = 0;

--
-- Schema upgrade from 2.0 to 2.1
--
CREATE TABLE `cloud`.`cluster` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name for the cluster',
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod id',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ext_lun_alloc` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `size` bigint unsigned NOT NULL COMMENT 'virtual size',
  `portal` varchar(255) NOT NULL COMMENT 'ip or host name to the storage server',
  `target_iqn` varchar(255) NOT NULL COMMENT 'target iqn',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center id this belongs to',
  `lun` int NOT NULL COMMENT 'lun',
  `taken` datetime COMMENT 'time occupied',
  `volume_id` bigint unsigned COMMENT 'vm taking this lun',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ext_lun_details` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `ext_lun_id` bigint unsigned NOT NULL COMMENT 'lun id',
  `tag` varchar(255) COMMENT 'tags associated with this vm',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`storage_pool_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `pool_id` bigint unsigned NOT NULL COMMENT 'pool the detail is related to',
  `name` varchar(255) NOT NULL COMMENT 'name of the detail',
  `value` varchar(255) NOT NULL COMMENT 'value of the detail',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_group` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `account_name` varchar(100) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_ingress_rule` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_group_id` bigint unsigned NOT NULL,
  `start_port` varchar(10) default NULL,
  `end_port` varchar(10) default NULL,
  `protocol` varchar(16) NOT NULL default 'TCP',
  `allowed_network_id` bigint unsigned,
  `allowed_network_group` varchar(255) COMMENT 'data duplicated from network_group table to avoid lots of joins when listing rules (the name of the group should be displayed rather than just id)',
  `allowed_net_grp_acct` varchar(100) COMMENT 'data duplicated from network_group table to avoid lots of joins when listing rules (the name of the group owner should be displayed)',
  `allowed_ip_cidr`  varchar(44),
  `create_status` varchar(32) COMMENT 'rule creation status',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_group_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_group_id` bigint unsigned NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_nwgrp_work` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs rules to be synced.',
  `mgmt_server_id` bigint unsigned COMMENT 'management server that has taken up the work of doing rule sync',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `taken` datetime COMMENT 'time it was taken by the management server',
  `step` varchar(32) NOT NULL COMMENT 'Step in the work',
  `seq_no` bigint unsigned  COMMENT 'seq number to be sent to agent, uniquely identifies ruleset update',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_vm_ruleset_log` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT COMMENT 'id',
  `instance_id` bigint unsigned NOT NULL COMMENT 'vm instance that needs rules to be synced.',
  `created` datetime NOT NULL COMMENT 'time the entry was requested',
  `logsequence` bigint unsigned  COMMENT 'seq number to be sent to agent, uniquely identifies ruleset update',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`account_vlan_map` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `account_id` bigint unsigned NOT NULL COMMENT 'account id. foreign key to account table',
  `vlan_db_id` bigint unsigned NOT NULL COMMENT 'database id of vlan. foreign key to vlan table',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_dc_link_local_ip_address_alloc` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `ip_address` varchar(15) NOT NULL COMMENT 'ip address',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center it belongs to',
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod it belongs to',
  `instance_id` bigint unsigned NULL COMMENT 'instance id',
  `taken` datetime COMMENT 'Date taken',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`op_lock` MODIFY COLUMN `key` varchar(128) NOT NULL UNIQUE; -- add UNIQUE constraint
ALTER TABLE `cloud`.`op_lock` MODIFY COLUMN `thread` varchar(255) NOT NULL; -- change from varchar(256) to varchar(255)

ALTER TABLE `cloud`.`volumes` DROP COLUMN `name_label`;
ALTER TABLE `cloud`.`volumes` DROP COLUMN `template_name`;
ALTER TABLE `cloud`.`volumes` ADD COLUMN `pool_type` varchar(64);
ALTER TABLE `cloud`.`volumes` ADD COLUMN `recreatable` tinyint(1) unsigned NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`volumes` ADD COLUMN `device_id` bigint unsigned NULL;

--
-- after we have data migrated, we can then enforce this at postprocess-20to21.sql
-- ALTER TABLE `cloud`.`volumes` MODIFY COLUMN `disk_offering_id` bigint unsigned NOT NULL; -- add NOT NULL constraint

ALTER TABLE `cloud`.`vlan` DROP COLUMN `vlan_name`;

ALTER TABLE `cloud`.`host` ADD COLUMN `cluster_id` bigint unsigned;

--
-- enforced in postporcess-20to21.sql
ALTER TABLE `cloud`.`host_pod_ref` ADD COLUMN `gateway` varchar(255);	-- need to migrage data with user input  

ALTER TABLE `cloud`.`service_offering` ADD COLUMN `recreatable` tinyint(1) unsigned NOT NULL DEFAULT 0; 
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `tags` varchar(255);

ALTER TABLE `cloud`.`user_vm` MODIFY COLUMN `domain_router_id` bigint unsigned;	-- change from NOT NULL to NULL    

ALTER TABLE `cloud`.`event` ADD COLUMN `state` varchar(32) NOT NULL DEFAULT 'Completed';	
ALTER TABLE `cloud`.`event` ADD COLUMN `start_id` bigint unsigned NOT NULL DEFAULT 0;

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `tags` varchar(4096);
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `created` datetime; -- will fill it with the oldest time in old data base

ALTER TABLE `cloud`.`storage_pool` ADD COLUMN `cluster_id` bigint unsigned; -- need to setup default cluster for pods

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `last_host_id` bigint unsigned;

ALTER TABLE `cloud`.`console_proxy` MODIFY COLUMN `gateway` varchar(15);	-- remove NOT NULL constraint
ALTER TABLE `cloud`.`console_proxy` MODIFY COLUMN `public_netmask` varchar(15); -- remove NOT NULL constraint
ALTER TABLE `cloud`.`console_proxy` ADD COLUMN `guest_mac_address` varchar(17); -- NOT NULL UNIQUE constraint will be added in postprocess
ALTER TABLE `cloud`.`console_proxy` ADD COLUMN `guest_ip_address`  varchar(15);
ALTER TABLE `cloud`.`console_proxy` ADD COLUMN `guest_netmask` varchar(15);

ALTER TABLE `cloud`.`secondary_storage_vm` MODIFY COLUMN `gateway` varchar(15); -- remove NOT NULL constraint
ALTER TABLE `cloud`.`secondary_storage_vm` MODIFY COLUMN `public_netmask` varchar(15); -- remove NOT NULL constrait
ALTER TABLE `cloud`.`secondary_storage_vm` ADD COLUMN `guest_mac_address` varchar(17); -- NOT NULL unique constrait will be added in postprocess
ALTER TABLE `cloud`.`secondary_storage_vm` ADD COLUMN `guest_ip_address`  varchar(15) UNIQUE;
ALTER TABLE `cloud`.`secondary_storage_vm` ADD COLUMN `guest_netmask` varchar(15);

CREATE TABLE  `cloud`.`service_offering_21` (
  `id` bigint unsigned NOT NULL,
  `cpu` int(10) unsigned NOT NULL COMMENT '# of cores',
  `speed` int(10) unsigned NOT NULL COMMENT 'speed per core in mhz',
  `ram_size` bigint unsigned NOT NULL,
  `nw_rate` smallint unsigned default 200 COMMENT 'network rate throttle mbits/s',
  `mc_rate` smallint unsigned default 10 COMMENT 'mcast rate throttle mbits/s',
  `ha_enabled` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Enable HA',
  `guest_ip_type` varchar(255) NOT NULL DEFAULT 'Virtualized' COMMENT 'Type of guest network -- direct or virtualized',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`disk_offering_21` (
  `id` bigint unsigned NOT NULL auto_increment,
  `domain_id` bigint unsigned,
  `name` varchar(255) NOT NULL,
  `display_text` varchar(4096) NULL COMMENT 'Description text set by the admin for display purpose only',
  `disk_size` bigint unsigned NOT NULL COMMENT 'disk space in mbs',
  `mirrored` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'Enable mirroring?',
  `type` varchar(32) COMMENT 'inheritted by who?',
  `tags` varchar(4096) COMMENT 'comma separated tags about the disk_offering',
  `recreatable` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'The root disk is always recreatable',
  `use_local_storage` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Indicates whether local storage pools should be used',
  `unique_name` varchar(32) UNIQUE COMMENT 'unique name',
  `removed` datetime COMMENT 'date removed',
  `created` datetime COMMENT 'date the disk offering was created',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

