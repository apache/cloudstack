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
-- Schema upgrade from 4.18.1.2
--;

-- Add property to enable/disable on-demand connection Host to PowerFlex storage pool
INSERT IGNORE INTO `cloud`.`configuration` (
    `category`,
    `instance`,
    `component`,
    `scope`,
    `name`,
    `value`,
    `default_value`,
    `is_dynamic`,
    `display_text`,
    `description`
) VALUES (
    'Storage',
    'DEFAULT',
    'StorageManager',
    'Global',
    'powerflex.connect.on.demand',
    'false',
    'false',
    1,
    'Connect PowerFlex client on Host on-demand',
    'Connect PowerFlex client on Host when first Volume created and disconnect when last Volume deleted (or always stay connected otherwise).'
);

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'mgmt_server_id', 'bigint unsigned NULL COMMENT "management server id" ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.resource_reservation', 'created', 'datetime DEFAULT NULL COMMENT "date when the reservation was created" ');

UPDATE `cloud`.`resource_reservation` SET `created` = now() WHERE `created` IS NULL;
