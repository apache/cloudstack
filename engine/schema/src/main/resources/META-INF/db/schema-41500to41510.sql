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
-- Schema upgrade from 4.15.0.0 to 4.15.1.0
--;
-- Correct guest OS names
UPDATE `cloud`.`guest_os` SET display_name='Fedora Linux (32 bit)' WHERE id=320;
UPDATE `cloud`.`guest_os` SET display_name='Mandriva Linux (32 bit)' WHERE id=323;
UPDATE `cloud`.`guest_os` SET display_name='OpenSUSE Linux (32 bit)' WHERE id=327;

-- Add support for SUSE Linux Enterprise Desktop 12 SP3 (64-bit) for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (330, UUID(), 5, 'SUSE Linux Enterprise Desktop 12 SP3 (64-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'SUSE Linux Enterprise Desktop 12 SP3 (64-bit)', 330, now(), 0);
-- Add support for SUSE Linux Enterprise Desktop 12 SP4 (64-bit) for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (331, UUID(), 5, 'SUSE Linux Enterprise Desktop 12 SP4 (64-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'SUSE Linux Enterprise Desktop 12 SP4 (64-bit)', 331, now(), 0);
-- Add support for SUSE Linux Enterprise Server 12 SP4 (64-bit) for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (332, UUID(), 5, 'SUSE Linux Enterprise Server 12 SP4 (64-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'SUSE Linux Enterprise Server 12 SP4 (64-bit)', 332, now(), 0);
-- Add support for Scientific Linux 7 for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (333, UUID(), 9, 'Scientific Linux 7', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'Scientific Linux 7', 333, now(), 0);
-- Add support for NeoKylin Linux Server 7 for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (334, UUID(), 9, 'NeoKylin Linux Server 7', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'NeoKylin Linux Server 7', 332, now(), 0);
-- Add support CentOS 8 for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'CentOS 8', 297, now(), 0);
-- Add support for Debian Buster 10 for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'Debian Buster 10', 292, now(), 0);
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'Debian Buster 10', 293, now(), 0);
-- Add support for SUSE Linux Enterprise 15 (64-bit) for Xenserver 8.1.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.1.0', 'SUSE Linux Enterprise 15 (64-bit)', 291, now(), 0);

-- Add XenServer 8.2.0 hypervisor capabilities
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) values (UUID(), 'XenServer', '8.2.0', 1000, 253, 64, 1);

-- Copy XenServer 8.1.0 hypervisor guest OS mappings to XenServer 8.2.0
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.2.0', guest_os_name, guest_os_id, utc_timestamp(), 0 FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='8.1.0';

-- Add support for Ubuntu Focal Fossa 20.04 for Xenserver 8.2.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (335, UUID(), 10, 'Ubuntu 20.04 LTS', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'Xenserver', '8.2.0', 'Ubuntu Focal Fossa 20.04', 330, now(), 0);

-------------------------------------------------------------------------------------------------------------

-- Add support for VMware 7.0
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '7.0', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '7.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='6.7';

-- Add support for darwin19_64Guest from VMware 7.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (336, UUID(), 7, 'macOS 10.15 (64 bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0', 'darwin19_64Guest', 336, now(), 0);

-- Add support for debian11_64Guest from VMware 7.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (337, UUID(), 2, 'Debian GNU/Linux 11 (64-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0', 'debian11_64Guest', 337, now(), 0);

-- Add support for debian11Guest from VMware 7.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (338, UUID(), 2, 'Debian GNU/Linux 11 (32-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0', 'debian11Guest', 338, now(), 0);

-- Add support for windows2019srv_64Guest from VMware 7.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0', 'windows2019srv_64Guest', 276, now(), 0);


-- Add support for VMware 7.0.1.0
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities` (uuid, hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) values (UUID(), 'VMware', '7.0.1.0', 1024, 0, 59, 64, 1, 1);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '7.0.1.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='7.0';

-- Add support for amazonlinux3_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (339, UUID(), 7, 'Amazon Linux 3 (64 bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'amazonlinux3_64Guest', 339, now(), 0);

-- Add support for asianux9_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (340, UUID(), 7, 'Asianux Server 9 (64 bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'asianux9_64Guest', 340, now(), 0);

-- Add support for centos9_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (341, UUID(), 1, 'CentOS 9', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'centos9_64Guest', 341, now(), 0);

-- Add support for darwin20_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (342, UUID(), 7, 'macOS 11 (64 bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'darwin20_64Guest', 342, now(), 0);

-- Add support for darwin21_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'darwin21_64Guest', 342, now(), 0);

-- Add support for freebsd13_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (343, UUID(), 9, 'FreeBSD 13 (64-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'freebsd13_64Guest', 343, now(), 0);

-- Add support for freebsd13Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (344, UUID(), 9, 'FreeBSD 13 (32-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'freebsd13Guest', 344, now(), 0);

-- Add support for oracleLinux9_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (345, UUID(), 3, 'Oracle Linux 9', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'oracleLinux9_64Guest', 345, now(), 0);

-- Add support for other5xLinux64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (346, UUID(), 2, 'Linux 5.x Kernel (64-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'other5xLinux64Guest', 346, now(), 0);

-- Add support for other5xLinuxGuest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (347, UUID(), 2, 'Linux 5.x Kernel (32-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'other5xLinuxGuest', 347, now(), 0);

-- Add support for rhel9_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (348, UUID(), 4, 'Red Hat Enterprise Linux 9.0', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'rhel9_64Guest', 348, now(), 0);

-- Add support for sles16_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (349, UUID(), 5, 'SUSE Linux Enterprise Server 16 (64-bit)', now());
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'sles16_64Guest', 349, now(), 0);

-- Add support for windows2019srvNext_64Guest from VMware 7.0.1.0
INSERT INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(),'VMware', '7.0.1.0', 'windows2019srvNext_64Guest', 276, now(), 0);

-- Added support for deploy as is in list_templates view
DROP VIEW IF EXISTS `cloud`.`user_vm_view`;
CREATE
VIEW `cloud`.`user_vm_view` AS
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
        `vm_template`.`deploy_as_is` AS `template_deploy_as_is`,
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
