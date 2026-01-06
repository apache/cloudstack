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


DROP VIEW IF EXISTS `cloud`.`mshost_peer_view`;

CREATE VIEW `cloud`.`mshost_peer_view` AS
SELECT
    `mshost_peer`.`id` AS `id`,
    `mshost_peer`.`peer_state` AS `peer_state`,
    `mshost_peer`.`last_update` AS `last_update`,
    `owner_mshost`.`id` AS `owner_mshost_id`,
    `owner_mshost`.`msid` AS `owner_mshost_msid`,
    `owner_mshost`.`runid` AS `owner_mshost_runid`,
    `owner_mshost`.`name` AS `owner_mshost_name`,
    `owner_mshost`.`uuid` AS `owner_mshost_uuid`,
    `owner_mshost`.`state` AS `owner_mshost_state`,
    `owner_mshost`.`service_ip` AS `owner_mshost_service_ip`,
    `owner_mshost`.`service_port` AS `owner_mshost_service_port`,
    `peer_mshost`.`id` AS `peer_mshost_id`,
    `peer_mshost`.`msid` AS `peer_mshost_msid`,
    `peer_mshost`.`runid` AS `peer_mshost_runid`,
    `peer_mshost`.`name` AS `peer_mshost_name`,
    `peer_mshost`.`uuid` AS `peer_mshost_uuid`,
    `peer_mshost`.`state` AS `peer_mshost_state`,
    `peer_mshost`.`service_ip` AS `peer_mshost_service_ip`,
    `peer_mshost`.`service_port` AS `peer_mshost_service_port`
FROM `cloud`.`mshost_peer`
LEFT JOIN `cloud`.`mshost` AS owner_mshost on `mshost_peer`.`owner_mshost` = `owner_mshost`.`id`
LEFT JOIN `cloud`.`mshost` AS peer_mshost on `mshost_peer`.`peer_mshost` = `peer_mshost`.`id`;
