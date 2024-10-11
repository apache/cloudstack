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
      v-ctrl-enter="handleSubmit"
      @finish="handleSubmit">
      <a-form-item name="name" ref="name">
        <template #label>
          <tooltip-label :title="$t('label.name')" :tooltip="apiParams.name.description"/>
        </template>
        <a-input
          v-model:value="form.name"
          v-focus="true" />
      </a-form-item>
      <a-form-item name="hosttags" ref="hosttags">
        <template #label>
          <tooltip-label :title="$t('label.hosttags')" :tooltip="$t('label.hosttags.explicit.description')"/>
        </template>
        <a-input v-model:value="form.hosttags" />
      </a-form-item>
      <a-form-item name="istagarule" ref="istagarule">
        <template #label>
          <tooltip-label :title="$t('label.istagarule')" :tooltip="apiParams.istagarule.description"/>
        </template>
        <a-switch v-model:checked="form.istagarule" />
      </a-form-item>
      <a-form-item name="oscategoryid" ref="oscategoryid">
        <template #label>
          <tooltip-label :title="$t('label.oscategoryid')" :tooltip="apiParams.oscategoryid.description"/>
        </template>
        <a-select
          showSearch
          optionFilterProp="label"
          :filterOption="(input, option) => {
            return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
          }"
          :loading="osCategories.loading"
          v-model:value="form.oscategoryid">
          <a-select-option v-for="(osCategory) in osCategories.opts" :key="osCategory.id" :label="osCategory.name">
            {{ osCategory.name }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <div :span="24" class="action-button">
        <a-button :loading="loading" @click="onCloseAction">{{ $t('label.cancel') }}</a-button>
        <a-button :loading="loading" ref="submit" type="primary" @click="handleSubmit">{{ $t('label.ok') }}</a-button>
      </div>
    </a-form>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'EditVM',
  components: {
    TooltipLabel
  },
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
      osCategories: {
        loading: false,
        opts: []
      }
    }
  },
  beforeCreate () {
    this.apiParams = this.$getApiParams('updateHost')
  },
  created () {
    this.initForm()
    this.fetchOsCategories()
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        name: this.resource.name,
        hosttags: this.resource.explicithosttags,
        istagarule: this.resource.istagarule,
        oscategoryid: this.resource.oscategoryid
      })
      this.rules = reactive({})
    },
    fetchOsCategories () {
      this.osCategories.loading = true
      this.osCategories.opts = []
      api('listOsCategories').then(json => {
        this.osCategories.opts = json.listoscategoriesresponse.oscategory || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.osCategories.loading = false
      })
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        const params = {}
        params.id = this.resource.id
        params.name = values.name
        params.hosttags = values.hosttags
        params.oscategoryid = values.oscategoryid
        if (values.istagarule !== undefined) {
          params.istagarule = values.istagarule
        }
        this.loading = true

        api('updateHost', params).then(json => {
          this.$message.success({
            content: `${this.$t('label.action.update.host')} - ${values.name}`,
            duration: 2
          })
          this.$emit('refresh-data')
          this.onCloseAction()
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => { this.loading = false })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
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
