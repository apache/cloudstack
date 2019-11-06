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
-- Schema upgrade from 4.12.0.0 to 4.13.0.0
--;

-- Add XenServer 7.1.2, 7.6 and 8.0 hypervisor capabilities
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) values (UUID(), 'XenServer', '7.6.0', 1000, 253, 64, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) values (UUID(), 'XenServer', '8.0.0', 1000, 253, 64, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) values (UUID(), 'XenServer', '7.1.1', 1000, 253, 64, 1);
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid, hypervisor_type, hypervisor_version, max_guests_limit, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported) values (UUID(), 'XenServer', '7.1.2', 1000, 253, 64, 1);

-- Add VMware 6.7 hypervisor capabilities
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid,hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) VALUES (UUID(), 'VMware', '6.7', '1024', '0', '59', '64', '1', '1');
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid,hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) VALUES (UUID(), 'VMware', '6.7.1', '1024', '0', '59', '64', '1', '1');
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid,hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) VALUES (UUID(), 'VMware', '6.7.2', '1024', '0', '59', '64', '1', '1');
INSERT IGNORE INTO `cloud`.`hypervisor_capabilities`(uuid,hypervisor_type, hypervisor_version, max_guests_limit, security_group_enabled, max_data_volumes_limit, max_hosts_per_cluster, storage_motion_supported, vm_snapshot_enabled) VALUES (UUID(), 'VMware', '6.7.3', '1024', '0', '59', '64', '1', '1');

-- Update VMware 6.x hypervisor capabilities
UPDATE `cloud`.`hypervisor_capabilities` SET max_guests_limit='1024', max_data_volumes_limit='59', max_hosts_per_cluster='64' WHERE (hypervisor_type='VMware' AND hypervisor_version='6.0' );
UPDATE `cloud`.`hypervisor_capabilities` SET max_guests_limit='1024', max_data_volumes_limit='59', max_hosts_per_cluster='64' WHERE (hypervisor_type='VMware' AND hypervisor_version='6.5' );

