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

#Schema upgrade from 3.0.5 to 3.0.6;

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `eip_associate_public_ip` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'true if public IP is associated with user VM creation by default when EIP service is enabled.' AFTER `elastic_ip_service`;
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('VmWare', 'Red Hat Enterprise Linux 6.0(32-bit)', 136);
INSERT INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, guest_os_name, guest_os_id) VALUES ('VmWare', 'Red Hat Enterprise Linux 6.0(64-bit)', 137);

UPDATE `cloud`.`user` SET PASSWORD=RAND() WHERE id=1;

ALTER TABLE `cloud`.`sync_queue` ADD COLUMN `queue_size` smallint DEFAULT 0 COMMENT 'number of items being processed by the queue';
ALTER TABLE `cloud`.`sync_queue` ADD COLUMN `queue_size_limit` smallint DEFAULT 1 COMMENT 'max number of items the queue can process concurrently';
ALTER TABLE `cloud`.`sync_queue_item` ADD COLUMN `queue_proc_time` datetime COMMENT 'when processing started for the item';
ALTER TABLE `cloud`.`sync_queue_item` ADD KEY `i_sync_queue__queue_proc_time`(`queue_proc_time`);

ALTER TABLE `cloud`.`usage_event` ADD COLUMN `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`usage_event` ADD COLUMN `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`usage_storage` ADD COLUMN `virtual_size` bigint unsigned;
ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `virtual_size` bigint unsigned;
ALTER TABLE `cloud`.`volumes` ADD COLUMN `iso_id` bigint unsigned COMMENT 'Records the iso id from which the vm is created' AFTER `template_id` ;

ALTER TABLE `cloud`.`external_load_balancer_devices` DROP COLUMN `is_inline`;
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `inline` int(1) unsigned NOT NULL DEFAULT 0 COMMENT 'Is this network offering LB provider is in inline mode';

ALTER TABLE `cloud`.`inline_load_balancer_nic_map` DROP FOREIGN KEY fk_inline_load_balancer_nic_map__load_balancer_id;
ALTER TABLE `cloud`.`inline_load_balancer_nic_map` DROP COLUMN load_balancer_id;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'concurrent.snapshots.threshold.perhost', null, 'Limits number of snapshots that can be handled by the host concurrently; default is NULL - unlimited');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'xen.update.url', 'http://updates.xensource.com/XenServer/updates.xml', 'URL to get the latest XenServer updates');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'update.check.interval', '10080', 'Interval to check XenServer updates(in minutes)');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'host.updates.enable', 'false', 'Enable/Disable Host updates checker');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Network','DEFAULT','NetworkManager','network.dhcp.nondefaultnetwork.setgateway.guestos','Windows','The guest OS\'s name start with this fields would result in DHCP server response gateway information even when the network it\'s on is not default network. Names are separated by comma.');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', 'router.check.poolsize' , '10', 'Numbers of threads using to check redundant router status.');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'timeout.baremetal.securitygroup.agent.echo' , '3600', 'Timeout to echo baremetal security group agent, in seconds, the provisioning process will be treated as a failure');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'interval.baremetal.securitygroup.agent.echo' , '10', 'Interval to echo baremetal security group agent, in seconds');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'enable.baremetal.securitygroup.agent.echo' , 'false', 'After starting provision process, periodcially echo security agent installed in the template. Treat provisioning as success only if echo successfully');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'recreate.systemvm.enabled' , 'false', 'If true, will recreate system vm root disk whenever starting system vm');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.instancename.flag' , 'false', 'If true, will append guest VMs display Name (if set) to its internal name and set hostname and display name to the conjoined value');
INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.additional.vnc.portrange.size' , '1000', 'Start port number of additional VNC port range');

INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit) VALUES ('XenServer', '6.1.0', 50, 1, 13);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled) VALUES ('VMware', '5.1', 128, 0);

INSERT INTO `cloud`.`guest_os` (id, category_id, name, uuid, display_name) VALUES (206, 6, NULL, '8ceb2da9-62cd-53d4-ac8a-d0563d9bec2d', 'Windows 8(64-bit)');


CREATE TABLE `cloud`.`host_updates` (
  `id` bigint unsigned NOT NULL auto_increment,
  `uuid` varchar(40),
  `label` varchar(40),
  `description` varchar(999),
  `after_apply_guidance` varchar(40),
  `url` varchar(999),
  `timestamp` varchar(80),
  PRIMARY KEY  (`id`),
  CONSTRAINT `uc_host_updates__uuid` UNIQUE (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`host_updates_ref` (
  `id` bigint unsigned NOT NULL auto_increment,
  `host_id` bigint unsigned NOT NULL,
  `patch_id` bigint unsigned NOT NULL,
  `update_applied` tinyint(1) NOT NULL default '0',
  PRIMARY KEY  (`id`),
  CONSTRAINT `uc_host_updates__host_patch_id` UNIQUE (`host_id`, `patch_id`),
  CONSTRAINT `fk_host_updates__host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'xen.nics.max', '7', 'Maximum allowed nics for Vms created on Xen');

UPDATE `cloud`.`networks` set name='Shared SG enabled network', display_text='Shared SG enabled network' WHERE name IS null AND traffic_type='Guest' AND data_center_id IN (select id from data_center where networktype='Advanced' and is_security_group_enabled=1) AND acl_type='Domain';

# patch UUID column with ID for volumes and snapshot_policy tables
UPDATE `cloud`.`volumes` set uuid=id WHERE uuid is NULL;
UPDATE `cloud`.`snapshot_policy` set uuid=id WHERE uuid is NULL;
