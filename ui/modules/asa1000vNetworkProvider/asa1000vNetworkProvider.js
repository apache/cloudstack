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
  cloudStack.modules.asa1000vNetworkProvider = function(module) {
    module.infrastructure.networkServiceProvider({
      id: 'ciscoAsa1000v',
      name: 'Cisco ASA 1000v',
      state: 'Disabled',
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
              { name: 'device1', ipaddress: '192.168.1.12', state: 'Enabled' },
              { name: 'device2', ipaddress: '192.168.1.13', state: 'Disabled' },
              { name: 'device3', ipaddress: '192.168.1.14', state: 'Enabled' }
            ]
          });
        },

        detailView: {
          tabs: {
            details: {
              title: 'label.details',
              fields: [
                {
                  name: { label: 'label.name' }
                },
                {
                  ipaddress: { label: 'label.ip.address' },
                  state: { label: 'label.state' }
                }
              ],
              dataProvider: function(args) {
                args.response.success({
                  data: args.context.asa1000vDevices[0]
                });
              }
            }
          }
        }
      },
      detailView: {
        id: 'asa1000vProvider',
        label: 'label.netScaler',
        viewAll: { label: 'label.devices', path: '_zone.asa1000vDevices' },
        tabs: {
          details: {
            title: 'label.details',
            fields: [
              {
                name: { label: 'label.name' }
              },
              {
                state: { label: 'label.state' }
              }
            ],
            dataProvider: function(args) {
              args.response.success({
                data: {
                  name: 'Cisco ASA 1000v',
                  state: 'Disabled'
                }
              });
            }
          }
        }
      }
    });
  };
}(jQuery, cloudStack));
