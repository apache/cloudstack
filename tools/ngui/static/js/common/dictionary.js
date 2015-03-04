// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

angular.module('cloudstack').factory("Dictionary", function(){
    var dictionary = {
        labels: {
            id : 'label.id',
            username : 'label.username',
            account : 'label.account',
            domain : 'label.domain',
            state : 'label.state',
            displayname : 'label.display.name',
            instancename : 'label.instance.name',
            zonename : 'label.zone.name',
            type : 'label.type',
            description : 'label.description',
            created : 'label.created',
            name : 'label.name',
            value : 'label.value',
            displaytext : 'label.description',
            networktype : 'label.network.type',
            allocationstate : 'label.allocation.state',
            vmdisplayname: 'label.vm.display.name',
            hypervisor : 'label.hypervisor',
            virtualmachine: 'label.virtual.machine',
            virtualmachines: 'label.virtual.machines',
            network: 'label.menu.network',
            networks: 'label.networks',
            instances: 'label.instances',
            event: 'label.event',
            events: 'label.menu.events',
            globalsettings: 'label.menu.global.settings',
            accounts: 'label.accounts',
            domains: 'label.menu.domains',
            storage: 'label.menu.storage',
            configurations: 'label.menu.global.settings',
            serviceofferings: 'label.menu.service.offerings',
            home: 'label.home',
            projects: 'label.projects',
            volumename: 'label.volume',
            intervaltype: 'label.interval.type',
            availabilityZone: 'label.availability.zone',
            diskoffering: 'label.disk.offering',
            format: 'label.format',
            url: 'label.url',
            checksum: 'label.md5.checksum',
            password: 'label.password',
            email: 'label.email',
            firstname: 'label.first.name',
            lastname: 'label.last.name',
        }
    };
    return dictionary;
});
