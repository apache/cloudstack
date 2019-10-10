<template>
  <a-table
    size="small"
    :loading="loading"
    :columns="columns"
    :dataSource="items"
    :rowKey="record => record.id || record.name"
    :scroll="{ x: '100%' }"
    :pagination="false"
    :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
    :rowClassName="getRowClassName"
  >
    <template slot="footer">
      <span v-if="hasSelected">
        {{ `Selected ${selectedRowKeys.length} items` }}
      </span>
    </template>

    <a slot="name" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }" v-if="record.id">{{ text }}</router-link>
      <span v-else>{{ text }}</span>
    </a>
    <a slot="displayname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
    </a>
    <a slot="username" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
    </a>
    <a slot="ipaddress" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: $route.path + '/' + record.id }">{{ text }}</router-link>
    </a>
    <a slot="vmname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/vm/' + record.virtualmachineid }">{{ text }}</router-link>
    </a>
    <template slot="state" slot-scope="text">
      <status :text="text ? text : ''" displayText />
    </template>

    <a slot="account" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/account/' + record.accountid }" v-if="record.accountid">{{ text }}</router-link>
      <router-link :to="{ path: '/account', query: { name: record.account } }" v-else>{{ text }}</router-link>
    </a>
    <a slot="domain" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/domain/' + record.domainid }">{{ text }}</router-link>
    </a>
    <a slot="zonename" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/zone/' + record.zoneid }">{{ text }}</router-link>
    </a>

    <a slot="guestnetworkname" slot-scope="text, record" href="javascript:;">
      <router-link :to="{ path: '/guestnetwork/' + record.guestnetworkid }">{{ text }}</router-link>
    </a>
  </a-table>
</template>

<script>
import Status from '@/components/widgets/Status'

export default {
  name: 'ListView',
  components: {
    Status
  },
  props: {
    columns: {
      type: Array,
      required: true
    },
    items: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      selectedRowKeys: []
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  methods: {
    getRowClassName (record, index) {
      if (index % 2 === 0) {
        return 'dark-row'
      }
      return 'light-row'
    },
    onSelectChange (selectedRowKeys) {
      console.log('selectedRowKeys changed: ', selectedRowKeys)
      this.selectedRowKeys = selectedRowKeys
    }
  }
}
</script>

<style scoped>
/deep/ .light-row {
  background-color: #fff;
}

/deep/ .dark-row {
  background-color: #f9f9f9;
}
</style>