-- Add new OS versions
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('277', UUID(), '1', 'Ubuntu 17.04', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('278', UUID(), '1', 'Ubuntu 17.10', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('279', UUID(), '1', 'Ubuntu 18.04 LTS', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('280', UUID(), '1', 'Ubuntu 18.10', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('281', UUID(), '1', 'Ubuntu 19.04', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('282', UUID(), '1', 'Red Hat Enterprise Linux 7.3', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('283', UUID(), '1', 'Red Hat Enterprise Linux 7.4', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('284', UUID(), '1', 'Red Hat Enterprise Linux 7.5', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('285', UUID(), '1', 'Red Hat Enterprise Linux 7.6', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('286', UUID(), '1', 'Red Hat Enterprise Linux 8.0', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('289', UUID(), '2', 'Debian GNU/Linux 9 (32-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('290', UUID(), '2', 'Debian GNU/Linux 9 (64-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('291', UUID(), '5', 'SUSE Linux Enterprise Server 15 (64-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('292', UUID(), '2', 'Debian GNU/Linux 10 (32-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('293', UUID(), '2', 'Debian GNU/Linux 10 (64-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('294', UUID(), '2', 'Linux 4.x Kernel (32-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('295', UUID(), '2', 'Linux 4.x Kernel (64-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('296', UUID(), '3', 'Oracle Linux 8', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('297', UUID(), '1', 'CentOS 8', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('298', UUID(), '9', 'FreeBSD 11 (32-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('299', UUID(), '9', 'FreeBSD 11 (64-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('300', UUID(), '9', 'FreeBSD 12 (32-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('301', UUID(), '9', 'FreeBSD 12 (64-bit)', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('302', UUID(), '1', 'CentOS 6.8', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('303', UUID(), '1', 'CentOS 6.9', now(), '0');
INSERT INTO cloud.guest_os (id, uuid, category_id, display_name, created, is_user_defined) VALUES ('304', UUID(), '1', 'CentOS 6.10', now(), '0');

-- Add New and missing VMware 6.5 Guest OSes
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6Guest', 235, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6_64Guest', 236, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6Guest', 147, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6_64Guest', 148, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6Guest', 213, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6_64Guest', 214, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6Guest', 215, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6_64Guest', 216, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6Guest', 217, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6_64Guest', 218, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6Guest', 219, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6_64Guest', 220, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6Guest', 250, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux6_64Guest', 251, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux7_64Guest', 247, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'ubuntuGuest', 255, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'ubuntu64Guest', 256, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'ubuntu64Guest', 277, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'ubuntu64Guest', 278, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'ubuntu64Guest', 279, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'ubuntu64Guest', 280, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'rhel7_64Guest', 282, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'rhel7_64Guest', 283, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'rhel7_64Guest', 284, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'rhel7_64Guest', 285, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'windows9Server64Guest', 276, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'debian9Guest', 289, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'debian9_64Guest', 290, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'debian10Guest', 282, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'debian10_64Guest', 293, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'sles15_64Guest', 291, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'centos6_64Guest', 302, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'centos6_64Guest', 303, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'centos6_64Guest', 304, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'rhel8_64Guest', 286, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'ubuntu64Guest', 281, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'other4xLinuxGuest', 294, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'other4xLinux64Guest', 295, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'oracleLinux8_64Guest', 296, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'centos8_64Guest', 297, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'freebsd11Guest', 298, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'freebsd11_64Guest', 299, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'freebsd12Guest', 300, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'VMware', '6.5', 'freebsd12_64Guest', 301, now(), 0);

-- Copy VMware 6.5 Guest OSes to VMware 6.7
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '6.7', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='6.5';
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '6.7.1', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='6.7';
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '6.7.2', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='6.7.1';
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'VMware', '6.7.3', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='VMware' AND hypervisor_version='6.7.2';

-- Copy XenServer 7.1.0 to XenServer 7.1.1
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.1.1', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.1.0';

-- Copy XenServer 7.1.1 to XenServer 7.1.2
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.1.2', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.1.1';

-- Add New XenServer 7.1.2 Guest OSes
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.2', 'Debian Stretch 9.0', 289, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.2', 'Debian Stretch 9.0', 290, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.2', 'Ubuntu Bionic Beaver 18.04', 279, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.2', 'Windows Server 2019 (64-bit)', 276, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.2', 'CentOS 6 (64-bit', 303, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.2', 'CentOS 7', 283, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.1.2', 'CentOS 7', 284, now(), 0);
-- Copy XenServer 7.5 hypervisor guest OS mappings to XenServer 7.6
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '7.6.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.5.0';

-- Add New XenServer 7.6 Guest OSes
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.6.0', 'Debian Jessie 8.0', 269, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.6.0', 'Debian Jessie 8.0', 270, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.6.0', 'Debian Stretch 9.0', 289, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.6.0', 'Debian Stretch 9.0', 290, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.6.0', 'Ubuntu Xenial Xerus 16.04', 255, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.6.0', 'Ubuntu Xenial Xerus 16.04', 256, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '7.6.0', 'Ubuntu Bionic Beaver 18.04', 279, now(), 0);

-- Copy XenServer 7.6 hypervisor guest OS mappings to XenServer8.0
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid,hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) SELECT UUID(),'Xenserver', '8.0.0', guest_os_name, guest_os_id, utc_timestamp(), 0  FROM `cloud`.`guest_os_hypervisor` WHERE hypervisor_type='Xenserver' AND hypervisor_version='7.6.0';

-- Add New XenServer 8.0 Guest OSes
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'Xenserver', '8.0.0', 'Windows Server 2019 (64-bit)', 276, now(), 0);

-- Add Missing KVM Guest OSes
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'CentOS 6.6', 262, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'CentOS 6.7', 263, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'CentOS 6.7', 264, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'CentOS 6.8', 302, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'CentOS 6.9', 303, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'CentOS 6.10', 304, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Red Hat Enterprise Linux 7.2', 269, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Red Hat Enterprise Linux 7.3', 282, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Red Hat Enterprise Linux 7.4', 283, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Red Hat Enterprise Linux 7.5', 284, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Red Hat Enterprise Linux 7.6', 285, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Red Hat Enterprise Linux 8', 286, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Ubuntu 17.04', 277, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Ubuntu 17.10', 278, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Ubuntu 18.04 LTS', 279, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Ubuntu 18.10', 280, now(), 0);
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created, is_user_defined) VALUES (UUID(), 'KVM', 'default', 'Ubuntu 19.04', 281, now(), 0);

-- DPDK client and server mode support
ALTER TABLE `cloud`.`service_offering_details` CHANGE COLUMN `value` `value` TEXT NOT NULL;

ALTER TABLE `cloud`.`vpc_offerings` ADD COLUMN `sort_key` int(32) NOT NULL default 0 COMMENT 'sort key used for customising sort method';

-- Add `sort_key` column to data_center
ALTER TABLE `cloud`.`data_center` ADD COLUMN `sort_key` INT(32) NOT NULL DEFAULT 0;

-- Move domain_id to disk offering details and drop the domain_id column
INSERT INTO `cloud`.`disk_offering_details` (offering_id, name, value, display) SELECT id, 'domainid', domain_id, 0 FROM `cloud`.`disk_offering` WHERE domain_id IS NOT NULL AND type='Disk';
INSERT INTO `cloud`.`service_offering_details` (service_offering_id, name, value, display) SELECT id, 'domainid', domain_id, 0 FROM `cloud`.`disk_offering` WHERE domain_id IS NOT NULL AND type='Service';

ALTER TABLE `cloud`.`disk_offering` DROP COLUMN `domain_id`;

ALTER TABLE `cloud`.`service_offering_details` DROP FOREIGN KEY `fk_service_offering_details__service_offering_id`, DROP KEY `uk_service_offering_id_name`;
ALTER TABLE `cloud`.`service_offering_details` ADD CONSTRAINT `fk_service_offering_details__service_offering_id` FOREIGN KEY (`service_offering_id`) REFERENCES `service_offering`(`id`) ON DELETE CASCADE;

-- Disk offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`disk_offering_view`;
CREATE VIEW `cloud`.`disk_offering_view` AS
    SELECT
        `disk_offering`.`id` AS `id`,
        `disk_offering`.`uuid` AS `uuid`,
        `disk_offering`.`name` AS `name`,
        `disk_offering`.`display_text` AS `display_text`,
        `disk_offering`.`provisioning_type` AS `provisioning_type`,
        `disk_offering`.`disk_size` AS `disk_size`,
        `disk_offering`.`min_iops` AS `min_iops`,
        `disk_offering`.`max_iops` AS `max_iops`,
        `disk_offering`.`created` AS `created`,
        `disk_offering`.`tags` AS `tags`,
        `disk_offering`.`customized` AS `customized`,
        `disk_offering`.`customized_iops` AS `customized_iops`,
        `disk_offering`.`removed` AS `removed`,
        `disk_offering`.`use_local_storage` AS `use_local_storage`,
        `disk_offering`.`system_use` AS `system_use`,
        `disk_offering`.`hv_ss_reserve` AS `hv_ss_reserve`,
        `disk_offering`.`bytes_read_rate` AS `bytes_read_rate`,
        `disk_offering`.`bytes_read_rate_max` AS `bytes_read_rate_max`,
        `disk_offering`.`bytes_read_rate_max_length` AS `bytes_read_rate_max_length`,
        `disk_offering`.`bytes_write_rate` AS `bytes_write_rate`,
        `disk_offering`.`bytes_write_rate_max` AS `bytes_write_rate_max`,
        `disk_offering`.`bytes_write_rate_max_length` AS `bytes_write_rate_max_length`,
        `disk_offering`.`iops_read_rate` AS `iops_read_rate`,
        `disk_offering`.`iops_read_rate_max` AS `iops_read_rate_max`,
        `disk_offering`.`iops_read_rate_max_length` AS `iops_read_rate_max_length`,
        `disk_offering`.`iops_write_rate` AS `iops_write_rate`,
        `disk_offering`.`iops_write_rate_max` AS `iops_write_rate_max`,
        `disk_offering`.`iops_write_rate_max_length` AS `iops_write_rate_max_length`,
        `disk_offering`.`cache_mode` AS `cache_mode`,
        `disk_offering`.`sort_key` AS `sort_key`,
        `disk_offering`.`type` AS `type`,
        `disk_offering`.`display_offering` AS `display_offering`,
        `disk_offering`.`state`  AS `state`,
        GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
        GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
        GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
        GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
        GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
        GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
        GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name
    FROM
        `cloud`.`disk_offering`
            LEFT JOIN
        `cloud`.`disk_offering_details` AS `domain_details` ON `domain_details`.`offering_id` = `disk_offering`.`id` AND `domain_details`.`name`='domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`disk_offering_details` AS `zone_details` ON `zone_details`.`offering_id` = `disk_offering`.`id` AND `zone_details`.`name`='zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
    WHERE
        `disk_offering`.`state`='Active'
    GROUP BY
        `disk_offering`.`id`;

-- Service offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`service_offering_view`;
CREATE VIEW `cloud`.`service_offering_view` AS
    SELECT
        `service_offering`.`id` AS `id`,
        `disk_offering`.`uuid` AS `uuid`,
        `disk_offering`.`name` AS `name`,
        `disk_offering`.`display_text` AS `display_text`,
        `disk_offering`.`provisioning_type` AS `provisioning_type`,
        `disk_offering`.`created` AS `created`,
        `disk_offering`.`tags` AS `tags`,
        `disk_offering`.`removed` AS `removed`,
        `disk_offering`.`use_local_storage` AS `use_local_storage`,
        `disk_offering`.`system_use` AS `system_use`,
        `disk_offering`.`customized_iops` AS `customized_iops`,
        `disk_offering`.`min_iops` AS `min_iops`,
        `disk_offering`.`max_iops` AS `max_iops`,
        `disk_offering`.`hv_ss_reserve` AS `hv_ss_reserve`,
        `disk_offering`.`bytes_read_rate` AS `bytes_read_rate`,
        `disk_offering`.`bytes_read_rate_max` AS `bytes_read_rate_max`,
        `disk_offering`.`bytes_read_rate_max_length` AS `bytes_read_rate_max_length`,
        `disk_offering`.`bytes_write_rate` AS `bytes_write_rate`,
        `disk_offering`.`bytes_write_rate_max` AS `bytes_write_rate_max`,
        `disk_offering`.`bytes_write_rate_max_length` AS `bytes_write_rate_max_length`,
        `disk_offering`.`iops_read_rate` AS `iops_read_rate`,
        `disk_offering`.`iops_read_rate_max` AS `iops_read_rate_max`,
        `disk_offering`.`iops_read_rate_max_length` AS `iops_read_rate_max_length`,
        `disk_offering`.`iops_write_rate` AS `iops_write_rate`,
        `disk_offering`.`iops_write_rate_max` AS `iops_write_rate_max`,
        `disk_offering`.`iops_write_rate_max_length` AS `iops_write_rate_max_length`,
        `disk_offering`.`cache_mode` AS `cache_mode`,
        `service_offering`.`cpu` AS `cpu`,
        `service_offering`.`speed` AS `speed`,
        `service_offering`.`ram_size` AS `ram_size`,
        `service_offering`.`nw_rate` AS `nw_rate`,
        `service_offering`.`mc_rate` AS `mc_rate`,
        `service_offering`.`ha_enabled` AS `ha_enabled`,
        `service_offering`.`limit_cpu_use` AS `limit_cpu_use`,
        `service_offering`.`host_tag` AS `host_tag`,
        `service_offering`.`default_use` AS `default_use`,
        `service_offering`.`vm_type` AS `vm_type`,
        `service_offering`.`sort_key` AS `sort_key`,
        `service_offering`.`is_volatile` AS `is_volatile`,
        `service_offering`.`deployment_planner` AS `deployment_planner`,
        GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
        GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
        GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
        GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
        GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
        GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
        GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name
    FROM
        `cloud`.`service_offering`
            INNER JOIN
        `cloud`.`disk_offering_view` AS `disk_offering` ON service_offering.id = disk_offering.id
            LEFT JOIN
        `cloud`.`service_offering_details` AS `domain_details` ON `domain_details`.`service_offering_id` = `disk_offering`.`id` AND `domain_details`.`name`='domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`service_offering_details` AS `zone_details` ON `zone_details`.`service_offering_id` = `disk_offering`.`id` AND `zone_details`.`name`='zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
    WHERE
        `disk_offering`.`state`='Active'
    GROUP BY
        `service_offering`.`id`;

-- Add display column for network offering details table
ALTER TABLE `cloud`.`network_offering_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user';

-- Network offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`network_offering_view`;
CREATE VIEW `cloud`.`network_offering_view` AS
    SELECT
        `network_offerings`.`id` AS `id`,
        `network_offerings`.`uuid` AS `uuid`,
        `network_offerings`.`name` AS `name`,
        `network_offerings`.`unique_name` AS `unique_name`,
        `network_offerings`.`display_text` AS `display_text`,
        `network_offerings`.`nw_rate` AS `nw_rate`,
        `network_offerings`.`mc_rate` AS `mc_rate`,
        `network_offerings`.`traffic_type` AS `traffic_type`,
        `network_offerings`.`tags` AS `tags`,
        `network_offerings`.`system_only` AS `system_only`,
        `network_offerings`.`specify_vlan` AS `specify_vlan`,
        `network_offerings`.`service_offering_id` AS `service_offering_id`,
        `network_offerings`.`conserve_mode` AS `conserve_mode`,
        `network_offerings`.`created` AS `created`,
        `network_offerings`.`removed` AS `removed`,
        `network_offerings`.`default` AS `default`,
        `network_offerings`.`availability` AS `availability`,
        `network_offerings`.`dedicated_lb_service` AS `dedicated_lb_service`,
        `network_offerings`.`shared_source_nat_service` AS `shared_source_nat_service`,
        `network_offerings`.`sort_key` AS `sort_key`,
        `network_offerings`.`redundant_router_service` AS `redundant_router_service`,
        `network_offerings`.`state` AS `state`,
        `network_offerings`.`guest_type` AS `guest_type`,
        `network_offerings`.`elastic_ip_service` AS `elastic_ip_service`,
        `network_offerings`.`eip_associate_public_ip` AS `eip_associate_public_ip`,
        `network_offerings`.`elastic_lb_service` AS `elastic_lb_service`,
        `network_offerings`.`specify_ip_ranges` AS `specify_ip_ranges`,
        `network_offerings`.`inline` AS `inline`,
        `network_offerings`.`is_persistent` AS `is_persistent`,
        `network_offerings`.`internal_lb` AS `internal_lb`,
        `network_offerings`.`public_lb` AS `public_lb`,
        `network_offerings`.`egress_default_policy` AS `egress_default_policy`,
        `network_offerings`.`concurrent_connections` AS `concurrent_connections`,
        `network_offerings`.`keep_alive_enabled` AS `keep_alive_enabled`,
        `network_offerings`.`supports_streched_l2` AS `supports_streched_l2`,
        `network_offerings`.`supports_public_access` AS `supports_public_access`,
        `network_offerings`.`for_vpc` AS `for_vpc`,
        `network_offerings`.`service_package_id` AS `service_package_id`,
        GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
        GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
        GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
        GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
        GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
        GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
        GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name
    FROM
        `cloud`.`network_offerings`
            LEFT JOIN
        `cloud`.`network_offering_details` AS `domain_details` ON `domain_details`.`network_offering_id` = `network_offerings`.`id` AND `domain_details`.`name`='domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`network_offering_details` AS `zone_details` ON `zone_details`.`network_offering_id` = `network_offerings`.`id` AND `zone_details`.`name`='zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
    GROUP BY
        `network_offerings`.`id`;

-- Create VPC offering details table
CREATE TABLE `vpc_offering_details` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `offering_id` bigint(20) unsigned NOT NULL COMMENT 'vpc offering id',
  `name` varchar(255) NOT NULL,
  `value` varchar(1024) NOT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user',
  PRIMARY KEY (`id`),
  KEY `fk_vpc_offering_details__vpc_offering_id` (`offering_id`),
  CONSTRAINT `fk_vpc_offering_details__vpc_offering_id` FOREIGN KEY (`offering_id`) REFERENCES `vpc_offerings` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- VPC offering with multi-domains and multi-zones
DROP VIEW IF EXISTS `cloud`.`vpc_offering_view`;
CREATE VIEW `cloud`.`vpc_offering_view` AS
    SELECT
        `vpc_offerings`.`id` AS `id`,
        `vpc_offerings`.`uuid` AS `uuid`,
        `vpc_offerings`.`name` AS `name`,
        `vpc_offerings`.`unique_name` AS `unique_name`,
        `vpc_offerings`.`display_text` AS `display_text`,
        `vpc_offerings`.`state` AS `state`,
        `vpc_offerings`.`default` AS `default`,
        `vpc_offerings`.`created` AS `created`,
        `vpc_offerings`.`removed` AS `removed`,
        `vpc_offerings`.`service_offering_id` AS `service_offering_id`,
        `vpc_offerings`.`supports_distributed_router` AS `supports_distributed_router`,
        `vpc_offerings`.`supports_region_level_vpc` AS `supports_region_level_vpc`,
        `vpc_offerings`.`redundant_router_service` AS `redundant_router_service`,
        `vpc_offerings`.`sort_key` AS `sort_key`,
        GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
        GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
        GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
        GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
        GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
        GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
        GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name
    FROM
        `cloud`.`vpc_offerings`
            LEFT JOIN
        `cloud`.`vpc_offering_details` AS `domain_details` ON `domain_details`.`offering_id` = `vpc_offerings`.`id` AND `domain_details`.`name`='domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`vpc_offering_details` AS `zone_details` ON `zone_details`.`offering_id` = `vpc_offerings`.`id` AND `zone_details`.`name`='zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
    GROUP BY
        `vpc_offerings`.`id`;

-- Recreate data_center_view
DROP VIEW IF EXISTS `cloud`.`data_center_view`;
CREATE VIEW `cloud`.`data_center_view` AS
    select
        data_center.id,
        data_center.uuid,
        data_center.name,
        data_center.is_security_group_enabled,
        data_center.is_local_storage_enabled,
        data_center.description,
        data_center.dns1,
        data_center.dns2,
        data_center.ip6_dns1,
        data_center.ip6_dns2,
        data_center.internal_dns1,
        data_center.internal_dns2,
        data_center.guest_network_cidr,
        data_center.domain,
        data_center.networktype,
        data_center.allocation_state,
        data_center.zone_token,
        data_center.dhcp_provider,
        data_center.removed,
        data_center.sort_key,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        dedicated_resources.affinity_group_id,
        dedicated_resources.account_id,
        affinity_group.uuid affinity_group_uuid
    from
        `cloud`.`data_center`
            left join
        `cloud`.`domain` ON data_center.domain_id = domain.id
            left join
        `cloud`.`dedicated_resources` ON data_center.id = dedicated_resources.data_center_id
            left join
        `cloud`.`affinity_group` ON dedicated_resources.affinity_group_id = affinity_group.id;

-- Remove key/value tags from project_view
DROP VIEW IF EXISTS `cloud`.`project_view`;
CREATE VIEW `cloud`.`project_view` AS
    select
        projects.id,
        projects.uuid,
        projects.name,
        projects.display_text,
        projects.state,
        projects.removed,
        projects.created,
        projects.project_account_id,
        account.account_name owner,
        pacct.account_id,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`projects`
            inner join
        `cloud`.`domain` ON projects.domain_id = domain.id
            inner join
        `cloud`.`project_account` ON projects.id = project_account.project_id
            and project_account.account_role = 'Admin'
            inner join
        `cloud`.`account` ON account.id = project_account.account_id
            left join
        `cloud`.`project_account` pacct ON projects.id = pacct.project_id;

-- KVM: Add background task to upload certificates for direct download
CREATE TABLE `cloud`.`direct_download_certificate` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(40) NOT NULL,
  `alias` varchar(255) NOT NULL,
  `certificate` text NOT NULL,
  `hypervisor_type` varchar(45) NOT NULL,
  `zone_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `i_direct_download_certificate_alias` (`alias`),
  KEY `fk_direct_download_certificate__zone_id` (`zone_id`),
  CONSTRAINT `fk_direct_download_certificate__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`direct_download_certificate_host_map` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `certificate_id` bigint(20) unsigned NOT NULL,
  `host_id` bigint(20) unsigned NOT NULL,
  `revoked` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `fk_direct_download_certificate_host_map__host_id` (`host_id`),
  KEY `fk_direct_download_certificate_host_map__certificate_id` (`certificate_id`),
  CONSTRAINT `fk_direct_download_certificate_host_map__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_direct_download_certificate_host_map__certificate_id` FOREIGN KEY (`certificate_id`) REFERENCES `direct_download_certificate` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- [Vmware] Allow configuring appliances on the VM instance wizard when OVF properties are available
CREATE TABLE `cloud`.`template_ovf_properties` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `template_id` bigint(20) unsigned NOT NULL,
  `key` VARCHAR(100) NOT NULL,
  `type` VARCHAR(45) DEFAULT NULL,
  `value` VARCHAR(100) DEFAULT NULL,
  `password` TINYINT(1) NOT NULL DEFAULT '0',
  `qualifiers` TEXT DEFAULT NULL,
  `user_configurable` TINYINT(1) NOT NULL DEFAULT '0',
  `label` TEXT DEFAULT NULL,
  `description` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_template_ovf_properties__template_id` FOREIGN KEY (`template_id`) REFERENCES `vm_template`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Add VM snapshot ID on usage helper tables
ALTER TABLE `cloud_usage`.`usage_vmsnapshot` ADD COLUMN `vm_snapshot_id` BIGINT(20) NULL DEFAULT NULL AFTER `processed`;
ALTER TABLE `cloud_usage`.`usage_snapshot_on_primary` ADD COLUMN `vm_snapshot_id` BIGINT(20) NULL DEFAULT NULL AFTER `deleted`;
