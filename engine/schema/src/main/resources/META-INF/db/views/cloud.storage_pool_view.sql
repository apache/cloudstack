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

-- VIEW `cloud`.`storage_pool_view`;

DROP VIEW IF EXISTS `cloud`.`storage_pool_view`;

CREATE VIEW `cloud`.`storage_pool_view` AS
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
