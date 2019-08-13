<template>
  <div class="head-info" :class="center && 'center'">
    <span class="head-info-title">{{ title }}</span>
    <p class="head-info-content">{{ content }}</p>
    <em class="head-info-border" v-if="bordered"/>
  </div>
</template>

<script>
export default {
  name: 'HeadInfo',
  props: {
    title: {
      type: String,
      default: ''
    },
    content: {
      type: String,
      default: ''
    },
    bordered: {
      type: Boolean,
      default: false
    },
    center: {
      type: Boolean,
      default: true
    }
  }
}
</script>

<style lang="less" scoped>
.head-info {
  position: relative;
  text-align: left;
  padding: 0 32px 0 0;
  min-width: 125px;

  &.center {
    text-align: center;
    padding: 0 32px;
  }

  &-title {
    color: rgba(0, 0, 0, .45);
    display: inline-block;
    font-size: 14px;
    line-height: 22px;
    margin-bottom: 4px;
  }

  &-content {
    color: rgba(0, 0, 0, .85);
    font-size: 24px;
    line-height: 32px;
    margin: 0;
  }

  &-border {
    background-color: #e8e8e8;
    position: absolute;
    height: 56px;
    width: 1px;
    top: 0;
    right: 0;
  }
}
</style>
