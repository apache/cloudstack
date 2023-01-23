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

-- select initial ip for VPCs --
ALTER TABLE cloud.network_offerings ADD specify_source_nat_address_allowed tinyint(1) DEFAULT 0 NOT NULL COMMENT 'true if it is allowed to specify the primary public ip for this network on creation';
ALTER TABLE cloud.vpc_offerings ADD specify_source_nat_address_allowed tinyint(1) DEFAULT 0 NOT NULL COMMENT 'true if it is allowed to specify the primary public ip for this vpc on creation';

-- cloud.network_offering_view source

DROP VIEW IF EXISTS `cloud`.`network_offering_view`;
CREATE VIEW `cloud`.`network_offering_view` AS
    SELECT
        `network_offerings`.`id` AS `id`,
        `network_offerings`.`uuid` AS `uuid`,
        `network_offerings`.`name` AS `name`,
        `network_offerings`.`unique_name` AS `unique_name`,
        `network_offerings`.`display_text` AS `display_text`,
        `network_offerings`.`nw_rate` AS `nw_rate`,
        `network_offerings`.`mc_rate` AS `mc_rate`,
        `network_offerings`.`traffic_type` AS `traffic_type`,
        `network_offerings`.`tags` AS `tags`,
        `network_offerings`.`system_only` AS `system_only`,
        `network_offerings`.`specify_vlan` AS `specify_vlan`,
        `network_offerings`.`service_offering_id` AS `service_offering_id`,
        `network_offerings`.`conserve_mode` AS `conserve_mode`,
        `network_offerings`.`created` AS `created`,
        `network_offerings`.`removed` AS `removed`,
        `network_offerings`.`default` AS `default`,
        `network_offerings`.`availability` AS `availability`,
        `network_offerings`.`dedicated_lb_service` AS `dedicated_lb_service`,
        `network_offerings`.`shared_source_nat_service` AS `shared_source_nat_service`,
        `network_offerings`.`sort_key` AS `sort_key`,
        `network_offerings`.`redundant_router_service` AS `redundant_router_service`,
        `network_offerings`.`state` AS `state`,
        `network_offerings`.`guest_type` AS `guest_type`,
        `network_offerings`.`elastic_ip_service` AS `elastic_ip_service`,
        `network_offerings`.`eip_associate_public_ip` AS `eip_associate_public_ip`,
        `network_offerings`.`elastic_lb_service` AS `elastic_lb_service`,
        `network_offerings`.`specify_ip_ranges` AS `specify_ip_ranges`,
        `network_offerings`.`inline` AS `inline`,
        `network_offerings`.`is_persistent` AS `is_persistent`,
        `network_offerings`.`internal_lb` AS `internal_lb`,
        `network_offerings`.`public_lb` AS `public_lb`,
        `network_offerings`.`egress_default_policy` AS `egress_default_policy`,
        `network_offerings`.`concurrent_connections` AS `concurrent_connections`,
        `network_offerings`.`keep_alive_enabled` AS `keep_alive_enabled`,
        `network_offerings`.`supports_streched_l2` AS `supports_streched_l2`,
        `network_offerings`.`supports_public_access` AS `supports_public_access`,
        `network_offerings`.`supports_vm_autoscaling` AS `supports_vm_autoscaling`,
        `network_offerings`.`for_vpc` AS `for_vpc`,
        `network_offerings`.`service_package_id` AS `service_package_id`,
        `network_offerings`.`specify_source_nat_address_allowed` as `specify_source_nat_address_allowed`,
        GROUP_CONCAT(DISTINCT domain.id) AS domain_id,
        GROUP_CONCAT(DISTINCT domain.uuid) AS domain_uuid,
        GROUP_CONCAT(DISTINCT domain.name) AS domain_name,
        GROUP_CONCAT(DISTINCT domain.path) AS domain_path,
        GROUP_CONCAT(DISTINCT zone.id) AS zone_id,
        GROUP_CONCAT(DISTINCT zone.uuid) AS zone_uuid,
        GROUP_CONCAT(DISTINCT zone.name) AS zone_name,
        `offering_details`.value AS internet_protocol
    FROM
        `cloud`.`network_offerings`
            LEFT JOIN
        `cloud`.`network_offering_details` AS `domain_details` ON `domain_details`.`network_offering_id` = `network_offerings`.`id` AND `domain_details`.`name` = 'domainid'
            LEFT JOIN
        `cloud`.`domain` AS `domain` ON FIND_IN_SET(`domain`.`id`, `domain_details`.`value`)
            LEFT JOIN
        `cloud`.`network_offering_details` AS `zone_details` ON `zone_details`.`network_offering_id` = `network_offerings`.`id` AND `zone_details`.`name` = 'zoneid'
            LEFT JOIN
        `cloud`.`data_center` AS `zone` ON FIND_IN_SET(`zone`.`id`, `zone_details`.`value`)
            LEFT JOIN
        `cloud`.`network_offering_details` AS `offering_details` ON `offering_details`.`network_offering_id` = `network_offerings`.`id` AND `offering_details`.`name` = 'internetProtocol'
    GROUP BY
        `network_offerings`.`id`;

