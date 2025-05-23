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
    <a-col :span="24">
      <a-input-search
        style="width: 25vw;float: right;margin-bottom: 10px; z-index: 8;"
        :placeholder="$t('label.search')"
        v-model:value="filter"
        @search="handleSearch" />
      <ConfigurationTable
        :columns="columns"
        :config="items" />
    </a-col>
  </div>
</template>

<script>
import { getAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import ConfigurationTable from '@/views/setting/ConfigurationTable.vue'

export default {
  components: {
    ConfigurationTable,
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
      filter: '',
      warningMessages: {
        'vr.private.interface.max.mtu': {
          scope: 'zone',
          warning: this.$t('message.warn.zone.mtu.update')
        },
        'vr.public.interface.max.mtu': {
          scope: 'zone',
          warning: this.$t('message.warn.zone.mtu.update')
        }
      },
      columns: [
        {
          title: 'name',
          dataIndex: 'name',
          key: 'name'
        },
        {
          title: 'value',
          dataIndex: 'value',
          key: 'value',
          width: '29%'
        }
      ]
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
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem.id) return
        this.fetchData()
      }
    }
  },
  methods: {
    fetchData (callback) {
      this.tabLoading = true
      const params = { [this.scopeKey]: this.resource.id }
      if (this.filter) {
        params.keyword = this.filter
      }
      getAPI('listConfigurations', params).then(response => {
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
      display: block;
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
