--;
-- Schema upgrade from 2.2.14 to 3.0;
--;

ALTER TABLE `cloud`.`template_host_ref` DROP COLUMN `pool_id`;

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
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('XenServer', '6.0 beta', 50, 1);
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
  CONSTRAINT `fk_project_invitations__project_id` FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'max.project.user.vms', '20', 'The default maximum number of user VMs that can be deployed for a project');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'max.project.public.ips', '20', 'The default maximum number of public IPs that can be consumed by a project');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'max.project.templates', '20', 'The default maximum number of templates that can be deployed for a project');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'max.project.snapshots', '20', 'The default maximum number of snapshots that can be created for a project');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'max.project.volumes', '20', 'The default maximum number of volumes that can be created for a project');

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.invite.required', 'false', 'If invitation confirmation is required when add account to project. Default value is false');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.invite.timeout', '86400', 'Invitation expiration time (in seconds). Default is 1 day - 86400 seconds');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'allow.user.create.projects', 'true', 'Invitation expiration time (in seconds). Default is 1 day - 86400 seconds');

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.email.sender', null, 'Sender of project invitation email (will be in the From header of the email).');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.host', null, 'SMTP hostname used for sending out email project invitations');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.password', null, 'Password for SMTP authentication (applies only if project.smtp.useAuth is true)');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.port', '465', 'Port the SMTP server is listening on');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.useAuth', null, 'If true, use SMTP authentication when sending emails');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.username', null, 'If regular user can create a project; true by default');

INSERT IGNORE INTO configuration VALUES ('Alert', 'DEFAULT', 'management-server', 'cluster.memory.allocated.capacity.disablethreshold' , .85, 'Percentage (as a value between 0 and 1) of memory utilization above which allocators will disable using the cluster for low memory available. Keep the corresponding notification threshold lower than this to be notified beforehand.');
INSERT IGNORE INTO configuration VALUES ('Alert', 'DEFAULT', 'management-server', 'cluster.cpu.allocated.capacity.disablethreshold' , .85, 'Percentage (as a value between 0 and 1) of cpu utilization above which allocators will disable using the cluster for low cpu available. Keep the corresponding notification threshold lower than this to be notified beforehand.');
INSERT IGNORE INTO configuration VALUES ('Alert', 'DEFAULT', 'management-server', 'pool.storage.allocated.capacity.disablethreshold' , .85, 'Percentage (as a value between 0 and 1) of allocated storage utilization above which allocators will disable using the cluster for low allocated storage available. Keep the corresponding notification threshold lower than this to be notified beforehand.');
INSERT IGNORE INTO configuration VALUES ('Alert', 'DEFAULT', 'management-server', 'pool.storage.capacity.disablethreshold' , .85, 'Percentage (as a value between 0 and 1) of allocated storage utilization above which allocators will disable using the cluster for low allocated storage available. Keep the corresponding notification threshold lower than this to be notified beforehand.');
INSERT IGNORE INTO configuration VALUES ('Alert', 'DEFAULT', 'management-server', 'zone.vlan.capacity.notificationthreshold' , .75, 'Percentage (as a value between 0 and 1) of Zone Vlan utilization above which alerts will be sent about low number of Zone Vlans.');
INSERT IGNORE INTO configuration VALUES ('Alert', 'DEFAULT', 'management-server', 'cluster.localStorage.capacity.notificationthreshold' , .75, 'Percentage (as a value between 0 and 1) of Direct Network Public Ip Utilization above which alerts will be sent about low number of direct network public ips.');
INSERT IGNORE INTO configuration VALUES ('Alert', 'DEFAULT', 'management-server', 'zone.directnetwork.publicip.capacity.notificationthreshold' , .75, 'Percentage (as a value between 0 and 1) of Direct Network Public Ip Utilization above which alerts will be sent about low number of direct network public ips.');
INSERT IGNORE INTO configuration VALUES ('Alert', 'DEFAULT', 'management-server', 'zone.secstorage.capacity.notificationthreshold' , .75, 'Percentage (as a value between 0 and 1) of secondary storage utilization above which alerts will be sent about low storage available.');

update configuration set name = 'cluster.storage.allocated.capacity.notificationthreshold' , category = 'Alert' where name = 'storage.allocated.capacity.threshold' ;
update configuration set name = 'cluster.storage.capacity.notificationthreshold' , category = 'Alert' where name = 'storage.capacity.threshold' ;
update configuration set name = 'cluster.cpu.capacity.notificationthreshold' , category = 'Alert' where name = 'cpu.capacity.threshold' ;
update configuration set name = 'cluster.memory.capacity.notificationthreshold' , category = 'Alert' where name = 'memory.capacity.threshold' ;
update configuration set name = 'zone.virtualnetwork.publicip.capacity.notificationthreshold' , category = 'Alert' where name = 'public.ip.capacity.threshold' ;
update configuration set name = 'pod.privateip.capacity.notificationthreshold' , category = 'Alert' where name = 'private.ip.capacity.threshold' ;

