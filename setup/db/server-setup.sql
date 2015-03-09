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

/* This file specifies default values that go into the database, before the Management Server is run. */

/* Root Domain */
INSERT INTO `cloud`.`domain` (id, uuid, name, parent, path, owner) VALUES (1, UUID(), 'ROOT', NULL, '/', 2);

/* Configuration Table */

INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) VALUES ('Hidden', 'DEFAULT', 'none', 'init', null, null);
-- INSERT INTO `cloud`.`configuration` (category, instance, component, name, value, description) VALUES ('Advanced', 'DEFAULT', 'AgentManager', 'xenserver.public.network.device', 'public-network', "[OPTIONAL]The name of the XenServer network containing the physical network interface that is connected to the public network ");


