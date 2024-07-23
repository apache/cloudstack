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

-- cloud.vpc_offering_view source

DROP VIEW IF EXISTS `cloud`.`vpc_offering_view`;

CREATE VIEW `cloud`.`vpc_offering_view` AS
select
    `vpc_offerings`.`id` AS `id`,
    `vpc_offerings`.`uuid` AS `uuid`,
    `vpc_offerings`.`name` AS `name`,
    `vpc_offerings`.`unique_name` AS `unique_name`,
    `vpc_offerings`.`display_text` AS `display_text`,
    `vpc_offerings`.`state` AS `state`,
    `vpc_offerings`.`default` AS `default`,
    `vpc_offerings`.`for_nsx` AS `for_nsx`,
    `vpc_offerings`.`network_mode` AS `network_mode`,
    `vpc_offerings`.`created` AS `created`,
    `vpc_offerings`.`removed` AS `removed`,
    `vpc_offerings`.`service_offering_id` AS `service_offering_id`,
    `vpc_offerings`.`supports_distributed_router` AS `supports_distributed_router`,
    `vpc_offerings`.`supports_region_level_vpc` AS `supports_region_level_vpc`,
    `vpc_offerings`.`redundant_router_service` AS `redundant_router_service`,
    `vpc_offerings`.`sort_key` AS `sort_key`,
    `vpc_offerings`.`routing_mode` AS `routing_mode`,
    `vpc_offerings`.`specify_as_number` AS `specify_as_number`,
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
