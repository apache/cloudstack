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

<template>
  <div>
    <autogen-view ref="autogenview" />
    <edit-tariff-value-wizard
      v-if="tariffAction"
      :showAction="tariffAction"
      :resource="tariffResource" />
  </div>
</template>

<script>
import AutogenView from '@/views/AutogenView.vue'
import EditTariffValueWizard from '@/views/plugins/quota/EditTariffValueWizard'

export default {
  name: 'QuotaTariff',
  components: {
    AutogenView,
    EditTariffValueWizard
  },
  data () {
    return {
      tariffAction: false,
      tariffResource: {}
    }
  },
  provide: function () {
    return {
      parentFetchData: this.fetchData,
      parentEditTariffAction: this.showTariffAction
    }
  },
  methods: {
    fetchData () {
      this.$refs.autogenview.fetchData()
    },
    showTariffAction (showAction, resource) {
      this.tariffAction = showAction
      this.tariffResource = resource
    }
  }
}
</script>
