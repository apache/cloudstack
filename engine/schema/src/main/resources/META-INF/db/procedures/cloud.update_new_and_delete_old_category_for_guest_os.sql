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

-- Move existing guest OS whose category will be deleted to Other category
DROP PROCEDURE IF EXISTS `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`;
CREATE PROCEDURE `cloud`.`UPDATE_NEW_AND_DELETE_OLD_CATEGORY_FOR_GUEST_OS`(IN to_category_name VARCHAR(255), IN from_category_name VARCHAR(255))
BEGIN
    DECLARE done INT DEFAULT 0
;   DECLARE to_category_id BIGINT
;   SELECT id INTO to_category_id
    FROM `cloud`.`guest_os_category`
    WHERE `name` = to_category_name
    LIMIT 1
;   IF to_category_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ToCategory not found'
;   END IF
;   UPDATE `cloud`.`guest_os`
    SET `category_id` = to_category_id
    WHERE `category_id` = (SELECT `id` FROM `cloud`.`guest_os_category` WHERE `name` = from_category_name)
;   UPDATE `cloud`.`guest_os_category` SET `removed`=now() WHERE `name` = from_category_name
; END;
