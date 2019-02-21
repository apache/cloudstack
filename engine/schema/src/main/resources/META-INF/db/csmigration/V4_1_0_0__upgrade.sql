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
-- Schema upgrade from 4.0.0 to 4.1.0;
--;

use cloud;
SET foreign_key_checks = 0;

alter table vm_template add size bigint unsigned;
alter table vm_template add state varchar(255);
alter table vm_template add update_count bigint unsigned;
alter table vm_template add updated  datetime;
alter table storage_pool add storage_provider_id bigint unsigned;
alter table storage_pool add scope varchar(255);
alter table storage_pool modify id bigint unsigned AUTO_INCREMENT UNIQUE NOT NULL;
alter table template_spool_ref add state varchar(255);
alter table template_spool_ref add update_count bigint unsigned;
alter table volumes add disk_type varchar(255);
alter table volumes drop foreign key `fk_volumes__account_id`;
alter table vm_instance add column disk_offering_id bigint unsigned;
alter table vm_instance add column cpu int(10) unsigned;
alter table vm_instance add column ram bigint unsigned;
alter table vm_instance add column owner varchar(255);
alter table vm_instance add column speed int(10) unsigned;
alter table vm_instance add column host_name varchar(255);
alter table vm_instance add column display_name varchar(255);
alter table vm_instance add column `desired_state` varchar(32) NULL;

alter table data_center add column owner varchar(255);
alter table data_center add column created datetime COMMENT 'date created';
alter table data_center add column lastUpdated datetime COMMENT 'last updated';
alter table data_center add column engine_state varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'the engine state of the zone';
alter table host_pod_ref add column owner varchar(255);
alter table host_pod_ref add column created datetime COMMENT 'date created';
alter table host_pod_ref add column lastUpdated datetime COMMENT 'last updated';
alter table host_pod_ref add column engine_state varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'the engine state of the zone';
alter table host add column owner varchar(255);
alter table host add column lastUpdated datetime COMMENT 'last updated';
alter table host add column engine_state varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'the engine state of the zone';

alter table cluster add column owner varchar(255);
alter table cluster add column created datetime COMMENT 'date created';
alter table cluster add column lastUpdated datetime COMMENT 'last updated';
alter table cluster add column engine_state varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'the engine state of the zone';

CREATE TABLE `cloud`.`vm_compute_tags` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `compute_tag` varchar(255) NOT NULL COMMENT 'name of tag',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vm_root_disk_tags` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `root_disk_tag` varchar(255) NOT NULL COMMENT 'name of tag',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`vm_network_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `network_id` bigint unsigned NOT NULL COMMENT 'network id',
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`vm_reservation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) NOT NULL COMMENT 'reservation id',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `data_center_id` bigint unsigned NOT NULL COMMENT 'zone id',
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod id',
  `cluster_id` bigint unsigned NOT NULL COMMENT 'cluster id',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id',
  `created` datetime COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  CONSTRAINT `uc_vm_reservation__uuid` UNIQUE (`uuid`),
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`volume_reservation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `vm_reservation_id` bigint unsigned NOT NULL COMMENT 'id of the vm reservation',
  `vm_id` bigint unsigned NOT NULL COMMENT 'vm id',
  `volume_id` bigint unsigned NOT NULL COMMENT 'volume id',
  `pool_id` bigint unsigned NOT NULL COMMENT 'pool assigned to the volume',
  CONSTRAINT `fk_vm_pool_reservation__vm_reservation_id` FOREIGN KEY (`vm_reservation_id`) REFERENCES `vm_reservation`(`id`) ON DELETE CASCADE,
  PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`s3` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `access_key` varchar(20) NOT NULL COMMENT ' The S3 access key',
  `secret_key` varchar(40) NOT NULL COMMENT ' The S3 secret key',
  `end_point` varchar(1024) COMMENT ' The S3 host',
  `bucket` varchar(63) NOT NULL COMMENT ' The S3 host',
  `https` tinyint unsigned DEFAULT NULL COMMENT ' Flag indicating whether or not to connect over HTTPS',
  `connection_timeout` integer COMMENT ' The amount of time to wait (in milliseconds) when initially establishing a connection before giving up and timing out.',
  `max_error_retry` integer  COMMENT ' The maximum number of retry attempts for failed retryable requests (ex: 5xx error responses from services).',
  `socket_timeout` integer COMMENT ' The amount of time to wait (in milliseconds) for data to be transfered over an established, open connection before the connection times out and is closed.',
  `created` datetime COMMENT 'date the s3 first signed on',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_s3__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`template_s3_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `s3_id` bigint unsigned NOT NULL COMMENT ' Associated S3 instance id',
  `template_id` bigint unsigned NOT NULL COMMENT ' Associated template id',
  `created` DATETIME NOT NULL COMMENT ' The creation timestamp',
  `size` bigint unsigned COMMENT ' The size of the object',
  `physical_size` bigint unsigned DEFAULT 0 COMMENT ' The physical size of the object',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_template_s3_ref__template_id` UNIQUE (`template_id`),
  CONSTRAINT `fk_template_s3_ref__s3_id` FOREIGN KEY `fk_template_s3_ref__s3_id` (`s3_id`) REFERENCES `s3` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_template_s3_ref__template_id` FOREIGN KEY `fk_template_s3_ref__template_id` (`template_id`) REFERENCES `vm_template` (`id`),
  INDEX `i_template_s3_ref__s3_id`(`s3_id`),
  INDEX `i_template_s3_ref__template_id`(`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 's3.enable', 'false', 'enable s3');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.check.poolsize' , '10', 'Numbers of threads using to check redundant router status.');

