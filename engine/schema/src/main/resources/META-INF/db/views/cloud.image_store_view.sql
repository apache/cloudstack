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

-- cloud.image_store_view source


DROP VIEW IF EXISTS `cloud`.`image_store_view`;

CREATE VIEW `cloud`.`image_store_view` AS
select
    `image_store`.`id` AS `id`,
    `image_store`.`uuid` AS `uuid`,
    `image_store`.`name` AS `name`,
    `image_store`.`image_provider_name` AS `image_provider_name`,
    `image_store`.`protocol` AS `protocol`,
    `image_store`.`url` AS `url`,
    `image_store`.`scope` AS `scope`,
    `image_store`.`role` AS `role`,
    `image_store`.`readonly` AS `readonly`,
    `image_store`.`removed` AS `removed`,
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `image_store_details`.`name` AS `detail_name`,
    `image_store_details`.`value` AS `detail_value`
from
    ((`image_store`
left join `data_center` on
    ((`image_store`.`data_center_id` = `data_center`.`id`)))
left join `image_store_details` on
    ((`image_store_details`.`store_id` = `image_store`.`id`)));
