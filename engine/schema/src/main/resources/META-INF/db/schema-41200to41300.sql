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

-- Add display column for network offering details table
ALTER TABLE `cloud`.`network_offering_details` ADD COLUMN `display` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'True if the detail can be displayed to the end user';

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