ALTER TABLE `cloud`.`snapshots` ADD COLUMN `s3_id` bigint unsigned COMMENT 'S3 to which this snapshot will be stored';

ALTER TABLE `cloud`.`snapshots` ADD CONSTRAINT `fk_snapshots__s3_id` FOREIGN KEY `fk_snapshots__s3_id` (`s3_id`) REFERENCES `s3` (`id`);

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `inline` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is this network offering LB provider is in inline mode';

ALTER TABLE `cloud`.`external_load_balancer_devices` DROP COLUMN `is_inline`;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network','DEFAULT','NetworkManager','network.dhcp.nondefaultnetwork.setgateway.guestos','Windows','The guest OS\'s name start with this fields would result in DHCP server response gateway information even when the network it\'s on is not default network. Names are separated by comma.');

ALTER TABLE `cloud`.`sync_queue` ADD `queue_size` SMALLINT NOT NULL DEFAULT '0' COMMENT 'number of items being processed by the queue';

ALTER TABLE `cloud`.`sync_queue` ADD `queue_size_limit` SMALLINT NOT NULL DEFAULT '1' COMMENT 'max number of items the queue can process concurrently';

ALTER TABLE `cloud`.`sync_queue` DROP INDEX `i_sync_queue__queue_proc_time`;

ALTER TABLE `cloud`.`sync_queue` DROP COLUMN `queue_proc_time`;

ALTER TABLE `cloud`.`sync_queue` DROP COLUMN `queue_proc_msid`;

ALTER TABLE `cloud`.`sync_queue_item` ADD `queue_proc_time` DATETIME COMMENT 'when processing started for the item' AFTER `queue_proc_number`;

ALTER TABLE `cloud`.`sync_queue_item` ADD INDEX `i_sync_queue__queue_proc_time`(`queue_proc_time`);

ALTER TABLE `cloud`.`inline_load_balancer_nic_map` DROP FOREIGN KEY fk_inline_load_balancer_nic_map__load_balancer_id;

ALTER TABLE `cloud`.`inline_load_balancer_nic_map` DROP COLUMN load_balancer_id;

ALTER TABLE upload ADD uuid VARCHAR(40);
ALTER TABLE async_job modify job_cmd VARCHAR(255);


ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `is_persistent` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if the network offering provides an ability to create persistent networks';


