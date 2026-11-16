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
-- Schema upgrade cleanup from 4.22.0.0 to 4.22.1.0
--;

-- Entries remaining on `cloud`.`resource_reservation` during the upgrade process are stale, so delete them.
-- This script was added to normalize volume/primary storage reservations that got stuck due to a bug on VM deployment,
-- but it is more interesting to introduce a smarter logic to clean these stale reservations in the future without the need
-- for upgrades (for instance, by having a heartbeat_time column for the reservations and automatically cleaning old entries).
DELETE FROM `cloud`.`resource_reservation`;
