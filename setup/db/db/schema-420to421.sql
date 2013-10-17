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
-- Schema upgrade from 4.2.0 to 4.2.1;
--;


INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'management-server', 's3.singleupload.max.size', '5', 
    'The maximum size limit for S3 single part upload API(in GB). If it is set to 0, then it means always use multi-part upload to upload object to S3. If it is set to -1, then it means always use single-part upload to upload object to S3.');

INSERT IGNORE INTO `cloud`.`configuration` VALUES ("Storage", 'DEFAULT', 'management-server', "enable.ha.storage.migration", "true", "Enable/disable storage migration across primary storage during HA"); 

-- Remove Windows Server 8 from guest_os_type dropdown to use Windows Server 2012
DELETE FROM `cloud`.`guest_os_hypervisor` where guest_os_id=168;
DELETE FROM `cloud`.`guest_os` where id=168;