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

-- cloud.instance_group_view source


DROP VIEW IF EXISTS `cloud`.`instance_group_view`;

CREATE VIEW `cloud`.`instance_group_view` AS
select
    `instance_group`.`id` AS `id`,
    `instance_group`.`uuid` AS `uuid`,
    `instance_group`.`name` AS `name`,
    `instance_group`.`removed` AS `removed`,
    `instance_group`.`created` AS `created`,
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
    (((`instance_group`
join `account` on
    ((`instance_group`.`account_id` = `account`.`id`)))
join `domain` on
    ((`account`.`domain_id` = `domain`.`id`)))
left join `projects` on
    ((`projects`.`project_account_id` = `instance_group`.`account_id`)));
