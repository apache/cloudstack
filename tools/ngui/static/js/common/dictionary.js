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
            id : 'ID',
            username : 'Username',
            account : 'Account',
            domain : 'Domain',
            state : 'State',
            displayname : 'Display Name',
            instancename : 'Instance Name',
            zonename : 'Zone Name',
            type : 'Type',
            description : 'Description',
            created : 'Created',
            name : 'Name',
            value : 'Value',
            displaytext : 'Description',
            networktype : 'Network Type',
            allocationstate : 'Allocation State',
            vmdisplayname: 'VM display name',
            hypervisor : 'Hypervisor',
            virtualmachine: 'Virtual Machine',
            virtualmachines: 'Virtual Machines',
            network: 'Network',
            networks: 'Networks',
            instances: 'Instances',
            event: 'Event',
            events: 'Events',
            globalsettings: 'Global Settings',
            accounts: 'Accounts',
            domains: 'Domains',
            storage: 'Storage',
            configurations: 'Global Settings',
            serviceofferings: 'Service Offerings',
            home: 'Home',
            projects: 'Projects',
            volumename: 'Volume',
            intervaltype: 'Interval Type',
            availabilityZone: 'Availability Zone',
            diskoffering: 'Disk Offering',
            format: 'Format',
            url: 'URL',
            checksum: 'MD5 Checksum',
            password: 'Password',
            email: 'Email',
            firstname: 'First Name',
            lastname: 'Last Name',
        }
    };
    return dictionary;
});
