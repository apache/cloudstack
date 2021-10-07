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
  <div class="form-layout" v-ctrl-enter="handleSubmit">
    <a-form :form="form" layout="vertical">
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.routeprefix')" :tooltip="apiParams.routeprefix.description"/>
        <a-input
          :auto-focus="true"
          v-decorator="['routeprefix', {
            rules: [{ required: true, message: $t('message.error.required.input') }]
          }]"
          :placeholder="apiParams.routeprefix.description"/>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.routenexthop')" :tooltip="apiParams.routenexthop.description"/>
        <a-input
          v-decorator="['routenexthop', {
            rules: [{ required: true, message: $t('message.error.required.input') }]
          }]"
          :placeholder="apiParams.routenexthop.description"/>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.routenexthoptype')" :tooltip="apiParams.routenexthoptype.description"/>
        <a-select
          v-decorator="['routenexthoptype', {
            initialValue: listRouteNextHopType[0].id,
            rules: [{ required: true, message: $t('message.error.select') }]
          }]"
          :placeholder="apiParams.routenexthoptype.description">
          <a-select-option v-for="item in listRouteNextHopType" :key="item.id">{{ item.description }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.communities')" :tooltip="apiParams.communities.description"/>
        <a-select
          mode="tags"
          :token-separators="[',']"
          v-decorator="['communities', { rules: [{ type: 'array' }] }]"
          :placeholder="apiParams.communities.description">
          <a-select-option v-for="item in listCommunities" :key="item.id">{{ item.name }}</a-select-option>
        </a-select>
      </a-form-item>

      <div :span="24" class="action-button">
        <a-button :loading="actionLoading" @click="closeAction"> {{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="actionLoading" type="primary" @click="handleSubmit" ref="submit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-form>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'AddTungstenStaticRoute',
  components: { TooltipLabel },
  props: {
    resource: {
      type: Object,
      default: () => {}
    },
    loading: {
      type: Boolean,
      default: false
    },
    action: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      zoneId: null,
      actionLoading: false,
      listRouteNextHopType: [{
        id: 'ip-address',
        description: 'ip-address'
      }],
      listCommunities: [{
        id: 'no-export',
        name: 'no-export'
      }, {
        id: 'no-export-subconfed',
        name: 'no-export-subconfed'
      }, {
        id: 'no-advertise',
        name: 'no-advertise'
      }, {
        id: 'no-reoriginate',
        name: 'no-reoriginate'
      }]
    }
  },
  beforeCreate () {
    this.form = this.$form.createForm(this)
    this.apiParams = this.$getApiParams('addTungstenFabricNetworkStaticRoute')
  },
  created () {
    this.zoneId = this.$route?.query?.zoneid || null
  },
  methods: {
    handleSubmit () {
      this.form.validateFields((error, values) => {
        if (error) {
          return
        }

        const params = {}
        params.zoneid = this.zoneId
        params.tungstennetworkroutetableuuid = this.resource.uuid
        params.routeprefix = values.routeprefix
        params.routenexthop = values.routenexthop
        params.routenexthoptype = values.routenexthoptype
        params.communities = values.communities ? values.communities.join(',') : null

        this.actionLoading = true
        api('addTungstenFabricNetworkStaticRoute', params).then(json => {
          this.$pollJob({
            jobId: json.addtungstenfabricnetworkstaticrouteresponse.jobid,
            title: this.$t('label.add.tungsten.network.static.route'),
            description: values.routeprefix,
            successMessage: `${this.$t('message.success.add.network.static.route')} ${values.routeprefix}`,
            errorMessage: this.$t('message.error.add.network.static.route'),
            loadingMessage: this.$t('message.loading.add.network.static.route'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.actionLoading = false
          this.closeAction()
        })
      })
    },
    closeAction () {
      this.$emit('close-action')
    }
  }
}
</script>

<style scoped lang="less">
.form-layout {
  width: 80vw;

  @media (min-width: 600px) {
    width: 450px;
  }
}
</style>
