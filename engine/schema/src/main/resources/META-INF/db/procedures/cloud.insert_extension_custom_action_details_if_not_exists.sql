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

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_DETAILS_IF_NOT_EXISTS` (
    IN ext_name VARCHAR(255),
    IN action_name VARCHAR(255),
    IN param_json TEXT
)
BEGIN
    DECLARE action_id BIGINT UNSIGNED
;   SELECT `eca`.`id` INTO action_id FROM `cloud`.`extension_custom_action` `eca`
    JOIN `cloud`.`extension` `e` ON `e`.`id` = `eca`.`extension_id`
    WHERE `eca`.`name` = action_name AND `e`.`name` = ext_name LIMIT 1
;   IF NOT EXISTS (
        SELECT 1 FROM `cloud`.`extension_custom_action_details`
        WHERE `extension_custom_action_id` = action_id
          AND `name` = 'parameters'
    ) THEN
        INSERT INTO `cloud`.`extension_custom_action_details` (
            `extension_custom_action_id`,
            `name`,
            `value`,
            `display`
        ) VALUES (
            action_id,
            'parameters',
            param_json,
            0
        )
;   END IF
;END;
