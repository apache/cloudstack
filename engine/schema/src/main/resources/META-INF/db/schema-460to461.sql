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
-- Schema upgrade from 4.6.0 to 4.6.1;
--;
DROP VIEW IF EXISTS `cloud`.`affinity_group_view`;
CREATE VIEW `affinity_group_view`
	AS SELECT
	   `affinity_group`.`id` AS `id`,
	   `affinity_group`.`name` AS `name`,
	   `affinity_group`.`type` AS `type`,
	   `affinity_group`.`description` AS `description`,
	   `affinity_group`.`uuid` AS `uuid`,
	   `affinity_group`.`acl_type` AS `acl_type`,
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
	   `projects`.`name` AS `project_name`,
	   `vm_instance`.`id` AS `vm_id`,
	   `vm_instance`.`uuid` AS `vm_uuid`,
	   `vm_instance`.`name` AS `vm_name`,
	   `vm_instance`.`state` AS `vm_state`,
	   `user_vm`.`display_name` AS `vm_display_name`
FROM `affinity_group`
	JOIN `account` ON`affinity_group`.`account_id` = `account`.`id`
	JOIN `domain` ON`affinity_group`.`domain_id` = `domain`.`id`
	LEFT JOIN `projects` ON`projects`.`project_account_id` = `account`.`id`
	LEFT JOIN `affinity_group_vm_map` ON`affinity_group`.`id` = `affinity_group_vm_map`.`affinity_group_id`
	LEFT JOIN `vm_instance` ON`vm_instance`.`id` = `affinity_group_vm_map`.`instance_id`
	LEFT JOIN `user_vm` ON`user_vm`.`id` = `vm_instance`.`id`;
