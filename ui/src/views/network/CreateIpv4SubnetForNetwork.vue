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
  <a-spin :spinning="loading">
    <a-form
      class="form"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit"
      v-ctrl-enter="handleSubmit"
     >
      <a-form-item ref="zoneid" name="zoneid">
        <template #label>
          <tooltip-label :title="$t('label.zoneid')"/>
        </template>
        <a-select
          v-model:value="form.zoneid"
          :loading="loading"
          @change="zone => fetchParentSubnets(zone)"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(zone, index) in zones"
            :value="zone.id"
            :key="index"
            :label="zone.name">
            <span>
              <resource-icon v-if="zone.icon" :image="zone.icon.base64image" size="1x" style="margin-right: 5px"/>
              <global-outlined v-else style="margin-right: 5px"/>
              {{ zone.name }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="parentid" name="parentid">
        <template #label>
          <tooltip-label :title="$t('label.parentsubnet')" :tooltip="apiParams.parentid.description"/>
        </template>
        <a-select
          v-model:value="form.parentid"
          :loading="loading"
          :placeholder="apiParams.parentid.description || $t('label.parentid')"
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }" >
          <a-select-option
            v-for="(parentSubnet, index) in parentSubnets"
            :value="parentSubnet.id"
            :key="index"
            :label="parentSubnet.subnet">
            {{ parentSubnet.subnet }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item ref="subnet" name="subnet">
        <template #label>
          <tooltip-label :title="$t('label.subnet')" :tooltip="apiParams.subnet.description"/>
        </template>
        <a-input
          v-focus="true"
          v-model:value="form.subnet"
          :placeholder="apiParams.subnet.description" />
      </a-form-item>
      <a-form-item ref="cidrsize" name="cidrsize">
        <template #label>
          <tooltip-label :title="$t('label.cidrsize')" :tooltip="apiParams.cidrsize.description"/>
        </template>
        <a-input
          v-model:value="form.cidrsize"
          :placeholder="apiParams.cidrsize.description" />
      </a-form-item>
      <div :span="24" class="action-button">
        <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
        <a-button type="primary" ref="submit" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import ResourceIcon from '@/components/view/ResourceIcon'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'CreateIpv4SubnetForNetwork',
  mixins: [mixinForm],
  components: {
    ResourceIcon,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      zones: [],
      parentSubnets: [],
      loading: false
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('createIpv4SubnetForGuestNetwork')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        zoneid: [{ required: true, message: this.$t('message.error.zone') }],
        parentid: [{ required: true, message: this.$t('message.error.parent.subnet') }]
      })
    },
    fetchData () {
      this.fetchZones()
    },
    fetchZones () {
      this.loading = true
      const params = { showicon: true }
      api('listZones', params).then(json => {
        this.zones = json.listzonesresponse.zone || []
        this.form.zoneid = this.zones[0].id || ''
        this.fetchParentSubnets(this.form.zoneid)
      }).finally(() => {
        this.loading = false
      })
    },
    fetchParentSubnets (zoneId) {
      this.loading = true
      var params = {
        zoneid: zoneId,
        listall: true
      }
      api('listIpv4SubnetsForZone', params).then(json => {
        this.parentSubnets = json.listipv4subnetsforzoneresponse.zoneipv4subnet || []
      }).finally(() => {
        this.loading = false
      })
    },
    handleSubmit (e) {
      if (this.loading) return
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        const params = {
          parentid: values.parentid
        }
        if (values.subnet && values.subnet !== '') {
          params.subnet = values.subnet
        }
        if (values.cidrsize && values.cidrsize !== '') {
          params.cidrsize = values.cidrsize
        }
        this.loading = true
        api('createIpv4SubnetForGuestNetwork', params).then(response => {
          this.$pollJob({
            jobId: response.createipv4subnetforguestnetworkresponse.jobid,
            title: this.$t('message.success.add.ipv4.subnet.for.guest.network'),
            description: values.subnet,
            successMessage: this.$t('message.success.add.ipv4.subnet.for.guest.network'),
            successMethod: (result) => {
              this.closeModal()
            },
            errorMessage: this.$t('message.add.ipv4.subnet.for.guest.network.failed'),
            loadingMessage: this.$t('message.add.ipv4.subnet.for.guest.network.processing'),
            catchMessage: this.$t('error.fetching.async.job.result')
          })
          this.closeModal()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }).catch((error) => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    closeModal () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    min-width: 400px;
    width: 100%;
  }
}
</style>
