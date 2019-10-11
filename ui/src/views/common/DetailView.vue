<template>
  <div style="padding-top: 12px" class="page-header-index-wide page-header-wrapper-grid-content-main">
    <a-row :gutter="12">
      <a-col :md="24" :lg="8" style="margin-bottom: 12px">
        <slot name="info-card">
          <info-card :resource="resource" :loading="loading" />
        </slot>
      </a-col>
      <a-col :md="24" :lg="16">
        <a-card
          :bordered="true"
          style="width:100%">
          <a-tabs defaultActiveKey="1" @change="onTabChange" style="width:100%">
            <a-tab-pane
              v-for="(tab, index) in tabs"
              :tab="$t(tab.name)"
              :key="index">
              <a-card
                style="width:100%"
                v-if="tab.name === 'details'"
              >
                <a-skeleton active v-if="loading" />
                <a-card-grid
                  style="width:50%; textAlign:'center'"
                  v-for="key in $route.meta.details"
                  v-if="!loading && key in resource"
                  :key="key" >
                  <strong>{{ $t(key) }}</strong><br/>{{ resource[key] }}
                </a-card-grid>
              </a-card>

              <list-view
                v-if="tab.name === 'settings'"
                :columns="settingsColumns"
                :items="settings " />
            </a-tab-pane>
          </a-tabs>
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
.page-header-wrapper-grid-content-main {
  width: 100%;
  height: 100%;
  min-height: 100%;
  transition: 0.3s;
}
</style>
