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

DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE
VIEW `user_vm_view` AS
    SELECT
        `vm_instance`.`id` AS `id`,
        `vm_instance`.`name` AS `name`,
        `user_vm`.`display_name` AS `display_name`,
        `user_vm`.`user_data` AS `user_data`,
        `account`.`id` AS `account_id`,
        `account`.`uuid` AS `account_uuid`,
        `account`.`account_name` AS `account_name`,
        `account`.`type` AS `account_type`,
        `domain`.`id` AS `domain_id`,
        `domain`.`uuid` AS `domain_uuid`,
        `domain`.`name` AS `domain_name`,
        `domain`.`path` AS `domain_path`,
        `projects`.`id` AS `project_id`,
        `projects`.`uuid` AS `project_uuid`,
        `projects`.`name` AS `project_name`,
        `instance_group`.`id` AS `instance_group_id`,
        `instance_group`.`uuid` AS `instance_group_uuid`,
        `instance_group`.`name` AS `instance_group_name`,
        `vm_instance`.`uuid` AS `uuid`,
        `vm_instance`.`user_id` AS `user_id`,
        `vm_instance`.`last_host_id` AS `last_host_id`,
        `vm_instance`.`vm_type` AS `type`,
        `vm_instance`.`limit_cpu_use` AS `limit_cpu_use`,
        `vm_instance`.`created` AS `created`,
        `vm_instance`.`state` AS `state`,
        `vm_instance`.`removed` AS `removed`,
        `vm_instance`.`ha_enabled` AS `ha_enabled`,
        `vm_instance`.`hypervisor_type` AS `hypervisor_type`,
        `vm_instance`.`instance_name` AS `instance_name`,
        `vm_instance`.`guest_os_id` AS `guest_os_id`,
        `vm_instance`.`display_vm` AS `display_vm`,
        `guest_os`.`uuid` AS `guest_os_uuid`,
        `vm_instance`.`pod_id` AS `pod_id`,
        `host_pod_ref`.`uuid` AS `pod_uuid`,
        `vm_instance`.`private_ip_address` AS `private_ip_address`,
        `vm_instance`.`private_mac_address` AS `private_mac_address`,
        `vm_instance`.`vm_type` AS `vm_type`,
        `data_center`.`id` AS `data_center_id`,
        `data_center`.`uuid` AS `data_center_uuid`,
        `data_center`.`name` AS `data_center_name`,
        `data_center`.`is_security_group_enabled` AS `security_group_enabled`,
        `data_center`.`networktype` AS `data_center_type`,
        `host`.`id` AS `host_id`,
        `host`.`uuid` AS `host_uuid`,
        `host`.`name` AS `host_name`,
        `vm_template`.`id` AS `template_id`,
        `vm_template`.`uuid` AS `template_uuid`,
        `vm_template`.`name` AS `template_name`,
        `vm_template`.`display_text` AS `template_display_text`,
        `vm_template`.`enable_password` AS `password_enabled`,
        `iso`.`id` AS `iso_id`,
        `iso`.`uuid` AS `iso_uuid`,
        `iso`.`name` AS `iso_name`,
        `iso`.`display_text` AS `iso_display_text`,
        `service_offering`.`id` AS `service_offering_id`,
        `svc_disk_offering`.`uuid` AS `service_offering_uuid`,
        `disk_offering`.`uuid` AS `disk_offering_uuid`,
        `disk_offering`.`id` AS `disk_offering_id`,
        (CASE
            WHEN ISNULL(`service_offering`.`cpu`) THEN `custom_cpu`.`value`
            ELSE `service_offering`.`cpu`
        END) AS `cpu`,
        (CASE
            WHEN ISNULL(`service_offering`.`speed`) THEN `custom_speed`.`value`
            ELSE `service_offering`.`speed`
        END) AS `speed`,
        (CASE
            WHEN ISNULL(`service_offering`.`ram_size`) THEN `custom_ram_size`.`value`
            ELSE `service_offering`.`ram_size`
        END) AS `ram_size`,
        `backup_offering`.`uuid` AS `backup_offering_uuid`,
        `backup_offering`.`id` AS `backup_offering_id`,
        `svc_disk_offering`.`name` AS `service_offering_name`,
        `disk_offering`.`name` AS `disk_offering_name`,
        `backup_offering`.`name` AS `backup_offering_name`,
        `storage_pool`.`id` AS `pool_id`,
        `storage_pool`.`uuid` AS `pool_uuid`,
        `storage_pool`.`pool_type` AS `pool_type`,
        `volumes`.`id` AS `volume_id`,
        `volumes`.`uuid` AS `volume_uuid`,
        `volumes`.`device_id` AS `volume_device_id`,
        `volumes`.`volume_type` AS `volume_type`,
        `security_group`.`id` AS `security_group_id`,
        `security_group`.`uuid` AS `security_group_uuid`,
        `security_group`.`name` AS `security_group_name`,
        `security_group`.`description` AS `security_group_description`,
        `nics`.`id` AS `nic_id`,
        `nics`.`uuid` AS `nic_uuid`,
        `nics`.`network_id` AS `network_id`,
        `nics`.`ip4_address` AS `ip_address`,
        `nics`.`ip6_address` AS `ip6_address`,
        `nics`.`ip6_gateway` AS `ip6_gateway`,
        `nics`.`ip6_cidr` AS `ip6_cidr`,
        `nics`.`default_nic` AS `is_default_nic`,
        `nics`.`gateway` AS `gateway`,
        `nics`.`netmask` AS `netmask`,
        `nics`.`mac_address` AS `mac_address`,
        `nics`.`broadcast_uri` AS `broadcast_uri`,
        `nics`.`isolation_uri` AS `isolation_uri`,
        `vpc`.`id` AS `vpc_id`,
        `vpc`.`uuid` AS `vpc_uuid`,
        `networks`.`uuid` AS `network_uuid`,
        `networks`.`name` AS `network_name`,
        `networks`.`traffic_type` AS `traffic_type`,
        `networks`.`guest_type` AS `guest_type`,
        `user_ip_address`.`id` AS `public_ip_id`,
        `user_ip_address`.`uuid` AS `public_ip_uuid`,
        `user_ip_address`.`public_ip_address` AS `public_ip_address`,
        `ssh_keypairs`.`keypair_name` AS `keypair_name`,
        `resource_tags`.`id` AS `tag_id`,
        `resource_tags`.`uuid` AS `tag_uuid`,
        `resource_tags`.`key` AS `tag_key`,
        `resource_tags`.`value` AS `tag_value`,
        `resource_tags`.`domain_id` AS `tag_domain_id`,
        `domain`.`uuid` AS `tag_domain_uuid`,
        `domain`.`name` AS `tag_domain_name`,
        `resource_tags`.`account_id` AS `tag_account_id`,
        `account`.`account_name` AS `tag_account_name`,
        `resource_tags`.`resource_id` AS `tag_resource_id`,
        `resource_tags`.`resource_uuid` AS `tag_resource_uuid`,
        `resource_tags`.`resource_type` AS `tag_resource_type`,
        `resource_tags`.`customer` AS `tag_customer`,
        `async_job`.`id` AS `job_id`,
        `async_job`.`uuid` AS `job_uuid`,
        `async_job`.`job_status` AS `job_status`,
        `async_job`.`account_id` AS `job_account_id`,
        `affinity_group`.`id` AS `affinity_group_id`,
        `affinity_group`.`uuid` AS `affinity_group_uuid`,
        `affinity_group`.`name` AS `affinity_group_name`,
        `affinity_group`.`description` AS `affinity_group_description`,
        `vm_instance`.`dynamically_scalable` AS `dynamically_scalable`
    FROM
        (((((((((((((((((((((((((((((((((`user_vm`
        JOIN `vm_instance` ON (((`vm_instance`.`id` = `user_vm`.`id`)
            AND ISNULL(`vm_instance`.`removed`))))
        JOIN `account` ON ((`vm_instance`.`account_id` = `account`.`id`)))
        JOIN `domain` ON ((`vm_instance`.`domain_id` = `domain`.`id`)))
        LEFT JOIN `guest_os` ON ((`vm_instance`.`guest_os_id` = `guest_os`.`id`)))
        LEFT JOIN `host_pod_ref` ON ((`vm_instance`.`pod_id` = `host_pod_ref`.`id`)))
        LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
        LEFT JOIN `instance_group_vm_map` ON ((`vm_instance`.`id` = `instance_group_vm_map`.`instance_id`)))
        LEFT JOIN `instance_group` ON ((`instance_group_vm_map`.`group_id` = `instance_group`.`id`)))
        LEFT JOIN `data_center` ON ((`vm_instance`.`data_center_id` = `data_center`.`id`)))
        LEFT JOIN `host` ON ((`vm_instance`.`host_id` = `host`.`id`)))
        LEFT JOIN `vm_template` ON ((`vm_instance`.`vm_template_id` = `vm_template`.`id`)))
        LEFT JOIN `vm_template` `iso` ON ((`iso`.`id` = `user_vm`.`iso_id`)))
        LEFT JOIN `service_offering` ON ((`vm_instance`.`service_offering_id` = `service_offering`.`id`)))
        LEFT JOIN `disk_offering` `svc_disk_offering` ON ((`vm_instance`.`service_offering_id` = `svc_disk_offering`.`id`)))
        LEFT JOIN `disk_offering` ON ((`vm_instance`.`disk_offering_id` = `disk_offering`.`id`)))
        LEFT JOIN `backup_offering` ON ((`vm_instance`.`backup_offering_id` = `backup_offering`.`id`)))
        LEFT JOIN `volumes` ON ((`vm_instance`.`id` = `volumes`.`instance_id`)))
        LEFT JOIN `storage_pool` ON ((`volumes`.`pool_id` = `storage_pool`.`id`)))
        LEFT JOIN `security_group_vm_map` ON ((`vm_instance`.`id` = `security_group_vm_map`.`instance_id`)))
        LEFT JOIN `security_group` ON ((`security_group_vm_map`.`security_group_id` = `security_group`.`id`)))
        LEFT JOIN `nics` ON (((`vm_instance`.`id` = `nics`.`instance_id`)
            AND ISNULL(`nics`.`removed`))))
        LEFT JOIN `networks` ON ((`nics`.`network_id` = `networks`.`id`)))
        LEFT JOIN `vpc` ON (((`networks`.`vpc_id` = `vpc`.`id`)
            AND ISNULL(`vpc`.`removed`))))
        LEFT JOIN `user_ip_address` ON ((`user_ip_address`.`vm_id` = `vm_instance`.`id`)))
        LEFT JOIN `user_vm_details` `ssh_details` ON (((`ssh_details`.`vm_id` = `vm_instance`.`id`)
            AND (`ssh_details`.`name` = 'SSH.PublicKey'))))
        LEFT JOIN `ssh_keypairs` ON (((`ssh_keypairs`.`public_key` = `ssh_details`.`value`)
            AND (`ssh_keypairs`.`account_id` = `account`.`id`))))
        LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_instance`.`id`)
            AND (`resource_tags`.`resource_type` = 'UserVm'))))
        LEFT JOIN `async_job` ON (((`async_job`.`instance_id` = `vm_instance`.`id`)
            AND (`async_job`.`instance_type` = 'VirtualMachine')
            AND (`async_job`.`job_status` = 0))))
        LEFT JOIN `affinity_group_vm_map` ON ((`vm_instance`.`id` = `affinity_group_vm_map`.`instance_id`)))
        LEFT JOIN `affinity_group` ON ((`affinity_group_vm_map`.`affinity_group_id` = `affinity_group`.`id`)))
        LEFT JOIN `user_vm_details` `custom_cpu` ON (((`custom_cpu`.`vm_id` = `vm_instance`.`id`)
            AND (`custom_cpu`.`name` = 'CpuNumber'))))
        LEFT JOIN `user_vm_details` `custom_speed` ON (((`custom_speed`.`vm_id` = `vm_instance`.`id`)
            AND (`custom_speed`.`name` = 'CpuSpeed'))))
        LEFT JOIN `user_vm_details` `custom_ram_size` ON (((`custom_ram_size`.`vm_id` = `vm_instance`.`id`)
            AND (`custom_ram_size`.`name` = 'memory'))));

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
