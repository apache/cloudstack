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

--
-- INDEX/CONSTRAINT upgrade from 2.0 to 2.1
--

ALTER TABLE `cloud`.`host` ADD CONSTRAINT `fk_host__cluster_id` FOREIGN KEY `fk_host__cluster_id`(`cluster_id`) REFERENCES `cloud`.`cluster`(`id`);

ALTER TABLE `cloud`.`storage_pool` ADD CONSTRAINT `fk_storage_pool__cluster_id` FOREIGN KEY `fk_storage_pool__cluster_id`(`cluster_id`) REFERENCES `cloud`.`cluster`(`id`);

ALTER TABLE `cloud`.`storage_pool_details` ADD CONSTRAINT `fk_storage_pool_details__pool_id` FOREIGN KEY `fk_storage_pool__pool_id`(`pool_id`) REFERENCES `storage_pool`(`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`storage_pool_details` ADD INDEX `i_storage_pool_details__name__value`(`name`, `value`);

ALTER TABLE `cloud`.`ext_lun_alloc` ADD UNIQUE `i_ext_lun_alloc__target_iqn__lun`(`target_iqn`, `lun`);
ALTER TABLE `cloud`.`ext_lun_details` ADD CONSTRAINT `fk_ext_lun_details__ext_lun_id` FOREIGN KEY `fk_ext_lun_details__ext_lun_id`(`ext_lun_id`) REFERENCES `ext_lun_alloc`(`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`cluster` ADD CONSTRAINT `fk_cluster__data_center_id` FOREIGN KEY `fk_cluster__data_center_id`(`data_center_id`) REFERENCES `cloud`.`data_center`(`id`);
ALTER TABLE `cloud`.`cluster` ADD CONSTRAINT `fk_cluster__pod_id` FOREIGN KEY `fd_cluster__pod_id`(`pod_id`) REFERENCES `cloud`.`host_pod_ref`(`id`);
ALTER TABLE `cloud`.`cluster` ADD UNIQUE `i_cluster__pod_id__name`(`pod_id`, `name`);

ALTER TABLE `cloud`.`account_vlan_map` ADD CONSTRAINT `fk_account_vlan_map__account_id` FOREIGN KEY `fk_account_vlan_map__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`account_vlan_map` ADD INDEX `i_account_vlan_map__account_id`(`account_id`);
ALTER TABLE `cloud`.`account_vlan_map` ADD CONSTRAINT `fk_account_vlan_map__vlan_id` FOREIGN KEY `fk_account_vlan_map__vlan_id` (`vlan_db_id`) REFERENCES `vlan` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`account_vlan_map` ADD INDEX `i_account_vlan_map__vlan_id`(`vlan_db_id`);

ALTER TABLE `cloud`.`network_group` ADD CONSTRAINT `fk_network_group___account_id` FOREIGN KEY `fk_network_group__account_id` (`account_id`) REFERENCES `account` (`id`);
ALTER TABLE `cloud`.`network_group` ADD CONSTRAINT `fk_network_group__domain_id` FOREIGN KEY `fk_network_group__domain_id` (`domain_id`) REFERENCES `domain` (`id`);
ALTER TABLE `cloud`.`network_group` ADD INDEX `i_network_group_name`(`name`);

ALTER TABLE `cloud`.`network_ingress_rule` ADD CONSTRAINT `fk_network_ingress_rule___network_group_id` FOREIGN KEY `fk_network_ingress_rule__network_group_id` (`network_group_id`) REFERENCES `network_group` (`id`);
ALTER TABLE `cloud`.`network_ingress_rule` ADD CONSTRAINT `fk_network_ingress_rule___allowed_network_id` FOREIGN KEY `fk_network_ingress_rule__allowed_network_id` (`allowed_network_id`) REFERENCES `network_group` (`id`);
ALTER TABLE `cloud`.`network_ingress_rule` ADD INDEX `i_network_ingress_rule_network_id`(`network_group_id`);
ALTER TABLE `cloud`.`network_ingress_rule` ADD INDEX `i_network_ingress_rule_allowed_network`(`allowed_network_id`);

ALTER TABLE `cloud`.`network_group_vm_map` ADD CONSTRAINT `fk_network_group_vm_map___network_group_id` FOREIGN KEY `fk_network_group_vm_map___network_group_id` (`network_group_id`) REFERENCES `network_group` (`id`) ON DELETE CASCADE;
ALTER TABLE `cloud`.`network_group_vm_map` ADD CONSTRAINT `fk_network_group_vm_map___instance_id` FOREIGN KEY `fk_network_group_vm_map___instance_id` (`instance_id`) REFERENCES `user_vm` (`id`) ON DELETE CASCADE;

ALTER TABLE `cloud`.`op_nwgrp_work` ADD INDEX `i_op_nwgrp_work__instance_id`(`instance_id`);
ALTER TABLE `cloud`.`op_nwgrp_work` ADD INDEX `i_op_nwgrp_work__mgmt_server_id`(`mgmt_server_id`);

ALTER TABLE `cloud`.`vm_instance` ADD INDEX `i_vm_instance__last_host_id`(`last_host_id`);

-- drop foreign key constraits temporarily to allow data update in migration process
ALTER TABLE `cloud`.`user_vm` DROP FOREIGN KEY `fk_user_vm__service_offering_id`;

