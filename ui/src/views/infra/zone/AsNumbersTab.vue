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
      type="primary"
      style="width: 100%; margin-bottom: 10px"
      @click="showCreateASRange"
      :loading="loading"
      :disabled="!('createASNRange' in $store.getters.apis)">
      <template #icon><plus-outlined /></template> {{ $t('label.create.asnrange') }}
    </a-button>
    <div v-ctrl-enter="handleSubmit">
      <a-table
        bordered
        :scroll="{ x: 500 }"
        :dataSource="asnRanges"
        :columns="columns"
        :pagination="false"
        style="margin-bottom: 24px; width: 100%" >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'range'">
            <div> {{  record.startasn }} - {{ record.endasn }} </div>
          </template>
          <template v-if="column.key === 'actions'">
            <tooltip-button
              :tooltip="$t('label.delete.asnrange')"
              type="primary"
              :danger="true"
              icon="delete-outlined"
              @onClick="onDelete(record.id)" />
          </template>
        </template>
      </a-table>
      <a-modal
      :visible="addASRangeModal"
      :title="$t('label.create.asnrange')"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="closeAction">
        <a-form
          :layout="isMobile() ? 'horizontal': 'inline'"
          :ref="formRef"
          :model="form"
          :rules="rules"
          @finish="handleSubmit"
          >
          <div class="form-row">
            <div class="form-col">
              <a-form-item name="startasn" ref="startasn">
                <a-input
                  v-model:value="form.startasn"
                  :placeholder="$t('label.startasn')"
                  v-focus="true"
                />
              </a-form-item>
            </div>
            <div class="form-col">
              <a-form-item name="endasn" ref="endasn">
                <a-input
                  v-model:value="form.endasn"
                  :placeholder="$t('label.endasn')"
                />
              </a-form-item>
            </div>
            <div class="form-col">
              <a-form-item :style="{ display: 'inline-block', float: 'right', marginRight: 0 }">
                <a-button type="primary" html-type="submit">{{ $t('label.add') }}</a-button>
              </a-form-item>
            </div>
          </div>
        </a-form>
      </a-modal>
    </div>
    <a-modal
      v-if="showError"
      :visible="showError"
      :closable="true"
      :maskClosable="false"
      :title="`${$t('label.error')}!`"
      :footer="null"
      @cancel="showError = false"
      centered
    >
      <div v-ctrl-enter="() => showError = false">
        <div :span="24" class="action-button">
          <a-button @click="showError = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="showError = false">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>
<script>

import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import { mixinDevice } from '@/utils/mixin.js'

export default {
  components: {
    TooltipButton
  },
  mixins: [mixinDevice],
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      asnRanges: [],
      showError: false,
      fetchLoading: false,
      loading: false,
      addASRangeModal: false,
      columns: [
        {
          key: 'range',
          title: this.$t('label.asnrange'),
          dataIndex: 'range',
          width: 140
        },
        {
          key: 'actions',
          title: this.$t('label.delete.asnrange'),
          dataIndex: 'actions',
          width: 70
        }
      ]
    }
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
        startasn: [{ required: true, message: this.$t('message.error.startasn') }],
        endasn: [{ required: true, message: this.$t('message.error.endasn') }]
      })
    },
    fetchData () {
      this.fetchLoading = true
      api('listASNRanges', { zoneid: this.resource.id }).then(json => {
        this.asnRanges = json?.listasnrangesresponse?.asnumberrange || []
      }).finally(() => { this.fetchLoading = false })
    },
    showCreateASRange () {
      this.addASRangeModal = true
    },
    closeAction () {
      this.addASRangeModal = false
      this.$emit('close-action')
    },
    handleSubmit () {
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        values.zoneid = this.resource.id
        this.loading = true
        api('createASNRange', values).then(() => {
          this.$notification.success({
            message: this.$t('message.success.create.asnrange')
          })
          this.closeAction()
        }).catch(error => {
          this.$notification.error({
            message: `${this.$t('label.error')} ${error.response.status}`,
            description: error.response.data.createasnrangeresponse
              ? error.response.data.createasnrangeresponse?.errortext : error?.response?.data?.errorresponse?.errortext,
            duration: 0
          })
        }).finally(() => {
          this.loading = false
          this.fetchData()
        })
      })
    },
    onDelete (rangeId) {
      this.loading = true
      api('deleteASNRange', { id: rangeId }).then(() => {
        this.$notification.success({
          message: this.$t('message.success.delete.asnrange')
        })
        this.$emit('refresh-data')
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
        this.fetchData()
      })
    }
  }
}
</script>

<style scoped lang="less">
.form-row {
  display: grid;
  grid-template-columns: 145px 145px 130px 145px 145px 70px;
  justify-content: center;

  @media (max-width: 768px) {
    display: flex;
    flex-direction: column;
  }
}
</style>
