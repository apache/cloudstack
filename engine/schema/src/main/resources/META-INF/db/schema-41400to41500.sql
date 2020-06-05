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
-- Schema upgrade from 4.14.0.0 to 4.15.0.0
--;
-- [ADD/DELETE USER TO PROJECT] add role permission for adding user to project
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'addUserToProject', 'ALLOW', 2) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'addUserToProject', 'ALLOW', 2) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'addUserToProject', 'ALLOW', 2) ON DUPLICATE KEY UPDATE rule=rule;

   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'deleteUserFromProject', 'ALLOW', 71) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'deleteUserFromProject', 'ALLOW', 69) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'deleteUserFromProject', 'ALLOW', 55) ON DUPLICATE KEY UPDATE rule=rule;

-- [PROJECT ROLE] add role permissions for the CRUD API commands for projectRole
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'createProjectRole', 'ALLOW', 51) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'deleteProjectRole', 'ALLOW', 98) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'listProjectRoles', 'ALLOW', 201) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'updateProjectRole', 'ALLOW', 301) ON DUPLICATE KEY UPDATE rule=rule;

   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'createProjectRole', 'ALLOW', 49) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'deleteProjectRole', 'ALLOW', 93) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'listProjectRoles', 'ALLOW', 188) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'updateProjectRole', 'ALLOW', 285) ON DUPLICATE KEY UPDATE rule=rule;

   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'createProjectRole', 'ALLOW', 39) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'deleteProjectRole', 'ALLOW', 78) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'listProjectRoles', 'ALLOW', 161) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'updateProjectRole', 'ALLOW', 246) ON DUPLICATE KEY UPDATE rule=rule;

-- [PROJECT ROLE PERMISSION] add role permissions for the CRUD API commands for projectRolePermission
    INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'createProjectRolePermission', 'ALLOW', 52) ON DUPLICATE KEY UPDATE rule=rule;
    INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'deleteProjectRolePermission', 'ALLOW', 99) ON DUPLICATE KEY UPDATE rule=rule;
    INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'listProjectRolePermissions', 'ALLOW', 202) ON DUPLICATE KEY UPDATE rule=rule;
    INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 2, 'updateProjectRolePermission', 'ALLOW', 302) ON DUPLICATE KEY UPDATE rule=rule;

   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'createProjectRolePermission', 'ALLOW', 50) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'deleteProjectRolePermission', 'ALLOW', 94) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'listProjectRolePermissions', 'ALLOW', 189) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 3, 'updateProjectRolePermission', 'ALLOW', 286) ON DUPLICATE KEY UPDATE rule=rule;

   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'createProjectRolePermission', 'ALLOW', 40) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'deleteProjectRolePermission', 'ALLOW', 79) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'listProjectRolePermissions', 'ALLOW', 162) ON DUPLICATE KEY UPDATE rule=rule;
   INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) values (UUID(), 4, 'updateProjectRolePermission', 'ALLOW', 247) ON DUPLICATE KEY UPDATE rule=rule;

-- Project roles
CREATE TABLE IF NOT EXISTS `cloud`.`project_role` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `name` varchar(255) COMMENT 'unique name of the dynamic project role',
  `removed` datetime COMMENT 'date removed',
  `description` text COMMENT 'description of the project role',
  `project_id` bigint(20) unsigned COMMENT 'Id of the project to which the role belongs',
  PRIMARY KEY (`id`),
  KEY `i_project_role__name` (`name`),
  UNIQUE KEY (`name`),
  CONSTRAINT `fk_project_role__project_id` FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Project role permissions table
CREATE TABLE IF NOT EXISTS `cloud`.`project_role_permissions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uuid` varchar(255) UNIQUE,
  `project_id` bigint(20) unsigned NOT NULL COMMENT 'id of the role',
  `project_role_id` bigint(20) unsigned NOT NULL COMMENT 'id of the role',
  `rule` varchar(255) NOT NULL COMMENT 'rule for the role, api name or wildcard',
  `permission` varchar(255) NOT NULL COMMENT 'access authority, allow or deny',
  `description` text COMMENT 'description of the rule',
  `sort_order` bigint(20) unsigned NOT NULL DEFAULT 0 COMMENT 'permission sort order',
  PRIMARY KEY (`id`),
  KEY `fk_project_role_permissions__project_role_id` (`project_role_id`),
  KEY `i_project_role_permissions__sort_order` (`sort_order`),
  UNIQUE KEY (`project_role_id`, `rule`),
  CONSTRAINT `fk_project_role_permissions__project_id` FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_project_role_permissions__project_role_id` FOREIGN KEY (`project_role_id`) REFERENCES `project_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Alter project accounts table to include user_id and project_role_id for role based users in projects
ALTER TABLE `cloud`.`project_account`
    ADD COLUMN `user_id` bigint unsigned COMMENT 'ID of user to be added to the project' AFTER `account_id`,
    ADD CONSTRAINT `fk_project_account__user_id` FOREIGN KEY `fk_project_account__user_id`(`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    ADD COLUMN `project_role_id` bigint unsigned COMMENT 'Project role id' AFTER `project_account_id`,
    ADD CONSTRAINT `fk_project_account__project_role_id` FOREIGN KEY (`project_role_id`) REFERENCES `project_role` (`id`),
    -- DROP INDEX `account_id`,
    ADD UNIQUE (`user_id`, `account_id`, `project_id`);

-- Alter project invitations table to include user_id for invites sent to specific users of an account
ALTER TABLE `cloud`.`project_invitations`
    ADD COLUMN `user_id` bigint unsigned COMMENT 'ID of user to be added to the project' AFTER `account_id`,
    ADD COLUMN `account_role` varchar(255) NOT NULL DEFAULT 'Regular' COMMENT 'Account role in the project (Owner or Regular)' AFTER `domain_id`,
    ADD CONSTRAINT `fk_project_invitations__user_id` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    ADD UNIQUE (`project_id`, `user_id`);

-- Alter project_invitation_view to incroporate user_id as a field
ALTER VIEW `cloud`.`project_invitation_view` AS
    select
        project_invitations.id,
        project_invitations.uuid,
        project_invitations.email,
        project_invitations.created,
        project_invitations.state,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name,
        account.type account_type,
        user.id user_id,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`project_invitations`
            left join
        `cloud`.`account` ON project_invitations.account_id = account.id
            left join
        `cloud`.`domain` ON project_invitations.domain_id = domain.id
            left join
        `cloud`.`projects` ON projects.id = project_invitations.project_id
            left join
        `cloud`.`user` ON project_invitations.user_id = user.id;

-- Alter project_account_view to incorporate user id
ALTER VIEW `cloud`.`project_account_view` AS
    select
        project_account.id,
        account.id account_id,
        account.uuid account_uuid,
        account.account_name,
        account.type account_type,
        user.id user_id,
        user.uuid user_uuid,
        project_account.account_role,
        projects.id project_id,
        projects.uuid project_uuid,
        projects.name project_name,
        domain.id domain_id,
        domain.uuid domain_uuid,
        domain.name domain_name,
        domain.path domain_path
    from
        `cloud`.`project_account`
            inner join
        `cloud`.`account` ON project_account.account_id = account.id
            inner join
        `cloud`.`domain` ON account.domain_id = domain.id
            inner join
        `cloud`.`projects` ON projects.id = project_account.project_id
            left join
        `cloud`.`user` ON (project_account.user_id = user.id);
