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
-- Schema upgrade from 4.19.0.0 to 4.19.1.0
--;

-- Updates the populated Quota tariff's types VM_DISK_BYTES_READ, VM_DISK_BYTES_WRITE, VM_DISK_IO_READ and VM_DISK_IO_WRITE to the correct unit.

UPDATE cloud_usage.quota_tariff
SET usage_unit = 'Bytes', updated_on = NOW()
WHERE effective_on = '2010-05-04 00:00:00'
AND name IN ('VM_DISK_BYTES_READ', 'VM_DISK_BYTES_WRITE');

UPDATE cloud_usage.quota_tariff
SET usage_unit = 'IOPS', updated_on = NOW()
WHERE effective_on = '2010-05-04 00:00:00'
AND name IN ('VM_DISK_IO_READ', 'VM_DISK_IO_WRITE');
