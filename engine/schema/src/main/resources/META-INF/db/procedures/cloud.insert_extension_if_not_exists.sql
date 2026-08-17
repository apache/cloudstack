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

DROP PROCEDURE IF EXISTS `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`;
CREATE PROCEDURE `cloud`.`INSERT_EXTENSION_IF_NOT_EXISTS`(
    IN ext_name VARCHAR(255),
    IN ext_desc VARCHAR(255),
    IN ext_path VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM `cloud`.`extension` WHERE `name` = ext_name
    ) THEN
        INSERT INTO `cloud`.`extension` (
            `uuid`, `name`, `description`, `type`,
            `relative_path`, `path_ready`,
            `is_user_defined`, `state`, `created`, `removed`
        )
        VALUES (
            UUID(), ext_name, ext_desc, 'Orchestrator',
            ext_path, 1, 0, 'Enabled', NOW(), NULL
        )
;   END IF
;END;
