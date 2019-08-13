<template>
  <div>
    <v-chart
      class="liquid-chart"
      :forceFit="true"
      :height="height"
      :width="width"
      :data="data"
      :scale="scale">
      <v-tooltip />
      <v-interval
        :shape="['liquid-fill-gauge']"
        position="transfer*value"
        color=""
        class="liquid-interval"
        :v-style="{
          lineWidth: 10
        }"
        :tooltip="[
          'transfer*value',
          (transfer, value) => {
            return {
              name: transfer,
              value,
            };
          },
        ]"
      ></v-interval>
      <v-guide
        class="liquid-guide"
        v-for="(row, index) in data"
        :key="index"
        type="text"
        :top="true"
        :position="{
          gender: row.transfer,
          value: 45
        }"
        :content="row.value + '%'"
      />
    </v-chart>
  </div>
</template>

<script>
export default {
  name: 'Liquid',
  props: {
    height: {
      type: Number,
      default: 0
    },
    width: {
      type: Number,
      default: 0
    }
  }
}
</script>

<style lang="less" scoped>
.liquid {
  &-graph {
    padding: 0;
  }

  &-interval {
    opacity: 0.75;
  }

  &-guide {
    text-align: 'center';
    opacity: 0.75;
  }
}
</style>
