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
        async_job.related,
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