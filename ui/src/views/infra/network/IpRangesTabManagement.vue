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
  <a-spin :spinning="componentLoading">
    <a-button
      :disabled="!('createManagementNetworkIpRange' in $store.getters.apis)"
      type="dashed"
      style="margin-bottom: 20px; width: 100%"
      @click="handleOpenAddIpRangeModal">
      <template #icon><plus-outlined /></template>
      {{ $t('label.add.ip.range') }}
    </a-button>

    <a-table
      style="overflow-y: auto"
      size="small"
      :columns="columns"
      :dataSource="items"
      :rowKey="record => record.id + record.startip"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'forsystemvms'">
          <a-checkbox v-model:checked="record.forsystemvms" />
        </template>
        <template v-if="column.key === 'actions'">
          <div class="actions">
            <tooltip-button
              tooltipPlacement="bottom"
              :tooltip="$t('label.remove.ip.range')"
              :disabled="!('deleteManagementNetworkIpRange' in $store.getters.apis)"
              icon="delete-outlined"
              type="primary"
              :danger="true"
              @onClick="handleDeleteIpRange(record)" />
          </div>
        </template>
      </template>
    </a-table>
    <a-pagination
      class="row-element pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="total"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="changePage"
      @showSizeChange="changePageSize"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      :visible="addIpRangeModal"
      :title="$t('label.add.ip.range')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      @cancel="addIpRangeModal = false">
      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        class="form"
        @finish="handleAddIpRange"
        v-ctrl-enter="handleAddIpRange"

      >
        <a-form-item name="pod" ref="pod" :label="$t('label.podid')" class="form__item">
          <a-select
            v-focus="true"
            v-model:value="form.pod"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option v-for="item in pods" :key="item.id" :value="item.id" :label="item.name">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="gateway" ref="gateway" :label="$t('label.gateway')" class="form__item">
          <a-input v-model:value="form.gateway" />
        </a-form-item>
        <a-form-item name="netmask" ref="netmask" :label="$t('label.netmask')" class="form__item">
          <a-input v-model:value="form.netmask" />
        </a-form-item>
        <a-form-item name="vlan" ref="vlan" :label="$t('label.vlan')" class="form__item">
          <a-input v-model:value="form.vlan" />
        </a-form-item>
        <a-form-item name="startip" ref="startip" :label="$t('label.startip')" class="form__item">
          <a-input v-model:value="form.startip" />
        </a-form-item>
        <a-form-item name="endip" ref="endip" :label="$t('label.endip')" class="form__item">
          <a-input
            v-model:value="form.endip">
          </a-input>
        </a-form-item>
        <a-form-item name="vms" ref="vms" :label="$t('label.system.vms')" class="form__item">
          <a-checkbox v-model:checked="form.vms"></a-checkbox>
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button @click="addIpRangeModal = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleAddIpRange">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'IpRangesTabManagement',
  components: {
    TooltipButton
  },
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
      componentLoading: false,
      items: [],
      pods: [],
      total: 0,
      domains: [],
      domainsLoading: false,
      addIpRangeModal: false,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.podid'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.gateway'),
          dataIndex: 'gateway'
        },
        {
          title: this.$t('label.netmask'),
          dataIndex: 'netmask'
        },
        {
          key: 'vlan',
          title: this.$t('label.vlan'),
          dataIndex: 'vlanid'
        },
        {
          key: 'startip',
          title: this.$t('label.startip'),
          dataIndex: 'startip'
        },
        {
          key: 'endip',
          title: this.$t('label.endip'),
          dataIndex: 'endip'
        },
        {
          key: 'forsystemvms',
          title: this.$t('label.system.vms'),
          dataIndex: 'forsystemvms'
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
        }
      ]
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem, oldItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({
        pod: [{ required: true, message: this.$t('label.required') }],
        gateway: [{ required: true, message: this.$t('label.required') }],
        netmask: [{ required: true, message: this.$t('label.required') }],
        startip: [{ required: true, message: this.$t('label.required') }],
        endip: [{ required: true, message: this.$t('label.required') }]
      })
    },
    fetchData () {
      this.componentLoading = true
      api('listPods', {
        zoneid: this.resource.zoneid,
        page: this.page,
        pagesize: this.pageSize
      }).then(response => {
        this.items = []
        this.total = response.listpodsresponse.count || 0
        this.pods = response.listpodsresponse.pod ? response.listpodsresponse.pod : []
        for (const pod of this.pods) {
          if (pod && pod.ipranges && pod.ipranges.length > 0) {
            for (var idx = 0; idx < pod.ipranges.length; idx++) {
              this.items.push({
                id: pod.id,
                name: pod.name,
                gateway: pod.gateway,
                netmask: pod.netmask,
                vlanid: pod.ipranges[idx].vlanid,
                startip: pod.ipranges[idx].startip,
                endip: pod.ipranges[idx].endip,
                forsystemvms: pod.ipranges[idx].forsystemvms === '1'
              })
            }
          }
        }
      }).catch(error => {
        console.log(error)
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    handleOpenAddIpRangeModal () {
      this.initForm()
      this.addIpRangeModal = true
      setTimeout(() => {
        if (this.items.length > 0) {
          this.form.pod = this.items[0].id
        }
      }, 200)
    },
    closeAction () {
      this.formRef.value.resetFields()
      this.addIpRangeModal = false
    },
    handleDeleteIpRange (record) {
      this.componentLoading = true
      api('deleteManagementNetworkIpRange', {
        podid: record.id,
        startip: record.startip,
        endip: record.endip,
        vlan: record.vlanid
      }).then(response => {
        this.$pollJob({
          jobId: response.deletemanagementnetworkiprangeresponse.jobid,
          title: this.$t('label.remove.ip.range'),
          description: record.id,
          successMessage: this.$t('message.success.remove.iprange'),
          successMethod: () => {
            this.componentLoading = false
            this.fetchData()
          },
          errorMessage: this.$t('message.remove.failed'),
          errorMethod: () => {
            this.componentLoading = false
            this.fetchData()
          },
          loadingMessage: this.$t('message.remove.iprange.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.componentLoading = false
            this.fetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.componentLoading = false
        this.fetchData()
      })
    },
    handleAddIpRange (e) {
      if (this.componentLoading) return
      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)
        this.componentLoading = true
        this.addIpRangeModal = false
        api('createManagementNetworkIpRange', {
          podid: values.pod,
          gateway: values.gateway,
          netmask: values.netmask,
          startip: values.startip,
          endip: values.endip,
          forsystemvms: values.vms,
          vlan: values.vlan || null
        }).then(response => {
          this.$pollJob({
            jobId: response.createmanagementnetworkiprangeresponse.jobid,
            title: this.$t('label.add.ip.range'),
            description: values.pod,
            successMessage: this.$t('message.success.add.iprange'),
            successMethod: () => {
              this.componentLoading = false
              this.fetchData()
            },
            errorMessage: this.$t('message.add.failed'),
            errorMethod: () => {
              this.componentLoading = false
              this.fetchData()
            },
            loadingMessage: this.$t('message.add.iprange.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.componentLoading = false
              this.fetchData()
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.componentLoading = false
          this.fetchData()
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    changePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    changePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    }
  }
}
</script>

<style lang="scss" scoped>
  .list {
    &__item {
      display: flex;
    }

    &__data {
      display: flex;
      flex-wrap: wrap;
    }

    &__col {
      flex-basis: calc((100% / 3) - 20px);
      margin-right: 20px;
      margin-bottom: 10px;
    }

    &__label {
    }
  }

  .ant-list-item {
    padding-top: 0;
    padding-bottom: 0;

    &:not(:first-child) {
      padding-top: 20px;
    }

    &:not(:last-child) {
      padding-bottom: 20px;
    }
  }

  .actions {
    button {
      &:not(:last-child) {
        margin-bottom: 10px;
      }
    }
  }

  .ant-select {
    width: 100%;
  }

  .form {
    .actions {
      display: flex;
      justify-content: flex-end;

      button {
        &:not(:last-child) {
          margin-right: 10px;
        }
      }

    }

    &__item {
    }
  }

  .pagination {
    margin-top: 20px;
  }
</style>
