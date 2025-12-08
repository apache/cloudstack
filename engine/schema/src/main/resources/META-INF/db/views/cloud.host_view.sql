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

-- VIEW `cloud`.`host_view`;

DROP VIEW IF EXISTS `cloud`.`host_view`;

CREATE VIEW `cloud`.`host_view` AS
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
    host.arch,
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
    GROUP_CONCAT(DISTINCT(explicit_host_tags.tag)) AS explicit_tag,
    GROUP_CONCAT(DISTINCT(implicit_host_tags.tag)) AS implicit_tag,
    `explicit_host_tags`.`is_tag_a_rule` AS `is_tag_a_rule`,
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
    `cloud`.`host_tags` AS explicit_host_tags ON explicit_host_tags.host_id = host.id AND explicit_host_tags.is_implicit = 0
        LEFT JOIN
    `cloud`.`host_tags` AS implicit_host_tags ON implicit_host_tags.host_id = host.id AND implicit_host_tags.is_implicit = 1
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
