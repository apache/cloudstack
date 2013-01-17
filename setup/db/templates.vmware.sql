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

INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text,  format, guest_os_id, featured, cross_zones)
    VALUES (2, UUID(), 'blank', 'BlankVM', 1, now(), 'ext3', 0, 32, 1, 'http://nfs1.lab.vmops.com/templates/vmware/blankvm.tar.bz2', '3eff7ce3d25cf9433efde8b245c63fcb', 0, 'BlankVM', 'VMDK', 47, 1, 1);
INSERT INTO `cloud`.`vm_template` (id, uuid, unique_name, name, public, created, type, hvm, bits, account_id, url, checksum, enable_password, display_text,  format, guest_os_id, featured, cross_zones)
    VALUES (3, UUID(), 'winxpsp3', 'WindowsXP-SP3', 1, now(), 'ntfs', 0, 32, 1, 'http://nfs1.lab.vmops.com/templates/vmware/winxpsp3.tar.bz2', '385e67d17a2cb3795bd0b0fb7f88dc5e', 0, 'WindowsXP-SP3', 'VMDK', 16, 1, 1);

INSERT INTO `cloud`.`guest_os_category` (id, uuid, name) VALUES (1, UUID(), 'Windows');
INSERT INTO `cloud`.`guest_os_category` (id, uuid, name) VALUES (2, UUID(), 'Linux');
INSERT INTO `cloud`.`guest_os_category` (id, uuid, name) VALUES (3, UUID(), 'Novell Netware');
INSERT INTO `cloud`.`guest_os_category` (id, uuid, name) VALUES (4, UUID(), 'Solaris');
INSERT INTO `cloud`.`guest_os_category` (id, uuid, name) VALUES (5, UUID(), 'Other');

INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (1, UUID(), 1, 'Microsoft Windows 7(32-bit)', 'Microsoft Windows 7(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (2, UUID(), 1, 'Microsoft Windows 7(64-bit)', 'Microsoft Windows 7(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (3, UUID(), 1, 'Microsoft Windows Server 2008 R2(64-bit)', 'Microsoft Windows Server 2008 R2(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (4, UUID(), 1, 'Microsoft Windows Server 2008(32-bit)', 'Microsoft Windows Server 2008(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (5, UUID(), 1, 'Microsoft Windows Server 2008(64-bit)', 'Windows Windows Server 2008(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (6, UUID(), 1, 'Microsoft Windows Server 2003, Enterprise Edition (32-bit)', 'Microsoft Windows Server 2003, Enterprise Edition (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (7, UUID(), 1, 'Microsoft Windows Server 2003, Enterprise Edition (64-bit)', 'Microsoft Windows Server 2003, Enterprise Edition (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (8, UUID(), 1, 'Microsoft Windows Server 2003, Datacenter Edition (32-bit)', 'Microsoft Windows Server 2003, Datacenter Edition (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (9, UUID(), 1, 'Microsoft Windows Server 2003, Datacenter Edition (64-bit)', 'Microsoft Windows Server 2003, Datacenter Edition (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (10, UUID(), 1, 'Microsoft Windows Server 2003, Standard Edition (32-bit)', 'Microsoft Windows Server 2003, Standard Edition (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (11, UUID(), 1, 'Microsoft Windows Server 2003, Standard Edition (64-bit)', 'Microsoft Windows Server 2003, Standard Edition (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (12, UUID(), 1, 'Microsoft Windows Server 2003, Web Edition', 'Microsoft Windows Server 2003, Web Edition');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (13, UUID(), 1, 'Microsoft Small Bussiness Server 2003', 'Microsoft Small Bussiness Server 2003');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (14, UUID(), 1, 'Microsoft Windows Vista (32-bit)', 'Microsoft Windows Vista (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (15, UUID(), 1, 'Microsoft Windows Vista (64-bit)', 'Microsoft Windows Vista (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (16, UUID(), 1, 'Microsoft Windows XP Professional (32-bit)', 'Microsoft Windows XP Professional (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (17, UUID(), 1, 'Microsoft Windows XP Professional (64-bit)', 'Microsoft Windows XP Professional (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (18, UUID(), 1, 'Microsoft Windows 2000 Advanced Server', 'Microsoft Windows 2000 Advanced Server');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (19, UUID(), 1, 'Microsoft Windows 2000 Server', 'Microsoft Windows 2000 Server');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (20, UUID(), 1, 'Microsoft Windows 2000 Professional', 'Microsoft Windows 2000 Professional');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (21, UUID(), 1, 'Microsoft Windows 98', 'Microsoft Windows 98');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (22, UUID(), 1, 'Microsoft Windows 95', 'Microsoft Windows 95');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (23, UUID(), 1, 'Microsoft Windows NT 4', 'Microsoft Windows NT 4');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (24, UUID(), 1, 'Microsoft Windows 3.1', 'Microsoft Windows 3.1');

INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (25, UUID(), 2, 'Red Hat Enterprise Linux 5(32-bit)', 'Red Hat Enterprise Linux 5(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (26, UUID(), 2, 'Red Hat Enterprise Linux 5(64-bit)', 'Red Hat Enterprise Linux 5(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (27, UUID(), 2, 'Red Hat Enterprise Linux 4(32-bit)', 'Red Hat Enterprise Linux 4(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (28, UUID(), 2, 'Red Hat Enterprise Linux 4(64-bit)', 'Red Hat Enterprise Linux 4(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (29, UUID(), 2, 'Red Hat Enterprise Linux 3(32-bit)', 'Red Hat Enterprise Linux 3(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (30, UUID(), 2, 'Red Hat Enterprise Linux 3(64-bit)', 'Red Hat Enterprise Linux 3(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (31, UUID(), 2, 'Red Hat Enterprise Linux 2', 'Red Hat Enterprise Linux 2');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (32, UUID(), 2, 'Suse Linux Enterprise 11(32-bit)', 'Suse Linux Enterprise 11(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (33, UUID(), 2, 'Suse Linux Enterprise 11(64-bit)', 'Suse Linux Enterprise 11(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (34, UUID(), 2, 'Suse Linux Enterprise 10(32-bit)', 'Suse Linux Enterprise 10(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (35, UUID(), 2, 'Suse Linux Enterprise 10(64-bit)', 'Suse Linux Enterprise 10(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (36, UUID(), 2, 'Suse Linux Enterprise 8/9(32-bit)', 'Suse Linux Enterprise 8/9(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (37, UUID(), 2, 'Suse Linux Enterprise 8/9(64-bit)', 'Suse Linux Enterprise 8/9(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (38, UUID(), 2, 'Open Enterprise Server', 'Open Enterprise Server');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (39, UUID(), 2, 'Asianux 3(32-bit)', 'Asianux 3(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (40, UUID(), 2, 'Asianux 3(64-bit)', 'Asianux 3(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (41, UUID(), 2, 'Debian GNU/Linux 5(32-bit)', 'Debian GNU/Linux 5(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (42, UUID(), 2, 'Debian GNU/Linux 5(64-bit)', 'Debian GNU/Linux 5(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (43, UUID(), 2, 'Debian GNU/Linux 4(32-bit)', 'Debian GNU/Linux 4(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (44, UUID(), 2, 'Debian GNU/Linux 4(64-bit)', 'Debian GNU/Linux 4(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (45, UUID(), 2, 'Ubuntu Linux (32-bit)', 'Ubuntu Linux (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (46, UUID(), 2, 'Ubuntu Linux (64-bit)', 'Ubuntu Linux (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (47, UUID(), 2, 'Other 2.6x Linux (32-bit)', 'Other 2.6x Linux (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (48, UUID(), 2, 'Other 2.6x Linux (64-bit)', 'Other 2.6x Linux (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (49, UUID(), 2, 'Other Linux (32-bit)', 'Other Linux (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (50, UUID(), 2, 'Other Linux (64-bit)', 'Other Linux (64-bit)');

INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (51, UUID(), 3, 'Novell Netware 6.x', 'Novell Netware 6.x');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (52, UUID(), 3, 'Novell Netware 5.1', 'Novell Netware 5.1');

INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (53, UUID(), 4, 'Sun Solaris 10(32-bit)', 'Sun Solaris 10(32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (54, UUID(), 4, 'Sun Solaris 10(64-bit)', 'Sun Solaris 10(64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (55, UUID(), 4, 'Sun Solaris 9(Experimental)', 'Sun Solaris 9(Experimental)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (56, UUID(), 4, 'Sun Solaris 8(Experimental)', 'Sun Solaris 8(Experimental)');

INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (57, UUID(), 5, 'FreeBSD (32-bit)', 'FreeBSD (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (58, UUID(), 5, 'FreeBSD (64-bit)', 'FreeBSD (64-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (59, UUID(), 5, 'OS/2', 'OS/2');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (60, UUID(), 5, 'SCO OpenServer 5', 'SCO OpenServer 5');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (61, UUID(), 5, 'SCO UnixWare 7', 'SCO UnixWare 7');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (62, UUID(), 5, 'DOS', 'DOS');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (63, UUID(), 5, 'Other (32-bit)', 'Other (32-bit)');
INSERT INTO `cloud`.`guest_os` (id, uuid, category_id, name, display_name) VALUES (64, UUID(), 5, 'Other (64-bit)', 'Other (64-bit)');


-- temporarily added for vmware, will be moved when vmware support is fully in-place
INSERT INTO `cloud`.`host_master`(`type`, `service_address`, `admin`, `password`) VALUES('VSphere', 'vsphere-1.lab.vmops.com', 'Administrator', 'Suite219');
