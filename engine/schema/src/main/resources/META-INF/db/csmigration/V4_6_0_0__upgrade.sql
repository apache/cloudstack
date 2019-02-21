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

--
-- Schema upgrade from 4.5.1 to 4.6.0
--

ALTER TABLE `cloud`.`snapshots` ADD COLUMN `min_iops` bigint(20) unsigned COMMENT 'Minimum IOPS';
ALTER TABLE `cloud`.`snapshots` ADD COLUMN `max_iops` bigint(20) unsigned COMMENT 'Maximum IOPS';

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Advanced", 'DEFAULT', 'management-server', "stats.output.uri", "", "URI to additionally send StatsCollector statistics to", "", NULL, NULL, 0);

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.user.vms','-1','The default maximum number of user VMs that can be deployed for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.public.ips','-1','The default maximum number of public IPs that can be consumed by a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.templates','-1','The default maximum number of templates that can be deployed for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.snapshots','-1','The default maximum number of snapshots that can be created for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.volumes','-1','The default maximum number of volumes that can be created for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.networks', '-1', 'The default maximum number of networks that can be created for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.vpcs', '-1', 'The default maximum number of vpcs that can be created for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.cpus', '-1', 'The default maximum number of cpu cores that can be used for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.memory', '-1', 'The default maximum memory (in MiB) that can be used for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.primary.storage', '-1', 'The default maximum primary storage space (in GiB) that can be used for a domain', '-1', NULL, NULL, 0);
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Domain Defaults', 'DEFAULT', 'management-server', 'max.domain.secondary.storage', '-1', 'The default maximum secondary storage space (in GiB) that can be used for a domain', '-1', NULL, NULL, 0);

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `user_id` bigint unsigned NOT NULL DEFAULT 1 COMMENT 'user id of VM deployer';

-- Additional checks to ensure duplicate keys are not registered and remove the previously stored duplicate keys.
DELETE `s1` FROM `ssh_keypairs` `s1`, `ssh_keypairs` `s2` WHERE `s1`.`id` > `s2`.`id` AND `s1`.`public_key` = `s2`.`public_key` AND `s1`.`account_id` = `s2`.`account_id`;
ALTER TABLE `ssh_keypairs` ADD UNIQUE `unique_index`(`fingerprint`,`account_id`);

-- ovm3 stuff
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Sun Solaris 10(32-bit)', 79);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Sun Solaris 10(64-bit)', 80);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Sun Solaris 11(32-bit)', 158);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Sun Solaris 11(64-bit)', 159);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Other Linux (32-bit)', 98);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Other Linux (64-bit)', 99);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('Ovm3', 'Other PV (32-bit)', 139);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('Ovm3', 'Other PV (64-bit)', 140);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ('Ovm3', 'DOS', 102);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Windows 8 (32-bit)', 165);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Windows 8 (64-bit)', 166);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES  ("Ovm3", 'Windows Server 2012 (64-bit)', 167);

INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('Ovm3', '3.2', 25, 0);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('Ovm3', '3.3', 50, 0);
UPDATE  `cloud`.`volumes` v,  `cloud`.`storage_pool` s,  `cloud`.`cluster` c  set v.format='RAW' where v.pool_id=s.id and s.cluster_id=c.id and c.hypervisor_type='Ovm3';
UPDATE configuration SET value='KVM,XenServer,VMware,BareMetal,Ovm,Ovm3,LXC' WHERE name='hypervisor.list';
INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text, format, guest_os_id,featured, cross_zones, hypervisor_type, state)
VALUES (12, UUID(), 'routing-12', 'SystemVM Template (Ovm3)', 0, now(), 'SYSTEM', 0, 64, 1, 'http://download.cloudstack.org/systemvm/4.6/systemvm64template-4.6.0-ovm.raw.bz2', 'c8577d27b2daafb2d9a4ed307ce2f00f', 0, 'SystemVM Template (Ovm3)', 'RAW', 183, 0, 1, 'Ovm3', 'Active' );

INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'ManagementServer', 'ovm3.heartbeat.timeout' , '180', '120', 'Timeout value to send to the checkheartbeat script for guarding the self fencing functionality on ovm3');
INSERT IGNORE INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `default_value`, `description`) VALUES ('Advanced', 'DEFAULT', 'ManagementServer', 'ovm3.heartbeat.interval' , '10', '1', 'Interval value the checkheartbeat script uses before triggering the timeout for ovm3');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.template.ovm3', 'SystemVM Template (Ovm3)', 'Name of the default router template on Ovm3.','SystemVM Template (Ovm3)', NULL, NULL, 0);

UPDATE IGNORE `cloud`.`configuration` SET `value`="PLAINTEXT" WHERE `name`="user.authenticators.exclude";

DROP TABLE IF EXISTS `cloud`.`external_bigswitch_vns_devices`;
CREATE TABLE `cloud`.`external_bigswitch_bcf_devices` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `physical_network_id` bigint unsigned NOT NULL COMMENT 'id of the physical network in to which bigswitch bcf device is added',
  `provider_name` varchar(255) NOT NULL COMMENT 'Service Provider name corresponding to this bigswitch bcf device',
  `device_name` varchar(255) NOT NULL COMMENT 'name of the bigswitch bcf device',
  `host_id` bigint unsigned NOT NULL COMMENT 'host id coresponding to the external bigswitch bcf device',
  `hostname` varchar(255) NOT NULL COMMENT 'host name or IP address for the bigswitch bcf device',
  `username` varchar(255) NOT NULL COMMENT 'username for the bigswitch bcf device',
  `password` varchar(255) NOT NULL COMMENT 'password for the bigswitch bcf device',
  `nat` boolean NOT NULL COMMENT 'NAT support for the bigswitch bcf device',
  `hash` varchar(255) NOT NULL COMMENT 'topology hash for the bigswitch bcf networks',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_external_bigswitch_bcf_devices__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_external_bigswitch_bcf_devices__physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `cloud`.`host` SET `resource`='com.cloud.hypervisor.xenserver.resource.XenServer600Resource' WHERE `resource`='com.cloud.hypervisor.xenserver.resource.XenServer602Resource';

CREATE TABLE `cloud`.`ldap_trust_map` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `domain_id` bigint unsigned NOT NULL,
  `type` varchar(10) NOT NULL,
  `name` varchar(255) NOT NULL,
  `account_type` int(1) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ldap_trust_map__domain_id` (`domain_id`),
  CONSTRAINT `fk_ldap_trust_map__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'KVM', 'default', 'Red Hat Enterprise Linux 7', 245, utc_timestamp(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'KVM', 'default', 'CentOS 7', 246, utc_timestamp(), 0);

UPDATE  `cloud`.`hypervisor_capabilities` SET  `max_data_volumes_limit` =  '32' WHERE  `hypervisor_capabilities`.`hypervisor_type` =  'KVM';


