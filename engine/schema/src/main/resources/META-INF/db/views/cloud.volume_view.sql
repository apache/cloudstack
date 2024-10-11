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

-- VIEW `cloud`.`volume_view`;

DROP VIEW IF EXISTS `cloud`.`volume_view`;

CREATE VIEW `cloud`.`volume_view` AS
SELECT
    `volumes`.`id` AS `id`,
    `volumes`.`uuid` AS `uuid`,
    `volumes`.`name` AS `name`,
    `volumes`.`device_id` AS `device_id`,
    `volumes`.`volume_type` AS `volume_type`,
    `volumes`.`provisioning_type` AS `provisioning_type`,
    `volumes`.`size` AS `size`,
    `volumes`.`min_iops` AS `min_iops`,
    `volumes`.`max_iops` AS `max_iops`,
    `volumes`.`created` AS `created`,
    `volumes`.`state` AS `state`,
    `volumes`.`attached` AS `attached`,
    `volumes`.`removed` AS `removed`,
    `volumes`.`display_volume` AS `display_volume`,
    `volumes`.`format` AS `format`,
    `volumes`.`path` AS `path`,
    `volumes`.`chain_info` AS `chain_info`,
    `volumes`.`external_uuid` AS `external_uuid`,
    `volumes`.`encrypt_format` AS `encrypt_format`,
    `volumes`.`delete_protection` AS `delete_protection`,
    `account`.`id` AS `account_id`,
    `account`.`uuid` AS `account_uuid`,
    `account`.`account_name` AS `account_name`,
    `account`.`type` AS `account_type`,
    `domain`.`id` AS `domain_id`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `domain`.`path` AS `domain_path`,
    `projects`.`id` AS `project_id`,
    `projects`.`uuid` AS `project_uuid`,
    `projects`.`name` AS `project_name`,
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `data_center`.`networktype` AS `data_center_type`,
    `vm_instance`.`id` AS `vm_id`,
    `vm_instance`.`uuid` AS `vm_uuid`,
    `vm_instance`.`name` AS `vm_name`,
    `vm_instance`.`state` AS `vm_state`,
    `vm_instance`.`vm_type` AS `vm_type`,
    `user_vm`.`display_name` AS `vm_display_name`,
    `volume_store_ref`.`size` AS `volume_store_size`,
    `volume_store_ref`.`download_pct` AS `download_pct`,
    `volume_store_ref`.`download_state` AS `download_state`,
    `volume_store_ref`.`error_str` AS `error_str`,
    `volume_store_ref`.`created` AS `created_on_store`,
    `disk_offering`.`id` AS `disk_offering_id`,
    `disk_offering`.`uuid` AS `disk_offering_uuid`,
    `disk_offering`.`name` AS `disk_offering_name`,
    `disk_offering`.`display_text` AS `disk_offering_display_text`,
    `disk_offering`.`use_local_storage` AS `use_local_storage`,
    `service_offering`.`system_use` AS `system_use`,
    `disk_offering`.`bytes_read_rate` AS `bytes_read_rate`,
    `disk_offering`.`bytes_write_rate` AS `bytes_write_rate`,
    `disk_offering`.`iops_read_rate` AS `iops_read_rate`,
    `disk_offering`.`iops_write_rate` AS `iops_write_rate`,
    `disk_offering`.`cache_mode` AS `cache_mode`,
    `storage_pool`.`id` AS `pool_id`,
    `storage_pool`.`uuid` AS `pool_uuid`,
    `storage_pool`.`name` AS `pool_name`,
    `cluster`.`id` AS `cluster_id`,
    `cluster`.`name` AS `cluster_name`,
    `cluster`.`uuid` AS `cluster_uuid`,
    `cluster`.`hypervisor_type` AS `hypervisor_type`,
    `vm_template`.`id` AS `template_id`,
    `vm_template`.`uuid` AS `template_uuid`,
    `vm_template`.`extractable` AS `extractable`,
    `vm_template`.`type` AS `template_type`,
    `vm_template`.`name` AS `template_name`,
    `vm_template`.`display_text` AS `template_display_text`,
    `iso`.`id` AS `iso_id`,
    `iso`.`uuid` AS `iso_uuid`,
    `iso`.`name` AS `iso_name`,
    `iso`.`display_text` AS `iso_display_text`,
    `resource_tags`.`id` AS `tag_id`,
    `resource_tags`.`uuid` AS `tag_uuid`,
    `resource_tags`.`key` AS `tag_key`,
    `resource_tags`.`value` AS `tag_value`,
    `resource_tags`.`domain_id` AS `tag_domain_id`,
    `resource_tags`.`account_id` AS `tag_account_id`,
    `resource_tags`.`resource_id` AS `tag_resource_id`,
    `resource_tags`.`resource_uuid` AS `tag_resource_uuid`,
    `resource_tags`.`resource_type` AS `tag_resource_type`,
    `resource_tags`.`customer` AS `tag_customer`,
    `async_job`.`id` AS `job_id`,
    `async_job`.`uuid` AS `job_uuid`,
    `async_job`.`job_status` AS `job_status`,
    `async_job`.`account_id` AS `job_account_id`,
    `host_pod_ref`.`id` AS `pod_id`,
    `host_pod_ref`.`uuid` AS `pod_uuid`,
    `host_pod_ref`.`name` AS `pod_name`,
    `resource_tag_account`.`account_name` AS `tag_account_name`,
    `resource_tag_domain`.`uuid` AS `tag_domain_uuid`,
    `resource_tag_domain`.`name` AS `tag_domain_name`
