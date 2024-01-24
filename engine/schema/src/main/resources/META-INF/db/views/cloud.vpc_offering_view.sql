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

-- VIEW `cloud`.`vpc_offering_view`;

DROP VIEW IF EXISTS `cloud`.`vpc_offering_view`;

CREATE VIEW `cloud`.`vpc_offering_view` AS
SELECT
    `vpc_offerings`.`id` AS `id`,
    `vpc_offerings`.`uuid` AS `uuid`,
    `vpc_offerings`.`name` AS `name`,
    `vpc_offerings`.`unique_name` AS `unique_name`,
    `vpc_offerings`.`display_text` AS `display_text`,
    `vpc_offerings`.`state` AS `state`,
    `vpc_offerings`.`default` AS `default`,
    `vpc_offerings`.`for_nsx` AS `for_nsx`,
    `vpc_offerings`.`nsx_mode` AS `nsx_mode`,
    `vpc_offerings`.`created` AS `created`,
    `vpc_offerings`.`removed` AS `removed`,
    `vpc_offerings`.`service_offering_id` AS `service_offering_id`,
    `vpc_offerings`.`supports_distributed_router` AS `supports_distributed_router`,
    `vpc_offerings`.`supports_region_level_vpc` AS `supports_region_level_vpc`,
    `vpc_offerings`.`redundant_router_service` AS `redundant_router_service`,
    `vpc_offerings`.`sort_key` AS `sort_key`,
    GROUP_CONCAT(DISTINCT(domain.id)) AS domain_id,
    GROUP_CONCAT(DISTINCT(domain.uuid)) AS domain_uuid,
    GROUP_CONCAT(DISTINCT(domain.name)) AS domain_name,
    GROUP_CONCAT(DISTINCT(domain.path)) AS domain_path,
    GROUP_CONCAT(DISTINCT(zone.id)) AS zone_id,
    GROUP_CONCAT(DISTINCT(zone.uuid)) AS zone_uuid,
    GROUP_CONCAT(DISTINCT(zone.name)) AS zone_name,
    `offering_details`.value AS internet_protocol
FROM
    `cloud`.`vpc_offerings`
        LEFT JOIN
    `cloud`.`vpc_offering_details` AS `domain_details` ON `domain_details`.`offering_id` = `vpc_offerings`.`id` AND `domain_details`.`name`='domainid'
        LEFT JOIN
    `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
        LEFT JOIN
    `cloud`.`vpc_offering_details` AS `zone_details` ON `zone_details`.`offering_id` = `vpc_offerings`.`id` AND `zone_details`.`name`='zoneid'
        LEFT JOIN
    `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
        LEFT JOIN
    `cloud`.`vpc_offering_details` AS `offering_details` ON `offering_details`.`offering_id` = `vpc_offerings`.`id` AND `offering_details`.`name`='internetprotocol'
GROUP BY
    `vpc_offerings`.`id`;
