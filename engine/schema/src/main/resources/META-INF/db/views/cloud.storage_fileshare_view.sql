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

-- VIEW `cloud`.`storage_fileshare_view`;

DROP VIEW IF EXISTS `cloud`.`storage_fileshare_view`;

CREATE VIEW `cloud`.`storage_fileshare_view` AS
SELECT
    `storage_fileshare`.`id` AS `id`,
    `storage_fileshare`.`uuid` AS `uuid`,
    `storage_fileshare`.`name` AS `name`,
    `storage_fileshare`.`description` AS `description`,
    `storage_fileshare`.`state` AS `state`,
    `storage_fileshare`.`fs_provider_name` AS `provider`,
    `storage_fileshare`.`fs_type` AS `fs_type`,
    `storage_fileshare`.`volume_id` AS `volume_id`,
    `storage_fileshare`.`account_id` AS `account_id`,
    `storage_fileshare`.`data_center_id` AS `zone_id`,
    `zone`.`uuid` AS `zone_uuid`,
    `zone`.`name` AS `zone_name`,
    `instance`.`id` AS `instance_id`,
    `instance`.`uuid` AS `instance_uuid`,
    `instance`.`name` AS `instance_name`,
    `volumes`.`size` AS `size`,
    `volumes`.`uuid` AS `volume_uuid`,
    `volumes`.`name` AS `volume_name`,
    `volumes`.`provisioning_type` AS `provisioning_type`,
    `volumes`.`format` AS `volume_format`,
    `volumes`.`path` AS `volume_path`,
    `volumes`.`chain_info` AS `volume_chain_info`,
    `storage_pool`.`uuid` AS `pool_uuid`,
    `storage_pool`.`name` AS `pool_name`,
    `account`.`account_name` AS `account_name`,
    `project`.`uuid` AS `project_uuid`,
    `project`.`name` AS `project_name`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `service_offering`.`uuid` AS `service_offering_uuid`,
    `service_offering`.`name` AS `service_offering_name`,
    `disk_offering`.`uuid` AS `disk_offering_uuid`,
    `disk_offering`.`name` AS `disk_offering_name`,
    `disk_offering`.`display_text` AS `disk_offering_display_text`,
    `disk_offering`.`disk_size` AS `disk_offering_size`,
    `disk_offering`.`customized` AS `disk_offering_custom`,
    GROUP_CONCAT(DISTINCT(nics.uuid) ORDER BY nics.id) AS nic_uuid,
    GROUP_CONCAT(DISTINCT(nics.ip4_address) ORDER BY nics.id) AS ip_address
FROM
    `cloud`.`storage_fileshare`
        LEFT JOIN
    `cloud`.`data_center` AS `zone` ON `storage_fileshare`.`data_center_id` = `zone`.`id`
        LEFT JOIN
    `cloud`.`vm_instance` AS `instance` ON `storage_fileshare`.`vm_id` = `instance`.`id`
        LEFT JOIN
    `cloud`.`volumes` AS `volumes` ON `storage_fileshare`.`volume_id` = `volumes`.`id`
        LEFT JOIN
    `cloud`.`storage_pool` AS `storage_pool` ON `volumes`.`pool_id` = `storage_pool`.`id`
        LEFT JOIN
    `cloud`.`nics` AS `nics` ON `storage_fileshare`.`vm_id` = `nics`.`instance_id`
        LEFT JOIN
    `cloud`.`account` AS `account` ON `storage_fileshare`.`account_id` = `account`.`id`
        LEFT JOIN
    `cloud`.`projects` AS `project` ON `storage_fileshare`.`project_id` = `project`.`id`
        LEFT JOIN
    `cloud`.`domain` AS `domain` ON `storage_fileshare`.`domain_id` = `domain`.`id`
        LEFT JOIN
    `cloud`.`service_offering` AS `service_offering` ON `storage_fileshare`.`service_offering_id` = `service_offering`.`id`
        LEFT JOIN
    `cloud`.`disk_offering` AS `disk_offering` ON `volumes`.`disk_offering_id` = `disk_offering`.`id`
GROUP BY
    `storage_fileshare`.`id`;
