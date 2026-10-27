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

DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION` (
    IN role VARCHAR(255),
    IN rule VARCHAR(255),
    IN permission VARCHAR(255)
)
BEGIN
     DECLARE role_id BIGINT(20) UNSIGNED
;    DECLARE max_sort_order BIGINT(20) UNSIGNED

;   SELECT `r`.`id` INTO role_id
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = role
        AND `r`.`is_default` = 1

;   SELECT MAX(`rp`.`sort_order`) INTO max_sort_order
    FROM `cloud`.`role_permissions` `rp`
    WHERE `rp`.`role_id` = role_id

;   IF NOT EXISTS (
        SELECT * FROM `cloud`.`role_permissions` `rp`
        WHERE `rp`.`role_id` = role_id
            AND `rp`.`rule` = rule
    ) THEN
        UPDATE `cloud`.`role_permissions` `rp`
        SET `rp`.`sort_order` = max_sort_order + 1
        WHERE `rp`.`sort_order` = max_sort_order
            AND `rp`.`role_id` = role_id

;       INSERT INTO `cloud`.`role_permissions`
            (uuid, role_id, rule, permission, sort_order)
        VALUES (uuid(), role_id, rule, permission, max_sort_order)
;   END IF
;END;
