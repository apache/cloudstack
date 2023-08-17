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

-- Dynamic roles changes
DROP VIEW IF EXISTS `cloud`.`account_view`;
CREATE VIEW `cloud`.`account_view` AS
    select
        account.id,
        account.uuid,
        account.account_name,
        account.type,
        account.role_id,
        account.state,
        account.removed,
        account.cleanup_needed,
        account.network_domain,
        account.default,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        data_center.id data_center_id,
        data_center.uuid data_center_uuid,
        data_center.name data_center_name,
        account_netstats_view.bytesReceived,
        account_netstats_view.bytesSent,
        vmlimit.max vmLimit,
        vmcount.count vmTotal,
        runningvm.vmcount runningVms,
        stoppedvm.vmcount stoppedVms,
        iplimit.max ipLimit,
        ipcount.count ipTotal,
        free_ip_view.free_ip ipFree,
        volumelimit.max volumeLimit,
        volumecount.count volumeTotal,
        snapshotlimit.max snapshotLimit,
        snapshotcount.count snapshotTotal,
        templatelimit.max templateLimit,
        templatecount.count templateTotal,
        vpclimit.max vpcLimit,
        vpccount.count vpcTotal,
        projectlimit.max projectLimit,
        projectcount.count projectTotal,
        networklimit.max networkLimit,
        networkcount.count networkTotal,
        cpulimit.max cpuLimit,
        cpucount.count cpuTotal,
        memorylimit.max memoryLimit,
        memorycount.count memoryTotal,
        primary_storage_limit.max primaryStorageLimit,
        primary_storage_count.count primaryStorageTotal,
        secondary_storage_limit.max secondaryStorageLimit,
        secondary_storage_count.count secondaryStorageTotal,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`free_ip_view`,
        `cloud`.`account`
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`data_center` ON account.default_zone_id = data_center.id
            left join
        `cloud`.`account_netstats_view` ON account.id = account_netstats_view.account_id
            left join
        `cloud`.`resource_limit` vmlimit ON account.id = vmlimit.account_id
            and vmlimit.type = 'user_vm'
            left join
        `cloud`.`resource_count` vmcount ON account.id = vmcount.account_id
            and vmcount.type = 'user_vm'
            left join
        `cloud`.`account_vmstats_view` runningvm ON account.id = runningvm.account_id
            and runningvm.state = 'Running'
            left join
        `cloud`.`account_vmstats_view` stoppedvm ON account.id = stoppedvm.account_id
           and stoppedvm.state = 'Stopped'
            left join
        `cloud`.`resource_limit` iplimit ON account.id = iplimit.account_id
            and iplimit.type = 'public_ip'
            left join
        `cloud`.`resource_count` ipcount ON account.id = ipcount.account_id
            and ipcount.type = 'public_ip'
            left join
        `cloud`.`resource_limit` volumelimit ON account.id = volumelimit.account_id
            and volumelimit.type = 'volume'
            left join
        `cloud`.`resource_count` volumecount ON account.id = volumecount.account_id
            and volumecount.type = 'volume'
            left join
        `cloud`.`resource_limit` snapshotlimit ON account.id = snapshotlimit.account_id
            and snapshotlimit.type = 'snapshot'
            left join
        `cloud`.`resource_count` snapshotcount ON account.id = snapshotcount.account_id
            and snapshotcount.type = 'snapshot'
            left join
        `cloud`.`resource_limit` templatelimit ON account.id = templatelimit.account_id
            and templatelimit.type = 'template'
            left join
        `cloud`.`resource_count` templatecount ON account.id = templatecount.account_id
            and templatecount.type = 'template'
            left join
        `cloud`.`resource_limit` vpclimit ON account.id = vpclimit.account_id
            and vpclimit.type = 'vpc'
            left join
        `cloud`.`resource_count` vpccount ON account.id = vpccount.account_id
            and vpccount.type = 'vpc'
            left join
        `cloud`.`resource_limit` projectlimit ON account.id = projectlimit.account_id
            and projectlimit.type = 'project'
            left join
        `cloud`.`resource_count` projectcount ON account.id = projectcount.account_id
            and projectcount.type = 'project'
            left join
        `cloud`.`resource_limit` networklimit ON account.id = networklimit.account_id
            and networklimit.type = 'network'
            left join
        `cloud`.`resource_count` networkcount ON account.id = networkcount.account_id
            and networkcount.type = 'network'
            left join
        `cloud`.`resource_limit` cpulimit ON account.id = cpulimit.account_id
            and cpulimit.type = 'cpu'
            left join
        `cloud`.`resource_count` cpucount ON account.id = cpucount.account_id
            and cpucount.type = 'cpu'
            left join
        `cloud`.`resource_limit` memorylimit ON account.id = memorylimit.account_id
            and memorylimit.type = 'memory'
            left join
        `cloud`.`resource_count` memorycount ON account.id = memorycount.account_id
            and memorycount.type = 'memory'
            left join
        `cloud`.`resource_limit` primary_storage_limit ON account.id = primary_storage_limit.account_id
            and primary_storage_limit.type = 'primary_storage'
            left join
        `cloud`.`resource_count` primary_storage_count ON account.id = primary_storage_count.account_id
            and primary_storage_count.type = 'primary_storage'
            left join
        `cloud`.`resource_limit` secondary_storage_limit ON account.id = secondary_storage_limit.account_id
            and secondary_storage_limit.type = 'secondary_storage'
            left join
        `cloud`.`resource_count` secondary_storage_count ON account.id = secondary_storage_count.account_id
            and secondary_storage_count.type = 'secondary_storage'
            left join
        `cloud`.`async_job` ON async_job.instance_id = account.id
            and async_job.instance_type = 'Account'
            and async_job.job_status = 0;

DROP VIEW IF EXISTS `cloud`.`user_view`;
CREATE VIEW `cloud`.`user_view` AS
    select
        user.id,
        user.uuid,
        user.username,
        user.password,
        user.firstname,
        user.lastname,
        user.email,
        user.state,
        user.api_key,
        user.secret_key,
        user.created,
        user.removed,
        user.timezone,
        user.registration_token,
        user.is_registered,
        user.incorrect_login_attempts,
        user.default,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name account_name,
        account.type account_type,
        account.role_id account_role_id,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path,
        async_job.id job_id,
        async_job.uuid job_uuid,
        async_job.job_status job_status,
        async_job.account_id job_account_id
    from
        `cloud`.`user`
            inner join
        `cloud`.`account` ON user.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            left join
        `cloud`.`async_job` ON async_job.instance_id = user.id
            and async_job.instance_type = 'User'
            and async_job.job_status = 0;
