<template>
  <div class="bar-wrapper">
    <h4 class="bar-headline">{{ title }}</h4>
    <v-chart
      class="bar-chart"
      :data="data"
      :forceFit="true">
      <v-tooltip />
      <v-axis />
      <v-bar position="x*y"/>
    </v-chart>
  </div>
</template>

<script>
const data = []
for (let i = 0; i < 12; i += 1) {
  data.push({
    x: `${i + 1}X`,
    y: Math.floor(Math.random() * 1000) + 200
  })
}
const tooltip = [
  'x*y',
  (x, y) => ({
    name: x,
    value: y
  })
]
const scale = [{
  dataKey: 'x',
  min: 2
}, {
  dataKey: 'y',
  title: 'Data',
  min: 1,
  max: 22
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
      data,
      scale,
      tooltip
    }
  }
}
</script>

<style lang="less" scoped>
.bar {
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
