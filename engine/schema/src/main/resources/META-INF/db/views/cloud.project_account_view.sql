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

-- cloud.project_account_view source


DROP VIEW IF EXISTS `cloud`.`project_account_view`;

CREATE VIEW `cloud`.`project_account_view` AS
select
    `project_account`.`id` AS `id`,
    `account`.`id` AS `account_id`,
    `account`.`uuid` AS `account_uuid`,
    `account`.`account_name` AS `account_name`,
    `account`.`type` AS `account_type`,
    `user`.`id` AS `user_id`,
    `user`.`uuid` AS `user_uuid`,
    `user`.`username` AS `user_name`,
    `project_account`.`account_role` AS `account_role`,
    `project_role`.`id` AS `project_role_id`,
    `project_role`.`uuid` AS `project_role_uuid`,
    `projects`.`id` AS `project_id`,
    `projects`.`uuid` AS `project_uuid`,
    `projects`.`name` AS `project_name`,
    `domain`.`id` AS `domain_id`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `domain`.`path` AS `domain_path`
from
    (((((`project_account`
join `account` on
    ((`project_account`.`account_id` = `account`.`id`)))
join `domain` on
    ((`account`.`domain_id` = `domain`.`id`)))
join `projects` on
    ((`projects`.`id` = `project_account`.`project_id`)))
left join `project_role` on
    ((`project_account`.`project_role_id` = `project_role`.`id`)))
left join `user` on
    ((`project_account`.`user_id` = `user`.`id`)));
