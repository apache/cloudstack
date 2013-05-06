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
  cloudStack.modules.vnmcNetworkProvider = function(module) {    
    var vnmcDeviceViewAll = window._m = [
      {
        label: 'VNMC Devices',
        path: '_zone.vnmcDevices'
      }
    ];

    var vnmcListView = {
      id: 'vnmcDevices',
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
      }
    };

    var vnmcProviderDetailView = vnmcListView.detailView = {
      id: 'vnmcProvider',
      label: 'VNMC',
      viewAll: vnmcDeviceViewAll,
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
                name: 'VNMC Devices',
                state: 'Disabled'
              }
            });
          }
        }
      }
    };

    var vnmcDeviceDetailView = {
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
              data: args.context.vnmcDevices[0]
            });
          }
        }
      }
    };
    
    module.pluginAPI.extend({
      addDevice: function(device) {
        cloudStack.sections.system.subsections[device.id] = device;
        vnmcDeviceViewAll.push({ label: device.title, path: '_zone.' + device.id });
      }
    });
    
    module.infrastructure.networkServiceProvider({
      id: 'vnmc',
      name: 'Cisco VNMC',
      state: 'Disabled',
      listView: vnmcListView,

      detailView: vnmcProviderDetailView
    });
  };
}(jQuery, cloudStack));
