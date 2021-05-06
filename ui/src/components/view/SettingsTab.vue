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
    <a-input-search
      style="width: 25vw;float: right;margin-bottom: 10px; z-index: 8;"
      :placeholder="$t('label.search')"
      v-model="filter"
      @search="handleSearch" />

    <a-list size="large" class="list" :loading="loading || tabLoading">
      <a-list-item :key="index" v-for="(item, index) in items" class="item">
        <a-list-item-meta>
          <span slot="title" style="word-break: break-all">{{ item.name }}</span>
          <span slot="description" style="word-break: break-all">{{ item.description }}</span>
        </a-list-item-meta>

        <div class="item__content">
          <a-input
            :autoFocus="editableValueKey === index"
            v-if="editableValueKey === index"
            class="editable-value value"
            :defaultValue="item.value"
            v-model="editableValue"
            @keydown.esc="editableValueKey = null"
            @pressEnter="updateData(item)">
          </a-input>
          <span v-else class="value">
            {{ item.value }}
          </span>
        </div>

        <div slot="actions" class="action">
          <tooltip-button
            :tooltip="$t('label.edit')"
            :disabled="!('updateConfiguration' in $store.getters.apis)"
            v-if="editableValueKey !== index"
            icon="edit"
            @click="setEditableSetting(item, index)" />
          <tooltip-button
            :tooltip="$t('label.cancel')"
            @click="editableValueKey = null"
            v-if="editableValueKey === index"
            iconType="close-circle"
            iconTwoToneColor="#f5222d" />
          <tooltip-button
            :tooltip="$t('label.ok')"
            @click="updateData(item)"
            v-if="editableValueKey === index"
            iconType="check-circle"
            iconTwoToneColor="#52c41a" />
        </div>
      </a-list-item>
    </a-list>
  </div>
</template>

<script>
import { api } from '@/api'
import TooltipButton from './TooltipButton.vue'

export default {
  components: {
    TooltipButton
  },
  name: 'SettingsTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      required: true
    }
  },
  data () {
    return {
      items: [],
      scopeKey: '',
      editableValueKey: null,
      editableValue: '',
      tabLoading: false,
      filter: ''
    }
  },
  created () {
    switch (this.$route.meta.name) {
      case 'account':
        this.scopeKey = 'accountid'
        break
      case 'domain':
        this.scopeKey = 'domainid'
        break
      case 'zone':
        this.scopeKey = 'zoneid'
        break
      case 'cluster':
        this.scopeKey = 'clusterid'
        break
      case 'storagepool':
        this.scopeKey = 'storageid'
        break
      case 'imagestore':
        this.scopeKey = 'imagestoreuuid'
        break
      default:
        this.scopeKey = ''
    }
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      if (!newItem.id) return
      this.resource = newItem
      this.fetchData()
    }
  },
  methods: {
    fetchData (callback) {
      this.tabLoading = true
      const params = {
        [this.scopeKey]: this.resource.id,
        listAll: true
      }
      if (this.filter) {
        params.keyword = this.filter
      }
      api('listConfigurations', params).then(response => {
        this.items = response.listconfigurationsresponse.configuration
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.loading.setting'))
      }).finally(() => {
        this.tabLoading = false
        if (!callback) return
        callback()
      })
    },
    updateData (item) {
      this.tabLoading = true
      api('updateConfiguration', {
        [this.scopeKey]: this.resource.id,
        name: item.name,
        value: this.editableValue
      }).then(() => {
        const message = `${this.$t('label.setting')} ${item.name} ${this.$t('label.update.to')} ${this.editableValue}`
        this.$message.success(message)
      }).catch(error => {
        console.error(error)
        this.$message.error(this.$t('message.error.save.setting'))
        this.$notification.error({
          message: this.$t('label.error'),
          description: this.$t('message.error.try.save.setting')
        })
      }).finally(() => {
        this.tabLoading = false
        this.fetchData(() => {
          this.editableValueKey = null
        })
      })
    },
    setEditableSetting (item, index) {
      this.editableValueKey = index
      this.editableValue = item.value
    },
    handleSearch (value) {
      this.filter = value
      this.fetchData()
    }
  }
}
</script>

<style scoped lang="scss">
  .list {
    clear:both;
  }
  .editable-value {

    @media (min-width: 760px) {
      text-align: right;
      margin-left: 40px;
      margin-right: -40px;
    }

  }
  .item {
    display: flex;
    flex-direction: column;
    align-items: stretch;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__content {
      width: 100%;
      display: flex;
      word-break: break-all;

      @media (min-width: 760px) {
        width: auto;
      }

    }

  }
  .action {
    margin-top: 20px;
    margin-left: -12px;

    @media (min-width: 480px) {
      margin-left: -24px;
    }

    @media (min-width: 760px) {
      margin-top: 0;
      margin-left: 0;
    }

  }

  .value {
    margin-top: 20px;

    @media (min-width: 760px) {
      margin-top: 0;
    }

  }

</style>
