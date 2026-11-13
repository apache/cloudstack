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
-- Schema upgrade from 4.13.1.0 to 4.14.0.0
--;

-- Update the description to indicate this only works with KVM + Ceph
-- (not implemented properly atm for KVM+NFS/local, and it accidentally works with XS + NFS. Not applicable for VMware)
UPDATE `cloud`.`configuration` SET `description`='Indicates whether to always backup primary storage snapshot to secondary storage. Keeping snapshots only on Primary storage is applicable for KVM + Ceph only.' WHERE  `name`='snapshot.backup.to.secondary';

-- KVM: enable storage data motion on KVM hypervisor_capabilities
UPDATE `cloud`.`hypervisor_capabilities` SET `storage_motion_supported` = 1 WHERE `hypervisor_capabilities`.`hypervisor_type` = 'KVM';

-- Use 'Other Linux 64-bit' as guest os for the default systemvmtemplate for XenServer
UPDATE `cloud`.`vm_template` SET guest_os_id=99 WHERE id=1;

-- #3659 Fix typo: the past tense of shutdown is shutdown, not shutdowned
UPDATE `cloud`.`vm_instance` SET state='Shutdown' WHERE state='Shutdowned';

-- Backup and Recovery

CREATE TABLE IF NOT EXISTS `cloud`.`backup_offering` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL COMMENT 'backup offering name',
  `description` varchar(255) NOT NULL COMMENT 'backup offering description',
  `external_id` varchar(255) DEFAULT NULL COMMENT 'external ID on provider side',
  `user_driven_backup` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'whether user can do adhoc backups and backup schedules allowed, default false',
  `zone_id` bigint(20) unsigned NOT NULL COMMENT 'zone id',
  `provider` varchar(255) NOT NULL COMMENT 'backup provider',
  `created` datetime DEFAULT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup_offering__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `backup_offering_id` bigint unsigned DEFAULT NULL COMMENT 'ID of backup offering';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `backup_external_id` varchar(255) DEFAULT NULL COMMENT 'ID of external backup job or container if any';
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN `backup_volumes` text DEFAULT NULL COMMENT 'details of backedup volumes';

CREATE TABLE IF NOT EXISTS `cloud`.`backups` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL UNIQUE,
  `vm_id` bigint(20) unsigned NOT NULL,
  `external_id` varchar(255) DEFAULT NULL COMMENT 'external ID',
  `type` varchar(255) NOT NULL COMMENT 'backup type',
  `date` varchar(255) NOT NULL COMMENT 'backup date',
  `size` bigint(20) DEFAULT 0 COMMENT 'size of the backup',
  `protected_size` bigint(20) DEFAULT 0,
  `status` varchar(32) DEFAULT NULL,
  `backup_offering_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_backup__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`backup_schedule` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vm_id` bigint(20) unsigned NOT NULL UNIQUE,
  `schedule_type` int(4) DEFAULT NULL COMMENT 'backup schedulet type e.g. hourly, daily, etc.',
  `schedule` varchar(100) DEFAULT NULL COMMENT 'schedule time of execution',
  `timezone` varchar(100) DEFAULT NULL COMMENT 'the timezone in which the schedule time is specified',
  `scheduled_timestamp` datetime DEFAULT NULL COMMENT 'Time at which the backup was scheduled for execution',
  `async_job_id` bigint(20) unsigned DEFAULT NULL COMMENT 'If this schedule is being executed, it is the id of the create aysnc_job. Before that it is null',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_backup_schedule__vm_id` FOREIGN KEY (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_backup` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `zone_id` bigint(20) unsigned NOT NULL,
  `account_id` bigint(20) unsigned NOT NULL,
  `domain_id` bigint(20) unsigned NOT NULL,
  `vm_id` bigint(20) unsigned NOT NULL,
  `backup_offering_id` bigint(20) unsigned NOT NULL,
  `size` bigint(20) DEFAULT 0,
  `protected_size` bigint(20) DEFAULT 0,
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `i_usage_backup` (`zone_id`,`account_id`,`vm_id`,`created`)
) ENGINE=InnoDB CHARSET=utf8;

-- Fix OS category for some Ubuntu and RedHat OS-es
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=277 AND display_name="Ubuntu 17.04";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=278 AND display_name="Ubuntu 17.10";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=279 AND display_name="Ubuntu 18.04 LTS";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=280 AND display_name="Ubuntu 18.10";
UPDATE `cloud`.`guest_os` SET `category_id`='10' WHERE `id`=281 AND display_name="Ubuntu 19.04";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=282 AND display_name="Red Hat Enterprise Linux 7.3";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=283 AND display_name="Red Hat Enterprise Linux 7.4";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=284 AND display_name="Red Hat Enterprise Linux 7.5";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=285 AND display_name="Red Hat Enterprise Linux 7.6";
UPDATE `cloud`.`guest_os` SET `category_id`='4' WHERE `id`=286 AND display_name="Red Hat Enterprise Linux 8.0";

