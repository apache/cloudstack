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
-- Schema upgrade from 4.18.1.0 to 4.19.0.0
--;

ALTER TABLE `cloud`.`mshost` MODIFY COLUMN `state` varchar(25);

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
        async_job.job_cmd,
        async_job.job_status,
        async_job.job_process_status,
        async_job.job_result_code,
        async_job.job_result,
        async_job.created,
        async_job.removed,
        async_job.instance_type,
        async_job.instance_id,
        async_job.job_executing_msid,
        CASE
            WHEN async_job.instance_type = 'Volume' THEN volumes.uuid
            WHEN
                async_job.instance_type = 'Template'
                    or async_job.instance_type = 'Iso'
            THEN
                vm_template.uuid
            WHEN
                async_job.instance_type = 'VirtualMachine'
                    or async_job.instance_type = 'ConsoleProxy'
                    or async_job.instance_type = 'SystemVm'
                    or async_job.instance_type = 'DomainRouter'
            THEN
                vm_instance.uuid
            WHEN async_job.instance_type = 'Snapshot' THEN snapshots.uuid
            WHEN async_job.instance_type = 'Host' THEN host.uuid
            WHEN async_job.instance_type = 'StoragePool' THEN storage_pool.uuid
            WHEN async_job.instance_type = 'IpAddress' THEN user_ip_address.uuid
            WHEN async_job.instance_type = 'SecurityGroup' THEN security_group.uuid
            WHEN async_job.instance_type = 'PhysicalNetwork' THEN physical_network.uuid
            WHEN async_job.instance_type = 'TrafficType' THEN physical_network_traffic_types.uuid
            WHEN async_job.instance_type = 'PhysicalNetworkServiceProvider' THEN physical_network_service_providers.uuid
            WHEN async_job.instance_type = 'FirewallRule' THEN firewall_rules.uuid
            WHEN async_job.instance_type = 'Account' THEN acct.uuid
            WHEN async_job.instance_type = 'User' THEN us.uuid
            WHEN async_job.instance_type = 'StaticRoute' THEN static_routes.uuid
            WHEN async_job.instance_type = 'PrivateGateway' THEN vpc_gateways.uuid
            WHEN async_job.instance_type = 'Counter' THEN counter.uuid
            WHEN async_job.instance_type = 'Condition' THEN conditions.uuid
            WHEN async_job.instance_type = 'AutoScalePolicy' THEN autoscale_policies.uuid
            WHEN async_job.instance_type = 'AutoScaleVmProfile' THEN autoscale_vmprofiles.uuid
            WHEN async_job.instance_type = 'AutoScaleVmGroup' THEN autoscale_vmgroups.uuid
            ELSE null
        END instance_uuid
    from
        `cloud`.`async_job`
            left join
        `cloud`.`account` ON async_job.account_id = account.id
            left join
        `cloud`.`domain` ON domain.id = account.domain_id
            left join
        `cloud`.`user` ON async_job.user_id = user.id
            left join
        `cloud`.`volumes` ON async_job.instance_id = volumes.id
            left join
        `cloud`.`vm_template` ON async_job.instance_id = vm_template.id
            left join
        `cloud`.`vm_instance` ON async_job.instance_id = vm_instance.id
            left join
        `cloud`.`snapshots` ON async_job.instance_id = snapshots.id
            left join
        `cloud`.`host` ON async_job.instance_id = host.id
            left join
        `cloud`.`storage_pool` ON async_job.instance_id = storage_pool.id
            left join
        `cloud`.`user_ip_address` ON async_job.instance_id = user_ip_address.id
            left join
        `cloud`.`security_group` ON async_job.instance_id = security_group.id
            left join
        `cloud`.`physical_network` ON async_job.instance_id = physical_network.id
            left join
        `cloud`.`physical_network_traffic_types` ON async_job.instance_id = physical_network_traffic_types.id
            left join
        `cloud`.`physical_network_service_providers` ON async_job.instance_id = physical_network_service_providers.id
            left join
        `cloud`.`firewall_rules` ON async_job.instance_id = firewall_rules.id
            left join
        `cloud`.`account` acct ON async_job.instance_id = acct.id
            left join
        `cloud`.`user` us ON async_job.instance_id = us.id
            left join
        `cloud`.`static_routes` ON async_job.instance_id = static_routes.id
            left join
        `cloud`.`vpc_gateways` ON async_job.instance_id = vpc_gateways.id
            left join
        `cloud`.`counter` ON async_job.instance_id = counter.id
            left join
        `cloud`.`conditions` ON async_job.instance_id = conditions.id
            left join
        `cloud`.`autoscale_policies` ON async_job.instance_id = autoscale_policies.id
            left join
        `cloud`.`autoscale_vmprofiles` ON async_job.instance_id = autoscale_vmprofiles.id
            left join
        `cloud`.`autoscale_vmgroups` ON async_job.instance_id = autoscale_vmgroups.id;

