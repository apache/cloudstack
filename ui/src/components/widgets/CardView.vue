<template>
  <a-row :gutter="12">
    <a-col v-for="item in items" :md="24" :lg="6" :key="item.id">
      <a-card
        hoverable
        style="margin-bottom: 12px">
        <template class="ant-card-actions" slot="actions">
          <a-icon type="edit" />
          <a-icon type="setting" />
          <a-icon type="ellipsis" />
        </template>
        <a-card-meta>
          <div slot="avatar">
            <a-icon :type="$route.meta.icon" style="padding-right: 5px" />
          </div>
          <div slot="title">
            <router-link :to="{ path: $route.path + '/' + item.id }" v-if="item.id">{{ item.name || item.displayname }}</router-link>
            <span v-else>{{ item.name }}</span>
          </div>
          <div slot="description" style="height: 80px">
            <status :text="item.state" displayText />
            <div v-if="item.ipaddress">
              <a-icon type="wifi" style="padding-right: 5px" />
              <router-link :to="{ path: $route.path + '/' + item.id }">{{ item.ipaddress }}</router-link>
            </div>
            <div v-if="item.vmname">
              <a-icon type="desktop" style="padding-right: 5px" />
              <router-link :to="{ path: '/vm/' + item.virtualmachineid }">{{ item.vmname }}</router-link>
            </div>
            <div v-if="item.zonename">
              <a-icon type="table" style="padding-right: 5px" />
              <router-link :to="{ path: '/zone/' + item.zoneid }">{{ item.zonename }}</router-link>
            </div>
          </div>
        </a-card-meta>
      </a-card>
    </a-col>
  </a-row>
</template>

<script>

import ChartCard from '@/components/chart/ChartCard'
import Status from '@/components/widgets/Status'

export default {
  name: 'CardView',
  components: {
    ChartCard,
    Status
  },
  props: {
    items: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style scoped>
</style>
