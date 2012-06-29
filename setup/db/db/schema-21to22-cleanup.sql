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

-- First the easy stuff.  Drop useless tables
DROP TABLE IF EXISTS `cloud`.`ext_lun_details`;
DROP TABLE IF EXISTS `cloud`.`ext_lun_alloc`;
DROP TABLE IF EXISTS `cloud`.`disk_template_ref`;

-- Then remove columns

DROP TABLE `cloud`.`ip_forwarding`;

ALTER TABLE `cloud`.`host` DROP COLUMN `sequence`;

DROP TABLE `cloud`.`op_vm_host`;

ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `iso_id`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `display_name`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `group`;
ALTER TABLE `cloud`.`vm_instance` DROP COLUMN `storage_ip`;

DROP TABLE `cloud`.`pricing`;

ALTER TABLE `cloud`.`user_vm` DROP FOREIGN KEY `fk_user_vm__service_offering_id`;
ALTER TABLE `cloud`.`user_vm` DROP INDEX `i_user_vm__service_offering_id`;
ALTER TABLE `cloud`.`user_vm` DROP FOREIGN KEY `fk_user_vm__account_id`;
ALTER TABLE `cloud`.`user_vm` DROP INDEX `i_user_vm__account_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `service_offering_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `account_id`;
ALTER TABLE `cloud`.`user_vm` DROP COLUMN `domain_id`;

#ALTER TABLE `cloud`.`secondary_storage_vm` DROP COLUMN `guid`;

#ALTER TABLE `cloud`.`vlan` ADD CONSTRAINT `fk_vlan__network_id` FOREIGN KEY `fk_vlan__network_id`(`network_id`) REFERENCES `networks`(`id`);

DROP TABLE IF EXISTS `cloud`.`vm_disk`;

ALTER TABLE `cloud`.`load_balancer_vm_map` DROP COLUMN `pending`;

ALTER TABLE `cloud`.`account_vlan_map` MODIFY COLUMN `account_id` bigint unsigned NOT NULL;

ALTER TABLE `cloud`.`load_balancer_vm_map` ADD UNIQUE KEY (`load_balancer_id`, `instance_id`);
ALTER TABLE `cloud`.`load_balancer_vm_map` ADD CONSTRAINT `fk_load_balancer_vm_map__load_balancer_id` FOREIGN KEY(`load_balancer_id`) REFERENCES `load_balancing_rules`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`load_balancer_vm_map` ADD CONSTRAINT `fk_load_balancer_vm_map__instance_id` FOREIGN KEY(`instance_id`) REFERENCES `vm_instance`(`id`) ON DELETE CASCADE;


ALTER TABLE `cloud`.`user_ip_address` DROP PRIMARY KEY;
ALTER TABLE `cloud`.`user_ip_address` ADD PRIMARY KEY (`id`);
ALTER TABLE `cloud`.`user_ip_address` DROP INDEX public_ip_address;
#ALTER TABLE `cloud`.`user_ip_address` DROP KEY `i_user_ip_address__public_ip_address`;
ALTER TABLE `cloud`.`user_ip_address` ADD UNIQUE (`public_ip_address`, `source_network_id`);
ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__source_network_id` FOREIGN KEY (`source_network_id`) REFERENCES `networks`(`id`);
ALTER TABLE `cloud`.`user_ip_address` ADD CONSTRAINT `fk_user_ip_address__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`);

ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__service_offering_id` FOREIGN KEY `fk_vm_instance__service_offering_id` (`service_offering_id`) REFERENCES `service_offering` (`id`);

ALTER TABLE `cloud`.`volumes` MODIFY COLUMN `state` VARCHAR(32) NOT NULL;

ALTER TABLE `cloud`.`snapshot_policy` ADD KEY  `volume_id` (`volume_id`);

DELETE FROM op_ha_work WHERE taken IS NOT NULL;
DELETE FROM op_ha_work WHERE host_id NOT IN (SELECT DISTINCT id FROM host);

UPDATE `cloud`.`vm_instance` SET last_host_id=NULL WHERE last_host_id NOT IN (SELECT DISTINCT id FROM host);

UPDATE `cloud`.`vm_instance` SET domain_id=1, account_id=1 where account_id not in (select distinct id from account) or domain_id not in (select distinct id from domain);
ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__account_id` FOREIGN KEY `fk_vm_instance__account_id` (`account_id`) REFERENCES `account` (`id`);


