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
-- Schema upgrade from 4.9.1.0 to 4.9.2.0;
--;

--;
-- Stored procedure to do idempotent insert;
--;

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

CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 4.5 (32-bit)', 1,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 4.6 (32-bit)', 2,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 4.7 (32-bit)', 3,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 4.8 (32-bit)', 4,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 5,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 6,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 7,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 8,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 9,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 10,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 11,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 12,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 13,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 14,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 111,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 112,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 141,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 142,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 161,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 162,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 173,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 174,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 175,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 176,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 231,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 232,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (32-bit)', 139,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 5 (64-bit)', 140,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (32-bit)', 143,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (64-bit)', 144,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (32-bit)', 177,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (64-bit)', 178,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (32-bit)', 179,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (64-bit)', 180,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (32-bit)', 171,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (64-bit)', 172,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (32-bit)', 181,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (64-bit)', 182,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (32-bit)', 227,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (64-bit)', 228,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (32-bit)', 248,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 6 (64-bit)', 249,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'CentOS 7', 246,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Debian Squeeze 6.0 (32-bit)', 132,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Debian Squeeze 6.0 (64-bit)', 133,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Debian Wheezy 7.0 (32-bit)', 183,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Debian Wheezy 7.0 (64-bit)', 184,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 16,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 17,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 18,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 19,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 20,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 21,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 22,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 23,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 24,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 25,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 134,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 135,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 145,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 146,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 207,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 208,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 209,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 210,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 211,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 212,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (32-bit)', 233,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 5 (64-bit)', 234,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (32-bit)', 147,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (64-bit)', 148,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (32-bit)', 213,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (64-bit)', 214,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (32-bit)', 215,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (64-bit)', 216,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (32-bit)', 217,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (64-bit)', 218,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (32-bit)', 219,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (64-bit)', 220,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (32-bit)', 235,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (64-bit)', 236,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (32-bit)', 250,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Enterprise Linux 6 (64-bit)', 251,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Oracle Linux 7', 247,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 4.5 (32-bit)', 26,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 4.6 (32-bit)', 27,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 4.7 (32-bit)', 28,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 4.8 (32-bit)', 29,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 30,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 31,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 32,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 33,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 34,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 35,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 36,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 37,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 38,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 39,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 113,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 114,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 149,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 150,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 189,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 190,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 191,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 192,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 193,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 194,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (32-bit)', 237,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 5 (64-bit)', 238,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (32-bit)', 136,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (64-bit)', 137,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (32-bit)', 195,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (64-bit)', 196,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (32-bit)', 197,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (64-bit)', 198,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (32-bit)', 199,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (64-bit)', 204,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (32-bit)', 205,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (64-bit)', 206,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (32-bit)', 239,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 6 (64-bit)', 240,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Red Hat Enterprise Linux 7', 245,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 10 SP1 (32-bit)', 41,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 10 SP1 (64-bit)', 42,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 10 SP2 (32-bit)', 43,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 10 SP2 (64-bit)', 44,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 10 SP3 (32-bit)', 151,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 10 SP3 (64-bit)', 45,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 10 SP4 (32-bit)', 153,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 10 SP4 (64-bit)', 152,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 11 (32-bit)', 46,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 11 (64-bit)', 47,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 11 SP1 (32-bit)', 155,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 11 SP1 (64-bit)', 154,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 11 SP2 (32-bit)', 186,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 11 SP2 (64-bit)', 185,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 11 SP3 (32-bit)', 188,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 11 SP3 (32-bit)', 187,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'SUSE Linux Enterprise Server 12 (64-bit)', 244,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows 7 (32-bit)', 48,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows 7 (64-bit)', 49,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows 8 (32-bit)', 165,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows 8 (64-bit)', 166,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2003 (32-bit)', 50,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2003 (64-bit)', 51,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2003 (32-bit)', 87,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2003 (64-bit)', 88,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2003 (32-bit)', 89,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2003 (64-bit)', 90,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2008 (32-bit)', 52,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2008 (64-bit)', 53,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2008 R2 (64-bit)', 54,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2012 (64-bit)', 167,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows Server 2012 R2 (64-bit)', 168,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Windows XP SP3 (32-bit)', 58,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Lucid Lynx 10.04 (32-bit)', 121,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Lucid Lynx 10.04 (64-bit)', 126,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Maverick Meerkat 10.10 (32-bit) (experimental)', 156,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Maverick Meerkat 10.10 (64-bit) (experimental)', 157,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Precise Pangolin 12.04 (32-bit)', 163,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Precise Pangolin 12.04 (64-bit)', 164,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Trusty Tahr 14.04', 241,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Trusty Tahr 14.04', 254,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 169,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 170,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 98,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 99,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 60,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 103,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 200,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 201,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 59,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 100,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 202,  0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Other install media', 203,  0);

CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Trusty Tahr 14.04', 255, 0);
CALL `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`('Xenserver', '7.0.0', 'Ubuntu Trusty Tahr 14.04', 256, 0);

DROP PROCEDURE `cloud`.`IDEMPOTENT_INSERT_GUESTOS_HYPERVISOR_MAPPING`
