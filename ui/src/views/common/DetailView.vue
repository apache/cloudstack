<template>
  <resource-layout>
    <div slot="left">
      <slot name="info-card">
        <info-card :resource="resource" :loading="loading" />
      </slot>
    </div>
    <div slot="right">
      <a-card
        :bordered="true"
        style="width:100%">
        <a-tabs defaultActiveKey="1" @change="onTabChange" style="width:100%">
          <a-tab-pane
            v-for="tab in tabs"
            :tab="$t(tab.name)"
            :key="tab.name">
            <div
              :bordered="false"
              style="width:100%"
              v-if="tab.name === 'details'" >
              <a-skeleton active v-if="loading" />
              <a-list
                v-else
                size="small"
                :dataSource="$route.meta.details"
              >
                <a-list-item slot="renderItem" slot-scope="item" v-if="item in resource">
                  <div>
                    <strong>{{ $t(item) }}</strong>
                    <br/>
                    <div>
                      {{ resource[item] }}
                    </div>
                  </div>
                </a-list-item>
              </a-list>
            </div>
            <list-view
              v-if="tab.name === 'settings'"
              :columns="settingsColumns"
              :items="settings " />

          </a-tab-pane>
        </a-tabs>
      </a-card>
    </div>
  </resource-layout>
</template>

<script>

import InfoCard from '@/views/common/InfoCard'
import ListView from '@/components/widgets/ListView'
import ResourceLayout from '@/layouts/ResourceLayout'
import Status from '@/components/widgets/Status'

export default {
  name: 'DetailView',
  components: {
    InfoCard,
    ListView,
    ResourceLayout,
    Status
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
      tabs: [{
        name: 'details'
      }],
      settingsColumns: [
        {
          title: this.$t('name'),
          dataIndex: 'name',
          sorter: true
        }, {
          title: this.$t('value'),
          dataIndex: 'value'
        }, {
          title: this.$t('action'),
          dataIndex: 'actions'
        }
      ],
      settings: []
    }
  },
  watch: {
    '$route' (to, from) {
      console.log(this.resource)
    },
    resource: function (newItem, oldItem) {
      this.resource = newItem
    }
  },
  methods: {
    onTabChange (key) {
      this.activeTab = key
    }
  }
}
</script>

<style lang="less" scoped>
</style>
