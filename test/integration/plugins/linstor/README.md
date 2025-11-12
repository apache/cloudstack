<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 -->

# Linstor storage plugin

This directory contains the basic VM, Volume life cycle tests for Linstor storage pool (in KVM hypervisor).

## Running tests

To run the basic volume tests, first update the below test data of the CloudStack environment

```
TestData.zoneId: <id of zone>
TestData.clusterId: <id of cluster>
TestData.domainId: <id of domain>
TestData.url: <management server IP>
TestData.primaryStorage "url": <Linstor storage pool url (see the format below) to use as primary storage>
```

and to enable and run volume migration tests, update the below test data

```
TestData.migrationTests: True
TestData.primaryStorageSameInstance "url": <Linstor url (see the format below) of the pool on same storage cluster as TestData.primaryStorage>
TestData.primaryStorageDistinctInstance "url": <Linstor url (see the format below) of the pool not on the same storage cluster as TestData.primaryStorage>
```

Then run the tests using python unittest runner: nosetests

```
nosetests --with-marvin --marvin-config=<marvin-cfg-file> <cloudstack-dir>/test/integration/plugins/linstor/test_linstor_volumes.py --zone=<zone> --hypervisor=kvm
```

You can also run these tests out of the box with PyDev or PyCharm or whatever.
