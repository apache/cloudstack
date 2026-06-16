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

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_DETAIL_IF_NOT_EXISTS`(
    IN ext_name VARCHAR(255),
    IN detail_key VARCHAR(255),
    IN detail_value TEXT,
    IN display TINYINT(1)
)
BEGIN
    DECLARE ext_id BIGINT
;   SELECT `id` INTO ext_id FROM `cloud`.`extension` WHERE `name` = ext_name LIMIT 1
;   IF NOT EXISTS (
        SELECT 1 FROM `cloud`.`extension_details`
        WHERE `extension_id` = ext_id AND `name` = detail_key
    ) THEN
        INSERT INTO `cloud`.`extension_details` (
            `extension_id`, `name`, `value`, `display`
        )
        VALUES (
            ext_id, detail_key, detail_value, display
        )
;   END IF
;END;
