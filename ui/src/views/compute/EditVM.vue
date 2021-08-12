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
      class="form-layout"
      layout="vertical"
      :ref="formRef"
      :model="form"
      :rules="rules"
      @finish="handleSubmit">
      <a-form-item>
        <template #label>
          {{ $t('label.name') }}
          <a-tooltip :title="apiParams.name.description">
            <info-circle-outlined />
          </a-tooltip>
        </template>
        <a-input
          v-model:value="form.name"
          autoFocus />
      </a-form-item>
      <a-form-item>
        <template #label>
          {{ $t('label.displayname') }}
          <a-tooltip :title="apiParams.displayname.description">
             <info-circle-outlined />
          </a-tooltip>
        </template>
        <a-input v-model:value="form.displayname" />
      </a-form-item>
      <a-form-item>
        <template #label>
          {{ $t('label.ostypeid') }}
          <a-tooltip :title="apiParams.ostypeid.description">
             <info-circle-outlined />
          </a-tooltip>
        </template>
        <a-select
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :loading="osTypes.loading"
          v-model:value="form.ostypeid">
          <a-select-option v-for="(ostype) in osTypes.opts" :key="ostype.id">
            {{ ostype.description }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <template #label>
          {{ $t('label.isdynamicallyscalable') }}
          <a-tooltip :title="apiParams.isdynamicallyscalable.description">
             <info-circle-outlined />
          </a-tooltip>
        </template>
        <a-switch v-model:checked="form.isdynamicallyscalable" />
      </a-form-item>
      <a-form-item>
        <template #label>
          {{ $t('label.haenable') }}
          <a-tooltip :title="apiParams.haenable.description">
             <info-circle-outlined />
          </a-tooltip>
        </template>
        <a-switch v-model:checked="form.haenable" />
      </a-form-item>
      <a-form-item>
        <template #label>
          {{ $t('label.group') }}
          <a-tooltip :title="apiParams.group.description">
             <info-circle-outlined />
          </a-tooltip>
        </template>
        <a-auto-complete
          v-model:value="form.group"
          :filterOption="(input, option) => {
            return option.children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :options="groups.opts" />
      </a-form-item>

      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="onCloseAction">{{ this.$t('label.cancel') }}</a-button>
        <a-button :loading="loading" type="primary" html-type="submit">{{ this.$t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'

export default {
  name: 'EditVM',
  props: {
    action: {
      type: Object,
      required: true
    },
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      osTypes: {
        loading: false,
        opts: []
      },
      groups: {
        loading: false,
        opts: []
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateVirtualMachine')
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: this.resource.name,
        displayname: this.resource.displayname,
        ostypeid: this.resource.ostypeid,
        isdynamicallyscalable: this.resource.isdynamicallyscalable,
        group: this.resource.group
      })
      this.rules = reactive({})
    },
    fetchData () {
      this.fetchOsTypes()
      this.fetchInstaceGroups()
    },
    fetchOsTypes () {
      this.osTypes.loading = true
      this.osTypes.opts = []
      api('listOsTypes', { listAll: true }).then(json => {
        this.osTypes.opts = json.listostypesresponse.ostype || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.osTypes.loading = false })
    },
    fetchInstaceGroups () {
      this.groups.loading = true
      this.groups.opts = []
      api('listInstanceGroups', {
        account: this.$store.getters.userInfo.account,
        domainid: this.$store.getters.userInfo.domainid,
        listall: true
      }).then(json => {
        const groups = json.listinstancegroupsresponse.instancegroup || []
        groups.forEach(x => {
          this.groups.opts.push(x.name)
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.groups.loading = false })
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.id = this.resource.id
        params.name = values.name
        params.displayname = values.displayname
        params.ostypeid = values.ostypeid
        params.isdynamicallyscalable = values.isdynamicallyscalable || false
        params.haenable = values.haenable || false
        params.group = values.group

        this.loading = true

        api('updateVirtualMachine', params).then(json => {
          this.$message.success({
            content: `${this.$t('label.action.edit.instance')} - ${values.name}`,
            duration: 2
          })
          this.$emit('refresh-data')
          this.onCloseAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => { this.loading = false })
      })
    },
    onCloseAction () {
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

  .action-button {
    text-align: right;
    margin-top: 20px;

    button {
      margin-right: 5px;
    }
  }
}
</style>
