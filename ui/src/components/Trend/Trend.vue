<template>
  <div class="trend" :class="[ reverseColor && 'reverse-color' ]">
    <span>
      <slot name="term"></slot>
      <span class="item-text">
        <slot></slot>
      </span>
    </span>
    <span :class="[flag]"><a-icon :type="`caret-${flag}`"/></span>
  </div>
</template>

<script>
export default {
  name: 'Trend',
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-trend'
    },
    flag: {
      type: String,
      required: true
    },
    reverseColor: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style lang="less" scoped>
@import "~ant-design-vue/lib/style/themes/default";
.trend {
  display: inline-block;
  font-size: @font-size-base;
  line-height: 22px;

  .up,
  .down {
    margin-left: 4px;
    position: relative;
    top: 1px;

    i {
      font-size: 12px;
      transform: scale(0.83);
    }
  }

  .item-text {
    display: inline-block;
    margin-left: 8px;
    color: rgba(0,0,0,.85);
  }

  .up {
    color: @red-6;
  }
  .down {
    color: @green-6;
    top: -1px;
  }

  &.reverse-color .up {
    color: @green-6;
  }
  &.reverse-color .down {
    color: @red-6;
  }
}
</style>