-- cloud.vpc_offering_view source

CREATE OR REPLACE
ALGORITHM = UNDEFINED VIEW `vpc_offering_view` AS
select
    `vpc_offerings`.`id` AS `id`,
    `vpc_offerings`.`uuid` AS `uuid`,
    `vpc_offerings`.`name` AS `name`,
    `vpc_offerings`.`unique_name` AS `unique_name`,
    `vpc_offerings`.`display_text` AS `display_text`,
    `vpc_offerings`.`state` AS `state`,
    `vpc_offerings`.`default` AS `default`,
    `vpc_offerings`.`created` AS `created`,
    `vpc_offerings`.`removed` AS `removed`,
    `vpc_offerings`.`service_offering_id` AS `service_offering_id`,
    `vpc_offerings`.`supports_distributed_router` AS `supports_distributed_router`,
    `vpc_offerings`.`supports_region_level_vpc` AS `supports_region_level_vpc`,
    `vpc_offerings`.`redundant_router_service` AS `redundant_router_service`,
    `vpc_offerings`.`sort_key` AS `sort_key`,
    `vpc_offerings`.`specify_source_nat_address_allowed` as `specify_source_nat_address_allowed`,
    group_concat(distinct `domain`.`id` separator ',') AS `domain_id`,
    group_concat(distinct `domain`.`uuid` separator ',') AS `domain_uuid`,
    group_concat(distinct `domain`.`name` separator ',') AS `domain_name`,
    group_concat(distinct `domain`.`path` separator ',') AS `domain_path`,
    group_concat(distinct `zone`.`id` separator ',') AS `zone_id`,
    group_concat(distinct `zone`.`uuid` separator ',') AS `zone_uuid`,
    group_concat(distinct `zone`.`name` separator ',') AS `zone_name`,
    `offering_details`.`value` AS `internet_protocol`
from
    (((((`vpc_offerings`
left join `vpc_offering_details` `domain_details` on
    (((`domain_details`.`offering_id` = `vpc_offerings`.`id`)
        and (`domain_details`.`name` = 'domainid'))))
left join `domain` on
    ((0 <> find_in_set(`domain`.`id`, `domain_details`.`value`))))
left join `vpc_offering_details` `zone_details` on
    (((`zone_details`.`offering_id` = `vpc_offerings`.`id`)
        and (`zone_details`.`name` = 'zoneid'))))
left join `data_center` `zone` on
    ((0 <> find_in_set(`zone`.`id`, `zone_details`.`value`))))
left join `vpc_offering_details` `offering_details` on
    (((`offering_details`.`offering_id` = `vpc_offerings`.`id`)
        and (`offering_details`.`name` = 'internetprotocol'))))
group by
    `vpc_offerings`.`id`;
