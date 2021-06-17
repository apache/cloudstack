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

ALTER TABLE `cloud`.`s2s_customer_gateway` ADD COLUMN `ike_version` varchar(5) NOT NULL DEFAULT 'ike' COMMENT 'one of ike, ikev1, ikev2';
ALTER TABLE `cloud`.`s2s_customer_gateway` ADD COLUMN `split_connections` int(1) NOT NULL DEFAULT 0;

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
