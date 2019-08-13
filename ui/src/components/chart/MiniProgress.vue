<template>
  <div class="mini-progress-wrapper">
    <div class="mini-progress-target target" :style="{ left: target + '%'}">
      <span class="mini-progress-background"/>
      <span class="mini-progress-background2"/>
    </div>
    <div class="mini-progress-wrapper-inner">
      <div class="mini-progress-progress"></div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MiniProgress',
  props: {
    target: {
      type: Number,
      default: 0
    },
    height: {
      type: String,
      default: '10px'
    },
    color: {
      type: String,
      default: '#13C2C2'
    },
    percentage: {
      type: Number,
      default: 0
    }
  }
}
</script>

<style lang="less" scoped>
@import "~ant-design-vue/lib/style/themes/default";

.mini-progress {
  &-wrapper {
    padding: 5px 0;
    position: relative;
    width: 100%;
  }

  &-wrapper-inner {
    background-color: #f5f5f5;
    position: relative;
  }

  &-target {
    left: 0;
    position: absolute;
    top: 0;
    bottom: 0;
  }

  &-background {
    background-color: @cyan-6;
    border-radius: 100px;
    position: absolute;
    top: 0;
    left: 0;
    height: 4px;
    width: 2px;
  }

  &-background2 {
    background-color: @cyan-6;
    border-radius: 100px;
    position: absolute;
    top: 0;
    left: 0;
    height: 4px;
    width: 2px;
    top: auto;
    bottom: 0;
  }

  &-progress {
    background-color: @cyan-6;
    width: 0%;
    height: 0;

    transition: all .4s cubic-bezier(.08, .82, .17, 1) 0s;
    border-radius: 1px 0 0 1px;
    background-color: #1890ff;
    width: 0;
    height: 100%;
  }
}
</style>
