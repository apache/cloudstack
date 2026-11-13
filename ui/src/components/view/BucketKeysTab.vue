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
    <p>{{ $t('message.bucket.keys.description') }}</p>
    <a-table
      size="small"
      style="overflow-y: auto"
      :columns="columns"
      :dataSource="slots"
      :rowKey="item => item.keyslot"
      :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'keyslot'">
          <strong>{{ record.keyslot }}</strong>
        </template>
        <template v-if="column.key === 'state'">
          <status :text="record.state" displayText />
        </template>
        <template v-if="column.key === 'accesskey'">
          <span v-if="record.accesskey">
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copy')"
              icon="CopyOutlined"
              type="dashed"
              size="small"
              @onClick="$message.success($t('label.copied.clipboard'))"
              :copyResource="record.accesskey" />
            {{ record.accesskey }}
          </span>
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'secretkey'">
          <span v-if="record.secretkey">
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copy')"
              icon="CopyOutlined"
              type="dashed"
              size="small"
              @onClick="$message.success($t('label.copied.clipboard'))"
              :copyResource="record.secretkey" />
            <a-button type="link" size="small" @click="record.reveal = !record.reveal">
              {{ record.reveal ? record.secretkey : '••••••••••••••••' }}
            </a-button>
          </span>
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'created'">
          {{ record.created ? $toLocaleDate(record.created) : '-' }}
        </template>
        <template v-if="column.key === 'actions'">
          <tooltip-button
            v-if="canRotate"
            :tooltip="record.state === 'Active' ? $t('label.bucket.key.rotate') : $t('label.bucket.key.create')"
            :icon="record.state === 'Active' ? 'ReloadOutlined' : 'PlusOutlined'"
            size="small"
            @onClick="openConfirm(record.state === 'Active' ? 'rotate' : 'create', record.keyslot)" />
          <tooltip-button
            v-if="canRevoke && record.state === 'Active'"
            :tooltip="activeCount <= 1 ? $t('message.bucket.key.revoke.last') : $t('label.bucket.key.revoke')"
            icon="StopOutlined"
            type="primary"
            :danger="true"
            :disabled="activeCount <= 1"
            size="small"
            @onClick="openConfirm('revoke', record.keyslot)" />
        </template>
      </template>
    </a-table>

    <a-modal
      :visible="confirm.visible"
      :title="confirmTitle"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      centered
      @cancel="closeConfirm">
      <a-spin :spinning="loading">
        <a-alert type="warning">
          <template #message>{{ splitMessage(confirmMessage).question }}</template>
        </a-alert>
        <p
          v-if="splitMessage(confirmMessage).detail"
          style="margin-top: 12px">{{ splitMessage(confirmMessage).detail }}</p>
        <div :span="24" class="action-button">
          <a-button @click="closeConfirm">{{ $t('label.cancel') }}</a-button>
          <a-button
            type="primary"
            :danger="confirm.action === 'revoke'"
            ref="submit"
            @click="submitConfirm">{{ $t('label.ok') }}</a-button>
        </div>
      </a-spin>
    </a-modal>
  </a-spin>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'

export default {
  name: 'BucketKeysTab',
  components: {
    Status,
    TooltipButton
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      keys: [],
      confirm: { visible: false, action: null, slot: null },
      columns: [
        { key: 'keyslot', title: this.$t('label.keyslot'), dataIndex: 'keyslot' },
        { key: 'state', title: this.$t('label.state'), dataIndex: 'state' },
        { key: 'accesskey', title: this.$t('label.accesskey'), dataIndex: 'accesskey' },
        { key: 'secretkey', title: this.$t('label.secretkey'), dataIndex: 'secretkey' },
        { key: 'created', title: this.$t('label.created'), dataIndex: 'created' },
        { key: 'actions', title: this.$t('label.actions') }
      ]
    }
  },
  computed: {
    confirmTitle () {
      return { rotate: this.$t('label.bucket.key.rotate'), create: this.$t('label.bucket.key.create'), revoke: this.$t('label.bucket.key.revoke') }[this.confirm.action] || ''
    },
    confirmMessage () {
      return { rotate: this.$t('message.bucket.key.rotate'), create: this.$t('message.bucket.key.create'), revoke: this.$t('message.bucket.key.revoke') }[this.confirm.action] || ''
    },
    slots () {
      // always render both slots so an empty one offers "create key"
      return [1, 2].map(slot => {
        const key = this.keys.find(k => k.keyslot === slot)
        return key ? { reveal: false, ...key } : { keyslot: slot, state: 'Empty' }
      })
    },
    activeCount () {
      return this.keys.filter(k => k.state === 'Active').length
    },
    canRotate () {
      return 'rotateBucketKey' in this.$store.getters.apis && this.resource.state === 'Created'
    },
    canRevoke () {
      return 'revokeBucketKey' in this.$store.getters.apis && this.resource.state === 'Created'
    }
  },
  created () {
    this.keys = this.resource.keys || []
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        this.keys = (newItem && newItem.keys) || []
      }
    }
  },
  methods: {
    // the first sentence (the question) goes in the warning box, the consequences below it
    splitMessage (text) {
      const m = /^(.*?[?.!])\s+(.*)$/s.exec(text || '')
      return m ? { question: m[1], detail: m[2] } : { question: text, detail: '' }
    },
    openConfirm (action, slot) {
      this.confirm = { visible: true, action, slot }
    },
    closeConfirm () {
      this.confirm = { visible: false, action: null, slot: null }
    },
    submitConfirm () {
      const { action, slot } = this.confirm
      this.closeConfirm()
      if (action === 'revoke') this.revoke(slot)
      else this.rotate(slot)
    },
    fetchKeys () {
      this.loading = true
      getAPI('listBuckets', { id: this.resource.id }).then(json => {
        const buckets = json.listbucketsresponse.bucket || []
        this.keys = buckets.length > 0 ? (buckets[0].keys || []) : []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    rotate (slot) {
      this.loading = true
      postAPI('rotateBucketKey', { id: this.resource.id, keyslot: slot }).then(() => {
        this.$message.success(this.$t('label.bucket.key.rotated', { slot: slot }))
        this.fetchKeys()
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })
    },
    revoke (slot) {
      this.loading = true
      postAPI('revokeBucketKey', { id: this.resource.id, keyslot: slot }).then(() => {
        this.$message.success(this.$t('label.bucket.key.revoked', { slot: slot }))
        this.fetchKeys()
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.action-button {
  text-align: right;
  margin-top: 20px;
}
.action-button button {
  margin-left: 8px;
}
</style>
