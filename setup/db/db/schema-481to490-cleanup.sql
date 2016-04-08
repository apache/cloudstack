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
-- Schema cleanup from 4.8.1 to 4.9.0;
--;

-- Added in CLOUDSTACK-9340: General DB optimization, 4 cases:

----- 1) Incorrect PRIMARY key
ALTER TABLE `cloud`.`ovs_tunnel_network` 
DROP PRIMARY KEY,
ADD PRIMARY KEY (`id`),
DROP INDEX `id` ,
ADD UNIQUE INDEX `i_to_from_network_id` (`to` ASC, `from` ASC, `network_id` ASC);

----- 2) Duplicate PRIMARY KEY
ALTER TABLE `cloud`.`user_vm` DROP INDEX `id_2` ,DROP INDEX `id` ;
ALTER TABLE `cloud`.`domain_router` DROP INDEX `id_2` ,DROP INDEX `id` ;
ALTER TABLE `cloud`.`vm_instance` DROP INDEX `id_2` ,DROP INDEX `id` ;
ALTER TABLE `cloud`.`account_vlan_map` DROP INDEX `id` ;
ALTER TABLE `cloud`.`account_vnet_map` DROP INDEX `id` ;
ALTER TABLE `cloud`.`baremetal_rct` DROP INDEX `id` ;
ALTER TABLE `cloud`.`cluster` DROP INDEX `id` ;
ALTER TABLE `cloud`.`conditions` DROP INDEX `id` ;
ALTER TABLE `cloud`.`counter` DROP INDEX `id` ;
ALTER TABLE `cloud`.`data_center` DROP INDEX `id` ;
ALTER TABLE `cloud`.`dc_storage_network_ip_range` DROP INDEX `id` ;
ALTER TABLE `cloud`.`dedicated_resources` DROP INDEX `id` ;
ALTER TABLE `cloud`.`host_pod_ref` DROP INDEX `id` ;
ALTER TABLE `cloud`.`iam_group` DROP INDEX `id` ;
ALTER TABLE `cloud`.`iam_policy` DROP INDEX `id` ;
ALTER TABLE `cloud`.`iam_policy_permission` DROP INDEX `id` ;
ALTER TABLE `cloud`.`image_store_details` DROP INDEX `id` ;
ALTER TABLE `cloud`.`instance_group` DROP INDEX `id` ;
ALTER TABLE `cloud`.`netapp_lun` DROP INDEX `id` ;
ALTER TABLE `cloud`.`netapp_pool` DROP INDEX `id` ;
ALTER TABLE `cloud`.`netapp_volume` DROP INDEX `id` ;
ALTER TABLE `cloud`.`network_acl_item_cidrs` DROP INDEX `id` ;
ALTER TABLE `cloud`.`network_offerings` DROP INDEX `id` ;
ALTER TABLE `cloud`.`nic_secondary_ips` DROP INDEX `id` ;
ALTER TABLE `cloud`.`nics` DROP INDEX `id` ;
ALTER TABLE `cloud`.`op_ha_work` DROP INDEX `id` ;
ALTER TABLE `cloud`.`op_host` DROP INDEX `id` ;
ALTER TABLE `cloud`.`op_host_transfer` DROP INDEX `id` ;
ALTER TABLE `cloud`.`op_networks` DROP INDEX `id` ;
ALTER TABLE `cloud`.`op_nwgrp_work` DROP INDEX `id` ;
ALTER TABLE `cloud`.`op_vm_ruleset_log` DROP INDEX `id` ;
ALTER TABLE `cloud`.`op_vpc_distributed_router_sequence_no` DROP INDEX `id` ;
ALTER TABLE `cloud`.`pod_vlan_map` DROP INDEX `id` ;
ALTER TABLE `cloud`.`portable_ip_address` DROP INDEX `id` ;
ALTER TABLE `cloud`.`portable_ip_range` DROP INDEX `id` ;
ALTER TABLE `cloud`.`region` DROP INDEX `id` ;
ALTER TABLE `cloud`.`remote_access_vpn` DROP INDEX `id` ;
ALTER TABLE `cloud`.`snapshot_details` DROP INDEX `id` ;
ALTER TABLE `cloud`.`snapshots` DROP INDEX `id` ;
ALTER TABLE `cloud`.`storage_pool` DROP INDEX `id` ;
ALTER TABLE `cloud`.`storage_pool_details` DROP INDEX `id` ;
ALTER TABLE `cloud`.`storage_pool_work` DROP INDEX `id` ;
ALTER TABLE `cloud`.`user_ip_address` DROP INDEX `id` ;
ALTER TABLE `cloud`.`user_ipv6_address` DROP INDEX `id` ;
ALTER TABLE `cloud`.`user_statistics` DROP INDEX `id` ;
ALTER TABLE `cloud`.`version` DROP INDEX `id` ;
ALTER TABLE `cloud`.`vlan` DROP INDEX `id` ;
ALTER TABLE `cloud`.`vm_disk_statistics` DROP INDEX `id` ;
ALTER TABLE `cloud`.`vm_snapshot_details` DROP INDEX `id` ;
ALTER TABLE `cloud`.`vm_work_job` DROP INDEX `id` ;
ALTER TABLE `cloud`.`vpc_gateways` DROP INDEX `id` ;
ALTER TABLE `cloud`.`vpn_users` DROP INDEX `id` ;


