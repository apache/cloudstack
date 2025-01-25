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
-- Schema upgrade from 4.20.1.0 to 4.21.0.0
--;

-- Add console_endpoint_creator_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'console_endpoint_creator_address', 'VARCHAR(45)');

-- Add client_address column to cloud.console_session table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.console_session', 'client_address', 'VARCHAR(45)');

-- Allow default roles to use quotaCreditsList
INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaCreditsList', permission, sort_order
FROM `cloud`.`role_permissions` rp
WHERE rp.rule = 'quotaStatement'
AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaCreditsList');

-- Grant access to 2FA APIs for the "Read-Only User - Default" role

UPDATE `cloud`.`role_permissions` `rp`
SET `rp`.`sort_order` = `rp`.`sort_order` + 3
WHERE `rp`.`rule` = '*'
    AND `rp`.`permission` = 'DENY'
    AND `rp`.`role_id` IN (
        SELECT `r`.`id`
        FROM `cloud`.`roles` `r`
        WHERE `r`.`name` = 'Read-Only User - Default'
            AND `r`.`is_default` = 1
    );

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'setupUserTwoFactorAuthentication','ALLOW',MAX(sort_order) - 3
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Read-Only User - Default'
    AND `r`.`is_default` = 1
);

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'validateUserTwoFactorAuthenticationCode','ALLOW',MAX(sort_order) - 2
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Read-Only User - Default'
      AND `r`.`is_default` = 1
);

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'listUserTwoFactorAuthenticatorProviders','ALLOW',MAX(sort_order) - 1
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Read-Only User - Default'
      AND `r`.`is_default` = 1
);

-- Grant access to 2FA APIs for the "Support User - Default" role

UPDATE `cloud`.`role_permissions` `rp`
SET `rp`.`sort_order` = `rp`.`sort_order` + 3
WHERE `rp`.`rule` = '*'
    AND `rp`.`permission` = 'DENY'
    AND `rp`.`role_id` IN (
        SELECT `r`.`id`
        FROM `cloud`.`roles` `r`
        WHERE `r`.`name` = 'Support User - Default'
            AND `r`.`is_default` = 1
        );

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'setupUserTwoFactorAuthentication','ALLOW',MAX(sort_order) - 3
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Support User - Default'
      AND `r`.`is_default` = 1
);

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'validateUserTwoFactorAuthenticationCode','ALLOW',MAX(sort_order) - 2
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Support User - Default'
      AND `r`.`is_default` = 1
);

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'listUserTwoFactorAuthenticatorProviders','ALLOW',MAX(sort_order) - 1
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Support User - Default'
      AND `r`.`is_default` = 1
);

-- Grant access to 2FA APIs for the "Read-Only Admin - Default" role

UPDATE `cloud`.`role_permissions` `rp`
SET `rp`.`sort_order` = `rp`.`sort_order` + 2
WHERE `rp`.`rule` = '*'
  AND `rp`.`permission` = 'DENY'
  AND `rp`.`role_id` IN (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Read-Only Admin - Default'
      AND `r`.`is_default` = 1
);

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'setupUserTwoFactorAuthentication','ALLOW',MAX(sort_order) - 2
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Read-Only Admin - Default'
      AND `r`.`is_default` = 1
);

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'validateUserTwoFactorAuthenticationCode','ALLOW',MAX(sort_order) - 1
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Read-Only Admin - Default'
      AND `r`.`is_default` = 1
);

-- Grant access to 2FA APIs for the "Support Admin - Default" role

UPDATE `cloud`.`role_permissions` `rp`
SET `rp`.`sort_order` = `rp`.`sort_order` + 2
WHERE `rp`.`rule` = '*'
  AND `rp`.`permission` = 'DENY'
  AND `rp`.`role_id` IN (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Support Admin - Default'
      AND `r`.`is_default` = 1
);

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'setupUserTwoFactorAuthentication','ALLOW',MAX(sort_order) - 2
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Support Admin - Default'
      AND `r`.`is_default` = 1
);

INSERT INTO `cloud`.`role_permissions`
    (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'validateUserTwoFactorAuthenticationCode','ALLOW',MAX(sort_order) - 1
FROM `cloud`.`role_permissions`
WHERE role_id = (
    SELECT `r`.`id`
    FROM `cloud`.`roles` `r`
    WHERE `r`.`name` = 'Support Admin - Default'
      AND `r`.`is_default` = 1
);
