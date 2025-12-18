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

DROP PROCEDURE IF EXISTS `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`;

CREATE PROCEDURE `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`(
                            IN in_hypervisor_type VARCHAR(32),
                            IN in_hypervisor_version VARCHAR(32),
                            IN in_guest_os_name VARCHAR(255),
                            IN in_guest_os_id BIGINT(20) UNSIGNED,
                            IN is_user_defined int(1) UNSIGNED)
BEGIN
        IF NOT EXISTS ((SELECT * FROM `cloud`.`guest_os_hypervisor` WHERE
            hypervisor_type=in_hypervisor_type AND
            hypervisor_version=in_hypervisor_version AND
            guest_os_id = in_guest_os_id))
        THEN
                INSERT INTO `cloud`.`guest_os_hypervisor` (
                        uuid,
                        hypervisor_type,
                        hypervisor_version,
                        guest_os_name,
                        guest_os_id,
                        created,
                        is_user_defined)
                        VALUES (
                            UUID(),
                            in_hypervisor_type,
                            in_hypervisor_version,
                            in_guest_os_name,
                            in_guest_os_id,
                            utc_timestamp(),
                            is_user_defined
                        ); END IF; END;;
