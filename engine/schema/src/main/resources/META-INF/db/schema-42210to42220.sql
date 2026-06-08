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
-- Schema upgrade from 4.22.1.0 to 4.22.2.0
--;

-- Change nw_rate and mc_rate column types from smallint unsigned to int unsigned
-- to align with the Java Integer type and support larger rate values
ALTER TABLE `cloud`.`service_offering`
    MODIFY COLUMN `nw_rate` int unsigned DEFAULT 200 COMMENT 'network rate throttle mbits/s',
    MODIFY COLUMN `mc_rate` int unsigned DEFAULT 10 COMMENT 'mcast rate throttle mbits/s';

ALTER TABLE `cloud`.`network_offerings`
    MODIFY COLUMN `nw_rate` int unsigned COMMENT 'network rate throttle mbits/s',
    MODIFY COLUMN `mc_rate` int unsigned COMMENT 'mcast rate throttle mbits/s';
