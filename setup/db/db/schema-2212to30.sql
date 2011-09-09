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
