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

-- cloud.mshost_view source


DROP VIEW IF EXISTS `cloud`.`mshost_view`;

CREATE VIEW `cloud`.`mshost_view` AS
select
    `mshost`.`id` AS `id`,
    `mshost`.`msid` AS `msid`,
    `mshost`.`runid` AS `runid`,
    `mshost`.`name` AS `name`,
    `mshost`.`uuid` AS `uuid`,
    `mshost`.`state` AS `state`,
    `mshost`.`version` AS `version`,
    `mshost`.`service_ip` AS `service_ip`,
    `mshost`.`service_port` AS `service_port`,
    `mshost`.`last_update` AS `last_update`,
    `mshost`.`removed` AS `removed`,
    `mshost`.`alert_count` AS `alert_count`,
    `mshost_status`.`last_jvm_start` AS `last_jvm_start`,
    `mshost_status`.`last_jvm_stop` AS `last_jvm_stop`,
    `mshost_status`.`last_system_boot` AS `last_system_boot`,
    `mshost_status`.`os_distribution` AS `os_distribution`,
    `mshost_status`.`java_name` AS `java_name`,
    `mshost_status`.`java_version` AS `java_version`
from
    (`mshost`
left join `mshost_status` on
    ((`mshost`.`uuid` = `mshost_status`.`ms_id`)));