ALTER TABLE `cloud`.`domain_router` ADD COLUMN `template_version` varchar(100) COMMENT 'template version' AFTER role;
ALTER TABLE `cloud`.`domain_router` ADD COLUMN `scripts_version` varchar(100) COMMENT 'scripts version' AFTER template_version;
ALTER TABLE `cloud`.`alert` ADD `cluster_id` bigint unsigned;

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `uuid` varchar(255); 
DELETE from `cloud`.`op_host_capacity` where capacity_type in (2,4,6);

ALTER TABLE `cloud`.`user_statistics` ADD COLUMN `agg_bytes_received` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud`.`user_statistics` ADD COLUMN `agg_bytes_sent` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `uuid` varchar(255); 
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `uc_vm_instance_uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`async_job` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`async_job` ADD CONSTRAINT `uc_async__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`domain` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`domain` ADD CONSTRAINT `uc_domain__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`account` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`account` ADD CONSTRAINT `uc_account__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`user` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`user` ADD CONSTRAINT `uc_user__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`projects` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`projects` ADD CONSTRAINT `uc_projects__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`project_invitations` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`project_invitations` ADD CONSTRAINT `uc_project_invitations__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`data_center` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`data_center` ADD CONSTRAINT `uc_data_center__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`host` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`host` ADD CONSTRAINT `uc_host__uuid` UNIQUE (`uuid`);

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

ALTER TABLE `cloud`.`security_ingress_rule` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`security_ingress_rule` ADD CONSTRAINT `uc_security_ingress_rule__uuid` UNIQUE (`uuid`);

ALTER TABLE `cloud`.`security_egress_rule` ADD COLUMN `uuid` varchar(40); 
ALTER TABLE `cloud`.`security_egress_rule` ADD CONSTRAINT `uc_security_egress_rule__uuid` UNIQUE (`uuid`);

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

CREATE TABLE `cloud`.`vm_template_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `template_id` bigint unsigned NOT NULL COMMENT 'template id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vm_template_details__template_id` FOREIGN KEY `fk_vm_template_details__template_id`(`template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`op_host_capacity` ADD COLUMN `created` datetime;
ALTER TABLE `cloud`.`op_host_capacity` ADD COLUMN `update_time` datetime;

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'apply.allocation.algorithm.to.pods', 'false', 'If true, deployment planner applies the allocation heuristics at pods first in the given datacenter during VM resource allocation');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.user.dispersion.weight', 1, 'Weight for user dispersion heuristic (as a value between 0 and 1) applied to resource allocation during vm deployment. Weight for capacity heuristic will be (1 - weight of user dispersion)');
DELETE FROM configuration WHERE name='use.user.concentrated.pod.allocation';
UPDATE configuration SET description = '[''random'', ''firstfit'', ''userdispersing'', ''userconcentratedpod''] : Order in which hosts within a cluster will be considered for VM/volume allocation.' WHERE name = 'vm.allocation.algorithm';



--;
-- Usage db upgrade from 2.2.13 to 3.0;
--;

ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `agg_bytes_received` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud_usage`.`user_statistics` ADD COLUMN `agg_bytes_sent` bigint unsigned NOT NULL default '0';

ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `agg_bytes_received` bigint unsigned NOT NULL default '0';
ALTER TABLE `cloud_usage`.`usage_network` ADD COLUMN `agg_bytes_sent` bigint unsigned NOT NULL default '0';

update `cloud_usage`.`usage_network` set agg_bytes_received = net_bytes_received + current_bytes_received, agg_bytes_sent = net_bytes_sent + current_bytes_sent;

ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `net_bytes_received`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `net_bytes_sent`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `current_bytes_received`;
ALTER TABLE `cloud_usage`.`usage_network` DROP COLUMN `current_bytes_sent`;

CREATE TABLE  `cloud_usage`.`usage_vpn_user` (
  `zone_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `user_name` varchar(32),
  `created` DATETIME NOT NULL,
  `deleted` DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM configuration WHERE name='host.capacity.checker.wait';
DELETE FROM configuration WHERE name='host.capacity.checker.interval'; 
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'disable.extraction' , 'false', 'Flag for disabling extraction of template, isos and volumes');
ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_vpn_user` ADD INDEX `i_usage_vpn_user__deleted`(`deleted`);
