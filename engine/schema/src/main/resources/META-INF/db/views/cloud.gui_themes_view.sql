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

-- VIEW `cloud`.`gui_themes_view`;

DROP VIEW IF EXISTS `cloud`.`gui_themes_view`;

CREATE VIEW `cloud`.`gui_themes_view` AS
SELECT
    `cloud`.`gui_themes`.`id` AS `id`,
    `cloud`.`gui_themes`.`uuid` AS `uuid`,
    `cloud`.`gui_themes`.`name` AS `name`,
    `cloud`.`gui_themes`.`description` AS `description`,
    `cloud`.`gui_themes`.`css` AS `css`,
    `cloud`.`gui_themes`.`json_configuration` AS `json_configuration`,
    (SELECT group_concat(gtd.`value` separator ',') FROM `cloud`.`gui_themes_details` gtd WHERE gtd.`type` = 'commonName' AND gtd.gui_theme_id = `cloud`.`gui_themes`.`id`) common_names,
    (SELECT group_concat(gtd.`value` separator ',') FROM `cloud`.`gui_themes_details` gtd WHERE gtd.`type` = 'domain' AND gtd.gui_theme_id = `cloud`.`gui_themes`.`id`) domains,
    (SELECT group_concat(gtd.`value` separator ',') FROM `cloud`.`gui_themes_details` gtd WHERE gtd.`type` = 'account' AND gtd.gui_theme_id = `cloud`.`gui_themes`.`id`) accounts,
    `cloud`.`gui_themes`.`recursive_domains` AS `recursive_domains`,
    `cloud`.`gui_themes`.`is_public` AS `is_public`,
    `cloud`.`gui_themes`.`created` AS `created`,
    `cloud`.`gui_themes`.`removed` AS `removed`
FROM `cloud`.`gui_themes` LEFT JOIN `cloud`.`gui_themes_details` ON `cloud`.`gui_themes_details`.`gui_theme_id` = `cloud`.`gui_themes`.`id`
GROUP BY `cloud`.`gui_themes`.`id`;
