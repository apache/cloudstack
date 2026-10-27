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
-- Schema upgrade from 4.20.2.0 to 4.20.3.0
--;

ALTER TABLE `cloud`.`template_store_ref` MODIFY COLUMN `download_url` varchar(2048);

UPDATE `cloud`.`alert` SET type = 33 WHERE name = 'ALERT.VR.PUBLIC.IFACE.MTU';
UPDATE `cloud`.`alert` SET type = 34 WHERE name = 'ALERT.VR.PRIVATE.IFACE.MTU';

-- Update configuration 'kvm.ssh.to.agent' description and is_dynamic fields
UPDATE `cloud`.`configuration` SET description = 'True if the management server will restart the agent service via SSH into the KVM hosts after or during maintenance operations', is_dynamic = 1 WHERE name = 'kvm.ssh.to.agent';
