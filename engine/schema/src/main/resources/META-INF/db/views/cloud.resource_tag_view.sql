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

-- cloud.resource_tag_view source


DROP VIEW IF EXISTS `cloud`.`resource_tag_view`;

CREATE VIEW `cloud`.`resource_tag_view` AS
select
    `resource_tags`.`id` AS `id`,
    `resource_tags`.`uuid` AS `uuid`,
    `resource_tags`.`key` AS `key`,
    `resource_tags`.`value` AS `value`,
    `resource_tags`.`resource_id` AS `resource_id`,
    `resource_tags`.`resource_uuid` AS `resource_uuid`,
    `resource_tags`.`resource_type` AS `resource_type`,
    `resource_tags`.`customer` AS `customer`,
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
    `projects`.`name` AS `project_name`
from
    (((`resource_tags`
join `account` on
    ((`resource_tags`.`account_id` = `account`.`id`)))
join `domain` on
    ((`resource_tags`.`domain_id` = `domain`.`id`)))
left join `projects` on
    ((`projects`.`project_account_id` = `resource_tags`.`account_id`)));
