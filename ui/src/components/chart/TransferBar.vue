<template>
  <div class="transfer-bar-wrapper">
    <h4 class="transfer-bar-headline">{{ title }}</h4>
    <v-chart
      class="transfer-bar-chart"
      :data="data"
      :scale="scale"
      :forceFit="true"
    >
      <v-tooltip />
      <v-axis />
      <v-bar position="x*y"/>
    </v-chart>
  </div>
</template>

<script>
const tooltip = [
  'x*y',
  (x, y) => ({
    name: x,
    value: y
  })
]
const scale = [{
  dataKey: 'x',
  title: '日期(天)',
  alias: '日期(天)',
  min: 2
}, {
  dataKey: 'y',
  title: 'Title',
  alias: 'Alias',
  min: 1
}]

export default {
  name: 'Bar',
  props: {
    title: {
      type: String,
      default: ''
    }
  },
  data () {
    return {
      data: [],
      scale,
      tooltip
    }
  },
  created () {
    this.getMonthBar()
  },
  methods: {
    getMonthBar () {
      this.$http.get('/analysis/month-bar')
        .then(res => {
          this.data = res.result
        })
    }
  }
}
</script>

<style lang="less" scoped>
.c-transfer-bar {
  &-wrapper {
    padding: 0 0 32px 32px;
  }

  &-headline {
    margin-bottom: 20px;
  }

  &-chart {
    height: 254px;
    padding: 0 0 40px 50px;
  }
}
</style>
