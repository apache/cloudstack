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
-- Schema upgrade from 4.19.1.0 to 4.19.2.0
--;

-- Add last_id to the volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'last_id', 'bigint(20) unsigned DEFAULT NULL');

SELECT
    COUNT(*)
INTO @exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'cloud'
  AND TABLE_NAME = 'counter'
  AND INDEX_NAME = 'uc_counter__provider__source__value';

-- Drop the key if it exists
SET @sql = IF(@exists > 0, 'ALTER TABLE counter DROP KEY uc_counter__provider__source__value', NULL);

-- Execute the drop statement if the index exists
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CALL `cloud`.`IDEMPOTENT_ADD_UNIQUE_KEY`('cloud.counter', 'uc_counter__provider__source__value__removed', '(provider, source, value, removed)');
