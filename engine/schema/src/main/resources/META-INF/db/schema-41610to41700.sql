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
-- Schema upgrade from 4.16.1.0 to 4.17.0.0
--;

-- PR#5668 Change the type of the 'ipsec_psk' field to allow large PSK.
ALTER TABLE cloud.remote_access_vpn MODIFY ipsec_psk text NOT NULL;

--- table for network permissions
CREATE TABLE  `cloud`.`network_permissions` (
  `id` bigint unsigned NOT NULL auto_increment,
  `network_id` bigint unsigned NOT NULL,
  `account_id` bigint unsigned NOT NULL,
  PRIMARY KEY  (`id`),
  INDEX `i_network_permission_network_id`(`network_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
