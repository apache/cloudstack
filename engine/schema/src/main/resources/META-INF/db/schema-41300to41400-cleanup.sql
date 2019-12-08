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
-- Schema upgrade cleanup from 4.13.0.0 to 4.14.0.0
--;

-- Delete an ancient template added in 2.2, which was set as removed in 4.5. For the sake of cleanup and easier overview of builtin templates in DB.
-- This DOES assume one is not running any VMs from this template, otherwise we are not deleting the template.
DELETE FROM `cloud`.`vm_template` WHERE `id`=2 AND `unique_name`="centos53-x86_64" AND `name`="CentOS 5.3(64-bit) no GUI (XenServer)" AND NOT EXISTS (SELECT 1 FROM `cloud`.`vm_instance` WHERE vm_template_id=2 AND removed IS NULL);

