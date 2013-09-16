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

-- Schema upgrade from 2.2.14 to 3.0;

ALTER TABLE `cloud`.`host` ADD COLUMN `hypervisor_version` varchar(32) COMMENT 'hypervisor version' AFTER hypervisor_type;

CREATE TABLE `cloud`.`hypervisor_capabilities` (
  `id` bigint unsigned NOT NULL auto_increment,
  `hypervisor_type` varchar(32) NOT NULL,
  `hypervisor_version` varchar(32),
  `max_guests_limit` bigint unsigned DEFAULT 50,
  `security_group_enabled` int(1) unsigned DEFAULT 1 COMMENT 'Is security group supported',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', 'default', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', 'XCP 1.0', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', '5.6', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', '5.6 FP1', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', '5.6 SP2', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', '6.0', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('VMware', 'default', 128, 0);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('VMware', '4.0', 128, 0);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('VMware', '4.1', 128, 0);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('KVM', 'default', 50, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('Ovm', 'default', 25, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('Ovm', '2.3', 25, 1);


CREATE TABLE  `cloud`.`projects` (
  `id` bigint unsigned NOT NULL auto_increment,
  `name` varchar(255) COMMENT 'project name',
  `display_text` varchar(255) COMMENT 'project name',
  `project_account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `created` datetime COMMENT 'date created',
  `removed` datetime COMMENT 'date removed',
  `state` varchar(255) NOT NULL COMMENT 'state of the project (Active/Inactive/Suspended)',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_projects__project_account_id` FOREIGN KEY(`project_account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_projects__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  INDEX `i_projects__removed`(`removed`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE  `cloud`.`project_account` (
  `id` bigint unsigned NOT NULL auto_increment,
  `account_id` bigint unsigned NOT NULL COMMENT'account id',
  `account_role` varchar(255) NOT NULL DEFAULT 'Regular' COMMENT 'Account role in the project (Owner or Regular)',
  `project_id` bigint unsigned NOT NULL COMMENT 'project id',
  `project_account_id` bigint unsigned NOT NULL,
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_project_account__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_account__project_id` FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_account__project_account_id` FOREIGN KEY(`project_account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  UNIQUE (`account_id`, `project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE  `cloud`.`project_invitations` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `project_id` bigint unsigned NOT NULL COMMENT 'project id',
  `account_id` bigint unsigned COMMENT 'account id',
  `domain_id` bigint unsigned COMMENT 'domain id',
  `email` varchar(255) COMMENT 'email',
  `token` varchar(255) COMMENT 'token',
  `state` varchar(255) NOT NULL DEFAULT 'Pending' COMMENT 'the state of the invitation',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_project_invitations__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_invitations__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_invitations__project_id` FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE,
  UNIQUE (`project_id`, `account_id`),
  UNIQUE (`project_id`, `email`),
  UNIQUE (`project_id`, `token`),
  CONSTRAINT `uc_project_invitations__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`load_balancer_stickiness_policies` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `load_balancer_id` bigint unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(4096) NULL COMMENT 'description',
  `method_name` varchar(255) NOT NULL,
  `params` varchar(4096) NOT NULL,
  `revoke` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 is when rule is set for Revoke',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_load_balancer_stickiness_policies__load_balancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `cloud`.`template_swift_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `swift_id` bigint unsigned NOT NULL,
  `template_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `path` varchar(255),
  `size` bigint unsigned,
  `physical_size` bigint unsigned DEFAULT 0,
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_template_swift_ref__swift_id` FOREIGN KEY `fk_template_swift_ref__swift_id` (`swift_id`) REFERENCES `swift` (`id`) ON DELETE CASCADE,
  INDEX `i_template_swift_ref__swift_id`(`swift_id`),
  CONSTRAINT `fk_template_swift_ref__template_id` FOREIGN KEY `fk_template_swift_ref__template_id` (`template_id`) REFERENCES `vm_template` (`id`),
  INDEX `i_template_swift_ref__template_id`(`template_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`snapshots` DROP COLUMN `swift_name`;
ALTER TABLE `cloud`.`swift` DROP COLUMN `hostname`;
ALTER TABLE `cloud`.`swift` DROP COLUMN `token`;
ALTER TABLE `cloud`.`swift` ADD COLUMN `uuid` varchar(40);
ALTER TABLE `cloud`.`swift` ADD COLUMN `url` varchar(255) NOT NULL;
ALTER TABLE `cloud`.`swift` ADD COLUMN `key` varchar(255) NOT NULL COMMENT 'token for this user';
ALTER TABLE `cloud`.`swift` ADD COLUMN `created` datetime COMMENT 'date the swift first signed on';
ALTER TABLE `cloud`.`swift` ADD CONSTRAINT `uc_swift__uuid` UNIQUE (`uuid`);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'swift.enable', 'false', 'enable swift');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.user.vms', '20', 'The default maximum number of user VMs that can be deployed for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.public.ips', '20', 'The default maximum number of public IPs that can be consumed by a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.templates', '20', 'The default maximum number of templates that can be deployed for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.snapshots', '20', 'The default maximum number of snapshots that can be created for a project');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'max.project.volumes', '20', 'The default maximum number of volumes that can be created for a project');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'project.invite.required', 'false', 'If invitation confirmation is required when add account to project. Default value is false');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'project.invite.timeout', '86400', 'Invitation expiration time (in seconds). Default is 1 day - 86400 seconds');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'allow.user.create.projects', 'true', 'If regular user can create a project; true by default');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'project.email.sender', null, 'Sender of project invitation email (will be in the From header of the email).');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'project.smtp.host', null, 'SMTP hostname used for sending out email project invitations');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'project.smtp.password', null, 'Password for SMTP authentication (applies only if project.smtp.useAuth is true)');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'project.smtp.port', '465', 'Port the SMTP server is listening on');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'project.smtp.useAuth', null, 'If true, use SMTP authentication when sending emails');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Project Defaults', 'DEFAULT', 'management-server', 'project.smtp.username', null, 'Username for SMTP authentication (applies only if project.smtp.useAuth is true)');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Alert', 'DEFAULT', 'management-server', 'cluster.memory.allocated.capacity.disablethreshold' , .85, 'Percentage (as a value between 0 and 1) of memory utilization above which allocators will disable using the cluster for low memory available. Keep the corresponding notification threshold lower than this to be notified beforehand.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Alert', 'DEFAULT', 'management-server', 'cluster.cpu.allocated.capacity.disablethreshold' , .85, 'Percentage (as a value between 0 and 1) of cpu utilization above which allocators will disable using the cluster for low cpu available. Keep the corresponding notification threshold lower than this to be notified beforehand.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Alert', 'DEFAULT', 'management-server', 'pool.storage.allocated.capacity.disablethreshold' , .85, 'Percentage (as a value between 0 and 1) of allocated storage utilization above which allocators will disable using the pool for low allocated storage available.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Alert', 'DEFAULT', 'management-server', 'pool.storage.capacity.disablethreshold' , .85, 'Percentage (as a value between 0 and 1) of storage utilization above which allocators will disable using the pool for low storage available.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Alert', 'DEFAULT', 'management-server', 'zone.vlan.capacity.notificationthreshold' , .75, 'Percentage (as a value between 0 and 1) of Zone Vlan utilization above which alerts will be sent about low number of Zone Vlans.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Alert', 'DEFAULT', 'management-server', 'cluster.localStorage.capacity.notificationthreshold' , .75, 'Percentage (as a value between 0 and 1) of local storage utilization above which alerts will be sent about low local storage available.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Alert', 'DEFAULT', 'management-server', 'zone.directnetwork.publicip.capacity.notificationthreshold' , .75, 'Percentage (as a value between 0 and 1) of Direct Network Public Ip Utilization above which alerts will be sent about low number of direct network public ips.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Alert', 'DEFAULT', 'management-server', 'zone.secstorage.capacity.notificationthreshold' , .75, 'Percentage (as a value between 0 and 1) of secondary storage utilization above which alerts will be sent about low storage available.');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'custom.diskoffering.size.min', '1', 'Minimum size in GB for custom disk offering');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'custom.diskoffering.size.max', '1024', 'Maximum size in GB for custom disk offering');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'system.vm.random.password', 'false', 'Randomize system vm password the first time management server starts');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network', 'DEFAULT', 'management-server', 'network.securitygroups.defaultadding' , 'true', 'If true, the user VM would be added to the default security group by default');

update `cloud`.`configuration` set name = 'cluster.storage.allocated.capacity.notificationthreshold' , category = 'Alert' where name = 'storage.allocated.capacity.threshold' ;
update `cloud`.`configuration` set name = 'cluster.storage.capacity.notificationthreshold' , category = 'Alert' where name = 'storage.capacity.threshold' ;
update `cloud`.`configuration` set name = 'cluster.cpu.allocated.capacity.notificationthreshold' , category = 'Alert' where name = 'cpu.capacity.threshold' ;
update `cloud`.`configuration` set name = 'cluster.memory.allocated.capacity.notificationthreshold' , category = 'Alert' where name = 'memory.capacity.threshold' ;
update `cloud`.`configuration` set name = 'zone.virtualnetwork.publicip.capacity.notificationthreshold' , category = 'Alert' where name = 'public.ip.capacity.threshold' ;
update `cloud`.`configuration` set name = 'pod.privateip.capacity.notificationthreshold' , category = 'Alert' where name = 'private.ip.capacity.threshold' ;

ALTER TABLE `cloud`.`domain_router` ADD COLUMN `template_version` varchar(100) COMMENT 'template version' AFTER role;
ALTER TABLE `cloud`.`domain_router` ADD COLUMN `scripts_version` varchar(100) COMMENT 'scripts version' AFTER template_version;
ALTER TABLE `cloud`.`alert` ADD `cluster_id` bigint unsigned;

ALTER TABLE `cloud`.`user_statistics` ADD COLUMN `agg_bytes_received` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud`.`user_statistics` ADD COLUMN `agg_bytes_sent` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `uc_vm_instance_uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`async_job` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`async_job` ADD CONSTRAINT `uc_async__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`domain` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`domain` ADD CONSTRAINT `uc_domain__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`account` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`account` ADD CONSTRAINT `uc_account__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud_usage`.`account` ADD COLUMN `uuid` varchar(40);

ALTER TABLE `cloud`.`user` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`user` ADD CONSTRAINT `uc_user__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`projects` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`projects` ADD CONSTRAINT `uc_projects__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`data_center` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`data_center` ADD CONSTRAINT `uc_data_center__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`host` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`host` ADD CONSTRAINT `uc_host__uuid` UNIQUE (`uuid`);
ALTER TABLE `cloud`.`host` ADD COLUMN `update_count` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'atomic increase count making status update operation atomical';

ALTER TABLE `cloud`.`vm_template` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`vm_template` ADD CONSTRAINT `uc_vm_template__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`disk_offering` ADD CONSTRAINT `uc_disk_offering__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`networks` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `uc_networks__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`security_group` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`security_group` ADD CONSTRAINT `uc_security_group__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`instance_group` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`instance_group` ADD CONSTRAINT `uc_instance_group__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`host_pod_ref` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`host_pod_ref` ADD CONSTRAINT `uc_host_pod_ref__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`snapshots` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`snapshots` ADD CONSTRAINT `uc_snapshots__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`snapshot_policy` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`snapshot_policy` ADD CONSTRAINT `uc_snapshot_policy__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`snapshot_schedule` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`snapshot_schedule` ADD CONSTRAINT `uc_snapshot_schedule__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`volumes` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`volumes` ADD CONSTRAINT `uc_volumes__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`vlan` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`vlan` ADD CONSTRAINT `uc_vlan__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `uc_user_ip_address__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`firewall_rules` ADD CONSTRAINT `uc_firewall_rules__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`cluster` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`cluster` ADD CONSTRAINT `uc_cluster__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`network_offerings` ADD CONSTRAINT `uc_network_offerings__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`hypervisor_capabilities` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`hypervisor_capabilities` ADD CONSTRAINT `uc_hypervisor_capabilities__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`vpn_users` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`vpn_users` ADD CONSTRAINT `uc_vpn_users__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`event` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`event` ADD CONSTRAINT `uc_event__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`alert` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`alert` ADD CONSTRAINT `uc_alert__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`guest_os` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`guest_os` ADD CONSTRAINT `uc_guest_os__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`guest_os_category` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`guest_os_category` ADD CONSTRAINT `uc_guest_os_category__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`nics` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`nics` ADD CONSTRAINT `uc_nics__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`op_host_capacity` ADD COLUMN `created` datetime;
ALTER TABLE `cloud`.`op_host_capacity` ADD COLUMN `update_time` datetime;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'apply.allocation.algorithm.to.pods', 'false', 'If true, deployment planner applies the allocation heuristics at pods first in the given datacenter during VM resource allocation');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.user.dispersion.weight', 1, 'Weight for user dispersion heuristic (as a value between 0 and 1) applied to resource allocation during vm deployment. Weight for capacity heuristic will be (1 - weight of user dispersion)');
UPDATE `cloud`.`configuration` SET description = '[''random'', ''firstfit'', ''userdispersing'', ''userconcentratedpod_random'', ''userconcentratedpod_firstfit''] : Order in which hosts within a cluster will be considered for VM/volume allocation.' WHERE name = 'vm.allocation.algorithm';


--;
-- Usage db upgrade from 2.2.14 to 3.0;
--;
CREATE TABLE  `cloud_usage`.`usage_vpn_user` (
 `zone_id` bigint unsigned NOT NULL,
 `account_id` bigint unsigned NOT NULL,
 `domain_id` bigint unsigned NOT NULL,
 `user_id` bigint unsigned NOT NULL,
 `user_name` varchar(32),
 `created` DATETIME NOT NULL,
 `deleted` DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `agg_bytes_received` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `agg_bytes_sent` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `agg_bytes_received` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `agg_bytes_sent` bigint unsigned NOT NULL default '0';
UPDATE `cloud_usage`.`usage_network` set agg_bytes_received = net_bytes_received + current_bytes_received, agg_bytes_sent = net_bytes_sent + current_bytes_sent;

ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__deleted`(`deleted`);
ALTER TABLE `cloud_usage`.`usage_ip_address` ADD COLUMN `is_system` smallint(1) NOT NULL default '0';
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'usage.sanity.check.interval', null, 'Interval (in days) to check sanity of usage data');

DELETE FROM `cloud`.`configuration` WHERE name='host.capacity.checker.wait';
DELETE FROM `cloud`.`configuration` WHERE name='host.capacity.checker.interval'; 
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'disable.extraction' , 'false', 'Flag for disabling extraction of template, isos and volumes');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.check.interval' , '30', 'Interval (in seconds) to report redundant router status.');

ALTER TABLE `cloud`.`security_ingress_rule` RENAME TO `cloud`.`security_group_rule`;

ALTER TABLE `cloud`.`security_group_rule` ADD COLUMN `type` varchar(10) default 'ingress' AFTER security_group_id;
ALTER TABLE `cloud`.`security_group_rule` ADD COLUMN `uuid` varchar(40) AFTER id;
ALTER TABLE `cloud`.`security_group_rule` ADD CONSTRAINT `uc_security_group_rule__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`security_group_rule` ADD CONSTRAINT `fk_security_group_rule___security_group_id` FOREIGN KEY `fk_security_group_rule__security_group_id` (`security_group_id`) REFERENCES `security_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`security_group_rule` ADD CONSTRAINT `fk_security_group_rule___allowed_network_id` FOREIGN KEY `fk_security_group_rule__allowed_network_id` (`allowed_network_id`) REFERENCES `security_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`security_group_rule` ADD INDEX `i_security_group_rule_network_id`(`security_group_id`);
ALTER TABLE `cloud`.`security_group_rule` ADD INDEX `i_security_group_rule_allowed_network`(`allowed_network_id`);
ALTER TABLE `cloud`.`vm_template` ADD COLUMN `enable_sshkey` int(1) unsigned NOT NULL default 0 COMMENT 'true if this template supports sshkey reset';
ALTER TABLE `cloud`.`vm_template` ADD COLUMN `sort_key` int(32) NOT NULL default 0 COMMENT 'sort key used for customising sort method';
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `sort_key` int(32) NOT NULL default 0 COMMENT 'sort key used for customising sort method';
ALTER TABLE `cloud`.`service_offering` ADD COLUMN `sort_key` int(32) NOT NULL default 0 COMMENT 'sort key used for customising sort method';

---;
--- Resource State;
---;

ALTER TABLE `cloud`.`host` ADD COLUMN `resource_state` varchar(32) NOT NULL DEFAULT 'Enabled' COMMENT 'Is this host enabled for allocation for new resources';
UPDATE `cloud`.`host` SET resource_state=allocation_state WHERE status != 'PrepareForMaintenance' AND status != 'ErrorInMaintenance' AND status != 'Maintenance';
UPDATE `cloud`.`host` SET resource_state='PrepareForMaintenance', status='Disconnected' WHERE status = 'PrepareForMaintenance';
UPDATE `cloud`.`host` SET resource_state='ErrorInMaintenance', status='Disconnected' WHERE status = 'ErrorInMaintenance';
UPDATE `cloud`.`host` SET resource_state='Maintenance', status='Disconnected' WHERE status = 'Maintenance';

---;
--- Storage network
---;
update `cloud`.`networks` set guru_name='StorageNetworkGuru' where traffic_type='Storage';
update `cloud`.`configuration` set value=NULL where name='xen.storage.network.device1' and value='cloud-stor1';
update `cloud`.`configuration` set value=NULL where name='xen.storage.network.device2' and value='cloud-stor2';

--;
--NAAS;
--;

CREATE TABLE  `cloud`.`ntwk_service_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_id` bigint unsigned NOT NULL COMMENT 'network_id',
  `service` varchar(255) NOT NULL COMMENT 'service',
  `provider` varchar(255) COMMENT 'service provider',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ntwk_service_map__network_id` FOREIGN KEY(`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  UNIQUE (`network_id`, `service`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`physical_network` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `name` varchar(255) NOT NULL,
  `data_center_id` bigint unsigned NOT NULL COMMENT 'data center id that this physical network belongs to',
  `vnet` varchar(255),
  `speed` varchar(32),  
  `domain_id` bigint unsigned COMMENT 'foreign key to domain id',
  `broadcast_domain_range` varchar(32) NOT NULL DEFAULT 'POD' COMMENT 'range of broadcast domain : POD/ZONE', 
  `state` varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'what state is this configuration in',
  `created` datetime COMMENT 'date created',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_physical_network__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_physical_network__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`),
  CONSTRAINT `uc_physical_networks__uuid` UNIQUE (`uuid`),
  INDEX `i_physical_network__removed`(`removed`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`physical_network_tags` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network',
  `tag` varchar(255) NOT NULL COMMENT 'tag',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_physical_network_tags__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE,
  UNIQUE KEY(`physical_network_id`, `tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`physical_network_isolation_methods` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network',
  `isolation_method` varchar(255) NOT NULL COMMENT 'isolation method(VLAN, L3 or GRE)',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_physical_network_imethods__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE,
  UNIQUE KEY(`physical_network_id`, `isolation_method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`physical_network_traffic_types` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network',
  `traffic_type` varchar(32) NOT NULL COMMENT 'type of traffic going through this network',
  `xen_network_label` varchar(255) COMMENT 'The network name label of the physical device dedicated to this traffic on a XenServer host',
  `kvm_network_label` varchar(255) DEFAULT 'cloudbr0' COMMENT 'The network name label of the physical device dedicated to this traffic on a KVM host',
  `vmware_network_label` varchar(255) DEFAULT 'vSwitch0' COMMENT 'The network name label of the physical device dedicated to this traffic on a VMware host',
  `simulator_network_label` varchar(255) COMMENT 'The name labels needed for identifying the simulator',
  `ovm_network_label` varchar(255) COMMENT 'The network name label of the physical device dedicated to this traffic on a Ovm host',
  `vlan` varchar(255) COMMENT 'The vlan tag to be sent down to a VMware host',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_physical_network_traffic_types__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_traffic_types__uuid` UNIQUE (`uuid`),
  UNIQUE KEY(`physical_network_id`, `traffic_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`physical_network_service_providers` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(40),
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name',
  `state` varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'provider state',
  `destination_physical_network_id` bigint unsigned COMMENT 'id of the physical network to bridge to',
  `vpn_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is VPN service provided',
  `dhcp_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is DHCP service provided',
  `dns_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is DNS service provided',
  `gateway_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is Gateway service provided',
  `firewall_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is Firewall service provided',
  `source_nat_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is Source NAT service provided',
  `load_balance_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is LB service provided',
  `static_nat_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is Static NAT service provided',
  `port_forwarding_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is Port Forwarding service provided',
  `user_data_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is UserData service provided',
  `security_group_service_provided` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is SG service provided',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_pnetwork_service_providers__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_service_providers__uuid` UNIQUE (`uuid`),
  UNIQUE KEY(`physical_network_id`, `provider_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_load_balancer_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which load balancer device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this load balancer device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the load balancer device',
  `capacity` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'Capacity of the load balancer device',
  `device_state` varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'state (enabled/disabled/shutdown) of the device',
  `allocation_state` varchar(32) NOT NULL DEFAULT 'Free' COMMENT 'Allocation state (Free/Shared/Dedicated/Provider) of the device',
  `is_dedicated` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if device/appliance is provisioned for dedicated use only',
  `is_inline` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if load balancer will be used in in-line configuration with firewall',
  `is_managed` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if load balancer appliance is provisioned and its life cycle is managed by by cloudstack',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external load balancer device',
  `parent_host_id` bigint unsigned COMMENT 'if the load balancer appliance is cloudstack managed, then host id on which this appliance is provisioned',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_lb_devices_host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_lb_devices_parent_host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_lb_devices_physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`external_firewall_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which firewall device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this firewall device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the firewall device',
  `device_state` varchar(32) NOT NULL DEFAULT 'Disabled' COMMENT 'state (enabled/disabled/shutdown) of the device',
  `is_dedicated` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if device/appliance meant for dedicated use only',
  `allocation_state` varchar(32) NOT NULL DEFAULT 'Free' COMMENT 'Allocation state (Free/Allocated) of the device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external firewall device',
  `capacity` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'Capacity of the external firewall device',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_external_firewall_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_firewall_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_external_lb_device_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `network_id` bigint unsigned NOT NULL COMMENT ' guest network id',
  `external_load_balancer_device_id` bigint unsigned NOT NULL COMMENT 'id of external load balancer device assigned for this network',
  `created` datetime COMMENT 'Date from when network started using the device',
  `removed` datetime COMMENT 'Date till the network stopped using the device ',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_external_lb_devices_network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_network_external_lb_devices_device_id` FOREIGN KEY (`external_load_balancer_device_id`) REFERENCES `external_load_balancer_devices`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_external_firewall_device_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `network_id` bigint unsigned NOT NULL COMMENT ' guest network id',
  `external_firewall_device_id` bigint unsigned NOT NULL COMMENT 'id of external firewall device assigned for this device',
  `created` datetime COMMENT 'Date from when network started using the device',
  `removed` datetime COMMENT 'Date till the network stopped using the device ',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_external_firewall_devices_network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_network_external_firewall_devices_device_id` FOREIGN KEY (`external_firewall_device_id`) REFERENCES `external_firewall_devices`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`virtual_router_providers` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `nsp_id` bigint unsigned NOT NULL COMMENT 'Network Service Provider ID',
  `uuid` varchar(40),
  `type` varchar(255) NOT NULL COMMENT 'Virtual router, or ElbVM',
  `enabled` int(1) NOT NULL COMMENT 'Enabled or disabled',
  `removed` datetime COMMENT 'date removed if not null',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_virtual_router_providers__nsp_id` FOREIGN KEY (`nsp_id`) REFERENCES `physical_network_service_providers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `uc_virtual_router_providers__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`domain_router` ADD COLUMN `element_id` bigint unsigned NOT NULL COMMENT 'correlated virtual router provider ID' AFTER id;

INSERT INTO `cloud`.`sequence` (name, value) VALUES ('physical_networks_seq', 200);
ALTER TABLE `cloud`.`networks` ADD COLUMN `physical_network_id` bigint unsigned COMMENT 'physical network id that this configuration is based on' AFTER network_offering_id;
ALTER TABLE `cloud`.`vlan` ADD COLUMN `physical_network_id` bigint unsigned NOT NULL COMMENT 'physical network id that this configuration is based on';
ALTER TABLE `cloud`.`op_dc_vnet_alloc` ADD COLUMN `physical_network_id` bigint unsigned NOT NULL COMMENT 'physical network the vnet belongs to';
ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `physical_network_id` bigint unsigned NOT NULL COMMENT 'physical network id that this configuration is based on';

ALTER TABLE `cloud`.`networks` ADD COLUMN `restart_required` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if restart is required for the network';
DELETE FROM `cloud`.`configuration` where name='cmd.wait';

UPDATE `cloud`.`configuration` set value='true' where name='firewall.rule.ui.enabled';
CREATE TABLE  `cloud`.`op_user_stats_log` (
  `user_stats_id` bigint unsigned NOT NULL,
  `net_bytes_received` bigint unsigned NOT NULL default '0',
  `net_bytes_sent` bigint unsigned NOT NULL default '0',
  `current_bytes_received` bigint unsigned NOT NULL default '0',
  `current_bytes_sent` bigint unsigned NOT NULL default '0',
  `agg_bytes_received` bigint unsigned NOT NULL default '0',
  `agg_bytes_sent` bigint unsigned NOT NULL default '0',
  `updated` datetime COMMENT 'stats update timestamp',
  UNIQUE KEY (`user_stats_id`, `updated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


--;
-- Network offerings
--;

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `sort_key` int(32) NOT NULL default 0 COMMENT 'sort key used for customising sort method';
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `dedicated_lb_service` int(1) unsigned NOT NULL DEFAULT 1 COMMENT 'true if the network offering provides a dedicated load balancer for each network';
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `redundant_router_service` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if the network offering provides the redundant router service';
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `conserve_mode` int(1) unsigned NOT NULL DEFAULT 1 COMMENT 'Is this network offering is IP conserve mode enabled';

ALTER TABLE `cloud`.`network_offerings` MODIFY `name` varchar(64) COMMENT 'name of the network offering';
ALTER TABLE `cloud`.`network_offerings` MODIFY `unique_name` varchar(64) COMMENT 'unique name of the network offering';
ALTER TABLE `cloud`.`network_offerings` MODIFY `service_offering_id` bigint unsigned COMMENT 'service offering id that virtual router is tied to';
ALTER TABLE `cloud`.`network_offerings` DROP KEY `service_offering_id`;

ALTER TABLE `cloud`.`network_offerings` DROP `concurrent_connections`;

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `state` char(32) COMMENT 'state of the network offering that has Disabled value by default';
UPDATE `cloud`.`network_offerings` SET `state`='Enabled';
UPDATE `cloud`.`network_offerings` SET `state`='Disabled' where `availability`='Unavailable';
UPDATE `cloud`.`network_offerings` SET `availability`='Optional' where `availability`='Unavailable';
UPDATE `cloud`.`network_offerings` SET `system_only`=0 where `traffic_type`='Guest';
UPDATE `cloud`.`network_offerings` SET `specify_vlan`=1 where `unique_name`='System-Guest-Network';

UPDATE `cloud`.`network_offerings` SET `unique_name`='DefaultSharedNetworkOfferingWithSGService' where `unique_name`='System-Guest-Network';
UPDATE `cloud`.`network_offerings` SET `unique_name`='DefaultIsolatedNetworkOfferingWithSourceNatService' where `unique_name`='DefaultVirtualizedNetworkOffering';
UPDATE `cloud`.`network_offerings` SET `unique_name`='DefaultSharedNetworkOffering' where `unique_name`='DefaultDirectNetworkOffering';

UPDATE `cloud`.`network_offerings` SET `name`='DefaultSharedNetworkOfferingWithSGService' where `name`='System-Guest-Network';
UPDATE `cloud`.`network_offerings` SET `name`='DefaultIsolatedNetworkOfferingWithSourceNatService' where `name`='DefaultVirtualizedNetworkOffering';
UPDATE `cloud`.`network_offerings` SET `name`='DefaultSharedNetworkOffering' where `name`='DefaultDirectNetworkOffering';

UPDATE `cloud`.`network_offerings` SET `guest_type`='Shared' where `guest_type`='Direct';
UPDATE `cloud`.`network_offerings` SET `guest_type`='Isolated' where `guest_type`='Virtual';
UPDATE `cloud`.`network_offerings` SET `availability`='Optional' where `availability`='Required' and `guest_type`='Shared';

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `elastic_ip_service` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'true if the network offering provides elastic ip service';
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `elastic_lb_service` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'true if the network offering provides elastic lb service';
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `specify_ip_ranges` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'true if the network offering provides an ability to define ip ranges';
ALTER TABLE `cloud`.`networks` ADD COLUMN `specify_ip_ranges` int(1) unsigned NOT NULL DEFAULT '0' COMMENT 'true if the network provides an ability to define ip ranges';
UPDATE `cloud`.`networks` SET specify_ip_ranges=(SELECT specify_ip_ranges from `cloud`.`network_offerings` no where no.id=network_offering_id);


insert into `cloud`.`network_offerings` (`name`, `unique_name`, `display_text`, `traffic_type`, `system_only`, `specify_vlan`, `default`, `availability`, `state`, `guest_type`, `created`, `userdata_service`, `dns_service`, `dhcp_service`) values ('DefaultIsolatedNetworkOffering', 'DefaultIsolatedNetworkOffering', 'Offering for Isolated networks with no Source Nat service', 'Guest', 0, 1, 1, 'Optional', 'Enabled', 'Isolated', now(), 1, 1, 1);


UPDATE `cloud`.`network_offerings` set specify_ip_ranges=1 where name in ('System-Public-Network', 'System-Storage-Network', 'DefaultSharedNetworkOfferingWithSGService', 'DefaultSharedNetworkOffering', 'DefaultIsolatedNetworkOffering');


CREATE TABLE  `cloud`.`ntwk_offering_service_map` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_offering_id` bigint unsigned NOT NULL COMMENT 'network_offering_id',
  `service` varchar(255) NOT NULL COMMENT 'service',
  `provider` varchar(255) COMMENT 'service provider',
  `created` datetime COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_ntwk_offering_service_map__network_offering_id` FOREIGN KEY(`network_offering_id`) REFERENCES `cloud`.`network_offerings`(`id`) ON DELETE CASCADE,
  UNIQUE (`network_offering_id`, `service`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`dc_storage_network_ip_range` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `uuid` varchar(40),
  `start_ip` char(40) NOT NULL COMMENT 'start ip address',
  `end_ip` char(40) NOT NULL COMMENT 'end ip address',
  `vlan` int unsigned DEFAULT NULL COMMENT 'vlan the storage network on',
  `netmask` varchar(15) NOT NULL COMMENT 'netmask for storage network',
  `data_center_id` bigint unsigned NOT NULL,
  `pod_id` bigint unsigned NOT NULL COMMENT 'pod it belongs to',
  `network_id` bigint unsigned NOT NULL COMMENT 'id of corresponding network offering',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_storage_ip_range__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`),
  CONSTRAINT `fk_storage_ip_range__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`),
  CONSTRAINT `fk_storage_ip_range__pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref`(`id`),
  CONSTRAINT `uc_storage_ip_range__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`op_dc_storage_network_ip_address` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  `range_id` bigint unsigned NOT NULL COMMENT 'id of ip range in dc_storage_network_ip_range',
  `ip_address` char(40) NOT NULL COMMENT 'ip address',
  `mac_address` bigint unsigned NOT NULL COMMENT 'mac address for storage ips',
  `taken` datetime COMMENT 'Date taken',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_storage_ip_address__range_id` FOREIGN KEY (`range_id`) REFERENCES `dc_storage_network_ip_range`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE  `cloud`.`event` ADD COLUMN `domain_id` bigint unsigned NOT NULL;
ALTER TABLE  `cloud`.`op_host_capacity` ADD COLUMN `capacity_state` varchar(32) NOT NULL DEFAULT 'Enabled';
UPDATE `cloud`.`event` set account_id=1, user_id=1 where account_id=0 and user_id=0;
UPDATE `cloud`.`event` e set e.domain_id = (select acc.domain_id from `cloud`.`account` acc where acc.id = e.account_id) where e.domain_id = 0;

update `cloud`.`vm_template` set removed=now() where id=2;

INSERT INTO `cloud`.`vm_template` (id, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text,  format, guest_os_id, featured, cross_zones, hypervisor_type, extractable)
    VALUES (5, 'centos56-x86_64-xen', 'CentOS 5.6(64-bit) no GUI (XenServer)', 1, now(), 'BUILTIN', 0, 64, 1, 'http://download.cloud.com/templates/builtin/centos56-x86_64.vhd.bz2', '905cec879afd9c9d22ecc8036131a180', 0, 'CentOS 5.6(64-bit) no GUI (XenServer)', 'VHD', 12, 1, 1, 'XenServer', 1);

DELETE from `cloud`.`configuration` where name='firewall.rule.ui.enabled';

DELETE FROM `cloud`.`resource_limit` WHERE domain_id = 1 AND account_id IS NULL;

ALTER TABLE `cloud`.`networks` ADD COLUMN `acl_type` varchar(15) COMMENT 'ACL access type. Null for system networks, can be Account/Domain for Guest networks';
UPDATE `cloud`.`networks` SET acl_type='Domain' where guest_type is not null and shared=1;
UPDATE `cloud`.`networks` SET acl_type='Account' where guest_type='Virtual';
UPDATE `cloud`.`networks` SET acl_type='Account' where guest_type='Direct' and shared=0;
ALTER TABLE `cloud`.`domain_network_ref` ADD COLUMN `subdomain_access` int(1) unsigned COMMENT '1 if network can be accessible from the subdomain';
UPDATE `cloud`.`networks` SET specify_ip_ranges=(SELECT specify_ip_ranges FROM `cloud`.`network_offerings` no where no.id=network_offering_id);


DELETE FROM `cloud`.`configuration` WHERE name='network.redundantrouter';

UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'xen.public.network.device';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'xen.storage.network.device1';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'xen.storage.network.device2';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'xen.private.network.device';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'xen.guest.network.device';

UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'vmware.private.vswitch';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'vmware.public.vswitch';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'vmware.guest.vswitch';

UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'ovm.public.network.device';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'ovm.private.network.device';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'ovm.guest.network.device';

UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'kvm.public.network.device';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'kvm.private.network.device';
UPDATE `cloud`.`configuration` SET category = 'Hidden' WHERE name = 'kvm.guest.network.device';

ALTER TABLE `cloud`.`dc_storage_network_ip_range` ADD COLUMN `gateway` varchar(15) NOT NULL COMMENT 'gateway ip address';

ALTER TABLE `cloud`.`volumes` ADD COLUMN `last_pool_id` bigint unsigned;
UPDATE `cloud`.`volumes` SET `last_pool_id` = `pool_id`;
UPDATE `cloud`.`volumes` SET `path` = SUBSTRING_INDEX(`path`, '/', -1);
UPDATE `cloud`.`template_spool_ref` SET `local_path` = SUBSTRING_INDEX(`local_path`, '/', -1);

ALTER TABLE `cloud`.`user_ip_address` ADD COLUMN `is_system` int(1) unsigned NOT NULL default '0';
ALTER TABLE `cloud`.`volumes` ADD COLUMN `update_count` bigint unsigned NOT NULL DEFAULT 0;
ALTER TABLE `cloud`.`volumes` ADD INDEX `i_volumes__update_count`(`update_count`);
ALTER TABLE `cloud`.`firewall_rules` ADD COLUMN `type` varchar(10) NOT NULL DEFAULT 'USER';

CREATE TABLE `cloud`.`account_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `account_id` bigint unsigned NOT NULL COMMENT 'account id',
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_account_details__account_id` FOREIGN KEY (`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud_usage`.`usage_security_group`;

CREATE TABLE  `cloud_usage`.`usage_security_group` (
  `zone_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `vm_instance_id` bigint unsigned NOT NULL,
  `security_group_id` bigint unsigned NOT NULL,
  `created` DATETIME NOT NULL,
  `deleted` DATETIME NULL  
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud_usage`.`usage_security_group` ADD INDEX `i_usage_security_group__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_security_group` ADD INDEX `i_usage_security_group__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_security_group` ADD INDEX `i_usage_security_group__deleted`(`deleted`);

UPDATE `cloud`.`configuration` SET category = 'Usage' where name in ('usage.execution.timezone', 'usage.stats.job.aggregation.range', 'usage.stats.job.exec.time', 'enable.usage.server', 'direct.network.stats.interval', 'usage.sanity.check.interval', 'usage.aggregation.timezone');

ALTER TABLE  `cloud`.`op_dc_vnet_alloc` ADD CONSTRAINT `fk_op_dc_vnet_alloc__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`domain` ADD COLUMN `type` varchar(255) NOT NULL DEFAULT 'Normal' COMMENT 'type of the domain - can be Normal or Project';

UPDATE `cloud`.`configuration` SET name='vm.destroy.forcestop' where name='vm.destory.forcestop';
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.destroy.forcestop', 'false', 'On destroy, force-stop takes this value');
DELETE FROM `cloud`.`configuration` where name='skip.steps';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'external.lb.default.capacity', '50', 'default number of networks permitted per external load balancer device');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'external.firewall.default.capacity', '50', 'default number of networks permitted per external load firewall device');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'resourcecount.check.interval', '0', 'Time (in seconds) to wait before retrying resource count check task. Default is 0 which is to never run the task');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'secstorage.proxy', null, 'http proxy used by ssvm, in http://username:password@proxyserver:port format');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'secstorage.vm.mtu.size', '1500', 'MTU size (in Byte) of storage network in secondary storage vms');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'sortkey.algorithm', 'false', 'Sort algorithm for those who use sort key(template, disk offering, service offering, network offering), true means ascending sort while false means descending sort');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'system.vm.default.hypervisor', null, 'Hypervisor type used to create system vm');

UPDATE `cloud`.`network_offerings` SET display_text='Offering for Shared Security group enabled networks' where display_text='System-Guest-Network';

UPDATE `cloud`.`configuration` SET category = 'Secure' where name in ('alert.smtp.password', 'network.loadbalancer.haproxy.stats.auth', 'security.singlesignon.key', 'project.smtp.password');

UPDATE `cloud`.`guest_os` set id = 158 where id = 141;
UPDATE `cloud`.`guest_os` set id = 159 where id = 142;
UPDATE `cloud`.`guest_os` set id = 160 where id = 143;
UPDATE `cloud`.`vm_template` set guest_os_id = 158 where guest_os_id = 141;
UPDATE `cloud`.`vm_template` set guest_os_id = 159 where guest_os_id = 142;
UPDATE `cloud`.`vm_template` set guest_os_id = 160 where guest_os_id = 143;
UPDATE `cloud`.`vm_instance` set guest_os_id = 158 where guest_os_id = 141;
UPDATE `cloud`.`vm_instance` set guest_os_id = 159 where guest_os_id = 142;
UPDATE `cloud`.`vm_instance` set guest_os_id = 160 where guest_os_id = 143;
UPDATE `cloud`.`guest_os_hypervisor` set guest_os_id = 158 where guest_os_id = 141;
UPDATE `cloud`.`guest_os_hypervisor` set guest_os_id = 159 where guest_os_id = 142;
UPDATE `cloud`.`guest_os_hypervisor` set guest_os_id = 160 where guest_os_id = 143;



INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (143, 1, 'CentOS 6.0 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (144, 1, 'CentOS 6.0 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (145, 3, 'Oracle Enterprise Linux 5.6 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (146, 3, 'Oracle Enterprise Linux 5.6 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (147, 3, 'Oracle Enterprise Linux 6.0 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (148, 3, 'Oracle Enterprise Linux 6.0 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (149, 4, 'Red Hat Enterprise Linux 5.6 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (150, 4, 'Red Hat Enterprise Linux 5.6 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (151, 5, 'SUSE Linux Enterprise Server 10 SP3 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (152, 5, 'SUSE Linux Enterprise Server 10 SP4 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (153, 5, 'SUSE Linux Enterprise Server 10 SP4 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (154, 5, 'SUSE Linux Enterprise Server 11 SP1 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (155, 5, 'SUSE Linux Enterprise Server 11 SP1 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (156, 10, 'Ubuntu 10.10 (32-bit)');
INSERT IGNORE INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (157, 10, 'Ubuntu 10.10 (64-bit)');

UPDATE `cloud`.`guest_os` SET category_id=4 where id=131;

UPDATE `cloud`.`networks` n  SET n.name=(CONCAT('guestNetworkForBasicZone_', (SELECT name from `cloud`.`data_center` d where d.id=n.data_center_id AND d.networktype='Basic'))) where n.name is null and n.traffic_type='Guest';
UPDATE `cloud`.`networks` n  SET n.display_text=(CONCAT('guestNetworkForBasicZone_', (SELECT name from `cloud`.`data_center` d where d.id=n.data_center_id AND d.networktype='Basic'))) where n.display_text is null and n.traffic_type='Guest';

UPDATE `cloud`.`configuration` SET description='Bypass internal dns, use exetrnal dns1 and dns2' WHERE name='use.external.dns';
UPDATE `cloud`.`configuration` SET category='Alert' WHERE name='capacity.check.period';
UPDATE `cloud`.`vm_instance` SET vnc_password = '' where removed is not null;
DELETE FROM `cloud`.`host_details` where name in ('storage.network.device1', 'storage.network.device2');
UPDATE `cloud`.`configuration` SET category = 'Hidden' where name in ('secondary.storage.vm', 'xen.create.pools.in.pod', 'cloud.identifier', 'security.hash.key', 'router.ram.size');
DELETE FROM `cloud`.`host_details` where name in ('storage.network.device1', 'storage.network.device2');
