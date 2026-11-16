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

-- cloud.project_invitation_view source


DROP VIEW IF EXISTS `cloud`.`project_invitation_view`;

CREATE VIEW `cloud`.`project_invitation_view` AS
select
    `project_invitations`.`id` AS `id`,
    `project_invitations`.`uuid` AS `uuid`,
    `project_invitations`.`email` AS `email`,
    `project_invitations`.`created` AS `created`,
    `project_invitations`.`state` AS `state`,
    `project_invitations`.`project_role_id` AS `project_role_id`,
    `projects`.`id` AS `project_id`,
    `projects`.`uuid` AS `project_uuid`,
    `projects`.`name` AS `project_name`,
    `account`.`id` AS `account_id`,
    `account`.`uuid` AS `account_uuid`,
    `account`.`account_name` AS `account_name`,
    `account`.`type` AS `account_type`,
    `user`.`id` AS `user_id`,
    `domain`.`id` AS `domain_id`,
    `domain`.`uuid` AS `domain_uuid`,
    `domain`.`name` AS `domain_name`,
    `domain`.`path` AS `domain_path`
from
    ((((`project_invitations`
left join `account` on
    ((`project_invitations`.`account_id` = `account`.`id`)))
left join `domain` on
    ((`project_invitations`.`domain_id` = `domain`.`id`)))
left join `projects` on
    ((`projects`.`id` = `project_invitations`.`project_id`)))
left join `user` on
    ((`project_invitations`.`user_id` = `user`.`id`)));
