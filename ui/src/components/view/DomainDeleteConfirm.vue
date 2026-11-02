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
  <a-modal
    :visible="true"
    :title="$t('label.action.delete.domain') + ': ' + domain.name"
    :okText="$t('label.delete.domain')"
    okType="danger"
    :confirmLoading="loading"
    :ok-button-props="{ disabled: !canDelete }"
    @cancel="emitClose"
    @ok="emitConfirm">

    <a-alert
      type="warning"
      show-icon
      style="margin-bottom: 16px">
      <template #message>
        <div v-html="$t('message.delete.domain.warning')"></div>
      </template>
    </a-alert>

    <a-spin v-if="loading" />

    <a-table
      v-else
      size="small"
      :columns="columns"
      :dataSource="accountVmSummary"
      :pagination="false"
      rowKey="account" />

    <div style="margin-top: 16px">
      <a-alert style="margin-bottom: 10px">
        <template #message>
          <div v-html="$t('message.delete.domain.confirm')"></div>
        </template>
      </a-alert>
      <a-input
        v-model:value="confirmText"
        :placeholder="$t('label.enter.domain.name')" />
    </div>

  </a-modal>
</template>

<script>
import { getAPI } from '@/api'

export default {
  name: 'DomainDeleteConfirm',
  props: {
    domain: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      confirmText: '',
      accountVmSummary: []
    }
  },
  computed: {
    canDelete () {
      return this.confirmText.trim() === this.domain.name.trim()
    },
    columns () {
      return [
        { title: this.$t('label.account'), dataIndex: 'account' },
        { title: this.$t('label.total') + ' VMs', dataIndex: 'total' },
        { title: this.$t('label.running') + ' VMs', dataIndex: 'running' },
        { title: this.$t('label.stopped') + ' VMs', dataIndex: 'stopped' }
      ]
    }
  },
  mounted () {
    this.fetchDomainImpact()
  },
  methods: {
    emitClose () {
      this.$emit('close')
    },
    emitConfirm () {
      if (this.canDelete) {
        this.$emit('confirm')
      }
    },
    async fetchDomainImpact () {
      this.loading = true
      try {
        const accResp = await getAPI('listAccounts', {
          domainid: this.domain.id,
          listall: true
        })

        const accounts =
          accResp.listaccountsresponse &&
          accResp.listaccountsresponse.account
            ? accResp.listaccountsresponse.account
            : []

        const vmResp = await getAPI('listVirtualMachines', {
          domainid: this.domain.id,
          listall: true
        })

        const vms =
          vmResp.listvirtualmachinesresponse &&
          vmResp.listvirtualmachinesresponse.virtualmachine
            ? vmResp.listvirtualmachinesresponse.virtualmachine
            : []

        this.accountVmSummary = accounts.map(account => {
          const accountVms = vms.filter(vm => vm.account === account.name)
          const running = accountVms.filter(vm => vm.state === 'Running').length
          const stopped = accountVms.length - running

          return {
            account: account.name,
            total: accountVms.length,
            running,
            stopped
          }
        })
      } catch (e) {
        this.$notification.error({
          message: this.$t('message.request.failed'),
          description: e.response?.headers['x-description'] || this.$t('message.request.failed')
        })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
</style>
