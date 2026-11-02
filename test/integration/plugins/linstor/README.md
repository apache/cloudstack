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

## Encrypted snapshot tests

`test_linstor_encrypted_snapshots.py` covers the encrypted-volume snapshot round trip
(create encrypted root disk -> snapshot -> revert / create-volume-from-snapshot) and that the
backed-up qcow2 on secondary storage is itself LUKS encrypted.

Extra prerequisites:

* At least one KVM host with volume-encryption support (`host.encryptionsupported == true`, i.e.
  cryptsetup/qemu LUKS available). Tests self-skip if none is found.
* The Linstor resource group used (`acs-basic`) must be able to add a LUKS layer to its volumes.
* `lin.backup.snapshots` must be enabled (default) so snapshots are backed up to secondary storage;
  the test sets it. With it disabled the qcow2 path is not exercised.

```
nosetests --with-marvin --marvin-config=<marvin-cfg-file> <cloudstack-dir>/test/integration/plugins/linstor/test_linstor_encrypted_snapshots.py --zone=<zone> --hypervisor=kvm
```

## Incremental snapshot tests

`test_linstor_incremental_snapshots.py` covers incremental (content-diff) snapshot backups on
NFS secondary storage: full -> delta chaining, `snapshot.delta.max` chain rotation, revert to a
mid-chain delta, template creation from a delta (chain flattening), the fallback to a full backup
when the parent file is missing on secondary storage, mid-chain deletion, and that encrypted
volumes are always backed up as full copies.

Extra prerequisites:

* NFS secondary storage (incremental snapshots are only supported there).
* `kvm.incremental.snapshot`, `kvm.snapshot.enabled` and `lin.backup.snapshots` are set by the
  tests themselves (and restored afterwards). Snapshots are taken while the VMs are running
  (crash-consistent storage snapshots; markers are synced before each snapshot); VMs are only
  stopped where CloudStack requires it (revert).
* The fallback and qcow2-inspection checks need host SSH credentials, either in the marvin config
  (zones->pods->clusters->hosts) or via the `HOST_SSH_USER` / `HOST_SSH_PASSWORD` env vars; those
  tests self-skip without them.
* The parent-link assertions read `snapshot_store_ref` directly and therefore need the DB
  connection of the marvin config to work; without it the tests fall back to comparing
  physical sizes.

```
nosetests --with-marvin --marvin-config=<marvin-cfg-file> <cloudstack-dir>/test/integration/plugins/linstor/test_linstor_incremental_snapshots.py --zone=<zone> --hypervisor=kvm
```

Note: select single tests with `... test_linstor_incremental_snapshots.py:TestLinstorIncrementalSnapshots -m <test-name-pattern>`
(class selection plus a method filter); the `file.py:Class.test_method` form bypasses the marvin
plugin's test client injection and fails with `'NoneType' object has no attribute 'getApiClient'`.
