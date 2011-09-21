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
  `project_domain_id` bigint unsigned NOT NULL,
  `domain_id` bigint unsigned NOT NULL,
  `created` datetime COMMENT 'date created',
  `removed` datetime COMMENT 'date removed',\
  `state` varchar(255) NOT NULL COMMENT 'state of the project (Active/Inactive/Suspended)',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_projects__project_account_id` FOREIGN KEY(`project_account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_projects__project_domain_id` FOREIGN KEY(`project_domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_projects__domain_id` FOREIGN KEY(`domain_id`) REFERENCES `domain`(`id`) ON DELETE CASCADE,
  INDEX `i_projects__removed`(`removed`) 
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE  `cloud`.`project_account` (
  `id` bigint unsigned NOT NULL auto_increment,
  `account_id` bigint unsigned NOT NULL COMMENT'account id',
  `account_role` varchar(255) COMMENT 'Account role in the project (Owner or Regular)',
  `project_id` bigint unsigned NOT NULL COMMENT 'project id',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_project_account__account_id` FOREIGN KEY(`account_id`) REFERENCES `account`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_account__project_id` FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE,
  UNIQUE (`account_id`, `project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE domain ADD COLUMN `type` varchar(255) NOT NULL DEFAULT 'Normal' COMMENT 'type of the domain - can be Normal or Project';
