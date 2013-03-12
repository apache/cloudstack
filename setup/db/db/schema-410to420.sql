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
-- Schema upgrade from 4.1.0 to 4.2.0;
--;

SET foreign_key_checks = 0;

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `max_hosts_per_cluster` int unsigned DEFAULT NULL COMMENT 'Max. hosts in cluster supported by hypervisor';
UPDATE `cloud`.`hypervisor_capabilities` SET `max_hosts_per_cluster`=32 WHERE `hypervisor_type`='VMware';
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_hosts_per_cluster) VALUES ('VMware', '5.1', 128, 0, 32);
DELETE FROM `cloud`.`configuration` where name='vmware.percluster.host.max';
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'xen.nics.max', '7', 'Maximum allowed nics for Vms created on Xen');

alter table template_host_ref add state varchar(255);
alter table template_host_ref add update_count bigint unsigned;
alter table template_host_ref add updated datetime;
alter table volume_host_ref add state varchar(255);
alter table volume_host_ref add update_count bigint unsigned;
alter table volume_host_ref add updated datetime;
alter table template_spool_ref add updated datetime;
CREATE TABLE  `cloud`.`object_datastore_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `datastore_uuid`  varchar(255) NOT NULL,
  `datastore_role` varchar(255) NOT NULL,
  `object_uuid` varchar(255) NOT NULL,
  `object_type` varchar(255) NOT NULL,
  `created` DATETIME NOT NULL,
  `last_updated` DATETIME,
  `job_id` varchar(255),
  `download_pct` int(10) unsigned,
  `download_state` varchar(255),
  `url` varchar(255),
  `format` varchar(255),
  `checksum` varchar(255),
  `error_str` varchar(255),
  `local_path` varchar(255),
  `install_path` varchar(255),
  `size` bigint unsigned COMMENT 'the size of the template on the pool',
  `state` varchar(255) NOT NULL,
  `update_count` bigint unsigned NOT NULL,
  `updated` DATETIME,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`data_store_provider` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of primary data store provider',
  `uuid` varchar(255) NOT NULL COMMENT 'uuid of primary data store provider',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`image_data_store` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(255) NOT NULL COMMENT 'name of data store',
  `image_provider_id` bigint unsigned NOT NULL COMMENT 'id of image_data_store_provider',
  `protocol` varchar(255) NOT NULL COMMENT 'protocol of data store',
  `data_center_id` bigint unsigned  COMMENT 'datacenter id of data store',
  `scope` varchar(255) COMMENT 'scope of data store',
  `uuid` varchar(255) COMMENT 'uuid of data store',
  PRIMARY KEY(`id`),
  CONSTRAINT `fk_tags__image_data_store_provider_id` FOREIGN KEY(`image_provider_id`) REFERENCES `data_store_provider`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `image_data_store_id` bigint unsigned;

ALTER TABLE `cloud`.`service_offering` ADD COLUMN `is_volatile` tinyint(1) unsigned NOT NULL DEFAULT 0  COMMENT 'true if the vm needs to be volatile, i.e., on every reboot of vm from API root disk is discarded and creates a new root disk';

ALTER TABLE `cloud`.`networks` ADD COLUMN `network_cidr` VARCHAR(18) COMMENT 'The network cidr for the isolated guest network which uses IP Reservation facility.For networks not using IP reservation, network_cidr is always null.';
ALTER TABLE `cloud`.`networks` CHANGE `cidr` `cidr` varchar(18) COMMENT 'CloudStack managed vms get IP address from cidr.In general this cidr also serves as the network CIDR. But in case IP reservation feature is being used by a Guest network, networkcidr is the Effective network CIDR for that network';


CREATE TABLE  `vpc_service_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_id` bigint unsigned NOT NULL COMMENT 'vpc_id',
  `service` varchar(255) NOT NULL COMMENT 'service',
  `provider` varchar(255) COMMENT 'service provider',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_service_map__vpc_id` FOREIGN KEY(`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE,
  UNIQUE (`vpc_id`, `service`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


SET foreign_key_checks = 1;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.instancename.flag', 'false', 'Append guest VM display Name (if set) to the internal name of the VM');

CREATE TABLE `cloud`.`user_vm_clone_setting` (
  `vm_id` bigint unsigned NOT NULL COMMENT 'guest VM id',
  `clone_type` varchar(10) NOT NULL COMMENT 'Full or Linked Clone (applicable to VMs on ESX)',
  PRIMARY KEY (`vm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'UserVmManager', 'vmware.create.full.clone' , 'false', 'If set to true, creates VMs as full clones on ESX hypervisor');

CREATE TABLE `cloud`.`external_cisco_vnmc_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which cisco vnmc device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this cisco vnmc device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the cisco vnmc device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external cisco vnmc device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_cisco_vnmc_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_cisco_vnmc_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_cisco_asa1000v_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which cisco asa1kv device is added',
  `management_ip` varchar(255) UNIQUE NOT NULL COMMENT 'mgmt. ip of cisco asa1kv device',
  `in_port_profile` varchar(255) NOT NULL COMMENT 'inside port profile name of cisco asa1kv device',
  `cluster_id` bigint unsigned NOT NULL COMMENT 'id of the Vmware cluster to which cisco asa1kv device is attached (cisco n1kv switch)',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_cisco_asa1000v_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_cisco_asa1000v_devices__cluster_id` FOREIGN KEY (`cluster_id`) REFERENCES `cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_asa1000v_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `network_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of guest network',
  `asa1000v_id` bigint unsigned NOT NULL UNIQUE COMMENT 'id of asa1000v device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_asa1000v_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_network_asa1000v_map__asa1000v_id` FOREIGN KEY (`asa1000v_id`) REFERENCES `external_cisco_asa1000v_devices`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
