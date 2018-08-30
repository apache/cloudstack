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
-- Schema upgrade from 2.2.12 to 2.2.13;
--;

UPDATE networks SET guru_name='ExternalGuestNetworkGuru' WHERE guru_name='GuestNetworkGuru';
UPDATE nics SET reserver_name='ExternalGuestNetworkGuru' WHERE reserver_name='GuestNetworkGuru';
UPDATE configuration SET value='KVM,XenServer,VMware,BareMetal,Ovm' WHERE name='hypervisor.list';


INSERT IGNORE INTO guest_os(id, category_id, display_name) VALUES (200, 1, 'Other CentOS (32-bit)');
INSERT IGNORE INTO guest_os(id, category_id, display_name) VALUES (201, 1, 'Other CentOS (64-bit)');
INSERT IGNORE INTO guest_os(id, category_id, display_name) VALUES (202, 5, 'Other SUSE Linux(32-bit)');
INSERT IGNORE INTO guest_os(id, category_id, display_name) VALUES (203, 5, 'Other SUSE Linux(64-bit)');
INSERT IGNORE INTO guest_os(id, category_id, display_name) VALUES (141, 9, 'Sun Solaris 11 (64-bit)');
INSERT IGNORE INTO guest_os(id, category_id, display_name) VALUES (142, 9, 'Sun Solaris 11 (32-bit)');

INSERT IGNORE INTO guest_os_hypervisor(hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'CentOS (32-bit)', 200);
INSERT IGNORE INTO guest_os_hypervisor(hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'CentOS (64-bit)', 201);
INSERT IGNORE INTO guest_os_hypervisor(hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu 10.10 (32-bit)', 59);
INSERT IGNORE INTO guest_os_hypervisor(hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Ubuntu 10.10 (64-bit)', 100);
INSERT IGNORE INTO guest_os_hypervisor(hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 6(32-bit)', 204);
INSERT IGNORE INTO guest_os_hypervisor(hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Red Hat Enterprise Linux 6(64-bit)', 205);
INSERT IGNORE INTO guest_os_hypervisor(hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Other Suse Linux Enterprise(32-bit)', 202);
INSERT IGNORE INTO guest_os_hypervisor(hypervisor_type, guest_os_name, guest_os_id) VALUES ("VmWare", 'Other Suse Linux Enterprise(64-bit)', 203);

UPDATE guest_os_hypervisor SET guest_os_name='Other Ubuntu Linux (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=59;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 10.04 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=121;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 9.10 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=122;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 9.04 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=123;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 8.10 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=124;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 8.04 (32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=125;
UPDATE guest_os_hypervisor SET guest_os_name='Other Ubuntu (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=100;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 10.04 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=126;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 9.10 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=127;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 9.04 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=128;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 8.10 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=129;
UPDATE guest_os_hypervisor SET guest_os_name='Ubuntu 8.04 (64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=130;

UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.0(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=30;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.1(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=32;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.2(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=34;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.3(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=36;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.4(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=38;

UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.0(64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=31;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.1(64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=33;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.2(64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=35;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.3(64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=37;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 5.4(64-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=39;

UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 4.5(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=26;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 4.6(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=27;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 4.7(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=28;
UPDATE guest_os_hypervisor SET guest_os_name='Red Hat Enterprise Linux 4.8(32-bit)' WHERE hypervisor_type='VmWare' AND guest_os_id=29;

update host_details set name='cpuspeed' where host_id in (select id from host where hypervisor_type='BareMetal') and name='cpuCapacity';
update host_details set name='cpunumber' where host_id in (select id from host where hypervisor_type='BareMetal') and name='cpuNum';
update host_details set name='hostmac' where host_id in (select id from host where hypervisor_type='BareMetal') and name='mac';
update host_details set name='memory' where host_id in (select id from host where hypervisor_type='BareMetal') and name='memCapacity';
update host_details set name='privateip' where host_id in (select id from host where hypervisor_type='BareMetal') and name='agentIp';

INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'vmware.root.disk.controller', 'ide', 'Specify the default disk controller for root volumes, valid values are scsi, ide');
INSERT IGNORE INTO configuration VALUES ('Advanced', 'DEFAULT', 'management-server', 'vm.destory.forcestop', 'false', 'On destory, force-stop takes this value');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.lock.timeout', '600', 'Lock wait timeout (seconds) while implementing network');
INSERT IGNORE INTO configuration VALUES ('Network', 'DEFAULT', 'management-server', 'network.disable.rpfilter','true','disable rp_filter on Domain Router VM public interfaces.');

