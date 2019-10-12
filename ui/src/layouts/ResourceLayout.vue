<template>
  <div style="padding-top: 12px" class="page-header-index-wide page-header-wrapper-grid-content-main">
    <a-row :gutter="12">
      <a-col :md="24" :lg="9" style="margin-bottom: 12px">
        <slot name="left">
        </slot>
      </a-col>
      <a-col :md="24" :lg="15">
        <slot name="right">
        </slot>
      </a-col>
    </a-row>
  </div>
</template>

<script>
export default {
  name: 'ResourceLayout'
}
</script>

<style lang="less" scoped>
.page-header-wrapper-grid-content-main {
  width: 100%;
  height: 100%;
  min-height: 100%;
  transition: 0.3s;
}
</style>
