<template>
  <div class="mini-bar-wrapper">
    <div class="mini-bar-wrapper2">
      <v-chart class="mini-bar-chart" :force-fit="true" :data="data">
        <v-tooltip />
        <v-bar position="x*y" />
      </v-chart>
    </div>
  </div>
</template>

<script>
import moment from 'moment'
const data = []
const beginDay = new Date().getTime()

for (let i = 0; i < 10; i++) {
  data.push({
    x: moment(new Date(beginDay + 1000 * 60 * 60 * 24 * i)).format('YYYY-MM-DD'),
    y: Math.round(Math.random() * 10)
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
  title: '时间',
  min: 1,
  max: 22
}]

export default {
  name: 'MiniArea',
  data () {
    return {
      data,
      tooltip,
      scale
    }
  }
}
</script>

<style lang="less" scoped>
.mini-bar {
  &-wrapper {
    position: relative;
    width: 100%;
  }

  &-wrapper2 {
    position: absolute;
    bottom: -28px;
    width: 100%;
    height: 46px;
  }

  &-chart {
    padding: 36px 0 18px 0;
    height: 100px;
  }
}
</style>