-- Invalidate existing console_session records
UPDATE `cloud`.`console_session` SET removed=now();
-- Modify acquired column in console_session to datetime type
ALTER TABLE `cloud`.`console_session` DROP `acquired`, ADD `acquired` datetime COMMENT 'When the session was acquired' AFTER `host_id`;

-- Flexible tags
ALTER TABLE `cloud`.`storage_pool_tags` ADD COLUMN is_tag_a_rule int(1) UNSIGNED not null DEFAULT 0;

ALTER TABLE `cloud`.`storage_pool_tags` MODIFY tag text NOT NULL;

CREATE OR REPLACE
    VIEW `cloud`.`storage_pool_view` AS
SELECT
    `storage_pool`.`id` AS `id`,
    `storage_pool`.`uuid` AS `uuid`,
    `storage_pool`.`name` AS `name`,
    `storage_pool`.`status` AS `status`,
    `storage_pool`.`path` AS `path`,
    `storage_pool`.`pool_type` AS `pool_type`,
    `storage_pool`.`host_address` AS `host_address`,
    `storage_pool`.`created` AS `created`,
    `storage_pool`.`removed` AS `removed`,
    `storage_pool`.`capacity_bytes` AS `capacity_bytes`,
    `storage_pool`.`capacity_iops` AS `capacity_iops`,
    `storage_pool`.`scope` AS `scope`,
    `storage_pool`.`hypervisor` AS `hypervisor`,
    `storage_pool`.`storage_provider_name` AS `storage_provider_name`,
    `storage_pool`.`parent` AS `parent`,
    `cluster`.`id` AS `cluster_id`,
    `cluster`.`uuid` AS `cluster_uuid`,
    `cluster`.`name` AS `cluster_name`,
    `cluster`.`cluster_type` AS `cluster_type`,
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `data_center`.`networktype` AS `data_center_type`,
    `host_pod_ref`.`id` AS `pod_id`,
    `host_pod_ref`.`uuid` AS `pod_uuid`,
    `host_pod_ref`.`name` AS `pod_name`,
    `storage_pool_tags`.`tag` AS `tag`,
    `storage_pool_tags`.`is_tag_a_rule` AS `is_tag_a_rule`,
    `op_host_capacity`.`used_capacity` AS `disk_used_capacity`,
    `op_host_capacity`.`reserved_capacity` AS `disk_reserved_capacity`,
    `async_job`.`id` AS `job_id`,
    `async_job`.`uuid` AS `job_uuid`,
    `async_job`.`job_status` AS `job_status`,
    `async_job`.`account_id` AS `job_account_id`
FROM
    ((((((`cloud`.`storage_pool`
        LEFT JOIN `cloud`.`cluster` ON ((`storage_pool`.`cluster_id` = `cluster`.`id`)))
        LEFT JOIN `cloud`.`data_center` ON ((`storage_pool`.`data_center_id` = `data_center`.`id`)))
        LEFT JOIN `cloud`.`host_pod_ref` ON ((`storage_pool`.`pod_id` = `host_pod_ref`.`id`)))
        LEFT JOIN `cloud`.`storage_pool_tags` ON (((`storage_pool_tags`.`pool_id` = `storage_pool`.`id`))))
        LEFT JOIN `cloud`.`op_host_capacity` ON (((`storage_pool`.`id` = `op_host_capacity`.`host_id`)
        AND (`op_host_capacity`.`capacity_type` IN (3 , 9)))))
        LEFT JOIN `cloud`.`async_job` ON (((`async_job`.`instance_id` = `storage_pool`.`id`)
        AND (`async_job`.`instance_type` = 'StoragePool')
        AND (`async_job`.`job_status` = 0))));


