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
-- Schema upgrade from 4.2.1 to 4.3.0;
--;

-- Disable foreign key checking
SET foreign_key_checks = 0;

ALTER TABLE `cloud`.`async_job` ADD COLUMN `related` CHAR(40) NOT NULL;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `session_key`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `job_cmd_originator`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `callback_type`;
ALTER TABLE `cloud`.`async_job` DROP COLUMN `callback_address`;

ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_type` VARCHAR(32);
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_dispatcher` VARCHAR(64);
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_executing_msid` bigint;
ALTER TABLE `cloud`.`async_job` ADD COLUMN `job_pending_signals` int(10) NOT NULL DEFAULT 0;

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `keep_alive_enabled` int(1) unsigned NOT NULL DEFAULT 1 COMMENT 'true if connection should be reset after requests.';

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state` VARCHAR(74) DEFAULT 'PowerUnknown';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state_update_time` DATETIME;
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_state_update_count` INT DEFAULT 0;
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `power_host` bigint unsigned;
ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__power_host` FOREIGN KEY (`power_host`) REFERENCES `cloud`.`host`(`id`);

ALTER TABLE `cloud`.`load_balancing_rules` ADD COLUMN `lb_protocol` VARCHAR(40);

DROP TABLE IF EXISTS `cloud`.`vm_snapshot_details`;
CREATE TABLE `cloud`.`vm_snapshot_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT,
  `vm_snapshot_id` bigint unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`snapshot_details`;
