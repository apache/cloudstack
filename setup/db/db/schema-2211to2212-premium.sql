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
-- Premium schema upgrade from 2.2.11 to 2.2.12;
--;

ALTER TABLE `cloud_usage`.`cloud_usage` ADD INDEX `i_cloud_usage__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`cloud_usage` ADD INDEX `i_cloud_usage__domain_id`(`domain_id`);
ALTER TABLE `cloud_usage`.`cloud_usage` ADD INDEX `i_cloud_usage__start_date`(`start_date`);
ALTER TABLE `cloud_usage`.`cloud_usage` ADD INDEX `i_cloud_usage__end_date`(`end_date`);

ALTER TABLE `cloud_usage`.`usage_vm_instance` ADD INDEX `i_usage_vm_instance__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_vm_instance` ADD INDEX `i_usage_vm_instance__start_date`(`start_date`);
ALTER TABLE `cloud_usage`.`usage_vm_instance` ADD INDEX `i_usage_vm_instance__end_date`(`end_date`);

ALTER TABLE `cloud_usage`.`usage_ip_address` ADD INDEX `i_usage_ip_address__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_ip_address` ADD INDEX `i_usage_ip_address__assigned`(`assigned`);
ALTER TABLE `cloud_usage`.`usage_ip_address` ADD INDEX `i_usage_ip_address__released`(`released`);

ALTER TABLE `cloud_usage`.`usage_job` ADD INDEX `i_usage_job__end_millis`(`end_millis`);

ALTER TABLE `cloud_usage`.`account` ADD INDEX `i_account__removed`(`removed`);

ALTER TABLE `cloud_usage`.`usage_volume` ADD INDEX `i_usage_volume__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_volume` ADD INDEX `i_usage_volume__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_volume` ADD INDEX `i_usage_volume__deleted`(`deleted`);

ALTER TABLE `cloud_usage`.`usage_storage` ADD INDEX `i_usage_storage__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_storage` ADD INDEX `i_usage_storage__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_storage` ADD INDEX `i_usage_storage__deleted`(`deleted`);

ALTER TABLE `cloud_usage`.`usage_load_balancer_policy` ADD INDEX `i_usage_load_balancer_policy__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_load_balancer_policy` ADD INDEX `i_usage_load_balancer_policy__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_load_balancer_policy` ADD INDEX `i_usage_load_balancer_policy__deleted`(`deleted`);

ALTER TABLE `cloud_usage`.`usage_port_forwarding` ADD INDEX `i_usage_port_forwarding__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_port_forwarding` ADD INDEX `i_usage_port_forwarding__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_port_forwarding` ADD INDEX `i_usage_port_forwarding__deleted`(`deleted`);

ALTER TABLE `cloud_usage`.`usage_network_offering` ADD INDEX `i_usage_network_offering__account_id`(`account_id`);
ALTER TABLE `cloud_usage`.`usage_network_offering` ADD INDEX `i_usage_network_offering__created`(`created`);
ALTER TABLE `cloud_usage`.`usage_network_offering` ADD INDEX `i_usage_network_offering__deleted`(`deleted`);

ALTER IGNORE TABLE  `cloud_usage`.`usage_vm_instance` ADD UNIQUE (`vm_instance_id`, `usage_type`, `start_date`);
ALTER IGNORE TABLE  `cloud_usage`.`usage_ip_address` ADD UNIQUE (`id`, `assigned`);
ALTER IGNORE TABLE  `cloud_usage`.`usage_volume` ADD UNIQUE (`id`, `created`);
ALTER IGNORE TABLE  `cloud_usage`.`usage_storage` ADD UNIQUE (`id`, `storage_type`, `zone_id`, `created`);
