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
        <tooltip-label slot="label" :title="$t('label.routeprefix')" :tooltip="apiParams.interfacerouteprefix.description"/>
        <a-input
          :auto-focus="true"
          v-decorator="['interfacerouteprefix', {
            rules: [{ required: true, message: $t('message.error.required.input') }]
          }]"
          :placeholder="apiParams.interfacerouteprefix.description"/>
      </a-form-item>
      <a-form-item>
        <tooltip-label slot="label" :title="$t('label.communities')" :tooltip="apiParams.interfacecommunities.description"/>
        <a-select
          mode="tags"
          :token-separators="[',']"
          v-decorator="['interfacecommunities', { rules: [{ type: 'array' }] }]"
          :placeholder="apiParams.interfacecommunities.description">
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
    this.apiParams = this.$getApiParams('addTungstenFabricInterfaceStaticRoute')
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
        params.tungsteninterfaceroutetableuuid = this.resource.uuid
        params.interfacerouteprefix = values.interfacerouteprefix
        params.interfacecommunities = values.interfacecommunities ? values.interfacecommunities.join(',') : null

        this.actionLoading = true
        api('addTungstenFabricInterfaceStaticRoute', params).then(json => {
          this.$pollJob({
            jobId: json.addtungstenfabricinterfacestaticrouteresponse.jobid,
            title: this.$t('label.add.tungsten.interface.static.route'),
            description: values.routeprefix,
            successMessage: `${this.$t('message.success.add.interface.static.route')} ${values.routeprefix}`,
            errorMessage: this.$t('message.error.add.interface.static.route'),
            loadingMessage: this.$t('message.loading.add.interface.static.route'),
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