-- populate uuid column with db id if uuid is null
UPDATE `cloud`.`account` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`alert` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`async_job` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`cluster` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`data_center` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`dc_storage_network_ip_range` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`disk_offering` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`domain` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`event` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`external_firewall_devices` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`external_load_balancer_devices` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`external_nicira_nvp_devices` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`firewall_rules` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`guest_os` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`guest_os_category` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`host` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`host_pod_ref` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`hypervisor_capabilities` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`instance_group` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`load_balancer_stickiness_policies` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`network_external_firewall_device_map` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`network_external_lb_device_map` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`network_offerings` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`networks` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`nics` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`physical_network` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`physical_network_service_providers` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`physical_network_traffic_types` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`port_profile` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`project_invitations` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`projects` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`resource_tags` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`s2s_customer_gateway` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`s2s_vpn_connection` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`s2s_vpn_gateway` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`security_group` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`security_group_rule` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`snapshot_schedule` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`snapshots` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`snapshot_policy` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`static_routes` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`storage_pool` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`swift` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`upload` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`user` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`user_ip_address` set uuid=id WHERE uuid is NULL;
-- UPDATE `cloud`.`user_vm_temp` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`virtual_router_providers` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`virtual_supervisor_module` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vlan` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vm_instance` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vm_template` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vpc` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vpc_gateways` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vpc_offerings` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`vpn_users` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`volumes` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`configuration` set value = '/var/cloudstack/mnt' where name = 'mount.parent';
-- UPDATE `cloud`.`autoscale_vmgroups` set uuid=id WHERE uuid is NULL;
-- UPDATE `cloud`.`autoscale_vmprofiles` set uuid=id WHERE uuid is NULL;
-- UPDATE `cloud`.`autoscale_policies` set uuid=id WHERE uuid is NULL;
-- UPDATE `cloud`.`counter` set uuid=id WHERE uuid is NULL;
-- UPDATE `cloud`.`conditions` set uuid=id WHERE uuid is NULL;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'detail.batch.query.size', '2000', 'Default entity detail batch query size for listing');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'api.throttling.enabled', 'false', 'Enable/Disable Api rate limit');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'api.throttling.interval', '1', 'Time interval (in seconds) to reset API count');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'api.throttling.max', '25', 'Max allowed number of APIs within fixed interval');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'api.throttling.cachesize', '50000', 'Account based API count cache size');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'direct.agent.pool.size', '500', 'Default size for DirectAgentPool');

ALTER TABLE `cloud`.`op_dc_vnet_alloc` DROP INDEX i_op_dc_vnet_alloc__vnet__data_center_id;

ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD CONSTRAINT UNIQUE `i_op_dc_vnet_alloc__vnet__data_center_id`(`vnet`, `physical_network_id`, `data_center_id`);

ALTER TABLE `cloud`.`op_dc_vnet_alloc` DROP INDEX i_op_dc_vnet_alloc__vnet__data_center_id__account_id;

