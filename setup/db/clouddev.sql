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


UPDATE `cloud`.`configuration` SET value = 'true' where name = 'use.local.storage';
UPDATE `cloud`.`configuration` SET value = 'true' where name = 'system.vm.use.local.storage';
INSERT INTO `cloud`.`disk_offering` (id, name, uuid, display_text, created, use_local_storage, type) VALUES (17, 'tinyOffering', UUID(), 'tinyOffering', NOW(), 1, 'Service');
INSERT INTO `cloud`.`service_offering` (id, cpu, speed, ram_size) VALUES (17, 1, 100, 100);
INSERT INTO `cloud`.`disk_offering` (id, name, uuid, display_text, created, type, disk_size) VALUES (18, 'tinyDiskOffering', UUID(), 'tinyDiskOffering', NOW(), 'Disk', 1073741824);
INSERT INTO `cloud`.`configuration` (name,value) VALUE('router.ram.size', '100');
INSERT INTO `cloud`.`configuration` (name,value) VALUE('router.cpu.mhz','100');
INSERT INTO `cloud`.`configuration` (name,value) VALUE('console.ram.size','100');
INSERT INTO `cloud`.`configuration` (name,value) VALUE('console.cpu.mhz', '100');
INSERT INTO `cloud`.`configuration` (name,value) VALUE('ssvm.ram.size','100');
INSERT INTO `cloud`.`configuration` (name,value) VALUE('ssvm.cpu.mhz','100');
UPDATE `cloud`.`configuration` SET value='10' where name = 'storage.overprovisioning.factor';
UPDATE `cloud`.`configuration` SET value='10' where name = 'cpu.overprovisioning.factor';
UPDATE `cloud`.`configuration` SET value='10' where name = 'mem.overprovisioning.factor';
UPDATE `cloud`.`vm_template` SET unique_name="tiny Linux",name="tiny Linux",url="http://nfs1.lab.vmops.com/templates/ttylinux_pv.vhd",checksum="046e134e642e6d344b34648223ba4bc1",display_text="tiny Linux" where id=5;
