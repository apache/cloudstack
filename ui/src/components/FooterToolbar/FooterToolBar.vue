<template>
  <div class="footer-toolbar">
    <div class="footer-toolbar-left">
      <slot name="extra">{{ extra }}</slot>
    </div>
    <div class="footer-toolbar-right">
      <slot></slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FooterToolBar',
  props: {
    prefixCls: {
      type: String,
      default: 'reduced'
    },
    extra: {
      type: [String, Object],
      default: ''
    }
  }
}
</script>

<style lang="less" scoped>
.footer-toolbar {
  position: fixed;
  width: 100%;
  bottom: 0;
  right: 0;
  height: 56px;
  line-height: 56px;
  box-shadow: 0 -1px 2px rgba(0, 0, 0, 0.03);
  background: #fff;
  border-top: 1px solid #e8e8e8;
  padding: 0 24px;
  z-index: 9;

  &:after {
    content: "";
    display: block;
    clear: both;
  }

  &-left {
    float: left;
  }

  &-right {
    float: right;
  }

}
</style>
