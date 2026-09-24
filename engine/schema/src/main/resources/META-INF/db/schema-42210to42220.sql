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

-- Widen the unique key on cloud_usage.usage_volume to include vm_id, so the cumulative and
-- per-VM volume usage records introduced in 4.22.1 can coexist. See #13399.
CALL `cloud_usage`.`IDEMPOTENT_DROP_INDEX`('id', 'cloud_usage.usage_volume');
CALL `cloud_usage`.`IDEMPOTENT_ADD_UNIQUE_INDEX`('cloud_usage.usage_volume', 'id', '(volume_id ASC, created ASC, vm_id ASC)');
