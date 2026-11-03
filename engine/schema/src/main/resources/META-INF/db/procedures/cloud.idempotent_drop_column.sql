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

-- in cloud
DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_DROP_COLUMN`;

-- Error 1091: Can't DROP column; check that column/key exists
CREATE PROCEDURE `cloud`.`IDEMPOTENT_DROP_COLUMN` (
    IN in_table_name VARCHAR(200),
    IN in_column_name VARCHAR(200)
)
BEGIN

    DECLARE CONTINUE HANDLER FOR 1091 BEGIN END; SET @ddl = CONCAT('ALTER TABLE ', in_table_name, ' DROP COLUMN ', in_column_name); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;
