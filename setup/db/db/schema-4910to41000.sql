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
-- Schema upgrade from 4.9.1.0 to 4.10.0.0;
--;

ALTER TABLE `cloud`.`domain_router` ADD COLUMN  update_state varchar(64) DEFAULT NULL;

INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (257, UUID(), 6, 'Windows 10 (32-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (258, UUID(), 6, 'Windows 10 (64-bit)', now());
INSERT IGNORE INTO `cloud`.`guest_os` (id, uuid, category_id, display_name, created) VALUES (259, UUID(), 6, 'Windows Server 2012 (64-bit)', now());

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, uuid, created) VALUES ('Xenserver', '6.5.0', 'Windows 10 (32-bit)', 257, UUID(), now());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, uuid, created) VALUES ('Xenserver', '6.5.0', 'Windows 10 (64-bit)', 258, UUID(), now());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, uuid, created) VALUES ('Xenserver', '6.5.0', 'Windows Server 2012 (64-bit)', 259, UUID(), now());

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created) VALUES (UUID(), 'VMware', '6.0', 'windows9Guest', 257, now());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created) VALUES (UUID(), 'VMware', '6.0', 'windows9_64Guest', 258, now());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created) VALUES (UUID(), 'VMware', '6.0', 'windows9Server64Guest', 259, now());

INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created) VALUES (UUID(), 'KVM', 'default', 'Windows 10', 257, now());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created) VALUES (UUID(), 'KVM', 'default', 'Windows 10', 258, now());
INSERT IGNORE INTO `cloud`.`guest_os_hypervisor` (uuid, hypervisor_type, hypervisor_version, guest_os_name, guest_os_id, created) VALUES (UUID(), 'KVM', 'default', 'Windows Server 2012', 259, now());