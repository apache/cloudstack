<script>
export default {
  name: 'RouteView',
  data () {
    return {}
  },
  render () {
    const { $route: { meta }, $store: { getters } } = this
    const inKeep = (
      <keep-alive>
        <router-view />
      </keep-alive>
    )
    const notKeep = (
      <router-view />
    )
    if (meta.keepAlive === false) {
      return notKeep
    }
    return getters.multiTab || meta.keepAlive ? inKeep : notKeep
  }
}
</script>