CREATE TABLE  `cloud`.`region` (
  `id` int unsigned NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL UNIQUE,
  `end_point` varchar(255) NOT NULL,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`region` values ('1','Local','http://localhost:8080/client/');

CREATE TABLE `cloud`.`nicira_nvp_router_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `logicalrouter_uuid` varchar(255) NOT NULL UNIQUE COMMENT 'nicira uuid of logical router',
  `network_id` bigint unsigned NOT NULL UNIQUE COMMENT 'cloudstack id of the network',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_nicira_nvp_router_map__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_bigswitch_vns_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which bigswitch vns device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this bigswitch vns device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the bigswitch vns device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external bigswitch vns device',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_external_bigswitch_vns_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_bigswitch_vns_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`counter` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `source` varchar(255) NOT NULL COMMENT 'source e.g. netscaler, snmp',
  `name` varchar(255) NOT NULL COMMENT 'Counter name',
  `value` varchar(255) NOT NULL COMMENT 'Value in case of source=snmp',
  `removed` datetime COMMENT 'date removed if not null',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `uc_counter__uuid` UNIQUE (`uuid`),
  INDEX `i_counter__removed`(`removed`),
  INDEX `i_counter__source`(`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`conditions` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `counter_id` bigint unsigned NOT NULL COMMENT 'Counter Id',
  `threshold` bigint unsigned NOT NULL COMMENT 'threshold value for the given counter',
  `relational_operator` char(2) COMMENT 'relational operator to be used upon the counter and condition',
  `domain_id` bigint unsigned NOT NULL COMMENT 'domain the Condition belongs to',
  `account_id` bigint unsigned NOT NULL COMMENT 'owner of this Condition',
  `removed` datetime COMMENT 'date removed if not null',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_conditions__counter_id` FOREIGN KEY `fk_condition__counter_id`(`counter_id`) REFERENCES `counter`(`id`),
  CONSTRAINT `fk_conditions__account_id` FOREIGN KEY `fk_condition__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conditions__domain_id` FOREIGN KEY `fk_condition__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_conditions__uuid` UNIQUE (`uuid`),
  INDEX `i_conditions__removed`(`removed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_vmprofiles` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `zone_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `autoscale_user_id` bigint unsigned NOT NULL,
  `service_offering_id` bigint unsigned NOT NULL,
  `template_id` bigint unsigned NOT NULL,
  `other_deploy_params` varchar(1024) COMMENT 'other deployment parameters that is in addition to zoneid,serviceofferingid,domainid',
  `destroy_vm_grace_period` int unsigned COMMENT 'the time allowed for existing connections to get closed before a vm is destroyed',
  `counter_params` varchar(1024) COMMENT 'the parameters for the counter to be used to get metric information from VMs',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmprofiles__domain_id` FOREIGN KEY `fk_autoscale_vmprofiles__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmprofiles__account_id` FOREIGN KEY `fk_autoscale_vmprofiles__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmprofiles__autoscale_user_id` FOREIGN KEY `fk_autoscale_vmprofiles__autoscale_user_id` (`autoscale_user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_autoscale_vmprofiles__uuid` UNIQUE (`uuid`),
  INDEX `i_autoscale_vmprofiles__removed`(`removed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_policies` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `duration` int unsigned NOT NULL,
  `quiet_time` int unsigned NOT NULL,
  `last_quiet_time` datetime DEFAULT NULL,
  `action` varchar(15),
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_policies__domain_id` FOREIGN KEY `fk_autoscale_policies__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_policies__account_id` FOREIGN KEY `fk_autoscale_policies__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_autoscale_policies__uuid` UNIQUE (`uuid`),
  INDEX `i_autoscale_policies__removed`(`removed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_vmgroups` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `zone_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `load_balancer_id` bigint unsigned NOT NULL,
  `min_members` int unsigned DEFAULT 1,
  `max_members` int unsigned NOT NULL,
  `member_port` int unsigned NOT NULL,
  `interval` int unsigned NOT NULL,
  `profile_id` bigint unsigned NOT NULL,
  `state` varchar(255) NOT NULL COMMENT 'enabled or disabled, a vmgroup is disabled to stop autoscaling activity',
  `created` datetime NOT NULL COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmgroup__autoscale_vmprofile_id` FOREIGN KEY(`profile_id`) REFERENCES `autoscale_vmprofiles`(`id`),
  CONSTRAINT `fk_autoscale_vmgroup__load_balancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroups__domain_id` FOREIGN KEY `fk_autoscale_vmgroups__domain_id` (`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroups__account_id` FOREIGN KEY `fk_autoscale_vmgroups__account_id` (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroups__zone_id` FOREIGN KEY `fk_autoscale_vmgroups__zone_id`(`zone_id`) REFERENCES `data_center`(`id`),
  CONSTRAINT `uc_autoscale_vmgroups__uuid` UNIQUE (`uuid`),
  INDEX `i_autoscale_vmgroups__removed`(`removed`),
  INDEX `i_autoscale_vmgroups__load_balancer_id`(`load_balancer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_policy_condition_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `policy_id` bigint unsigned NOT NULL,
  `condition_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_policy_condition_map__policy_id` FOREIGN KEY `fk_autoscale_policy_condition_map__policy_id` (`policy_id`) REFERENCES `autoscale_policies` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_policy_condition_map__condition_id` FOREIGN KEY `fk_autoscale_policy_condition_map__condition_id` (`condition_id`) REFERENCES `conditions` (`id`),
  INDEX `i_autoscale_policy_condition_map__policy_id`(`policy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_vmgroup_policy_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vmgroup_id` bigint unsigned NOT NULL,
  `policy_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmgroup_policy_map__vmgroup_id` FOREIGN KEY `fk_autoscale_vmgroup_policy_map__vmgroup_id` (`vmgroup_id`) REFERENCES `autoscale_vmgroups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroup_policy_map__policy_id` FOREIGN KEY `fk_autoscale_vmgroup_policy_map__policy_id` (`policy_id`) REFERENCES `autoscale_policies` (`id`),
  INDEX `i_autoscale_vmgroup_policy_map__vmgroup_id`(`vmgroup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`autoscale_vmgroup_vm_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vmgroup_id` bigint unsigned NOT NULL,
  `instance_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_autoscale_vmgroup_vm_map__vmgroup_id` FOREIGN KEY `fk_autoscale_vmgroup_vm_map__vmgroup_id` (`vmgroup_id`) REFERENCES `autoscale_vmgroups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_autoscale_vmgroup_vm_map__instance_id` FOREIGN KEY `fk_autoscale_vmgroup_vm_map__instance_id` (`instance_id`) REFERENCES `vm_instance` (`id`),
  INDEX `i_autoscale_vmgroup_vm_map__vmgroup_id`(`vmgroup_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (1, UUID(), 'snmp','Linux User CPU - percentage', '1.3.6.1.4.1.2021.11.9.0', now());
INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (2, UUID(), 'snmp','Linux System CPU - percentage', '1.3.6.1.4.1.2021.11.10.0', now());
INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (3, UUID(), 'snmp','Linux CPU Idle - percentage', '1.3.6.1.4.1.2021.11.11.0', now());
INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (100, UUID(), 'netscaler','Response Time - microseconds', 'RESPTIME', now());
INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (4, UUID(), 'cpu','Linux User CPU - percentage - native', '1.3.6.1.4.1.2021.11.9.1', now());
INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (5, UUID(), 'memory','Linux User RAM - percentage - native', '1.3.6.1.4.1.2021.11.10.1', now());

CREATE TABLE  `cloud`.`user_ipv6_address` (
  `id` bigint unsigned NOT NULL UNIQUE auto_increment,
  `uuid` varchar(40),
  `account_id` bigint unsigned NULL,
  `domain_id` bigint unsigned NULL,
  `ip_address` char(50) NOT NULL,
  `data_center_id` bigint unsigned NOT NULL COMMENT 'zone that it belongs to',
  `vlan_id` bigint unsigned NOT NULL,
  `state` char(32) NOT NULL default 'Free' COMMENT 'state of the ip address',
  `mac_address` varchar(40) NOT NULL COMMENT 'mac address of this ip',
  `source_network_id` bigint unsigned NOT NULL COMMENT 'network id ip belongs to',
  `network_id` bigint unsigned COMMENT 'network this public ip address is associated with',
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'physical network id that this configuration is based on',
  `created` datetime NULL COMMENT 'Date this ip was allocated to someone',
  PRIMARY KEY (`id`),
  UNIQUE (`ip_address`, `source_network_id`),
  CONSTRAINT `fk_user_ipv6_address__source_network_id` FOREIGN KEY (`source_network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `fk_user_ipv6_address__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `fk_user_ipv6_address__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`),
  CONSTRAINT `fk_user_ipv6_address__vlan_id` FOREIGN KEY (`vlan_id`) REFERENCES `vlan`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_ipv6_address__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_user_ipv6_address__uuid` UNIQUE (`uuid`),
  CONSTRAINT `fk_user_ipv6_address__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`networks` ADD COLUMN `ip6_gateway` varchar(50) COMMENT 'IPv6 gateway for this network';
ALTER TABLE `cloud`.`networks` ADD COLUMN `ip6_cidr` varchar(50) COMMENT 'IPv6 cidr for this network';

ALTER TABLE `cloud`.`nics` ADD COLUMN `ip6_gateway` varchar(50) COMMENT 'gateway for ip6 address';
ALTER TABLE `cloud`.`nics` ADD COLUMN `ip6_cidr` varchar(50) COMMENT 'cidr for ip6 address';

ALTER TABLE `cloud`.`vlan` ADD COLUMN `ip6_gateway` varchar(255);
ALTER TABLE `cloud`.`vlan` ADD COLUMN `ip6_cidr` varchar(255);
ALTER TABLE `cloud`.`vlan` ADD COLUMN `ip6_range` varchar(255);

ALTER TABLE `cloud`.`data_center` ADD COLUMN `ip6_dns1` varchar(255);
ALTER TABLE `cloud`.`data_center` ADD COLUMN `ip6_dns2` varchar(255);

UPDATE `cloud`.`networks` INNER JOIN `cloud`.`vlan` ON networks.id = vlan.network_id
SET networks.gateway = vlan.vlan_gateway, networks.ip6_gateway = vlan.ip6_gateway, networks.ip6_cidr = vlan.ip6_cidr
WHERE networks.data_center_id = vlan.data_center_id AND networks.physical_network_id = vlan.physical_network_id;

CREATE TABLE `cloud`.`baremetal_dhcp_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) UNIQUE,
  `nsp_id` bigint unsigned DEFAULT NULL COMMENT 'Network Service Provider ID',
  `pod_id` bigint unsigned DEFAULT NULL COMMENT 'Pod id where this dhcp server in',
  `device_type` varchar(255) DEFAULT NULL COMMENT 'type of the external device',
  `physical_network_id` bigint unsigned DEFAULT NULL COMMENT 'id of the physical network in to which external dhcp device is added',
  `host_id` bigint unsigned DEFAULT NULL COMMENT 'host id coresponding to the external dhcp device',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`baremetal_pxe_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) UNIQUE,
  `nsp_id` bigint unsigned DEFAULT NULL COMMENT 'Network Service Provider ID',
  `pod_id` bigint unsigned DEFAULT NULL COMMENT 'Pod id where this pxe server in, for pxe per zone this field is null',
  `device_type` varchar(255) DEFAULT NULL COMMENT 'type of the pxe device',
  `physical_network_id` bigint unsigned DEFAULT NULL COMMENT 'id of the physical network in to which external pxe device is added',
  `host_id` bigint unsigned DEFAULT NULL COMMENT 'host id coresponding to the external pxe device',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`ucs_blade` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) UNIQUE,
  `ucs_manager_id` bigint unsigned NOT NULL,
  `host_id` bigint unsigned DEFAULT NULL,
  `dn` varchar(512) NOT NULL,
  `profile_dn` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`ucs_manager` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40) UNIQUE,
  `zone_id` bigint unsigned NOT NULL,
  `name` varchar(128) DEFAULT NULL,
  `url` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


SET foreign_key_checks = 1;

UPDATE `cloud`.`configuration` SET value='KVM,XenServer,VMware,Ovm' WHERE name='hypervisor.list';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'concurrent.snapshots.threshold.perhost' , NULL, 'Limit number of snapshots that can be handled concurrently; default is NULL - unlimited.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.ipv6.search.retry.max' , 10000, 'The maximum number of retrying times to search for an available IPv6 address in the table');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Usage', 'DEFAULT', 'management-server', 'traffic.sentinel.exclude.zones' , '', 'Traffic going into specified list of zones is not metered');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Usage', 'DEFAULT', 'management-server', 'traffic.sentinel.include.zones' , 'EXTERNAL', 'Traffic going into specified list of zones is metered. For metering all traffic leave this parameter empty');


INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (163, UUID(), 10, 'Ubuntu 12.04 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (164, UUID(), 10, 'Ubuntu 12.04 (64-bit)');

DROP TABLE IF EXISTS `cloud`.`netscaler_pod_ref`;
CREATE TABLE  `cloud`.`netscaler_pod_ref` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `external_load_balancer_device_id` bigint unsigned NOT NULL COMMENT 'id of external load balancer device',
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod id',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_ns_pod_ref__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `cloud`.`host_pod_ref`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ns_pod_ref__device_id` FOREIGN KEY (`external_load_balancer_device_id`) REFERENCES `external_load_balancer_devices`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'eip.use.multiple.netscalers' , 'false', 'Should be set to true, if there will be multiple NetScaler devices providing EIP service in a zone');

UPDATE `cloud`.`configuration` set category='Advanced' where category='Advanced ';
UPDATE `cloud`.`configuration` set category='Hidden' where category='Hidden ';

