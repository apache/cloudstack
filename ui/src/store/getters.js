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

const getters = {
  device: state => state.app.device,
  version: state => state.app.version,
  theme: state => state.app.theme,
  color: state => state.app.color,
  metrics: state => state.app.metrics,
  token: state => state.user.token,
  project: state => state.user.project,
  avatar: state => state.user.avatar,
  nickname: state => state.user.name,
  apis: state => state.user.apis,
  features: state => state.user.features,
  userInfo: state => state.user.info,
  addRouters: state => state.permission.addRouters,
  multiTab: state => state.app.multiTab,
  asyncJobIds: state => state.user.asyncJobIds,
  isLdapEnabled: state => state.user.isLdapEnabled,
  cloudian: state => state.user.cloudian,
  zones: state => state.user.zones,
  timezoneoffset: state => state.user.timezoneoffset,
  usebrowsertimezone: state => state.user.usebrowsertimezone,
  domainStore: state => state.user.domainStore
}

export default getters
