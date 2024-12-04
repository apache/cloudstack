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
-- Schema upgrade from 4.20.0.0 to 4.20.1.0
--;

-- Add column api_key_access to user and account tables
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.user', 'api_key_access', 'boolean DEFAULT NULL COMMENT "is api key access allowed for the user" AFTER `secret_key`');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.account', 'api_key_access', 'boolean DEFAULT NULL COMMENT "is api key access allowed for the account" ');

-- Modify index for mshost_peer
DELETE FROM `cloud`.`mshost_peer`;
CALL `cloud`.`IDEMPOTENT_DROP_FOREIGN_KEY`('cloud.mshost_peer','fk_mshost_peer__owner_mshost');
CALL `cloud`.`IDEMPOTENT_DROP_INDEX`('i_mshost_peer__owner_peer_runid','mshost_peer');
CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_KEY`('cloud.mshost_peer', 'i_mshost_peer__owner_peer', '(owner_mshost, peer_mshost)');
CALL `cloud`.`IDEMPOTENT_ADD_FOREIGN_KEY`('cloud.mshost_peer', 'fk_mshost_peer__owner_mshost', '(owner_mshost)', '`mshost`(`id`)');

-- Add last_id to the volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'last_id', 'bigint(20) unsigned DEFAULT NULL');
