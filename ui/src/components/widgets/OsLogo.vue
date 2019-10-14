<template>
  <a-tooltip placement="right">
    <template slot="title">
      {{ name }}
    </template>
    <font-awesome-icon :icon="['fab', logo]" :size="size" style="color: #666;" />
  </a-tooltip>
</template>

<script>
import { api } from '@/api'

export default {
  name: 'OsLogo',
  props: {
    osId: {
      type: String,
      required: true
    },
    osName: {
      type: String,
      default: ''
    },
    size: {
      type: String,
      default: 'lg'
    }
  },
  data () {
    return {
      name: '',
      osLogo: 'linux'
    }
  },
  computed: {
    logo: function () {
      if (!this.name) {
        this.fetchData()
      }
      return this.osLogo
    }
  },
  watch: {
    osId: function (newItem, oldItem) {
      this.osId = newItem
      this.fetchData()
    }
  },
  methods: {
    fetchData () {
      if (this.osName) {
        this.discoverOsLogo(this.osName)
      } else if (this.osId) {
        this.findOsName(this.osId)
      }
    },
    findOsName (osId) {
      if (!('listOsTypes' in this.$store.getters.apis)) {
        return
      }
      this.name = 'linux'
      api('listOsTypes', { 'id': osId }).then(json => {
        if (json && json.listostypesresponse && json.listostypesresponse.ostype && json.listostypesresponse.ostype.length > 0) {
          this.discoverOsLogo(json.listostypesresponse.ostype[0].description)
        } else {
          this.discoverOsLogo('Linux')
        }
      })
    },
    discoverOsLogo (name) {
      this.name = name
      const osname = name.toLowerCase()
      if (osname.includes('centos')) {
        this.osLogo = 'centos'
      } else if (osname.includes('ubuntu')) {
        this.osLogo = 'ubuntu'
      } else if (osname.includes('suse')) {
        this.osLogo = 'suse'
      } else if (osname.includes('redhat')) {
        this.osLogo = 'redhat'
      } else if (osname.includes('fedora')) {
        this.osLogo = 'fedora'
      } else if (osname.includes('linux')) {
        this.osLogo = 'linux'
      } else if (osname.includes('bsd')) {
        this.osLogo = 'freebsd'
      } else if (osname.includes('apple')) {
        this.osLogo = 'apple'
      } else if (osname.includes('window') || osname.includes('dos')) {
        this.osLogo = 'windows'
      } else if (osname.includes('oracle')) {
        this.osLogo = 'java'
      } else {
        this.osLogo = 'linux'
      }
    }
  }
}
</script>

<style scoped>
</style>
