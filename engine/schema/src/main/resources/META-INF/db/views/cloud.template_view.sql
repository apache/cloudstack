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

-- VIEW `cloud`.`template_view`;

DROP VIEW IF EXISTS `cloud`.`template_view`;

CREATE VIEW `cloud`.`template_view` AS
SELECT
    `vm_template`.`id` AS `id`,
    `vm_template`.`uuid` AS `uuid`,
    `vm_template`.`unique_name` AS `unique_name`,
    `vm_template`.`name` AS `name`,
    `vm_template`.`public` AS `public`,
    `vm_template`.`featured` AS `featured`,
    `vm_template`.`type` AS `type`,
    `vm_template`.`hvm` AS `hvm`,
    `vm_template`.`bits` AS `bits`,
    `vm_template`.`url` AS `url`,
    `vm_template`.`format` AS `format`,
    `vm_template`.`created` AS `created`,
    `vm_template`.`checksum` AS `checksum`,
    `vm_template`.`display_text` AS `display_text`,
    `vm_template`.`enable_password` AS `enable_password`,
    `vm_template`.`dynamically_scalable` AS `dynamically_scalable`,
    `vm_template`.`state` AS `template_state`,
    `vm_template`.`guest_os_id` AS `guest_os_id`,
    `guest_os`.`uuid` AS `guest_os_uuid`,
    `guest_os`.`display_name` AS `guest_os_name`,
    `vm_template`.`bootable` AS `bootable`,
    `vm_template`.`prepopulate` AS `prepopulate`,
    `vm_template`.`cross_zones` AS `cross_zones`,
    `vm_template`.`hypervisor_type` AS `hypervisor_type`,
    `vm_template`.`extractable` AS `extractable`,
    `vm_template`.`template_tag` AS `template_tag`,
    `vm_template`.`sort_key` AS `sort_key`,
    `vm_template`.`removed` AS `removed`,
    `vm_template`.`enable_sshkey` AS `enable_sshkey`,
    `vm_template`.`arch` AS `arch`,
    `parent_template`.`id` AS `parent_template_id`,
    `parent_template`.`uuid` AS `parent_template_uuid`,
    `source_template`.`id` AS `source_template_id`,
    `source_template`.`uuid` AS `source_template_uuid`,
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
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `launch_permission`.`account_id` AS `lp_account_id`,
    `template_store_ref`.`store_id` AS `store_id`,
    `image_store`.`scope` AS `store_scope`,
    `template_store_ref`.`state` AS `state`,
    `template_store_ref`.`download_state` AS `download_state`,
    `template_store_ref`.`download_pct` AS `download_pct`,
    `template_store_ref`.`error_str` AS `error_str`,
    `template_store_ref`.`size` AS `size`,
    `template_store_ref`.physical_size AS `physical_size`,
    `template_store_ref`.`destroyed` AS `destroyed`,
    `template_store_ref`.`created` AS `created_on_store`,
    `vm_template_details`.`name` AS `detail_name`,
    `vm_template_details`.`value` AS `detail_value`,
    `resource_tags`.`id` AS `tag_id`,
    `resource_tags`.`uuid` AS `tag_uuid`,
    `resource_tags`.`key` AS `tag_key`,
    `resource_tags`.`value` AS `tag_value`,
    `resource_tags`.`domain_id` AS `tag_domain_id`,
    `domain`.`uuid` AS `tag_domain_uuid`,
    `domain`.`name` AS `tag_domain_name`,
    `resource_tags`.`account_id` AS `tag_account_id`,
    `account`.`account_name` AS `tag_account_name`,
    `resource_tags`.`resource_id` AS `tag_resource_id`,
    `resource_tags`.`resource_uuid` AS `tag_resource_uuid`,
    `resource_tags`.`resource_type` AS `tag_resource_type`,
    `resource_tags`.`customer` AS `tag_customer`,
    CONCAT(`vm_template`.`id`,
           '_',
           IFNULL(`data_center`.`id`, 0)) AS `temp_zone_pair`,
    `vm_template`.`direct_download` AS `direct_download`,
    `vm_template`.`deploy_as_is` AS `deploy_as_is`,
    `user_data`.`id` AS `user_data_id`,
    `user_data`.`uuid` AS `user_data_uuid`,
    `user_data`.`name` AS `user_data_name`,
    `user_data`.`params` AS `user_data_params`,
    `vm_template`.`user_data_link_policy` AS `user_data_policy`
FROM
    (((((((((((((`vm_template`
        JOIN `guest_os` ON ((`guest_os`.`id` = `vm_template`.`guest_os_id`)))
        JOIN `account` ON ((`account`.`id` = `vm_template`.`account_id`)))
        JOIN `domain` ON ((`domain`.`id` = `account`.`domain_id`)))
        LEFT JOIN `projects` ON ((`projects`.`project_account_id` = `account`.`id`)))
        LEFT JOIN `vm_template_details` ON ((`vm_template_details`.`template_id` = `vm_template`.`id`)))
        LEFT JOIN `vm_template` `source_template` ON ((`source_template`.`id` = `vm_template`.`source_template_id`)))
        LEFT JOIN `template_store_ref` ON (((`template_store_ref`.`template_id` = `vm_template`.`id`)
            AND (`template_store_ref`.`store_role` = 'Image')
            AND (`template_store_ref`.`destroyed` = 0))))
        LEFT JOIN `vm_template` `parent_template` ON ((`parent_template`.`id` = `vm_template`.`parent_template_id`)))
        LEFT JOIN `image_store` ON ((ISNULL(`image_store`.`removed`)
            AND (`template_store_ref`.`store_id` IS NOT NULL)
            AND (`image_store`.`id` = `template_store_ref`.`store_id`))))
        LEFT JOIN `template_zone_ref` ON (((`template_zone_ref`.`template_id` = `vm_template`.`id`)
            AND ISNULL(`template_store_ref`.`store_id`)
            AND ISNULL(`template_zone_ref`.`removed`))))
        LEFT JOIN `data_center` ON (((`image_store`.`data_center_id` = `data_center`.`id`)
            OR (`template_zone_ref`.`zone_id` = `data_center`.`id`))))
        LEFT JOIN `launch_permission` ON ((`launch_permission`.`template_id` = `vm_template`.`id`)))
        LEFT JOIN `user_data` ON ((`user_data`.`id` = `vm_template`.`user_data_id`))
        LEFT JOIN `resource_tags` ON (((`resource_tags`.`resource_id` = `vm_template`.`id`)
        AND ((`resource_tags`.`resource_type` = 'Template')
            OR (`resource_tags`.`resource_type` = 'ISO')))));
