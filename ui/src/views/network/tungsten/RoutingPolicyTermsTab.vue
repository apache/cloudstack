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
    <a-button
      :disabled="!('addTungstenFabricPolicyRule' in $store.getters.apis)"
      type="dashed"
      icon="plus"
      style="width: 100%; margin-bottom: 15px"
      @click="onShowAction">
      {{ $t('label.add.routing.policy') }}
    </a-button>
    <a-table
      size="small"
      :loading="loading"
      :columns="columns"
      :dataSource="dataSource"
      :rowKey="(item, index) => index"
      :pagination="false">
    </a-table>
    <a-modal
      :visible="showAction"
      :title="$t('label.add.routing.policy')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="() => { showAction = false }"
      centered>
      <routing-policy-terms @onChangeFields="onChangeFields" />
    </a-modal>
  </div>
</template>

<script>
export default {
  name: 'RoutingPolicyTermsTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      showAction: false,
      columns: [],
      dataSource: []
    }
  },
  watch: {
    resource () {
      this.dataSource = this.resource?.tungstenroutingpolicyterm || []
    }
  },
  created () {
    this.dataSource = this.resource?.tungstenroutingpolicyterm || []
  },
  methods: {
    onShowAction () {
      this.showAction = true
    },
    onChangeFields (formModel, prefixList) {
      console.log(formModel, prefixList)
    }
  }
}
</script>
