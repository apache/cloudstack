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

LOCK TABLES `vm_template` WRITE;
INSERT INTO `vm_template` VALUES (13,'macchinina-x86_64-xen','Macchinina (64-bit) no GUI (XenServer)','9511a500-620b-11e6-843c-00505635944c',1,1,'BUILTIN',0,64,'http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-xen.vhd.bz2','VHD','2016-08-14 16:11:00',NULL,1,'30985504bc31bf0cd3b9d2c6ca7944d3','Macchinina(64-bit) no GUI (XenServer)',0,0,99,1,0,1,1,'XenServer',NULL,NULL,0,NULL,'Active',0,NULL,0),(14,'macchinina-x86_64','Macchinina (64-bit) no GUI (KVM)','9512a500-620b-11e6-843c-00505635944c',1,1,'BUILTIN',0,64,'http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-kvm.qcow2.bz2','QCOW2','2016-08-14 16:11:02',NULL,1,'f50acb3a4387019b473d0f25a42bf06e','Macchinina (64-bit) no GUI (KVM)',0,0,99,1,0,1,1,'KVM',NULL,NULL,0,NULL,'Active',0,NULL,0),(15,'macchinina-x64','Macchinina (64-bit) no GUI (vSphere)','9513a500-620b-11e6-843c-00505635944c',1,1,'BUILTIN',0,64,'http://dl.openvm.eu/cloudstack/macchinina/x86_64/macchinina-vmware.ova','OVA','2016-08-14 16:11:05',NULL,1,'f6f881b7f2292948d8494db837fe0f47','Macchinina (64-bit) no GUI (vSphere)',0,0,99,1,0,1,1,'VMware',NULL,NULL,0,NULL,'Active',0,NULL,0);
UNLOCK TABLES;
