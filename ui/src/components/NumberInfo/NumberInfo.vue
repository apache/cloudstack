<template>
  <div :class="[prefixCls]">
    <slot name="subtitle">
      <div :class="[`${prefixCls}-subtitle`]">{{ typeof subTitle === 'string' ? subTitle : subTitle() }}</div>
    </slot>
    <div class="number-info-value">
      <span>{{ total }}</span>
      <span class="sub-total">
        {{ subTotal }}
        <icon :type="`caret-${status}`" />
      </span>
    </div>
  </div>
</template>

<script>
import Icon from 'ant-design-vue/es/icon'

export default {
  name: 'NumberInfo',
  props: {
    prefixCls: {
      type: String,
      default: 'number-info'
    },
    total: {
      type: Number,
      required: true
    },
    subTotal: {
      type: Number,
      required: true
    },
    subTitle: {
      type: [String, Function],
      default: ''
    },
    status: {
      type: String,
      default: 'up'
    }
  },
  components: {
    Icon
  },
  data () {
    return {}
  }
}
</script>

<style lang="less" scoped>
  @import '~ant-design-vue/lib/style/themes/default';

  .number-info {

    .number-info-subtitle {
      color: @text-color-secondary;
      font-size: @font-size-base;
      height: 22px;
      line-height: 22px;
      overflow: hidden;
      text-overflow: ellipsis;
      word-break: break-all;
      white-space: nowrap;
    }

    .number-info-value {
      margin-top: 4px;
      font-size: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      word-break: break-all;
      white-space: nowrap;

      & > span {
        color: @heading-color;
        display: inline-block;
        line-height: 32px;
        height: 32px;
        font-size: 24px;
        margin-right: 32px;
      }

      .sub-total {
        color: @text-color-secondary;
        font-size: @font-size-lg;
        vertical-align: top;
        margin-right: 0;
        i {
          font-size: 12px;
          transform: scale(0.82);
          margin-left: 4px;
        }
        :global {
          .anticon-caret-up {
            color: @red-6;
          }
          .anticon-caret-down {
            color: @green-6;
          }
        }
      }
    }
  }
</style>
