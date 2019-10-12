<template>
  <a-breadcrumb class="breadcrumb">
    <a-card :bordered="true">
      <a-breadcrumb-item v-for="(item, index) in breadList" :key="index">
        <router-link
          v-if="item.name"
          :to="{ path: item.path === '' ? '/' : item.path }"
        >
          <a-icon v-if="index == 0" :type="item.meta.icon" />
          {{ $t(item.meta.title) }}
        </router-link>
        <span v-else-if="$route.params.id">
          {{ $route.params.id }}
          <a-button shape="circle" type="dashed" size="small" v-clipboard:copy="$route.params.id">
            <a-icon type="copy" style="margin-left: -1px; margin-top: 1px"/>
          </a-button>
        </span>
        <span v-else>{{ $t(item.meta.title) }}</span>
      </a-breadcrumb-item>
    </a-card>
  </a-breadcrumb>
</template>

<script>

export default {
  name: 'Breadcrumb',
  components: {
  },
  data () {
    return {
      name: '',
      breadList: []
    }
  },
  created () {
    this.getBreadcrumb()
  },
  watch: {
    $route () {
      this.getBreadcrumb()
    }
  },
  methods: {
    getBreadcrumb () {
      this.breadList = []
      this.name = this.$route.name
      this.$route.matched.forEach((item) => {
        this.breadList.push(item)
      })
    }
  }
}
</script>

<style>
.ant-breadcrumb {
  vertical-align: text-bottom;
  margin-bottom: 8px;
}

.ant-breadcrumb .anticon {
  margin-left: 8px;
}
</style>
