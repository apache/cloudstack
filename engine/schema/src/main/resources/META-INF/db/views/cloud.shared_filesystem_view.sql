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

-- VIEW `cloud`.`shared_filesystem_view`;

DROP VIEW IF EXISTS `cloud`.`shared_filesystem_view`;

CREATE VIEW `cloud`.`shared_filesystem_view` AS
SELECT
    `shared_filesystem`.`id` AS `id`,
    `shared_filesystem`.`uuid` AS `uuid`,
    `shared_filesystem`.`name` AS `name`,
    `shared_filesystem`.`description` AS `description`,
    `shared_filesystem`.`state` AS `state`,
    `shared_filesystem`.`fs_provider_name` AS `provider`,
    `shared_filesystem`.`fs_type` AS `fs_type`,
    `shared_filesystem`.`volume_id` AS `volume_id`,
    `shared_filesystem`.`account_id` AS `account_id`,
    `shared_filesystem`.`data_center_id` AS `zone_id`,
    `zone`.`uuid` AS `zone_uuid`,
    `zone`.`name` AS `zone_name`,
    `instance`.`id` AS `instance_id`,
    `instance`.`uuid` AS `instance_uuid`,
    `instance`.`name` AS `instance_name`,
    `instance`.`state` AS `instance_state`,
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
    `domain`.`path` AS `domain_path`,
    `service_offering`.`uuid` AS `service_offering_uuid`,
    `service_offering`.`name` AS `service_offering_name`,
    `disk_offering`.`uuid` AS `disk_offering_uuid`,
    `disk_offering`.`name` AS `disk_offering_name`,
    `disk_offering`.`display_text` AS `disk_offering_display_text`,
    `disk_offering`.`disk_size` AS `disk_offering_size`,
    `disk_offering`.`customized` AS `disk_offering_custom`
FROM
    `cloud`.`shared_filesystem`
        LEFT JOIN
    `cloud`.`data_center` AS `zone` ON `shared_filesystem`.`data_center_id` = `zone`.`id`
        LEFT JOIN
    `cloud`.`vm_instance` AS `instance` ON `shared_filesystem`.`vm_id` = `instance`.`id`
        LEFT JOIN
    `cloud`.`volumes` AS `volumes` ON `shared_filesystem`.`volume_id` = `volumes`.`id`
        LEFT JOIN
    `cloud`.`storage_pool` AS `storage_pool` ON `volumes`.`pool_id` = `storage_pool`.`id`
        LEFT JOIN
    `cloud`.`account` AS `account` ON `shared_filesystem`.`account_id` = `account`.`id`
        LEFT JOIN
    `cloud`.`projects` AS `project` ON `project`.`project_account_id` = `account`.`id`
        LEFT JOIN
    `cloud`.`domain` AS `domain` ON `shared_filesystem`.`domain_id` = `domain`.`id`
        LEFT JOIN
    `cloud`.`service_offering` AS `service_offering` ON `shared_filesystem`.`service_offering_id` = `service_offering`.`id`
        LEFT JOIN
    `cloud`.`disk_offering` AS `disk_offering` ON `volumes`.`disk_offering_id` = `disk_offering`.`id`
GROUP BY
    `shared_filesystem`.`id`;