CREATE TABLE `cloud`.`snapshot_details` (
  `id` bigint unsigned UNIQUE NOT NULL AUTO_INCREMENT,
  `snapshot_id` bigint unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`vm_work_job` (
  `id` bigint unsigned UNIQUE NOT NULL,
  `step` char(32) NOT NULL COMMENT 'state',
  `vm_type` char(32) NOT NULL COMMENT 'type of vm',
  `vm_instance_id` bigint unsigned NOT NULL COMMENT 'vm instance',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vm_work_job__instance_id` FOREIGN KEY (`vm_instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE,
  INDEX `i_vm_work_job__vm`(`vm_type`, `vm_instance_id`),
  INDEX `i_vm_work_job__step`(`step`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`async_job_journal` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `job_id` bigint unsigned NOT NULL,
  `journal_type` varchar(32),
  `journal_text` varchar(1024) COMMENT 'journal descriptive informaton',
  `journal_obj` varchar(1024) COMMENT 'journal strutural information, JSON encoded object',
  `created` datetime NOT NULL COMMENT 'date created',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_async_job_journal__job_id` FOREIGN KEY (`job_id`) REFERENCES `async_job`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`async_job_join_map` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `job_id` bigint unsigned NOT NULL,
  `join_job_id` bigint unsigned NOT NULL,
  `join_status` int NOT NULL,
  `join_result` varchar(1024),
  `join_msid` bigint,
  `complete_msid` bigint,
  `sync_source_id` bigint COMMENT 'upper-level job sync source info before join',
  `wakeup_handler` varchar(64),
  `wakeup_dispatcher` varchar(64),
  `wakeup_interval` bigint NOT NULL DEFAULT 3000 COMMENT 'wakeup interval in seconds',
  `created` datetime NOT NULL,
  `last_updated` datetime,
  `next_wakeup` datetime,
  `expiration` datetime,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_async_job_join_map__job_id` FOREIGN KEY (`job_id`) REFERENCES `async_job`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_async_job_join_map__join_job_id` FOREIGN KEY (`join_job_id`) REFERENCES `async_job`(`id`),
  CONSTRAINT `fk_async_job_join_map__join` UNIQUE (`job_id`, `join_job_id`),
  INDEX `i_async_job_join_map__join_job_id`(`join_job_id`),
  INDEX `i_async_job_join_map__created`(`created`),
  INDEX `i_async_job_join_map__last_updated`(`last_updated`),
  INDEX `i_async_job_join_map__next_wakeup`(`next_wakeup`),
  INDEX `i_async_job_join_map__expiration`(`expiration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#realhostip changes, before changing table and adding default value
UPDATE `cloud`.`configuration` SET value=CONCAT("*.",value) WHERE `name`="consoleproxy.url.domain" OR `name`="secstorage.ssl.cert.domain";

ALTER TABLE `cloud`.`configuration` ADD COLUMN `default_value` VARCHAR(4095) COMMENT 'Default value for a configuration parameter';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `updated` datetime COMMENT 'Time this was updated by the server. null means this row is obsolete.';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `scope` VARCHAR(255) DEFAULT NULL COMMENT 'Can this parameter be scoped';
ALTER TABLE `cloud`.`configuration` ADD COLUMN `is_dynamic` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Can the parameter be change dynamically without restarting the server';

UPDATE `cloud`.`configuration` SET `default_value` = `value`;

#Upgrade the offerings and template table to have actual remove and states
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `state` CHAR(40) NOT NULL DEFAULT 'Active' COMMENT 'state for disk offering';
ALTER TABLE `cloud`.`disk_offering` ADD COLUMN `hv_ss_reserve` int(32) unsigned DEFAULT NULL COMMENT 'Hypervisor snapshot reserve space as a percent of a volume (for managed storage using Xen or VMware)';

ALTER TABLE `cloud`.`volumes` ADD COLUMN `hv_ss_reserve` int(32) unsigned DEFAULT NULL COMMENT 'Hypervisor snapshot reserve space as a percent of a volume (for managed storage using Xen or VMware)';

UPDATE `cloud`.`disk_offering` SET `state`='Inactive' WHERE `removed` IS NOT NULL;
UPDATE `cloud`.`disk_offering` SET `removed`=NULL;

UPDATE `cloud`.`vm_template` SET `guest_os_id`= "142" WHERE `id` = "5";
UPDATE `cloud`.`vm_template` SET `state`='Inactive' WHERE `removed` IS NOT NULL;
UPDATE `cloud`.`vm_template` SET `state`='Active' WHERE `removed` IS NULL;
UPDATE `cloud`.`vm_template` SET `removed`=NULL;

ALTER TABLE `cloud`.`remote_access_vpn` MODIFY COLUMN `network_id` bigint unsigned;
ALTER TABLE `cloud`.`remote_access_vpn` ADD COLUMN `vpc_id` bigint unsigned default NULL;

ALTER TABLE `cloud`.`s2s_vpn_connection` ADD COLUMN `passive` int(1) unsigned NOT NULL DEFAULT 0;

CREATE TABLE `sslcerts` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `uuid` varchar(40) DEFAULT NULL,
      `account_id` bigint(20) unsigned NOT NULL,
      `domain_id` bigint(20) unsigned NOT NULL,
      `certificate` text NOT NULL,
      `fingerprint` varchar(62) NOT NULL,
      `key` text NOT NULL,
      `chain` text,
      `password` varchar(255) DEFAULT NULL,
      PRIMARY KEY (`id`),
      CONSTRAINT `fk_sslcert__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE,
      CONSTRAINT `fk_sslcert__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`) ON DELETE CASCADE
);

CREATE TABLE `load_balancer_cert_map` (
      `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
      `uuid` varchar(40) DEFAULT NULL,
      `load_balancer_id` bigint(20) unsigned NOT NULL,
      `certificate_id` bigint(20) unsigned NOT NULL,
      `revoke` tinyint(1) NOT NULL DEFAULT '0',
      PRIMARY KEY (`id`),
      CONSTRAINT `fk_load_balancer_cert_map__certificate_id` FOREIGN KEY (`certificate_id`) REFERENCES `sslcerts` (`id`) ON DELETE CASCADE,
      CONSTRAINT `fk_load_balancer_cert_map__load_balancer_id` FOREIGN KEY (`load_balancer_id`) REFERENCES `load_balancing_rules` (`id`) ON DELETE CASCADE);

ALTER TABLE `cloud`.`host` ADD COLUMN `cpu_sockets` int(10) unsigned DEFAULT NULL COMMENT "the number of CPU sockets on the host" AFTER pod_id;
ALTER TABLE `cloud`.`physical_network_traffic_types` ADD COLUMN `hyperv_network_label` varchar(255) DEFAULT NULL COMMENT 'The network name label of the physical device dedicated to this traffic on a HyperV host';

CREATE TABLE `cloud`.`firewall_rule_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `firewall_rule_id` bigint unsigned NOT NULL COMMENT 'Firewall rule id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_firewall_rule_details__firewall_rule_id` FOREIGN KEY `fk_firewall_rule_details__firewall_rule_id`(`firewall_rule_id`) REFERENCES `firewall_rules`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`data_center_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`network_details` CHANGE `display_detail` `display` tinyint(0) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`vm_template_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`volume_details` CHANGE `display_detail` `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`nic_details` CHANGE `display_detail` `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`user_vm_details` CHANGE `display_detail` `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`service_offering_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';
ALTER TABLE `cloud`.`storage_pool_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user';

UPDATE `cloud`.`configuration` SET name='ldap.basedn' WHERE name='ldap.searchbase';
UPDATE `cloud`.`configuration` SET name='ldap.bind.principal' WHERE name='ldap.dn' ;
UPDATE `cloud`.`configuration` SET name='ldap.bind.password'  WHERE name='ldap.passwd';
UPDATE `cloud`.`configuration` SET name='ldap.truststore.password' WHERE name='ldap.truststorepass' ;

INSERT INTO `cloud`.`configuration`(category, instance, component, name, value, description, default_value) VALUES ('Secure', 'DEFAULT', 'management-server', 'ldap.bind.principal', NULL, 'Specifies the bind principal to use for bind to LDAP', NULL) ON DUPLICATE KEY UPDATE category='Secure';
INSERT INTO `cloud`.`configuration`(category, instance, component, name, value, description, default_value) VALUES ('Secure', 'DEFAULT', 'management-server', 'ldap.bind.password', NULL, 'Specifies the password to use for binding to LDAP', NULL) ON DUPLICATE KEY UPDATE category='Secure';
INSERT INTO `cloud`.`configuration`(category, instance, component, name, value, description, default_value) VALUES ('Secure', 'DEFAULT', 'management-server', 'ldap.basedn', NULL, 'Sets the basedn for LDAP', NULL) ON DUPLICATE KEY UPDATE category='Secure';
INSERT INTO `cloud`.`configuration`(category, instance, component, name, value, description, default_value) VALUES ('Secure', 'DEFAULT', 'management-server', 'ldap.search.group.principle', NULL, 'Sets the principle of the group that users must be a member of', NULL) ON DUPLICATE KEY UPDATE category='Secure';
INSERT INTO `cloud`.`configuration`(category, instance, component, name, value, description, default_value) VALUES ('Secure', 'DEFAULT', 'management-server', 'ldap.truststore', NULL, 'Sets the path to the truststore to use for LDAP SSL', NULL) ON DUPLICATE KEY UPDATE category='Secure';
INSERT INTO `cloud`.`configuration`(category, instance, component, name, value, description, default_value) VALUES ('Secure', 'DEFAULT', 'management-server', 'ldap.truststore.password', NULL, 'Sets the password for the truststore', NULL) ON DUPLICATE KEY UPDATE category='Secure';

CREATE TABLE `cloud`.`ldap_configuration` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `hostname` varchar(255) NOT NULL COMMENT 'the hostname of the ldap server',
  `port` int(10) COMMENT 'port that the ldap server is listening on',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `cloud`.`volumes` SET display_volume=1 where id>0;

create table `cloud`.`monitoring_services` (
`id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
`uuid` varchar(40), `service` varchar(255) COMMENT 'Service name',
`process_name` varchar(255) COMMENT 'running process name',
`service_name` varchar(255) COMMENT 'exact name of the running service',
`service_path` varchar(255) COMMENT 'path of the service in system',
`pidfile` varchar(255) COMMENT 'path of the pid file of the service',
`isDefault` boolean COMMENT 'Default service', PRIMARY KEY (`id`)
);

insert into cloud.monitoring_services(id, uuid, service, process_name, service_name, service_path, pidfile, isDefault) values(1, UUID(), 'ssh','sshd', 'ssh','/etc/init.d/ssh','/var/run/sshd.pid',true);
insert into cloud.monitoring_services(id, uuid, service, process_name, service_name, service_path, pidfile, isDefault) values(2, UUID(), 'dhcp','dnsmasq','dnsmasq','/etc/init.d/dnsmasq','/var/run/dnsmasq/dnsmasq.pid',false);
insert into cloud.monitoring_services(id, uuid, service, process_name, service_name, service_path, pidfile, isDefault) values(3, UUID(), 'loadbalancing','haproxy','haproxy','/etc/init.d/haproxy','/var/run/haproxy.pid',false);
insert into cloud.monitoring_services(id, uuid, service, process_name,  service_name, service_path, pidfile, isDefault) values(4, UUID(), 'webserver','apache2','apache2','/etc/init.d/apache2','/var/run/apache2.pid', true);

ALTER TABLE `cloud`.`service_offering` CHANGE COLUMN `cpu` `cpu` INT(10) UNSIGNED NULL COMMENT '# of cores'  , CHANGE COLUMN `speed` `speed` INT(10) UNSIGNED NULL COMMENT 'speed per core in mhz'  , CHANGE COLUMN `ram_size` `ram_size` BIGINT(20) UNSIGNED NULL  ;

CREATE TABLE `cloud`.`usage_event_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `usage_event_id` bigint unsigned NOT NULL COMMENT 'usage event id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_usage_event_details__usage_event_id` FOREIGN KEY `fk_usage_event_details__usage_event_id`(`usage_event_id`) REFERENCES `usage_event`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud_usage`.`usage_event_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `usage_event_id` bigint unsigned NOT NULL COMMENT 'usage event id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_usage_event_details__usage_event_id` FOREIGN KEY `fk_usage_event_details__usage_event_id`(`usage_event_id`) REFERENCES `usage_event`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`user_ip_address_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `user_ip_address_id` bigint unsigned NOT NULL COMMENT 'User ip address id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_user_ip_address_details__user_ip_address_id` FOREIGN KEY `fk_user_ip_address_details__user_ip_address_id`(`user_ip_address_id`) REFERENCES `user_ip_address`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`remote_access_vpn_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `remote_access_vpn_id` bigint unsigned NOT NULL COMMENT 'Remote access vpn id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_remote_access_vpn_details__remote_access_vpn_id` FOREIGN KEY `fk_remote_access_vpn_details__remote_access_vpn_id`(`remote_access_vpn_id`) REFERENCES `remote_access_vpn`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'management-server', "vmware.vcenter.session.timeout", "1200", "VMware client timeout in seconds", "1200", NULL,NULL,0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'management-server', "mgt.server.vendor", "ACS", "the vendor of management server", "ACS", NULL,NULL,0);
Update `cloud`.`configuration` set `component` = "VolumeOrchestrationService", `scope` = "Global" where `name`="custom.diskoffering.size.max";
Update `cloud`.`configuration` set `component` = "VolumeOrchestrationService", `scope` = "Global" where `name`="custom.diskoffering.size.min";

ALTER TABLE `cloud_usage`.`usage_vm_instance` ADD COLUMN `cpu_speed` INT(10) UNSIGNED NULL  COMMENT 'speed per core in Mhz',
    ADD COLUMN `cpu_cores` INT(10) UNSIGNED NULL  COMMENT 'number of cpu cores',
    ADD COLUMN  `memory` INT(10) UNSIGNED NULL  COMMENT 'memory in MB';

CREATE TABLE `cloud`.`vpc_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_id` bigint unsigned NOT NULL COMMENT 'VPC id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_details__vpc_id` FOREIGN KEY `fk_vpc_details__vpc_id`(`vpc_id`) REFERENCES `vpc`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`vpc_gateway_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vpc_gateway_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vpc_gateway_details__vpc_gateway_id` FOREIGN KEY `fk_vpc_gateway_details__vpc_gateway_id`(`vpc_gateway_id`) REFERENCES `vpc_gateways`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_acl_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_acl_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_acl_details__network_acl_id` FOREIGN KEY `fk_network_acl_details__network_acl_id`(`network_acl_id`) REFERENCES `network_acl`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`network_acl_item_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_acl_item_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_network_acl_item_details__network_acl_item_id` FOREIGN KEY `fk_network_acl_item_details__network_acl_item_id`(`network_acl_item_id`) REFERENCES `network_acl_item`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `cloud`.`alert` ADD COLUMN `name` varchar(255) DEFAULT NULL COMMENT 'name of the alert';

UPDATE `cloud`.`hypervisor_capabilities` SET `max_data_volumes_limit`=13 WHERE `hypervisor_type`='Vmware';
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, storage_motion_supported) VALUES (UUID(), 'Hyperv', '6.2', 1024, 0, 64, 0);

