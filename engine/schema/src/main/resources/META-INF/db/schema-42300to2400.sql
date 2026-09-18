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

-- Custom DHCP (guest IP allocation) range for isolated guest networks
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.networks', 'dhcp_start_ip', 'VARCHAR(15) DEFAULT NULL COMMENT ''start of the custom DHCP range for an isolated network, within its CIDR'' ');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.networks', 'dhcp_end_ip', 'VARCHAR(15) DEFAULT NULL COMMENT ''end of the custom DHCP range for an isolated network, within its CIDR'' ');
