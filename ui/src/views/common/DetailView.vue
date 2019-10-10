<template>
  <div style="padding-top: 12px" class="page-header-index-wide page-header-wrapper-grid-content-main">
    <a-row :gutter="12">
      <a-col :md="24" :lg="8" style="margin-bottom: 12px">
        <slot name="info-card">
          <info-card :resource="resource" resourceType="Volume" />
        </slot>
      </a-col>
      <a-col :md="24" :lg="16">
        <a-card
          style="width:100%"
          title="Details"
          :bordered="true"
        >
        </a-card>
        <a-card
          style="width:100%; margin-top: 12px"
          title="Settings"
          :bordered="true"
        >
          <list-view
            :columns="settingsColumns"
            :items="settings " />
        </a-card>
      </a-col>
    </a-row>

  </div>
</template>

<script>

import InfoCard from '@/views/common/InfoCard'
import ListView from '@/components/widgets/ListView'
import Status from '@/components/widgets/Status'

export default {
  name: 'DetailView',
  components: {
    InfoCard,
    ListView,
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
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
    resource: function (newItem, oldItem) {
      this.resource = newItem
    }
  },
  methods: {
  }
}
</script>

<style lang="less" scoped>
.page-header-wrapper-grid-content-main {
  width: 100%;
  height: 100%;
  min-height: 100%;
  transition: 0.3s;
}
</style>
