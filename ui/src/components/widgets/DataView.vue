<template>
  <a-row :gutter="12">
    <a-col :xl="12">
      <chart-card class="info-card" v-if="resource.name">
        <h4>Name</h4>
        <template slot="footer"><span>{{ resource.name }}</span></template>
      </chart-card>
    </a-col>
    <a-col :xl="12">
      <chart-card class="info-card" v-if="resource.id">
        <h4>ID</h4>
        <template slot="footer"><span>{{ resource.id }}</span></template>
      </chart-card>
    </a-col>
    <a-col :xl="6" v-for="(value, key) in resource" :key="key">
      <chart-card class="info-card" v-if="key !== 'id' && key !== 'name'">
        <h4>{{ key }}</h4>
        <template slot="footer"><span>{{ value }}</span></template>
      </chart-card>
    </a-col>
  </a-row>
</template>

<script>

import ChartCard from '@/components/chart/ChartCard'

export default {
  name: 'DataView',
  components: {
    ChartCard
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  }
}
</script>

<style scoped>
</style>
