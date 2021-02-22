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

import Vue from 'vue'
import VueStorage from 'vue-ls'
import config from '@/config/settings'

// base library'
// import Viser from 'viser-vue'
import VueCropper from 'vue-cropper'
import '@/core/lazy_lib/components_use'

import 'ant-design-vue/dist/antd.min.css'
import '@/style/vars.less'

// ext library
import VueClipboard from 'vue-clipboard2'
import PermissionHelper from '@/utils/helper/permission'

// customisation
import Spin from 'ant-design-vue/es/spin/Spin'

VueClipboard.config.autoSetContainer = true

// Vue.use(Viser)

Vue.use(VueStorage, config.storageOptions)
Vue.use(VueClipboard)
Vue.use(PermissionHelper)
Vue.use(VueCropper)

Spin.setDefaultIndicator({
  indicator: (h) => {
    return <a-icon type="loading" style="font-size: 30px" spin />
  }
})
