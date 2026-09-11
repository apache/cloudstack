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
-- Schema upgrade from 4.23.0.0 to 24.0.0
--;

-- Add host_stats table for the host usage history
CREATE TABLE IF NOT EXISTS `cloud`.`host_stats` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
  `host_id` bigint unsigned NOT NULL,
  `mgmt_server_id` bigint unsigned NOT NULL,
  `timestamp` datetime NOT NULL,
  `host_stats_data` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `i_host_stats__host_id` (`host_id`),
  KEY `i_host_stats__timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='historical per-host stats samples';