ALTER TABLE `cloud`.`host_tags` ADD COLUMN is_tag_a_rule int(1) UNSIGNED not null DEFAULT 0;

ALTER TABLE `cloud`.`host_tags` MODIFY tag text NOT NULL;

CREATE OR REPLACE VIEW `cloud`.`host_view` AS
SELECT
    host.id,
    host.uuid,
    host.name,
    host.status,
    host.disconnected,
    host.type,
    host.private_ip_address,
    host.version,
    host.hypervisor_type,
    host.hypervisor_version,
    host.capabilities,
    host.last_ping,
    host.created,
    host.removed,
    host.resource_state,
    host.mgmt_server_id,
    host.cpu_sockets,
    host.cpus,
    host.speed,
    host.ram,
    cluster.id cluster_id,
    cluster.uuid cluster_uuid,
    cluster.name cluster_name,
    cluster.cluster_type,
    data_center.id data_center_id,
    data_center.uuid data_center_uuid,
    data_center.name data_center_name,
    data_center.networktype data_center_type,
    host_pod_ref.id pod_id,
    host_pod_ref.uuid pod_uuid,
    host_pod_ref.name pod_name,
    GROUP_CONCAT(DISTINCT(host_tags.tag)) AS tag,
    `host_tags`.`is_tag_a_rule` AS `is_tag_a_rule`,
    guest_os_category.id guest_os_category_id,
    guest_os_category.uuid guest_os_category_uuid,
    guest_os_category.name guest_os_category_name,
    mem_caps.used_capacity memory_used_capacity,
    mem_caps.reserved_capacity memory_reserved_capacity,
    cpu_caps.used_capacity cpu_used_capacity,
    cpu_caps.reserved_capacity cpu_reserved_capacity,
    async_job.id job_id,
    async_job.uuid job_uuid,
    async_job.job_status job_status,
    async_job.account_id job_account_id,
    oobm.enabled AS `oobm_enabled`,
    oobm.power_state AS `oobm_power_state`,
    ha_config.enabled AS `ha_enabled`,
    ha_config.ha_state AS `ha_state`,
    ha_config.provider AS `ha_provider`,
    `last_annotation_view`.`annotation` AS `annotation`,
    `last_annotation_view`.`created` AS `last_annotated`,
    `user`.`username` AS `username`
FROM
    `cloud`.`host`
        LEFT JOIN
    `cloud`.`cluster` ON host.cluster_id = cluster.id
        LEFT JOIN
    `cloud`.`data_center` ON host.data_center_id = data_center.id
        LEFT JOIN
    `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
        LEFT JOIN
    `cloud`.`host_details` ON host.id = host_details.host_id
        AND host_details.name = 'guest.os.category.id'
        LEFT JOIN
    `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT ( host_details.value, UNSIGNED )
        LEFT JOIN
    `cloud`.`host_tags` ON host_tags.host_id = host.id
        LEFT JOIN
    `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
        AND mem_caps.capacity_type = 0
        LEFT JOIN
    `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
        AND cpu_caps.capacity_type = 1
        LEFT JOIN
    `cloud`.`async_job` ON async_job.instance_id = host.id
        AND async_job.instance_type = 'Host'
        AND async_job.job_status = 0
        LEFT JOIN
    `cloud`.`oobm` ON oobm.host_id = host.id
        left join
    `cloud`.`ha_config` ON ha_config.resource_id=host.id
        and ha_config.resource_type='Host'
        LEFT JOIN
    `cloud`.`last_annotation_view` ON `last_annotation_view`.`entity_uuid` = `host`.`uuid`
        LEFT JOIN
    `cloud`.`user` ON `user`.`uuid` = `last_annotation_view`.`user_uuid`
GROUP BY
    `host`.`id`;
