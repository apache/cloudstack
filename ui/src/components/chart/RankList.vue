<template>
  <div class="rank-list-wrapper">
    <h4>{{ title }}</h4>
    <ul class="rank-list-list">
      <li class="rank-list-item" :key="index" v-for="(item, index) in list">
        <span class="rank-list-index" :class="index < 3 ? 'is-active' : null">{{ index + 1 }}</span>
        <span class="rank-list-name">{{ item.name }}</span>
        <span class="rank-list-total">{{ item.total }}</span>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'RankList',
  // ['title', 'list']
  props: {
    title: {
      type: String,
      default: ''
    },
    list: {
      type: Array,
      default: null
    }
  }
}
</script>

<style lang="less" scoped>
.rank-list {
  margin: 25px 0 0;
  padding: 0;
  list-style: none;

  &-wrapper {
    padding: 0 32px 32px 72px;

    .mobile & {
      padding-left: 32px;
    }
  }

  &-item {
    margin-top: 16px;
    color: rgba(0, 0, 0, .65);
    font-size: 14px;
    line-height: 22px;
  }

  &-index {
    background-color: #f5f5f5;
    border-radius: 20px;
    display: inline-block;
    font-size: 12px;
    font-weight: 600;
    margin-right: 24px;
    height: 20px;
    line-height: 20px;
    width: 20px;
    text-align: center;

    &.is-active {
      background-color: #314659;
      color: #fff;
    }
  }

  &-total {
    float: right;
  }
}
</style>
