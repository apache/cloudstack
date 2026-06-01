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
-- Schema upgrade from 4.19.1.0 to 4.19.2.0
--;

-- Add last_id to the volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'last_id', 'bigint(20) unsigned DEFAULT NULL');

-- Grant access to 2FA APIs for the "Read-Only User - Default" role

CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only User - Default', 'setupUserTwoFactorAuthentication', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only User - Default', 'validateUserTwoFactorAuthenticationCode', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only User - Default', 'listUserTwoFactorAuthenticatorProviders', 'ALLOW');

-- Grant access to 2FA APIs for the "Support User - Default" role

CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support User - Default', 'setupUserTwoFactorAuthentication', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support User - Default', 'validateUserTwoFactorAuthenticationCode', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support User - Default', 'listUserTwoFactorAuthenticatorProviders', 'ALLOW');

-- Grant access to 2FA APIs for the "Read-Only Admin - Default" role

CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only Admin - Default', 'setupUserTwoFactorAuthentication', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Read-Only Admin - Default', 'validateUserTwoFactorAuthenticationCode', 'ALLOW');

-- Grant access to 2FA APIs for the "Support Admin - Default" role

CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support Admin - Default', 'setupUserTwoFactorAuthentication', 'ALLOW');
CALL `cloud`.`IDEMPOTENT_UPDATE_API_PERMISSION`('Support Admin - Default', 'validateUserTwoFactorAuthenticationCode', 'ALLOW');
