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

-- in usage
DROP PROCEDURE IF EXISTS `cloud_usage`.`IDEMPOTENT_DROP_INDEX`;
CREATE PROCEDURE `cloud_usage`.`IDEMPOTENT_DROP_INDEX` (
    IN in_index_name VARCHAR(200)
, IN in_table_name VARCHAR(200)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1091 BEGIN END; SET @ddl = CONCAT('DROP INDEX ', in_index_name); SET @ddl = CONCAT(@ddl, ' ', ' ON ') ; SET @ddl = CONCAT(@ddl, ' ', in_table_name); PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt; END;
