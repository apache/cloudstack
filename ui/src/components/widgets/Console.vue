<template>
  <a
    v-if="['vm', 'systemvm', 'router'].includes($route.meta.name)"
    :href="'/client/console?cmd=access&vm=' + resource.id"
    target="_blank">
    <a-button shape="circle" type="dashed" :size="size" :disabled="['Stopped', 'Error', 'Destroyed'].includes(resource.state)" >
      <a-icon type="code" />
    </a-button>
  </a>
</template>

<script>
export default {
  name: 'Console',
  props: {
    resource: {
      type: Object,
      required: true
    },
    size: {
      type: String,
      default: 'small'
    }
  }
}
</script>