ALTER TABLE `cloud`.`external_load_balancer_devices` ADD COLUMN `is_exclusive_gslb_provider` int(1) unsigned NOT NULL DEFAULT 0 COMMENT '1 if load balancer appliance is acting exclusively as gslb service provider in the zone and can not be used for LB';

DELETE FROM `cloud`.`configuration` WHERE `name` IN ("xen.update.url", "update.check.interval", "baremetal_dhcp_devices", "host.updates.enable");

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'VMSnapshotManager', "vmsnapshot.create.wait", "1800", "In second, timeout for create vm snapshot", NULL, NULL,NULL,0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'VMSnapshotManager', "vmsnapshot.max", "10", "Maximum vm snapshots for a vm", NULL, NULL,NULL,0);

UPDATE `cloud`.`configuration` SET `component` = 'VMSnapshotManager' WHERE `name` IN ("vmsnapshot.create.wait", "vmsnapshot.max");

CREATE TABLE `cloud`.`s2s_vpn_gateway_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `s2s_vpn_gateway_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_s2s_vpn_gateway_details__s2s_vpn_gateway_id` FOREIGN KEY `fk_s2s_vpn_gateway_details__s2s_vpn_gateway_id`(`s2s_vpn_gateway_id`) REFERENCES `s2s_vpn_gateway`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Hidden', 'DEFAULT', 'management-server', 'hyperv.guest.network.device', null, 'Specify the virtual switch on host for guest network', NULL, NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Hidden', 'DEFAULT', 'management-server', 'hyperv.private.network.device', null, 'Specify the virtual switch on host for private network', NULL, NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Hidden', 'DEFAULT', 'management-server', 'hyperv.public.network.device', null, 'Specify the public virtual switch on host for public network', NULL, NULL, NULL, 0);

DELETE FROM `cloud`.`configuration` WHERE `name` IN ("xen.update.url", "update.check.interval", "baremetal_dhcp_devices", "host.updates.enable");

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'VMSnapshotManager', "vmsnapshot.create.wait", "1800", "In second, timeout for create vm snapshot", NULL, NULL,NULL,0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'VMSnapshotManager', "vmsnapshot.max", "10", "Maximum vm snapshots for a vm", NULL, NULL,NULL,0);

INSERT IGNORE INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id, featured, cross_zones, hypervisor_type, state)
    VALUES (9, UUID(), 'routing-9', 'SystemVM Template (HyperV)', 0, now(), 'SYSTEM', 0, 64, 1, 'http://download.cloudstack.org/templates/4.3/systemvm64template-2013-12-23-hyperv.vhd.bz2', '5df45ee6ebe1b703a8805f4e1f4d0818', 0, 'SystemVM Template (HyperV)', 'VHD', 15, 0, 1, 'Hyperv', 'Active' );

UPDATE `cloud`.`vm_template` SET `bits` = "64", `url` = "http://download.cloudstack.org/templates/4.3/systemvm64template-2013-12-23-hyperv.vhd.bz2", `state` = "Active", `checksum` = "5df45ee6ebe1b703a8805f4e1f4d0818" WHERE `id` = "9";

INSERT IGNORE INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text,  format, guest_os_id, featured, cross_zones, hypervisor_type, extractable, state)
    VALUES (6, UUID(), 'centos64-x64', 'CentOS 6.4(64-bit) GUI (Hyperv)', 1, now(), 'BUILTIN', 0, 64, 1, 'http://download.cloudstack.org/releases/4.3/centos6_4_64bit.vhd.bz2', 'eef6b9940ea3ed01221d963d4a012d0a', 0, 'CentOS 6.4 (64-bit) GUI (Hyperv)', 'VHD', 182, 1, 1, 'Hyperv', 1, 'Active');

UPDATE `cloud`.`configuration` SET `component` = 'VMSnapshotManager' WHERE `name` IN ("vmsnapshot.create.wait", "vmsnapshot.max");

CREATE TABLE `cloud`.`s2s_customer_gateway_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `s2s_customer_gateway_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_s2s_customer_gateway_details__s2s_customer_gateway_id` FOREIGN KEY `fk_s2s_customer_gateway_details__s2s_customer_gateway_id`(`s2s_customer_gateway_id`) REFERENCES `s2s_customer_gateway`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cloud`.`s2s_vpn_connection_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `s2s_vpn_connection_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_s2s_vpn_connection_details__s2s_vpn_connection_id` FOREIGN KEY `fk_s2s_vpn_connection_details__s2s_vpn_connection_id`(`s2s_vpn_connection_id`) REFERENCES `s2s_vpn_connection`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `cpu`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `ram`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `speed`;

INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) VALUES (UUID(), 'VMware', '5.5', 128, 0, 13, 32, 1, 1);

ALTER TABLE `cloud`.`network_acl_item` modify `cidr` varchar(2048);

INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name) VALUES (168, UUID(), 6, 'Windows Server 2012 R2 (64-bit)');
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("XenServer", 'Windows Server 2012 R2 (64-bit)', 168);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("VmWare", 'Windows Server 2012 R2 (64-bit)', 168);
