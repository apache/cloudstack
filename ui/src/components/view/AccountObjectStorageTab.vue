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
    <h3 style="margin-bottom: 4px">{{ $t('label.account.object.storage.title') }}</h3>
    <p
      v-if="anyStoreOutstanding"
      style="max-width: 70em; white-space: pre-line">{{ $t('message.account.object.storage.intro') }}</p>
    <a-empty v-if="!loading && stores.length === 0" :description="$t('message.account.object.storage.none')" />
    <a-alert
      v-else-if="!loading && !anyStoreSupported"
      type="info"
      show-icon
      style="max-width: 70em; margin-bottom: 16px"
      :message="$t('label.account.object.storage.unsupported')"
      :description="$t('message.account.object.storage.unsupported.all')" />
    <a-card
      v-for="store in stores"
      :key="store.id"
      size="small"
      style="margin-bottom: 16px">
      <template #title>
        <router-link :to="{ path: '/objectstore/' + store.id }">{{ store.name }}</router-link>
      </template>
      <a-alert
        v-if="store.perbucketcredentialssupported === false"
        type="info"
        show-icon
        :message="$t('label.account.object.storage.unsupported')"
        :description="$t('message.account.object.storage.unsupported')" />
      <a-alert
        v-else-if="stageOf(store) === 3"
        type="success"
        show-icon
        :message="$t('label.account.object.storage.complete')"
        :description="$t('message.account.object.storage.now.complete')" />
      <div v-else>
        <a-steps :current="stageOf(store)" status="process" size="small" class="migration-steps">
          <a-step :title="$t('label.account.object.storage.stage.legacy')" :description="$t('message.account.object.storage.stage.legacy')" />
          <a-step :title="$t('label.account.object.storage.stage.buckets')" :description="stageOf(store) === 1 ? $t('message.account.object.storage.stage.buckets.remaining', { count: store.legacybuckets }) : $t('message.account.object.storage.stage.buckets')" />
          <a-step :title="$t('label.account.object.storage.stage.rotate')" :description="$t('message.account.object.storage.stage.rotate')" />
          <a-step :title="$t('label.account.object.storage.stage.complete')" :description="$t('message.account.object.storage.stage.complete')" />
        </a-steps>

        <div v-if="stageOf(store) === 0">
          <p>{{ $t('message.account.object.storage.now.legacy', { count: store.legacybuckets }) }}</p>
          <a-button
            v-if="canMigrate"
            type="primary"
            @click="openConfirm('migrate', store)">
            <template #icon><lock-outlined /></template>{{ $t('label.account.object.storage.migrate') }}
          </a-button>
        </div>

        <div v-else-if="stageOf(store) === 1">
          <p>{{ $t('message.account.object.storage.now.buckets', { count: store.legacybuckets }) }}</p>
          <router-link :to="{ path: '/buckets', query: bucketListQuery(store) }">
            <a-button>{{ $t('label.account.object.storage.show.buckets') }}</a-button>
          </router-link>
        </div>

        <div v-else-if="stageOf(store) === 2">
          <a-alert
            type="warning"
            show-icon
            style="margin-bottom: 12px"
            :message="$t('label.account.key.rotation.pending')"
            :description="$t('message.account.key.rotation.pending', { store: store.name })" />
          <a-button
            v-if="canRotate"
            type="primary"
            danger
            @click="openConfirm('rotate', store)">
            <template #icon><reload-outlined /></template>{{ $t('label.account.key.rotate') }}
          </a-button>
        </div>

      </div>
    </a-card>

    <a-modal
      :visible="confirm.visible"
      :title="confirm.action === 'migrate' ? $t('label.account.object.storage.migrate') : $t('label.account.key.rotate')"
      :closable="true"
      :maskClosable="false"
      :footer="null"
      centered
      @cancel="closeConfirm">
      <a-spin :spinning="loading">
        <a-alert type="warning">
          <template #message>{{ splitMessage(confirm.message).question }}</template>
        </a-alert>
        <p
          v-if="splitMessage(confirm.message).detail"
          style="margin-top: 12px">{{ splitMessage(confirm.message).detail }}</p>
        <div :span="24" class="action-button">
          <a-button @click="closeConfirm">{{ $t('label.cancel') }}</a-button>
          <a-button
            type="primary"
            :danger="confirm.action === 'rotate'"
            ref="submit"
            @click="submitConfirm">{{ $t('label.ok') }}</a-button>
        </div>
      </a-spin>
    </a-modal>
  </a-spin>
