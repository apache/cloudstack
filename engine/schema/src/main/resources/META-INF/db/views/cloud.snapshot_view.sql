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

-- VIEW `cloud`.`snapshot_view`;

DROP VIEW IF EXISTS `cloud`.`snapshot_view`;

CREATE VIEW `cloud`.`snapshot_view` AS
SELECT
    `snapshots`.`id` AS `id`,
    `snapshots`.`uuid` AS `uuid`,
    `snapshots`.`name` AS `name`,
    `snapshots`.`status` AS `status`,
    `snapshots`.`disk_offering_id` AS `disk_offering_id`,
    `snapshots`.`snapshot_type` AS `snapshot_type`,
    `snapshots`.`type_description` AS `type_description`,
    `snapshots`.`size` AS `size`,
    `snapshots`.`created` AS `created`,
    `snapshots`.`removed` AS `removed`,
    `snapshots`.`location_type` AS `location_type`,
    `snapshots`.`hypervisor_type` AS `hypervisor_type`,
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
    `volumes`.`id` AS `volume_id`,
    `volumes`.`uuid` AS `volume_uuid`,
    `volumes`.`name` AS `volume_name`,
    `volumes`.`volume_type` AS `volume_type`,
    `volumes`.`state` AS `volume_state`,
    `volumes`.`size` AS `volume_size`,
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `snapshot_store_ref`.`store_id` AS `store_id`,
    IFNULL(`image_store`.`uuid`, `storage_pool`.`uuid`) AS `store_uuid`,
    IFNULL(`image_store`.`name`, `storage_pool`.`name`) AS `store_name`,
    `snapshot_store_ref`.`store_role` AS `store_role`,
    `snapshot_store_ref`.`state` AS `store_state`,
    `snapshot_store_ref`.`download_state` AS `download_state`,
    `snapshot_store_ref`.`download_pct` AS `download_pct`,
    `snapshot_store_ref`.`error_str` AS `error_str`,
    `snapshot_store_ref`.`size` AS `store_size`,
    `snapshot_store_ref`.`created` AS `created_on_store`,
    `resource_tags`.`id` AS `tag_id`,
    `resource_tags`.`uuid` AS `tag_uuid`,
    `resource_tags`.`key` AS `tag_key`,
    `resource_tags`.`value` AS `tag_value`,
    `resource_tags`.`domain_id` AS `tag_domain_id`,
    `domain`.`uuid` AS `tag_domain_uuid`,
    `domain`.`name` AS `tag_domain_name`,
    `resource_tags`.`account_id` AS `tag_account_id`,
    `account`.`account_name` AS `tag_account_name`,
    `resource_tags`.`resource_id` AS `tag_resource_id`,
    `resource_tags`.`resource_uuid` AS `tag_resource_uuid`,
    `resource_tags`.`resource_type` AS `tag_resource_type`,
    `resource_tags`.`customer` AS `tag_customer`,
    CONCAT(`snapshots`.`id`,
           '_',
           IFNULL(`snapshot_store_ref`.`store_role`, 'UNKNOWN'),
           '_',
           IFNULL(`snapshot_store_ref`.`store_id`, 0)) AS `snapshot_store_pair`
FROM
    ((((((((((`snapshots`
        JOIN `account` ON ((`account`.`id` = `snapshots`.`account_id`)))
        JOIN `domain` ON ((`domain`.`id` = `account`.`domain_id`)))
        LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
        LEFT JOIN `volumes` ON ((`volumes`.`id` = `snapshots`.`volume_id`)))
        LEFT JOIN `snapshot_store_ref` ON (((`snapshot_store_ref`.`snapshot_id` = `snapshots`.`id`)
        AND (`snapshot_store_ref`.`state` != 'Destroyed')
        AND (`snapshot_store_ref`.`display` = 1))))
        LEFT JOIN `image_store` ON ((ISNULL(`image_store`.`removed`)
        AND (`snapshot_store_ref`.`store_role` = 'Image')
        AND (`snapshot_store_ref`.`store_id` IS NOT NULL)
        AND (`image_store`.`id` = `snapshot_store_ref`.`store_id`))))
        LEFT JOIN `storage_pool` ON ((ISNULL(`storage_pool`.`removed`)
        AND (`snapshot_store_ref`.`store_role` = 'Primary')
        AND (`snapshot_store_ref`.`store_id` IS NOT NULL)
        AND (`storage_pool`.`id` = `snapshot_store_ref`.`store_id`))))
        LEFT JOIN `snapshot_zone_ref` ON (((`snapshot_zone_ref`.`snapshot_id` = `snapshots`.`id`)
        AND ISNULL(`snapshot_store_ref`.`store_id`)
        AND ISNULL(`snapshot_zone_ref`.`removed`))))
        LEFT JOIN `data_center` ON (((`image_store`.`data_center_id` = `data_center`.`id`)
        OR (`storage_pool`.`data_center_id` = `data_center`.`id`)
        OR (`snapshot_zone_ref`.`zone_id` = `data_center`.`id`))))
        LEFT JOIN `resource_tags` ON ((`resource_tags`.`resource_id` = `snapshots`.`id`)
        AND (`resource_tags`.`resource_type` = 'Snapshot')));
