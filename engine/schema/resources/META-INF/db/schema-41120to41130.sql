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
-- Schema upgrade from 4.11.2.0 to 4.11.3.0
--;

CREATE TABLE `cloud`.`direct_download_certificate` (
     `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
     `uuid` varchar(40) NOT NULL,
     `alias` varchar(255) NOT NULL,
     `certificate` text NOT NULL,
     `hypervisor_type` varchar(45) NOT NULL,
     `zone_id` bigint(20) unsigned NOT NULL,
     PRIMARY KEY (`id`),
     KEY `i_direct_download_certificate_alias` (`alias`),
     KEY `fk_direct_download_certificate__zone_id` (`zone_id`),
     CONSTRAINT `fk_direct_download_certificate__zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cloud`.`direct_download_certificate_host_map` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `certificate_id` bigint(20) unsigned NOT NULL,
    `host_id` bigint(20) unsigned NOT NULL,
    PRIMARY KEY (`id`),
    KEY `fk_direct_download_certificate_host_map__host_id` (`host_id`),
    KEY `fk_direct_download_certificate_host_map__certificate_id` (`certificate_id`),
    CONSTRAINT `fk_direct_download_certificate_host_map__host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_direct_download_certificate_host_map__certificate_id` FOREIGN KEY (`certificate_id`) REFERENCES `direct_download_certificate` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;