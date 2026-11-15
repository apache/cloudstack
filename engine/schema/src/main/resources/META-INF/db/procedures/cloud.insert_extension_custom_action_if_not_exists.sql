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

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_CUSTOM_ACTION_IF_NOT_EXISTS`(
    IN ext_name VARCHAR(255),
    IN action_name VARCHAR(255),
    IN action_desc VARCHAR(4096),
    IN resource_type VARCHAR(255),
    IN allowed_roles INT UNSIGNED,
    IN success_msg VARCHAR(4096),
    IN error_msg VARCHAR(4096),
    IN timeout_seconds INT UNSIGNED
)
BEGIN
    DECLARE ext_id BIGINT
;   SELECT `id` INTO ext_id FROM `cloud`.`extension` WHERE `name` = ext_name LIMIT 1
;   IF NOT EXISTS (
        SELECT 1 FROM `cloud`.`extension_custom_action` WHERE `name` = action_name AND `extension_id` = ext_id
    ) THEN
        INSERT INTO `cloud`.`extension_custom_action` (
            `uuid`, `name`, `description`, `extension_id`, `resource_type`,
            `allowed_role_types`, `success_message`, `error_message`,
            `enabled`, `timeout`, `created`, `removed`
        )
        VALUES (
            UUID(), action_name, action_desc, ext_id, resource_type,
            allowed_roles, success_msg, error_msg,
            1, timeout_seconds, NOW(), NULL
        )
;   END IF
;END;
