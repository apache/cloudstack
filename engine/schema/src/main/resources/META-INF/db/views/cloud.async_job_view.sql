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

-- VIEW `cloud`.`async_job_view`;

DROP VIEW IF EXISTS `cloud`.`async_job_view`;

CREATE VIEW `cloud`.`async_job_view` AS
select
    account.id account_id,
    account.uuid account_uuid,
    account.account_name account_name,
    account.type account_type,
    domain.id domain_id,
    domain.uuid domain_uuid,
    domain.name domain_name,
    domain.path domain_path,
    user.id user_id,
    user.uuid user_uuid,
    async_job.id,
    async_job.uuid,
    async_job.related,
    async_job.job_cmd,
    async_job.job_status,
    async_job.job_process_status,
    async_job.job_result_code,
    async_job.job_result,
    async_job.created,
    async_job.removed,
    async_job.instance_type,
    async_job.instance_id,
    async_job.job_init_msid,
    async_job.job_executing_msid,
    async_job.job_complete_msid,
    async_job.cancel_requested,
    COALESCE(
        volumes.uuid,
        vm_template.uuid,
        vm_instance.uuid,
        snapshots.uuid,
        host.uuid,
        storage_pool.uuid,
        user_ip_address.uuid,
        security_group.uuid,
        physical_network.uuid,
        physical_network_traffic_types.uuid,
        physical_network_service_providers.uuid,
        firewall_rules.uuid,
        acct.uuid,
        us.uuid,
        static_routes.uuid,
        vpc_gateways.uuid,
        counter.uuid,
        conditions.uuid,
        autoscale_policies.uuid,
        autoscale_vmprofiles.uuid,
        autoscale_vmgroups.uuid) instance_uuid,
    COALESCE(
        volumes.name,
        vm_template.name,
        vm_instance.name,
        snapshots.name,
        host.name,
        storage_pool.name,
        user_ip_address.public_ip_address,
        security_group.name,
        physical_network.name,
        physical_network_traffic_types.traffic_type,
        physical_network_service_providers.provider_name,
        acct.account_name,
        us.username,
        static_routes.cidr,
        vpc_gateways.ip4_address,
        counter.name,
        autoscale_policies.name,
        autoscale_vmgroups.name) instance_name
from
    `cloud`.`async_job`
        left join
    `cloud`.`account` ON async_job.account_id = account.id
        left join
    `cloud`.`domain` ON domain.id = account.domain_id
        left join
    `cloud`.`user` ON async_job.user_id = user.id
        left join
    `cloud`.`volumes` ON async_job.instance_type = 'Volume' and async_job.instance_id = volumes.id
        left join
    `cloud`.`vm_template` ON async_job.instance_type in ('Template', 'Iso') and async_job.instance_id = vm_template.id
        left join
    `cloud`.`vm_instance` ON async_job.instance_type in ('VirtualMachine', 'ConsoleProxy', 'SystemVm', 'DomainRouter') and async_job.instance_id = vm_instance.id
        left join
    `cloud`.`snapshots` ON async_job.instance_type = 'Snapshot' and async_job.instance_id = snapshots.id
        left join
    `cloud`.`host` ON async_job.instance_type = 'Host' and async_job.instance_id = host.id
        left join
    `cloud`.`storage_pool` ON async_job.instance_type = 'StoragePool' and async_job.instance_id = storage_pool.id
        left join
    `cloud`.`user_ip_address` ON async_job.instance_type = 'IpAddress' and async_job.instance_id = user_ip_address.id
        left join
    `cloud`.`security_group` ON async_job.instance_type = 'SecurityGroup' and async_job.instance_id = security_group.id
        left join
    `cloud`.`physical_network` ON async_job.instance_type = 'PhysicalNetwork' and async_job.instance_id = physical_network.id
        left join
    `cloud`.`physical_network_traffic_types` ON async_job.instance_type = 'TrafficType' and async_job.instance_id = physical_network_traffic_types.id
        left join
    `cloud`.`physical_network_service_providers` ON async_job.instance_type = 'PhysicalNetworkServiceProvider' and async_job.instance_id = physical_network_service_providers.id
        left join
    `cloud`.`firewall_rules` ON async_job.instance_type = 'FirewallRule' and async_job.instance_id = firewall_rules.id
        left join
    `cloud`.`account` acct ON async_job.instance_type = 'Account' and async_job.instance_id = acct.id
        left join
    `cloud`.`user` us ON async_job.instance_type = 'User' and async_job.instance_id = us.id
        left join
    `cloud`.`static_routes` ON async_job.instance_type = 'StaticRoute' and async_job.instance_id = static_routes.id
        left join
    `cloud`.`vpc_gateways` ON async_job.instance_type = 'PrivateGateway' and async_job.instance_id = vpc_gateways.id
        left join
    `cloud`.`counter` ON async_job.instance_type = 'Counter' and async_job.instance_id = counter.id
        left join
    `cloud`.`conditions` ON async_job.instance_type = 'Condition' and async_job.instance_id = conditions.id
        left join
    `cloud`.`autoscale_policies` ON async_job.instance_type = 'AutoScalePolicy' and async_job.instance_id = autoscale_policies.id
        left join
    `cloud`.`autoscale_vmprofiles` ON async_job.instance_type = 'AutoScaleVmProfile' and async_job.instance_id = autoscale_vmprofiles.id
        left join
    `cloud`.`autoscale_vmgroups` ON async_job.instance_type = 'AutoScaleVmGroup' and async_job.instance_id = autoscale_vmgroups.id;
