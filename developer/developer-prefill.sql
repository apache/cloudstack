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

-- Add a default ROOT domain
use cloud;

INSERT INTO `cloud`.`domain` (id, uuid, name, parent, path, owner) VALUES
            (1, UUID(), 'ROOT', NULL, '/', 2);

-- Add system and admin accounts
INSERT INTO `cloud`.`account` (id, uuid, account_name, type, domain_id, state) VALUES
            (1, UUID(), 'system', 1, 1, 'enabled');

INSERT INTO `cloud`.`account` (id, uuid, account_name, type, domain_id, state) VALUES
            (2, UUID(), 'admin', 1, 1, 'enabled');

-- Add system user
INSERT INTO `cloud`.`user` (id, uuid, username, password, account_id, firstname,
            lastname, email, state, created) VALUES (1, UUID(), 'system', RAND(),
            '1', 'system', 'cloud', NULL, 'enabled', NOW());

-- Add system user with encrypted password=password
INSERT INTO `cloud`.`user` (id, uuid, username, password, account_id, firstname,
            lastname, email, state, created) VALUES (2, UUID(), 'admin', '5f4dcc3b5aa765d61d8327deb882cf99',
            '2', 'Admin', 'User', 'admin@mailprovider.com', 'disabled', NOW());

-- Add configurations
INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Hidden', 'DEFAULT', 'management-server', 'init', 'false');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'integration.api.port', '8096');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'secstorage.allowed.internal.sites', '0.0.0.0/0');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'account.cleanup.interval', '60');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'expunge.delay', '60');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'expunge.interval', '60');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'cluster.cpu.allocated.capacity.disablethreshold', '0.95');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'cluster.memory.allocated.capacity.disablethreshold', '0.95');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'pool.storage.allocated.capacity.disablethreshold', '0.95');

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'pool.storage.capacity.disablethreshold', '0.95');

-- Add developer configuration entry; allows management server to be run as a user other than "cloud"
INSERT INTO `cloud`.`configuration` (category, instance, component, name, value)
            VALUES ('Advanced', 'DEFAULT', 'management-server',
            'developer', 'true');

commit;
