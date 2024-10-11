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

-- cloud.event_view source


DROP VIEW IF EXISTS `cloud`.`event_view`;

CREATE VIEW `cloud`.`event_view` AS
select
    `event`.`id` AS `id`,
    `event`.`uuid` AS `uuid`,
    `event`.`type` AS `type`,
    `event`.`state` AS `state`,
    `event`.`description` AS `description`,
    `event`.`resource_id` AS `resource_id`,
    `event`.`resource_type` AS `resource_type`,
    `event`.`created` AS `created`,
    `event`.`level` AS `level`,
    `event`.`parameters` AS `parameters`,
    `event`.`start_id` AS `start_id`,
    `eve`.`uuid` AS `start_uuid`,
    `event`.`user_id` AS `user_id`,
    `event`.`archived` AS `archived`,
    `event`.`display` AS `display`,
    `user`.`username` AS `user_name`,
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
    (((((`event`
join `account` on
    ((`event`.`account_id` = `account`.`id`)))
join `domain` on
    ((`event`.`domain_id` = `domain`.`id`)))
join `user` on
    ((`event`.`user_id` = `user`.`id`)))
left join `projects` on
    ((`projects`.`project_account_id` = `event`.`account_id`)))
left join `event` `eve` on
    ((`event`.`start_id` = `eve`.`id`)));
