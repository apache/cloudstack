<template>
  <resource-layout>
    <div slot="left">
      <slot name="info-card">
        <info-card :key="resource.totalStorage || resource.name || resource.id" :resource="resource" :loading="loading" @resourceChange="resourceChange" />
      </slot>
    </div>
    <div slot="right">
      <a-card
        :bordered="true"
        style="width:100%">
        <a-skeleton active v-if="loading" />
        <a-tabs
          v-else
          :defaultActiveKey="tabs[0].name"
          style="width: 100%"
          @change="onTabChange" >
          <a-tab-pane
            v-for="tab in tabs"
            :tab="$t(tab.name)"
            :key="tab.name">
            <component :is="tab.component" :resource="resource" :loading="loading" @resourceChange="resourceChange" />
          </a-tab-pane>
        </a-tabs>
      </a-card>
    </div>
  </resource-layout>
</template>

<script>

import DetailsTab from '@/views/common/DetailsTab'
import InfoCard from '@/views/common/InfoCard'
import ResourceLayout from '@/layouts/ResourceLayout'

export default {
  name: 'ResourceView',
  components: {
    InfoCard,
    ResourceLayout
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    tabs: {
      type: Array,
      default: function () {
        return [{
          name: 'details',
          component: DetailsTab
        }]
      }
    }
  },
  data () {
    return {
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
    },
    resourceChange (newResource) {
      this.resource = newResource
    }
  }
}
</script>

<style lang="less" scoped>
</style>
