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


ALTER TABLE `cloud`.`alert` ADD INDEX `last_sent` (`last_sent` DESC) ;

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `is_persistent` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if the network offering provides an ability to create persistent networks';


-- populate uuid column with db id if uuid is null
UPDATE `cloud`.`account` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`alert` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`async_job` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`cluster` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`data_center` set uuid=id WHERE uuid is NULL;
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

ALTER TABLE `cloud_usage`.`account` ADD COLUMN `region_id` int unsigned NOT NULL DEFAULT '1';

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

INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (1, UUID(), 'snmp','Linux User CPU - percentage', '1.3.6.1.4.1.2021.11.9.0', now());
INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (2, UUID(), 'snmp','Linux System CPU - percentage', '1.3.6.1.4.1.2021.11.10.0', now());
INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (3, UUID(), 'snmp','Linux CPU Idle - percentage', '1.3.6.1.4.1.2021.11.11.0', now());
INSERT INTO `cloud`.`counter` (id, uuid, source, name, value,created) VALUES (100, UUID(), 'netscaler','Response Time - microseconds', 'RESPTIME', now());
CREATE TABLE `cloud`.`vm_snapshots` (
  `id` bigint(20) unsigned NOT NULL auto_increment COMMENT 'Primary Key',
  `uuid` varchar(40) NOT NULL,
  `name` varchar(255) NOT NULL,
  `display_name` varchar(255) default NULL,
  `description` varchar(255) default NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `vm_snapshot_type` varchar(32) default NULL,
  `state` varchar(32) NOT NULL,
  `parent` bigint unsigned default NULL,
  `current` int(1) unsigned default NULL,
  `update_count` bigint unsigned NOT NULL DEFAULT 0,
  `updated` datetime default NULL,
  `created` datetime default NULL,
  `removed` datetime default NULL,
  PRIMARY KEY  (`id`),
  CONSTRAINT UNIQUE KEY `uc_vm_snapshots_uuid` (`uuid`),
  INDEX `vm_snapshots_name` (`name`),
  INDEX `vm_snapshots_vm_id` (`vm_id`),
  INDEX `vm_snapshots_account_id` (`account_id`),
  INDEX `vm_snapshots_display_name` (`display_name`),
  INDEX `vm_snapshots_removed` (`removed`),
  INDEX `vm_snapshots_parent` (`parent`),
  CONSTRAINT `fk_vm_snapshots_vm_id__vm_instance_id` FOREIGN KEY `fk_vm_snapshots_vm_id__vm_instance_id` (`vm_id`) REFERENCES `vm_instance` (`id`),
  CONSTRAINT `fk_vm_snapshots_account_id__account_id` FOREIGN KEY `fk_vm_snapshots_account_id__account_id` (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_vm_snapshots_domain_id__domain_id` FOREIGN KEY `fk_vm_snapshots_domain_id__domain_id` (`domain_id`) REFERENCES `domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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

-- DB views for list api

DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE VIEW `cloud`.`user_vm_view` AS
    select 
        vm_instance.id id,
        vm_instance.name name,
        user_vm.display_name display_name,
        user_vm.user_data user_data,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        instance_group.id instance_group_id,
        instance_group.uuid instance_group_uuid,
        instance_group.name instance_group_name,
        vm_instance.uuid uuid,
        vm_instance.last_host_id last_host_id,
        vm_instance.vm_type type,
        vm_instance.vnc_password vnc_password,
        vm_instance.limit_cpu_use limit_cpu_use,
        vm_instance.created created,
        vm_instance.state state,
        vm_instance.removed removed,
        vm_instance.ha_enabled ha_enabled,
        vm_instance.hypervisor_type hypervisor_type,
        vm_instance.instance_name instance_name,
        vm_instance.guest_os_id guest_os_id,
        guest_os.uuid guest_os_uuid,
        vm_instance.pod_id pod_id,
        host_pod_ref.uuid pod_uuid,
        vm_instance.private_ip_address private_ip_address,
        vm_instance.private_mac_address private_mac_address,
        vm_instance.vm_type vm_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.is_security_group_enabled security_group_enabled,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.name template_name,
        vm_template.display_text template_display_text,
        vm_template.enable_password password_enabled,
        iso.id iso_id,
        iso.uuid iso_uuid,
        iso.name iso_name,
        iso.display_text iso_display_text,
        service_offering.id service_offering_id,
        disk_offering.uuid service_offering_uuid,
        service_offering.cpu cpu,
        service_offering.speed speed,
        service_offering.ram_size ram_size,
        disk_offering.name service_offering_name,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.pool_type pool_type,
        volumes.id volume_id,
        volumes.uuid volume_uuid,
        volumes.device_id volume_device_id,
        volumes.volume_type volume_type,
        security_group.id security_group_id,
        security_group.uuid security_group_uuid,
        security_group.name security_group_name,
        security_group.description security_group_description,
        nics.id nic_id,
        nics.uuid nic_uuid,
        nics.network_id network_id,
        nics.ip4_address ip_address,
        nics.ip6_address ip6_address,
        nics.ip6_gateway ip6_gateway,
        nics.ip6_cidr ip6_cidr,
        nics.default_nic is_default_nic,
        nics.gateway gateway,
        nics.netmask netmask,
        nics.mac_address mac_address,
        nics.broadcast_uri broadcast_uri,
        nics.isolation_uri isolation_uri,
        vpc.id vpc_id,
        vpc.uuid vpc_uuid,
        networks.uuid network_uuid,
        networks.name network_name,
        networks.traffic_type traffic_type,
        networks.guest_type guest_type,
        user_ip_address.id public_ip_id,
        user_ip_address.uuid public_ip_uuid,
        user_ip_address.public_ip_address public_ip_address,
        ssh_keypairs.keypair_name keypair_name,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`user_vm`
            inner join
        `cloud`.`vm_instance` ON vm_instance.id = user_vm.id
            and vm_instance.removed is NULL
            inner join
        `cloud`.`account` ON vm_instance.account_id = account.id
            inner join
        `cloud`.`domain` ON vm_instance.domain_id = domain.id
            left join
        `cloud`.`guest_os` ON vm_instance.guest_os_id = guest_os.id
            left join
        `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`instance_group_vm_map` ON vm_instance.id = instance_group_vm_map.instance_id
            left join
        `cloud`.`instance_group` ON instance_group_vm_map.group_id = instance_group.id
            left join
        `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
            left join
        `cloud`.`host` ON vm_instance.host_id = host.id
            left join
        `cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
            left join
        `cloud`.`vm_template` iso ON iso.id = user_vm.iso_id
            left join
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
            left join
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
            left join
        `cloud`.`volumes` ON vm_instance.id = volumes.instance_id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`security_group_vm_map` ON vm_instance.id = security_group_vm_map.instance_id
            left join
        `cloud`.`security_group` ON security_group_vm_map.security_group_id = security_group.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON networks.vpc_id = vpc.id
            left join
        `cloud`.`user_ip_address` ON user_ip_address.vm_id = vm_instance.id
            left join
        `cloud`.`user_vm_details` ON user_vm_details.vm_id = vm_instance.id
            and user_vm_details.name = 'SSH.PublicKey'
            left join
        `cloud`.`ssh_keypairs` ON ssh_keypairs.public_key = user_vm_details.value
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = vm_instance.id
            and resource_tags.resource_type = 'UserVm'
            left join
        `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
            and async_job.instance_type = 'VirtualMachine'
            and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`domain_router_view`;
CREATE VIEW `cloud`.`domain_router_view` AS
    select 
        vm_instance.id id,
        vm_instance.name name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        vm_instance.uuid uuid,
        vm_instance.created created,
        vm_instance.state state,
        vm_instance.removed removed,
        vm_instance.pod_id pod_id,
        vm_instance.instance_name instance_name,
        host_pod_ref.uuid pod_uuid,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        data_center.dns1 dns1,
        data_center.dns2 dns2,
        data_center.ip6_dns1 ip6_dns1,
        data_center.ip6_dns2 ip6_dns2,
        host.id host_id,
        host.uuid host_uuid,
        host.name host_name,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        service_offering.id service_offering_id,
        disk_offering.uuid service_offering_uuid,
        disk_offering.name service_offering_name,
        nics.id nic_id,
        nics.uuid nic_uuid,
        nics.network_id network_id,
        nics.ip4_address ip_address,
        nics.ip6_address ip6_address,
        nics.ip6_gateway ip6_gateway,
        nics.ip6_cidr ip6_cidr,
        nics.default_nic is_default_nic,
        nics.gateway gateway,
        nics.netmask netmask,
        nics.mac_address mac_address,
        nics.broadcast_uri broadcast_uri,
        nics.isolation_uri isolation_uri,
        vpc.id vpc_id,
        vpc.uuid vpc_uuid,
        networks.uuid network_uuid,
        networks.name network_name,
        networks.network_domain network_domain,
        networks.traffic_type traffic_type,
        networks.guest_type guest_type,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id,
        domain_router.template_version template_version,
        domain_router.scripts_version scripts_version,
        domain_router.is_redundant_router is_redundant_router,
        domain_router.redundant_state redundant_state,
        domain_router.stop_pending stop_pending
    from
        `cloud`.`domain_router`
            inner join
        `cloud`.`vm_instance` ON vm_instance.id = domain_router.id
            inner join
        `cloud`.`account` ON vm_instance.account_id = account.id
            inner join
        `cloud`.`domain` ON vm_instance.domain_id = domain.id
            left join
        `cloud`.`host_pod_ref` ON vm_instance.pod_id = host_pod_ref.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`data_center` ON vm_instance.data_center_id = data_center.id
            left join
        `cloud`.`host` ON vm_instance.host_id = host.id
            left join
        `cloud`.`vm_template` ON vm_instance.vm_template_id = vm_template.id
            left join
        `cloud`.`service_offering` ON vm_instance.service_offering_id = service_offering.id
            left join
        `cloud`.`disk_offering` ON vm_instance.service_offering_id = disk_offering.id
            left join
        `cloud`.`volumes` ON vm_instance.id = volumes.instance_id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`nics` ON vm_instance.id = nics.instance_id
            left join
        `cloud`.`networks` ON nics.network_id = networks.id
            left join
        `cloud`.`vpc` ON domain_router.vpc_id = vpc.id
            left join
        `cloud`.`async_job` ON async_job.instance_id = vm_instance.id
            and async_job.instance_type = 'DomainRouter'
            and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`security_group_view`;
CREATE VIEW `cloud`.`security_group_view` AS
    select 
        security_group.id id,
        security_group.name name,
        security_group.description description,
        security_group.uuid uuid,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        security_group_rule.id rule_id,
        security_group_rule.uuid rule_uuid,
        security_group_rule.type rule_type,
        security_group_rule.start_port rule_start_port,
        security_group_rule.end_port rule_end_port,
        security_group_rule.protocol rule_protocol,
        security_group_rule.allowed_network_id rule_allowed_network_id,
        security_group_rule.allowed_ip_cidr rule_allowed_ip_cidr,
        security_group_rule.create_status rule_create_status,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`security_group`
            left join
        `cloud`.`security_group_rule` ON security_group.id = security_group_rule.security_group_id
            inner join
        `cloud`.`account` ON security_group.account_id = account.id
            inner join
        `cloud`.`domain` ON security_group.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = security_group.account_id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = security_group.id
            and resource_tags.resource_type = 'SecurityGroup'
            left join
        `cloud`.`async_job` ON async_job.instance_id = security_group.id
            and async_job.instance_type = 'SecurityGroup'
            and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`resource_tag_view`;
CREATE VIEW `cloud`.`resource_tag_view` AS
    select 
        resource_tags.id,
        resource_tags.uuid,
        resource_tags.key,
        resource_tags.value,
        resource_tags.resource_id,
        resource_tags.resource_uuid,
        resource_tags.resource_type,
        resource_tags.customer,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name
    from
        `cloud`.`resource_tags`
            inner join
        `cloud`.`account` ON resource_tags.account_id = account.id
            inner join
        `cloud`.`domain` ON resource_tags.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = resource_tags.account_id;


DROP VIEW IF EXISTS `cloud`.`event_view`;
CREATE VIEW `cloud`.`event_view` AS
    select 
        event.id,
        event.uuid,
        event.type,
        event.state,
        event.description,
        event.created,
        event.level,
        event.parameters,
        event.start_id,
        eve.uuid start_uuid,
        event.user_id,
        user.username user_name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name
    from
        `cloud`.`event`
            inner join
        `cloud`.`account` ON event.account_id = account.id
            inner join
        `cloud`.`domain` ON event.domain_id = domain.id
            inner join
        `cloud`.`user` ON event.user_id = user.id
            left join
        `cloud`.`projects` ON projects.project_account_id = event.account_id
            left join
        `cloud`.`event` eve ON event.start_id = eve.id;

DROP VIEW IF EXISTS `cloud`.`instance_group_view`;
CREATE VIEW `cloud`.`instance_group_view` AS
    select 
        instance_group.id,
        instance_group.uuid,
        instance_group.name,
        instance_group.removed,
        instance_group.created,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name
    from
        `cloud`.`instance_group`
            inner join
        `cloud`.`account` ON instance_group.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = instance_group.account_id;

DROP VIEW IF EXISTS `cloud`.`user_view`;
CREATE VIEW `cloud`.`user_view` AS
    select 
        user.id,
        user.uuid,
        user.username,
        user.password,
        user.firstname,
        user.lastname,
        user.email,
        user.state,
        user.api_key,
        user.secret_key,
        user.created,
        user.removed,
        user.timezone,
        user.registration_token,
        user.is_registered,
        user.incorrect_login_attempts,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`user`
            inner join
        `cloud`.`account` ON user.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`async_job` ON async_job.instance_id = user.id
            and async_job.instance_type = 'User'
            and async_job.job_status = 0;


DROP VIEW IF EXISTS `cloud`.`project_view`;
CREATE VIEW `cloud`.`project_view` AS
    select 
        projects.id,
        projects.uuid,
        projects.name,
        projects.display_text,
        projects.state,
        projects.removed,
        projects.created,
        account.account_name owner,
        pacct.account_id,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer
    from
        `cloud`.`projects`
            inner join
        `cloud`.`domain` ON projects.domain_id = domain.id
            inner join
        `cloud`.`project_account` ON projects.id = project_account.project_id
            and project_account.account_role = 'Admin'
            inner join
        `cloud`.`account` ON account.id = project_account.account_id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = projects.id
            and resource_tags.resource_type = 'Project'
            left join
        `cloud`.`project_account` pacct ON projects.id = pacct.project_id;

DROP VIEW IF EXISTS `cloud`.`project_account_view`;
CREATE VIEW `cloud`.`project_account_view` AS
    select 
        project_account.id,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name,
        account.type account_type,
        project_account.account_role,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`project_account`
            inner join
        `cloud`.`account` ON project_account.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            inner join
        `cloud`.`projects` ON projects.id = project_account.project_id;

DROP VIEW IF EXISTS `cloud`.`project_invitation_view`;
CREATE VIEW `cloud`.`project_invitation_view` AS
    select 
        project_invitations.id,
        project_invitations.uuid,
        project_invitations.email,
        project_invitations.created,
        project_invitations.state,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`project_invitations`
            left join
        `cloud`.`account` ON project_invitations.account_id = account.id
            left join
        `cloud`.`domain` ON project_invitations.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.id = project_invitations.project_id;

DROP VIEW IF EXISTS `cloud`.`host_view`;
CREATE VIEW `cloud`.`host_view` AS
    select 
        host.id,
        host.uuid,
        host.name,
        host.status,
        host.disconnected,
        host.type,
        host.private_ip_address,
        host.version,
        host.hypervisor_type,
        host.hypervisor_version,
        host.capabilities,
        host.last_ping,
        host.created,
        host.removed,
        host.resource_state,
        host.mgmt_server_id,
        host.cpus,
        host.speed,
        host.ram,
        cluster.id cluster_id,
        cluster.uuid cluster_uuid,
        cluster.name cluster_name,
        cluster.cluster_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        host_tags.tag,
        guest_os_category.id guest_os_category_id,
        guest_os_category.uuid guest_os_category_uuid,
        guest_os_category.name guest_os_category_name,
        mem_caps.used_capacity memory_used_capacity,
        mem_caps.reserved_capacity memory_reserved_capacity,
        cpu_caps.used_capacity cpu_used_capacity,
        cpu_caps.reserved_capacity cpu_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`host`
            left join
        `cloud`.`cluster` ON host.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON host.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
            left join
        `cloud`.`host_details` ON host.id = host_details.id
            and host_details.name = 'guest.os.category.id'
            left join
        `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT( host_details.value , UNSIGNED)
            left join
        `cloud`.`host_tags` ON host_tags.host_id = host.id
            left join
        `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
            and mem_caps.capacity_type = 0
            left join
        `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
            and cpu_caps.capacity_type = 1
            left join
        `cloud`.`async_job` ON async_job.instance_id = host.id
            and async_job.instance_type = 'Host'
            and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE VIEW `cloud`.`volume_view` AS
    select 
        volumes.id,
        volumes.uuid,
        volumes.name,
        volumes.device_id,
        volumes.volume_type,
        volumes.size,
        volumes.created,
        volumes.state,
        volumes.attached,
        volumes.removed,
        volumes.pod_id,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        vm_instance.id vm_id,
        vm_instance.uuid vm_uuid,
        vm_instance.name vm_name,
        vm_instance.state vm_state,
        vm_instance.vm_type,
        user_vm.display_name vm_display_name,
        volume_host_ref.size volume_host_size,
        volume_host_ref.created volume_host_created,
        volume_host_ref.format,
        volume_host_ref.download_pct,
        volume_host_ref.download_state,
        volume_host_ref.error_str,
        disk_offering.id disk_offering_id,
        disk_offering.uuid disk_offering_uuid,
        disk_offering.name disk_offering_name,
        disk_offering.display_text disk_offering_display_text,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.name pool_name,
        cluster.hypervisor_type,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.extractable,
        vm_template.type template_type,
        resource_tags.id tag_id,
        resource_tags.uuid tag_uuid,
        resource_tags.key tag_key,
        resource_tags.value tag_value,
        resource_tags.domain_id tag_domain_id,
        resource_tags.account_id tag_account_id,
        resource_tags.resource_id tag_resource_id,
        resource_tags.resource_uuid tag_resource_uuid,
        resource_tags.resource_type tag_resource_type,
        resource_tags.customer tag_customer,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`volumes`
            inner join
        `cloud`.`account` ON volumes.account_id = account.id
            inner join
        `cloud`.`domain` ON volumes.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.project_account_id = account.id
            left join
        `cloud`.`data_center` ON volumes.data_center_id = data_center.id
            left join
        `cloud`.`vm_instance` ON volumes.instance_id = vm_instance.id
            left join
        `cloud`.`user_vm` ON user_vm.id = vm_instance.id
            left join
        `cloud`.`volume_host_ref` ON volumes.id = volume_host_ref.volume_id
            and volumes.data_center_id = volume_host_ref.zone_id
            left join
        `cloud`.`disk_offering` ON volumes.disk_offering_id = disk_offering.id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`vm_template` ON volumes.template_id = vm_template.id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = volumes.id
            and resource_tags.resource_type = 'Volume'
            left join
        `cloud`.`async_job` ON async_job.instance_id = volumes.id
            and async_job.instance_type = 'Volume'
            and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`account_netstats_view`;
CREATE VIEW `cloud`.`account_netstats_view` AS
    SELECT 
        account_id,
        sum(net_bytes_received) + sum(current_bytes_received) as bytesReceived,
        sum(net_bytes_sent) + sum(current_bytes_sent) as bytesSent
    FROM
        `cloud`.`user_statistics`
    group by account_id;


DROP VIEW IF EXISTS `cloud`.`account_vmstats_view`;
CREATE VIEW `cloud`.`account_vmstats_view` AS
    SELECT 
        account_id, state, count(*) as vmcount
    from
        `cloud`.`vm_instance`
    group by account_id , state;

DROP VIEW IF EXISTS `cloud`.`free_ip_view`;
CREATE VIEW `cloud`.`free_ip_view` AS
    select 
        count(user_ip_address.id) free_ip
    from
        `cloud`.`user_ip_address`
            inner join
        `cloud`.`vlan` ON vlan.id = user_ip_address.vlan_db_id
            and vlan.vlan_type = 'VirtualNetwork'
    where
        state = 'Free';

DROP VIEW IF EXISTS `cloud`.`account_view`;
CREATE VIEW `cloud`.`account_view` AS
    select 
        account.id,
        account.uuid,
        account.account_name,
        account.type,
        account.state,
        account.removed,
        account.cleanup_needed,
        account.network_domain,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        account_netstats_view.bytesReceived,
        account_netstats_view.bytesSent,
        vmlimit.max vmLimit,
        vmcount.count vmTotal,
        runningvm.vmcount runningVms,
        stoppedvm.vmcount stoppedVms,
        iplimit.max ipLimit,
        ipcount.count ipTotal,
        free_ip_view.free_ip ipFree,
        volumelimit.max volumeLimit,
        volumecount.count volumeTotal,
        snapshotlimit.max snapshotLimit,
        snapshotcount.count snapshotTotal,
        templatelimit.max templateLimit,
        templatecount.count templateTotal,
        vpclimit.max vpcLimit,
        vpccount.count vpcTotal,
        projectlimit.max projectLimit,
        projectcount.count projectTotal,
        networklimit.max networkLimit,
        networkcount.count networkTotal,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`free_ip_view`,
        `cloud`.`account`
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`data_center` ON account.default_zone_id = data_center.id
            left join
        `cloud`.`account_netstats_view` ON account.id = account_netstats_view.account_id
            left join
        `cloud`.`resource_limit` vmlimit ON account.id = vmlimit.account_id
            and vmlimit.type = 'user_vm'
            left join
        `cloud`.`resource_count` vmcount ON account.id = vmcount.account_id
            and vmcount.type = 'user_vm'
            left join
        `cloud`.`account_vmstats_view` runningvm ON account.id = runningvm.account_id
            and runningvm.state = 'Running'
            left join
        `cloud`.`account_vmstats_view` stoppedvm ON account.id = stoppedvm.account_id
            and stoppedvm.state = 'Stopped'
            left join
        `cloud`.`resource_limit` iplimit ON account.id = iplimit.account_id
            and iplimit.type = 'public_ip'
            left join
        `cloud`.`resource_count` ipcount ON account.id = ipcount.account_id
            and ipcount.type = 'public_ip'
            left join
        `cloud`.`resource_limit` volumelimit ON account.id = volumelimit.account_id
            and volumelimit.type = 'volume'
            left join
        `cloud`.`resource_count` volumecount ON account.id = volumecount.account_id
            and volumecount.type = 'volume'
            left join
        `cloud`.`resource_limit` snapshotlimit ON account.id = snapshotlimit.account_id
            and snapshotlimit.type = 'snapshot'
            left join
        `cloud`.`resource_count` snapshotcount ON account.id = snapshotcount.account_id
            and snapshotcount.type = 'snapshot'
            left join
        `cloud`.`resource_limit` templatelimit ON account.id = templatelimit.account_id
            and templatelimit.type = 'template'
            left join
        `cloud`.`resource_count` templatecount ON account.id = templatecount.account_id
            and templatecount.type = 'template'
            left join
        `cloud`.`resource_limit` vpclimit ON account.id = vpclimit.account_id
            and vpclimit.type = 'vpc'
            left join
        `cloud`.`resource_count` vpccount ON account.id = vpccount.account_id
            and vpccount.type = 'vpc'
            left join
        `cloud`.`resource_limit` projectlimit ON account.id = projectlimit.account_id
            and projectlimit.type = 'project'
            left join
        `cloud`.`resource_count` projectcount ON account.id = projectcount.account_id
            and projectcount.type = 'project'
            left join
        `cloud`.`resource_limit` networklimit ON account.id = networklimit.account_id
            and networklimit.type = 'network'
            left join
        `cloud`.`resource_count` networkcount ON account.id = networkcount.account_id
            and networkcount.type = 'network'
            left join
        `cloud`.`async_job` ON async_job.instance_id = account.id
            and async_job.instance_type = 'Account'
            and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`async_job_view`;
CREATE VIEW `cloud`.`async_job_view` AS
    select 
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        user.id user_id,
        user.uuid user_uuid,
        async_job.id,
        async_job.uuid,
        async_job.job_cmd,
        async_job.job_status,
        async_job.job_process_status,
        async_job.job_result_code,
        async_job.job_result,
        async_job.created,
        async_job.removed,
        async_job.instance_type,
        async_job.instance_id,
        CASE
            WHEN async_job.instance_type = 'Volume' THEN volumes.uuid
            WHEN
                async_job.instance_type = 'Template'
                    or async_job.instance_type = 'Iso'
            THEN
                vm_template.uuid
            WHEN
                async_job.instance_type = 'VirtualMachine'
                    or async_job.instance_type = 'ConsoleProxy'
                    or async_job.instance_type = 'SystemVm'
                    or async_job.instance_type = 'DomainRouter'
            THEN
                vm_instance.uuid
            WHEN async_job.instance_type = 'Snapshot' THEN snapshots.uuid
            WHEN async_job.instance_type = 'Host' THEN host.uuid
            WHEN async_job.instance_type = 'StoragePool' THEN storage_pool.uuid
            WHEN async_job.instance_type = 'IpAddress' THEN user_ip_address.uuid
            WHEN async_job.instance_type = 'SecurityGroup' THEN security_group.uuid
            WHEN async_job.instance_type = 'PhysicalNetwork' THEN physical_network.uuid
            WHEN async_job.instance_type = 'TrafficType' THEN physical_network_traffic_types.uuid
            WHEN async_job.instance_type = 'PhysicalNetworkServiceProvider' THEN physical_network_service_providers.uuid
            WHEN async_job.instance_type = 'FirewallRule' THEN firewall_rules.uuid
            WHEN async_job.instance_type = 'Account' THEN acct.uuid
            WHEN async_job.instance_type = 'User' THEN us.uuid
            WHEN async_job.instance_type = 'StaticRoute' THEN static_routes.uuid
            WHEN async_job.instance_type = 'PrivateGateway' THEN vpc_gateways.uuid
            WHEN async_job.instance_type = 'Counter' THEN counter.uuid
            WHEN async_job.instance_type = 'Condition' THEN conditions.uuid
            WHEN async_job.instance_type = 'AutoScalePolicy' THEN autoscale_policies.uuid
            WHEN async_job.instance_type = 'AutoScaleVmProfile' THEN autoscale_vmprofiles.uuid
            WHEN async_job.instance_type = 'AutoScaleVmGroup' THEN autoscale_vmgroups.uuid
            ELSE null
        END instance_uuid
    from
        `cloud`.`async_job`
            left join
        `cloud`.`account` ON async_job.account_id = account.id
            left join
        `cloud`.`domain` ON domain.id = account.domain_id
            left join
        `cloud`.`user` ON async_job.user_id = user.id
            left join
        `cloud`.`volumes` ON async_job.instance_id = volumes.id
            left join
        `cloud`.`vm_template` ON async_job.instance_id = vm_template.id
            left join
        `cloud`.`vm_instance` ON async_job.instance_id = vm_instance.id
            left join
        `cloud`.`snapshots` ON async_job.instance_id = snapshots.id
            left join
        `cloud`.`host` ON async_job.instance_id = host.id
            left join
        `cloud`.`storage_pool` ON async_job.instance_id = storage_pool.id
            left join
        `cloud`.`user_ip_address` ON async_job.instance_id = user_ip_address.id
            left join
        `cloud`.`security_group` ON async_job.instance_id = security_group.id
            left join
        `cloud`.`physical_network` ON async_job.instance_id = physical_network.id
            left join
        `cloud`.`physical_network_traffic_types` ON async_job.instance_id = physical_network_traffic_types.id
            left join
        `cloud`.`physical_network_service_providers` ON async_job.instance_id = physical_network_service_providers.id
            left join
        `cloud`.`firewall_rules` ON async_job.instance_id = firewall_rules.id
            left join
        `cloud`.`account` acct ON async_job.instance_id = acct.id
            left join
        `cloud`.`user` us ON async_job.instance_id = us.id
            left join
        `cloud`.`static_routes` ON async_job.instance_id = static_routes.id
            left join
        `cloud`.`vpc_gateways` ON async_job.instance_id = vpc_gateways.id
            left join
        `cloud`.`counter` ON async_job.instance_id = counter.id
            left join
        `cloud`.`conditions` ON async_job.instance_id = conditions.id
            left join
        `cloud`.`autoscale_policies` ON async_job.instance_id = autoscale_policies.id
            left join
        `cloud`.`autoscale_vmprofiles` ON async_job.instance_id = autoscale_vmprofiles.id
            left join
        `cloud`.`autoscale_vmgroups` ON async_job.instance_id = autoscale_vmgroups.id;

DROP VIEW IF EXISTS `cloud`.`storage_pool_view`;
CREATE VIEW `cloud`.`storage_pool_view` AS
    select 
        storage_pool.id,
        storage_pool.uuid,
        storage_pool.name,
        storage_pool.status,
        storage_pool.path,
        storage_pool.pool_type,
        storage_pool.host_address,
        storage_pool.created,
        storage_pool.removed,
        storage_pool.capacity_bytes,
        storage_pool.scope,
        cluster.id cluster_id,
        cluster.uuid cluster_uuid,
        cluster.name cluster_name,
        cluster.cluster_type,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        storage_pool_details.name tag,
        op_host_capacity.used_capacity disk_used_capacity,
        op_host_capacity.reserved_capacity disk_reserved_capacity,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`storage_pool`
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`data_center` ON storage_pool.data_center_id = data_center.id
            left join
        `cloud`.`host_pod_ref` ON storage_pool.pod_id = host_pod_ref.id
            left join
        `cloud`.`storage_pool_details` ON storage_pool_details.pool_id = storage_pool.id
            and storage_pool_details.value = 'true'
            left join
        `cloud`.`op_host_capacity` ON storage_pool.id = op_host_capacity.host_id
            and op_host_capacity.capacity_type = 3
            left join
        `cloud`.`async_job` ON async_job.instance_id = storage_pool.id
            and async_job.instance_type = 'StoragePool'
            and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`disk_offering_view`;
CREATE VIEW `cloud`.`disk_offering_view` AS
    select 
        disk_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.disk_size,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.customized,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.sort_key,
        disk_offering.type,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`disk_offering`
            left join
        `cloud`.`domain` ON disk_offering.domain_id = domain.id;

DROP VIEW IF EXISTS `cloud`.`service_offering_view`;
CREATE VIEW `cloud`.`service_offering_view` AS
    select 
        service_offering.id,
        disk_offering.uuid,
        disk_offering.name,
        disk_offering.display_text,
        disk_offering.created,
        disk_offering.tags,
        disk_offering.removed,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        service_offering.cpu,
        service_offering.speed,
        service_offering.ram_size,
        service_offering.nw_rate,
        service_offering.mc_rate,
        service_offering.ha_enabled,
        service_offering.limit_cpu_use,
        service_offering.host_tag,
        service_offering.default_use,
        service_offering.vm_type,
        service_offering.sort_key,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`service_offering`
            inner join
        `cloud`.`disk_offering` ON service_offering.id = disk_offering.id
            left join
        `cloud`.`domain` ON disk_offering.domain_id = domain.id;
        
DROP VIEW IF EXISTS `cloud`.`data_center_view`;
CREATE VIEW `cloud`.`data_center_view` AS
    select 
        data_center.id,
        data_center.uuid,
        data_center.name,
        data_center.is_security_group_enabled,
        data_center.is_local_storage_enabled,
        data_center.description,
        data_center.dns1,
        data_center.dns2,
        data_center.ip6_dns1,
        data_center.ip6_dns2,
        data_center.internal_dns1,
        data_center.internal_dns2,
        data_center.guest_network_cidr,
        data_center.domain,
        data_center.networktype,
        data_center.allocation_state,
        data_center.zone_token,
        data_center.dhcp_provider,
        data_center.removed,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`data_center`
            left join
        `cloud`.`domain` ON data_center.domain_id = domain.id;               
        

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
