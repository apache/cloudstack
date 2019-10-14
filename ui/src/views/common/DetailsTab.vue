<template>
  <a-list
    size="small"
    :dataSource="$route.meta.details">
    <a-list-item slot="renderItem" slot-scope="item" v-if="item in resource">
      <div>
        <strong>{{ $t(item) }}</strong>
        <br/>
        <div>
          {{ resource[item] }}
        </div>
      </div>
    </a-list-item>
  </a-list>
</template>

<script>
export default {
  name: 'DetailsTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  }
}
</script>
