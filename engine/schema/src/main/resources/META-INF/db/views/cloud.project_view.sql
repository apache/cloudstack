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

-- cloud.project_view source


DROP VIEW IF EXISTS `cloud`.`project_view`;

CREATE VIEW `cloud`.`project_view` AS
select
    `projects`.`id` AS `id`,
    `projects`.`uuid` AS `uuid`,
    `projects`.`name` AS `name`,
    `projects`.`display_text` AS `display_text`,
    `projects`.`state` AS `state`,
    `projects`.`removed` AS `removed`,
    `projects`.`created` AS `created`,
    `projects`.`project_account_id` AS `project_account_id`,
    `account`.`account_name` AS `owner`,
    `pacct`.`account_id` AS `account_id`,
    `pacct`.`user_id` AS `user_id`,
    `domain`.`id` AS `domain_id`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `domain`.`path` AS `domain_path`
from
    ((((`projects`
join `domain` on
    ((`projects`.`domain_id` = `domain`.`id`)))
join `project_account` on
    (((`projects`.`id` = `project_account`.`project_id`)
        and (`project_account`.`account_role` = 'Admin'))))
join `account` on
    ((`account`.`id` = `project_account`.`account_id`)))
left join `project_account` `pacct` on
    ((`projects`.`id` = `pacct`.`project_id`)));
