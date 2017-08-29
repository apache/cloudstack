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
-- Schema upgrade from 4.9.3.0 to 4.10.0.0;
--;

ALTER TABLE `cloud`.`domain_router` ADD COLUMN  update_state varchar(64) DEFAULT NULL;

INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (257, UUID(), 6, 'Windows 10 (32-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (258, UUID(), 6, 'Windows 10 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (259, UUID(), 6, 'Windows Server 2016 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (260, UUID(), 1, 'CentOS 7.1', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (261, UUID(), 1, 'CentOS 6.6 (32-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (262, UUID(), 1, 'CentOS 6.6 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (263, UUID(), 1, 'CentOS 6.7 (32-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (264, UUID(), 1, 'CentOS 6.7 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (265, UUID(), 4, 'Red Hat Enterprise Linux 6.6 (32-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (266, UUID(), 4, 'Red Hat Enterprise Linux 6.6 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (267, UUID(), 4, 'Red Hat Enterprise Linux 6.7 (32-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (268, UUID(), 4, 'Red Hat Enterprise Linux 6.7 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (269, UUID(), 2, 'Debian GNU/Linux 8 (32-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (270, UUID(), 2, 'Debian GNU/Linux 8 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (271, UUID(), 7, 'CoreOS', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (272, UUID(), 4, 'Red Hat Enterprise Linux 7.1', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (273, UUID(), 4, 'Red Hat Enterprise Linux 7.2', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (274, UUID(), 1, 'CentOS 7.2', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (275, UUID(), 6, 'Other PV Virtio-SCSI (64-bit)', now());

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '6.5.0', 'Windows 10 (32-bit)', 257, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.0', 'windows9Guest', 257, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Windows 10', 257, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '6.5.0', 'Windows 10 (64-bit)', 258, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.0', 'windows9_64Guest', 258, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Windows 10', 258, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '6.5.0', 'Windows Server 2016 (64-bit)', 259, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.0.0', 'Windows Server 2016 (64-bit)', 259, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.0', 'windows9Server64Guest', 259, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Windows Server 2016', 259, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'CentOS 7', 260, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.5', 'centos64Guest', 260, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'centos64Guest', 260, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'KVM', 'default', 'CentOS 7.1', 260, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'CentOS 6 (32-bit)', 261, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'centosGuest', 261, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'CentOS 6 (64-bit)', 262, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'centos64Guest', 262, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'CentOS 6 (32-bit)', 263, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'centosGuest', 263, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'CentOS 6 (64-bit)', 264, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'centos64Guest', 264, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'Red Hat Enterprise Linux 6 (32-bit)', 265, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'rhel6Guest', 265, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'Red Hat Enterprise Linux 6 (64-bit)', 266, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'rhel6_64Guest', 266, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'Red Hat Enterprise Linux 6 (32-bit)', 267, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'rhel6Guest', 267, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'Red Hat Enterprise Linux 6 (64-bit)', 268, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'rhel6_64Guest', 268, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'debian8Guest', 269, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'debian8Guest', 270, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', 'default', 'CoreOS', 271, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', 'default', 'coreos64Guest', 271, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.5', 'coreos64Guest', 271, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'coreos64Guest', 271, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'KVM', 'default', 'CoreOS', 271, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '6.5.0', 'Red Hat Enterprise Linux 7', 272, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '5.5', 'rhel7_64Guest', 272, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '6.0', 'rhel7_64Guest', 272, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'KVM', 'default', 'Red Hat Enterprise Linux 7.1', 272, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '6.5.0', 'Red Hat Enterprise Linux 7', 273, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.0.0', 'Red Hat Enterprise Linux 7', 273, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'rhel7_64Guest', 273, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.0', 'rhel7_64Guest', 273, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Red Hat Enterprise Linux 7.2', 273, now(), 0);

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '6.5.0', 'CentOS 7', 274, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.0.0', 'CentOS 7', 274, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '5.5', 'centos64Guest', 274, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.0', 'centos64Guest', 274, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'CentOS 7.2', 274, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Other PV Virtio-SCSI (64-bit)', 275, now(), 0);

CREATE TABLE `cloud`.`vlan_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `vlan_id` bigint unsigned NOT NULL COMMENT 'vlan id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Should detail be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_vlan_details__vlan_id` FOREIGN KEY `fk_vlan_details__vlan_id`(`vlan_id`) REFERENCES `vlan`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`network_offerings` ADD COLUMN supports_public_access boolean default false;

ALTER TABLE `cloud`.`image_store_details` CHANGE COLUMN `value` `value` VARCHAR(255) NULL DEFAULT NULL COMMENT 'value of the detail', ADD COLUMN `display` tinyint(1) NOT
NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user' AFTER `value`;

ALTER TABLE `snapshots` ADD COLUMN `location_type` VARCHAR(32) COMMENT 'Location of snapshot (ex. Primary)';

-- Database change for CLOUDSTACK-8746 (VM Snapshotting implementation for KVM)
UPDATE `cloud`.`hypervisor_capabilities` SET `vm_snapshot_enabled` = 1 WHERE `hypervisor_type` ='KVM' AND `hypervisor_version` = 'default';

-- [VM-SNAPSHOT] add role permissions for new API command createSnapshotFromVMSnapshot
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'createSnapshotFromVMSnapshot', 'ALLOW', 318) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'createSnapshotFromVMSnapshot', 'ALLOW', 302) ON DUPLICATE KEY UPDATE rule=rule;
INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'createSnapshotFromVMSnapshot', 'ALLOW', 260) ON DUPLICATE KEY UPDATE rule=rule;

-- Create table storage_pool_tags
CREATE TABLE `cloud`.`storage_pool_tags` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pool_id` bigint(20) unsigned NOT NULL COMMENT "pool id",
  `tag` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_storage_pool_tags__pool_id` (`pool_id`),
  CONSTRAINT `fk_storage_pool_tags__pool_id` FOREIGN KEY (`pool_id`) REFERENCES `storage_pool` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- Insert storage tags from storage_pool_details
INSERT INTO `cloud`.`storage_pool_tags` (pool_id, tag) SELECT pool_id, 
name FROM `cloud`.`storage_pool_details` WHERE value = 'true';

-- Alter view storage_pool_view
CREATE OR REPLACE
VIEW `storage_pool_view` AS
    SELECT
        `storage_pool`.`id` AS `id`,
        `storage_pool`.`uuid` AS `uuid`,
        `storage_pool`.`name` AS `name`,
        `storage_pool`.`status` AS `status`,
        `storage_pool`.`path` AS `path`,
        `storage_pool`.`pool_type` AS `pool_type`,
        `storage_pool`.`host_address` AS `host_address`,
        `storage_pool`.`created` AS `created`,
        `storage_pool`.`removed` AS `removed`,
        `storage_pool`.`capacity_bytes` AS `capacity_bytes`,
        `storage_pool`.`capacity_iops` AS `capacity_iops`,
        `storage_pool`.`scope` AS `scope`,
        `storage_pool`.`hypervisor` AS `hypervisor`,
        `storage_pool`.`storage_provider_name` AS `storage_provider_name`,
        `cluster`.`id` AS `cluster_id`,
        `cluster`.`uuid` AS `cluster_uuid`,
        `cluster`.`name` AS `cluster_name`,
        `cluster`.`cluster_type` AS `cluster_type`,
        `data_center`.`id` AS `data_center_id`,
        `data_center`.`uuid` AS `data_center_uuid`,
        `data_center`.`name` AS `data_center_name`,
        `data_center`.`networktype` AS `data_center_type`,
        `host_pod_ref`.`id` AS `pod_id`,
        `host_pod_ref`.`uuid` AS `pod_uuid`,
        `host_pod_ref`.`name` AS `pod_name`,
        `storage_pool_tags`.`tag` AS `tag`,
        `op_host_capacity`.`used_capacity` AS `disk_used_capacity`,
        `op_host_capacity`.`reserved_capacity` AS `disk_reserved_capacity`,
        `async_job`.`id` AS `job_id`,
        `async_job`.`uuid` AS `job_uuid`,
        `async_job`.`job_status` AS `job_status`,
        `async_job`.`account_id` AS `job_account_id`
    FROM
        ((((((`storage_pool`
        LEFT JOIN `cluster` ON ((`storage_pool`.`cluster_id` = `cluster`.`id`)))
        LEFT JOIN `data_center` ON ((`storage_pool`.`data_center_id` = `data_center`.`id`)))
        LEFT JOIN `host_pod_ref` ON ((`storage_pool`.`pod_id` = `host_pod_ref`.`id`)))
        LEFT JOIN `storage_pool_tags` ON (((`storage_pool_tags`.`pool_id` = `storage_pool`.`id`))))
        LEFT JOIN `op_host_capacity` ON (((`storage_pool`.`id` = `op_host_capacity`.`host_id`)
            AND (`op_host_capacity`.`capacity_type` IN (3 , 9)))))
        LEFT JOIN `async_job` ON (((`async_job`.`instance_id` = `storage_pool`.`id`)
            AND (`async_job`.`instance_type` = 'StoragePool')
            AND (`async_job`.`job_status` = 0))));

-- Alter view image_store_view
CREATE OR REPLACE
VIEW `image_store_view` AS
    SELECT
        `image_store`.`id` AS `id`,
        `image_store`.`uuid` AS `uuid`,
        `image_store`.`name` AS `name`,
        `image_store`.`image_provider_name` AS `image_provider_name`,
        `image_store`.`protocol` AS `protocol`,
        `image_store`.`url` AS `url`,
        `image_store`.`scope` AS `scope`,
        `image_store`.`role` AS `role`,
        `image_store`.`removed` AS `removed`,
        `data_center`.`id` AS `data_center_id`,
        `data_center`.`uuid` AS `data_center_uuid`,
        `data_center`.`name` AS `data_center_name`
    FROM
        (`image_store`
        LEFT JOIN `data_center` ON ((`image_store`.`data_center_id` = `data_center`.`id`)));

-- Add service_offering_id column to vm_snapshots table
ALTER TABLE `cloud`.`vm_snapshots` ADD COLUMN `service_offering_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '' AFTER `domain_id`;
UPDATE `cloud`.`vm_snapshots` s JOIN `cloud`.`vm_instance` v ON v.id = s.vm_id SET s.service_offering_id = v.service_offering_id;
ALTER TABLE `cloud`.`vm_snapshots` ADD CONSTRAINT `fk_vm_snapshots_service_offering_id` FOREIGN KEY (`service_offering_id`) REFERENCES `cloud`.`service_offering` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION;

-- Update vm snapshot details for instances with custom service offerings
INSERT INTO `cloud`.`vm_snapshot_details` (vm_snapshot_id, name, value)
SELECT s.id, d.name, d.value
FROM `cloud`.`user_vm_details` d JOIN `cloud`.`vm_instance` v ON (d.vm_id = v.id)
JOIN `cloud`.`service_offering` o ON (v.service_offering_id = o.id) 
JOIN `cloud`.`vm_snapshots` s ON (s.service_offering_id = o.id AND s.vm_id = v.id)
WHERE (o.cpu is null AND o.speed IS NULL AND o.ram_size IS NULL) AND
(d.name = 'cpuNumber' OR d.name = 'cpuSpeed' OR d.name = 'memory');

-- CLOUDSTACK-9827: Storage tags stored in multiple places
DROP VIEW IF EXISTS `cloud`.`storage_tag_view`;

CREATE TABLE `cloud`.`guest_os_details` (
  `id` bigint unsigned NOT NULL auto_increment,
  `guest_os_id` bigint unsigned NOT NULL COMMENT 'VPC gateway id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_guest_os_details__guest_os_id` FOREIGN KEY `fk_guest_os_details__guest_os_id`(`guest_os_id`) REFERENCES `guest_os`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `user_ip_address` ADD COLUMN `rule_state` VARCHAR(32) COMMENT 'static  rule state while removing';
CREATE TABLE `cloud`.`firewall_rules_dcidrs`(
  `id` BIGINT(20) unsigned NOT NULL AUTO_INCREMENT,
  `firewall_rule_id` BIGINT(20) unsigned NOT NULL,
  `destination_cidr` VARCHAR(18) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY `unique_rule_dcidrs` (`firewall_rule_id`, `destination_cidr`),
  KEY `fk_firewall_dcidrs_firewall_rules` (`firewall_rule_id`),
  CONSTRAINT `fk_firewall_dcidrs_firewall_rules` FOREIGN KEY (`firewall_rule_id`) REFERENCES `firewall_rules` (`id`) ON DELETE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`netscaler_servicepackages`;
CREATE TABLE `cloud`.`netscaler_servicepackages` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) UNIQUE COMMENT 'name of the service package',
  `description` varchar(255) COMMENT 'description of the service package',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `cloud`.`external_netscaler_controlcenter`;
CREATE TABLE `cloud`.`external_netscaler_controlcenter` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
  `uuid` varchar(255) UNIQUE,
  `username` varchar(255) COMMENT 'username of the NCC',
  `password` varchar(255) COMMENT 'password of NCC',
  `ncc_ip` varchar(255) COMMENT 'IP of NCC Manager',
  `num_retries` bigint unsigned NOT NULL default 2 COMMENT 'Number of retries in ncc for command failure',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`sslcerts` ADD COLUMN `name` varchar(255) NULL default NULL COMMENT 'Name of the Certificate';
ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `service_package_id` varchar(255) NULL default NULL COMMENT 'Netscaler ControlCenter Service Package';


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Premium', 'DEFAULT', 'management-server', 'volume.stats.interval', '600000', 'Interval (in seconds) to report volume statistics', '600000', now(), NULL, NULL);

DROP VIEW IF EXISTS `cloud`.`volume_view`;
CREATE VIEW `cloud`.`volume_view` AS
    select
        volumes.id,
        volumes.uuid,
        volumes.name,
        volumes.device_id,
        volumes.volume_type,
        volumes.provisioning_type,
        volumes.size,
        volumes.min_iops,
        volumes.max_iops,
        volumes.created,
        volumes.state,
        volumes.attached,
        volumes.removed,
        volumes.display_volume,
        volumes.format,
        volumes.path,
        volumes.chain_info,
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
        data_center.networktype data_center_type,
        vm_instance.id vm_id,
        vm_instance.uuid vm_uuid,
        vm_instance.name vm_name,
        vm_instance.state vm_state,
        vm_instance.vm_type,
        user_vm.display_name vm_display_name,
        volume_store_ref.size volume_store_size,
        volume_store_ref.download_pct,
        volume_store_ref.download_state,
        volume_store_ref.error_str,
        volume_store_ref.created created_on_store,
        disk_offering.id disk_offering_id,
        disk_offering.uuid disk_offering_uuid,
        disk_offering.name disk_offering_name,
        disk_offering.display_text disk_offering_display_text,
        disk_offering.use_local_storage,
        disk_offering.system_use,
        disk_offering.bytes_read_rate,
        disk_offering.bytes_write_rate,
        disk_offering.iops_read_rate,
        disk_offering.iops_write_rate,
        disk_offering.cache_mode,
        storage_pool.id pool_id,
        storage_pool.uuid pool_uuid,
        storage_pool.name pool_name,
        cluster.id cluster_id,
        cluster.name cluster_name,
        cluster.uuid cluster_uuid,
        cluster.hypervisor_type,
        vm_template.id template_id,
        vm_template.uuid template_uuid,
        vm_template.extractable,
        vm_template.type template_type,
        vm_template.name template_name,
        vm_template.display_text template_display_text,
        iso.id iso_id,
        iso.uuid iso_uuid,
        iso.name iso_name,
        iso.display_text iso_display_text,
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
        async_job.account_id job_account_id,
        host_pod_ref.id pod_id,
        host_pod_ref.uuid pod_uuid,
        host_pod_ref.name pod_name,
        resource_tag_account.account_name tag_account_name,
        resource_tag_domain.uuid tag_domain_uuid,
        resource_tag_domain.name tag_domain_name
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
        `cloud`.`volume_store_ref` ON volumes.id = volume_store_ref.volume_id
            left join
        `cloud`.`disk_offering` ON volumes.disk_offering_id = disk_offering.id
            left join
        `cloud`.`storage_pool` ON volumes.pool_id = storage_pool.id
            left join
        `cloud`.`host_pod_ref` ON storage_pool.pod_id = host_pod_ref.id
            left join
        `cloud`.`cluster` ON storage_pool.cluster_id = cluster.id
            left join
        `cloud`.`vm_template` ON volumes.template_id = vm_template.id
            left join
        `cloud`.`vm_template` iso ON iso.id = volumes.iso_id
            left join
        `cloud`.`resource_tags` ON resource_tags.resource_id = volumes.id
            and resource_tags.resource_type = 'Volume'
            left join
        `cloud`.`async_job` ON async_job.instance_id = volumes.id
            and async_job.instance_type = 'Volume'
            and async_job.job_status = 0
            left join
        `cloud`.`account` resource_tag_account ON resource_tag_account.id = resource_tags.account_id
            left join
        `cloud`.`domain` resource_tag_domain ON resource_tag_domain.id = resource_tags.domain_id;
