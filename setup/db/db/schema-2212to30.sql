--;
-- Schema upgrade from 2.2.12 to 3.0;
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

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.email.sender', null, 'Sender of project invitation email (will be in the From header of the email).');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.host', null, 'SMTP hostname used for sending out email project invitations');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.password', null, 'Password for SMTP authentication (applies only if project.smtp.useAuth is true)');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.port', '465', 'Port the SMTP server is listening on');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.useAuth', null, 'If true, use SMTP authentication when sending emails');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'project.smtp.username', null, 'Username for SMTP authentication (applies only if project.smtp.useAuth is true)');


ALTER TABLE `cloud`.`domain_router` ADD COLUMN `template_version` varchar(100) COMMENT 'template version' AFTER role;
ALTER TABLE `cloud`.`domain_router` ADD COLUMN `scripts_version` varchar(100) COMMENT 'scripts version' AFTER template_version;

