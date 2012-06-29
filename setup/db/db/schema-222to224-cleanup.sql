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


DROP TABLE IF EXISTS `cloud`.`host_master`;
DROP TABLE IF EXISTS `cloud`.`hypervsior_properties`;

ALTER TABLE `cloud`.`data_center` DROP COLUMN `enable`;
ALTER TABLE `cloud`.`host_pod_ref` DROP COLUMN `enabled`;
DELETE FROM `cloud`.`configuration` WHERE name in ('direct.attach.security.groups.enabled', 'direct.attach.untagged.vlan.enabled', 'hypervisor.type', 'management-server', 'max.volume.size.gb', 'multicast.throttling.rate', 'network.type', 'xen.preallocated.lun.size.range');

ALTER TABLE `cloud`.`volumes` DROP COLUMN `status`;
ALTER TABLE `cloud`.`volumes` DROP COLUMN `resource_type`;
ALTER TABLE `cloud`.`volumes` DROP COLUMN `source_id`;
ALTER TABLE `cloud`.`volumes` DROP COLUMN `source_type`;

ALTER TABLE `cloud`.`user_vm` DROP COLUMN guest_ip_address;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN guest_mac_address;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN guest_netmask;

ALTER TABLE `cloud`.`secondary_storage_vm` DROP KEY `public_ip_address_2`;
ALTER TABLE `cloud`.`console_proxy` DROP KEY `public_ip_address_2`;

ALTER TABLE `cloud`.`snapshot_policy` DROP KEY `volume_id`;