-- Create table for router health checks. We only save last check result for each.
CREATE TABLE  `cloud`.`router_health_check` (
  `id` bigint unsigned NOT NULL auto_increment,
  `router_id` bigint unsigned NOT NULL COMMENT 'router id',
  `check_name` varchar(255) NOT NULL COMMENT 'name of the health check',
  `check_type` varchar(255) NOT NULL COMMENT 'type of the health check',
  `last_update` DATETIME NULL COMMENT 'last check update time',
  `check_result` boolean NOT NULL COMMENT 'check executions success or failure',
  `check_details` BLOB NULL COMMENT 'check result detailed message',
  PRIMARY KEY  (`id`),
  CONSTRAINT `fk_router_health_checks__router_id` FOREIGN KEY (`router_id`) REFERENCES `domain_router`(`id`) ON DELETE CASCADE,
  UNIQUE `i_router_health_checks__router_id__check_name__check_type`(`router_id`, `check_name`, `check_type`),
  INDEX `i_router_health_checks__router_id`(`router_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- Kubernetes service
CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_supported_version` (
    `id` bigint unsigned NOT NULL auto_increment,
    `uuid` varchar(40) DEFAULT NULL,
    `name` varchar(255) NOT NULL COMMENT 'the name of this Kubernetes version',
    `semantic_version` varchar(32) NOT NULL COMMENT 'the semantic version for this Kubernetes version',
    `iso_id` bigint unsigned NOT NULL COMMENT 'the ID of the binaries ISO for this Kubernetes version',
    `zone_id` bigint unsigned DEFAULT NULL COMMENT 'the ID of the zone for which this Kubernetes version is made available',
    `state` char(32) DEFAULT NULL COMMENT 'the enabled or disabled state for this Kubernetes version',
    `min_cpu` int(10) unsigned NOT NULL COMMENT 'the minimum CPU needed by cluster nodes for using this Kubernetes version',
    `min_ram_size` bigint(20) unsigned NOT NULL COMMENT 'the minimum RAM in MB needed by cluster nodes for this Kubernetes version',
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime COMMENT 'date removed or null, if still present',

    PRIMARY KEY(`id`),
    CONSTRAINT `fk_kubernetes_supported_version__iso_id` FOREIGN KEY `fk_kubernetes_supported_version__iso_id`(`iso_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_kubernetes_supported_version__zone_id` FOREIGN KEY `fk_kubernetes_supported_version__zone_id`(`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_cluster` (
    `id` bigint unsigned NOT NULL auto_increment,
    `uuid` varchar(40) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(4096) COMMENT 'display text for this Kubernetes cluster',
    `zone_id` bigint unsigned NOT NULL COMMENT 'the ID of the zone in which this Kubernetes cluster is deployed',
    `kubernetes_version_id` bigint unsigned NOT NULL COMMENT 'the ID of the Kubernetes version of this Kubernetes cluster',
    `service_offering_id` bigint unsigned COMMENT 'service offering id for the cluster VM',
    `template_id` bigint unsigned COMMENT 'the ID of the template used by this Kubernetes cluster',
    `network_id` bigint unsigned COMMENT 'the ID of the network used by this Kubernetes cluster',
    `master_node_count` bigint NOT NULL default '0' COMMENT 'the number of the master nodes deployed for this Kubernetes cluster',
    `node_count` bigint NOT NULL default '0' COMMENT 'the number of the worker nodes deployed for this Kubernetes cluster',
    `account_id` bigint unsigned NOT NULL COMMENT 'the ID of owner account of this Kubernetes cluster',
    `domain_id` bigint unsigned NOT NULL COMMENT 'the ID of the domain of this cluster',
    `state` char(32) NOT NULL COMMENT 'the current state of this Kubernetes cluster',
    `key_pair` varchar(40),
    `cores` bigint unsigned NOT NULL COMMENT 'total number of CPU cores used by this Kubernetes cluster',
    `memory` bigint unsigned NOT NULL COMMENT 'total memory used by this Kubernetes cluster',
    `node_root_disk_size` bigint(20) unsigned DEFAULT 0 COMMENT 'root disk size of root disk for each node',
    `endpoint` varchar(255) COMMENT 'url endpoint of the Kubernetes cluster manager api access',
    `created` datetime NOT NULL COMMENT 'date created',
    `removed` datetime COMMENT 'date removed or null, if still present',
    `gc` tinyint unsigned NOT NULL DEFAULT 1 COMMENT 'gc this Kubernetes cluster or not',

    PRIMARY KEY(`id`),
    CONSTRAINT `fk_cluster__zone_id` FOREIGN KEY `fk_cluster__zone_id`(`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__kubernetes_version_id` FOREIGN KEY `fk_cluster__kubernetes_version_id`(`kubernetes_version_id`) REFERENCES `kubernetes_supported_version` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__service_offering_id` FOREIGN KEY `fk_cluster__service_offering_id`(`service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__template_id` FOREIGN KEY `fk_cluster__template_id`(`template_id`) REFERENCES `vm_template`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cluster__network_id` FOREIGN KEY `fk_cluster__network_id`(`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_cluster_vm_map` (
    `id` bigint unsigned NOT NULL auto_increment,
    `cluster_id` bigint unsigned NOT NULL COMMENT 'the ID of the Kubernetes cluster',
    `vm_id` bigint unsigned NOT NULL COMMENT 'the ID of the VM',

    PRIMARY KEY(`id`),
    CONSTRAINT `fk_kubernetes_cluster_vm_map__cluster_id` FOREIGN KEY `fk_kubernetes_cluster_vm_map__cluster_id`(`cluster_id`) REFERENCES `kubernetes_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `cloud`.`kubernetes_cluster_details` (
    `id` bigint unsigned NOT NULL auto_increment,
    `cluster_id` bigint unsigned NOT NULL COMMENT 'the ID of the Kubernetes cluster',
    `name` varchar(255) NOT NULL,
    `value` varchar(10240) NOT NULL,
    `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user else false',

    PRIMARY KEY(`id`),
    CONSTRAINT `fk_kubernetes_cluster_details__cluster_id` FOREIGN KEY `fk_kubernetes_cluster_details__cluster_id`(`cluster_id`) REFERENCES `kubernetes_cluster`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
