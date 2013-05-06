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
(function($, cloudStack) {
  cloudStack.modules.vnmcAsa1000v = function(module) {
    module.vnmcNetworkProvider.addDevice({
      id: 'asa1000v',
      title: 'ASA 1000v',
      listView: {
        id: 'asa1000vDevices',
        fields: {
          name: { label: 'label.name' },
          ipaddress: { label: 'label.ip.address' },
          state: { label: 'label.state', indicator: {
            'Enabled': 'on',
            'Disabled': 'off'
          }}
        },
        dataProvider: function(args) {
          args.response.success({
            data: [
              { name: 'asadevice1', ipaddress: '192.168.1.12', state: 'Enabled' },
              { name: 'asadevice2', ipaddress: '192.168.1.13', state: 'Disabled' },
              { name: 'asadevice3', ipaddress: '192.168.1.14', state: 'Enabled' }
            ]
          });
        }
      }
    });
  };
}(jQuery, cloudStack));