</template>

<script>
import { getAPI, postAPI } from '@/api'

export default {
  name: 'AccountObjectStorageTab',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: false,
      stores: [],
      confirm: { visible: false, action: null, store: null, message: '' }
    }
  },
  computed: {
    anyStoreSupported () {
      return this.stores.some(store => store.perbucketcredentialssupported !== false)
    },
    anyStoreOutstanding () {
      return this.stores.some(store => store.perbucketcredentialssupported !== false && this.stageOf(store) !== 3)
    },
    canMigrate () {
      return 'migrateObjectStoreAccount' in this.$store.getters.apis
    },
    canRotate () {
      return 'rotateObjectStoreAccountKey' in this.$store.getters.apis
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem, oldItem) {
        if (newItem && oldItem && newItem.id !== oldItem.id) {
          this.fetchData()
        }
      }
    }
  },
  methods: {
    // the first sentence (the question) goes in the warning box, the consequences below it
    splitMessage (text) {
      const m = /^(.*?[?.!])\s+(.*)$/s.exec(text || '')
      return m ? { question: m[1], detail: m[2] } : { question: text, detail: '' }
    },
    // matches what the card counts: the account's buckets still on the account key. objectstorageid
    // on listBuckets is root-admin only, so domain admins get the same list across every store.
    bucketListQuery (store) {
      const query = { account: this.resource.name, domainid: this.resource.domainid, credentialscope: 'account' }
      if (this.$store.getters.userInfo.roletype === 'Admin') {
        query.objectstorageid = store.id
      }
      return query
    },
    // 0 = account still on the shared key, 1 = migrated but buckets remain on the account key,
    // 2 = every bucket migrated but the pre-migration key is still valid, 3 = complete
    stageOf (store) {
      if (store.accountcredentialscope !== 'bucket') return 0
      if (store.legacybuckets > 0) return 1
      if (store.accountkeyrotationpending) return 2
      return 3
    },
    fetchData () {
      if (!this.resource || !this.resource.id) {
        return
      }
      this.loading = true
      getAPI('listObjectStoragePools', { accountid: this.resource.id }).then(json => {
        const stores = (json.listobjectstoragepoolsresponse.objectstore || []).filter(s => s.accountcredentialscope)
        // the ones that can be acted on first; those that cannot support the feature at the end
        stores.sort((a, b) => (b.perbucketcredentialssupported === false ? 0 : 1) - (a.perbucketcredentialssupported === false ? 0 : 1))
        this.stores = stores
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    },
    openConfirm (action, store) {
      const key = action === 'migrate' ? 'message.account.object.storage.migrate' : 'message.account.key.rotate'
      this.confirm = { visible: true, action, store, message: this.$t(key, { account: this.resource.name, store: store.name }) }
    },
    closeConfirm () {
      this.confirm = { visible: false, action: null, store: null, message: '' }
    },
    submitConfirm () {
      const { action, store } = this.confirm
      this.closeConfirm()
      if (action === 'migrate') this.migrate(store)
      if (action === 'rotate') this.rotate(store)
    },
    migrate (store) {
      this.loading = true
      postAPI('migrateObjectStoreAccount', { accountid: this.resource.id, objectstorageid: store.id }).then(() => {
        this.$message.success(this.$t('message.account.object.storage.migrated', { store: store.name }))
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })
    },
    rotate (store) {
      this.loading = true
      postAPI('rotateObjectStoreAccountKey', { accountid: this.resource.id, objectstorageid: store.id }).then(() => {
        this.$message.success(this.$t('message.account.key.rotated', { store: store.name }))
        this.fetchData()
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })
    }
  }
}
</script>

<style scoped>
.migration-steps {
  margin: 8px 0 32px;
}
.migration-steps :deep(.ant-steps-item-description) {
  max-width: 150px;
}
.action-button {
  text-align: right;
  margin-top: 20px;
}
.action-button button {
  margin-left: 8px;
}
</style>