FROM
    ((((((((((((((((((`volumes`
JOIN `account`ON
    ((`volumes`.`account_id` = `account`.`id`)))
JOIN `domain`ON
    ((`volumes`.`domain_id` = `domain`.`id`)))
LEFT JOIN `projects`ON
    ((`projects`.`project_account_id` = `account`.`id`)))
LEFT JOIN `data_center`ON
    ((`volumes`.`data_center_id` = `data_center`.`id`)))
LEFT JOIN `vm_instance`ON
    ((`volumes`.`instance_id` = `vm_instance`.`id`)))
LEFT JOIN `user_vm`ON
    ((`user_vm`.`id` = `vm_instance`.`id`)))
LEFT JOIN `volume_store_ref`ON
    ((`volumes`.`id` = `volume_store_ref`.`volume_id`)))
LEFT JOIN `service_offering`ON
    ((`vm_instance`.`service_offering_id` = `service_offering`.`id`)))
LEFT JOIN `disk_offering`ON
    ((`volumes`.`disk_offering_id` = `disk_offering`.`id`)))
LEFT JOIN `storage_pool`ON
    ((`volumes`.`pool_id` = `storage_pool`.`id`)))
LEFT JOIN `host_pod_ref`ON
    ((`storage_pool`.`pod_id` = `host_pod_ref`.`id`)))
LEFT JOIN `cluster`ON
    ((`storage_pool`.`cluster_id` = `cluster`.`id`)))
LEFT JOIN `vm_template`ON
    ((`volumes`.`template_id` = `vm_template`.`id`)))
LEFT JOIN `vm_template` `iso`ON
    ((`iso`.`id` = `volumes`.`iso_id`)))
LEFT JOIN `resource_tags`ON
    (((`resource_tags`.`resource_id` = `volumes`.`id`)
        and (`resource_tags`.`resource_type` = 'Volume'))))
LEFT JOIN `async_job`ON
    (((`async_job`.`instance_id` = `volumes`.`id`)
        and (`async_job`.`instance_type` = 'Volume')
            and (`async_job`.`job_status` = 0))))
LEFT JOIN `account` `resource_tag_account`ON
    ((`resource_tag_account`.`id` = `resource_tags`.`account_id`)))
LEFT JOIN `domain` `resource_tag_domain`ON
    ((`resource_tag_domain`.`id` = `resource_tags`.`domain_id`)));
