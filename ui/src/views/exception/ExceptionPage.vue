<template>
  <div class="exception">
    <div class="img" v-if="type == '403'">
      <img src="@/assets/403.png"/>
    </div>
    <div class="img" v-if="type == '404'">
      <img src="@/assets/404.png"/>
    </div>
    <div class="img" v-if="type == '500'">
      <img src="@/assets/500.png"/>
    </div>
    <div class="content">
      <h1>{{ config[type].title }}</h1>
      <div class="desc">{{ config[type].desc }}</div>
      <div class="action">
        <a-button type="primary" @click="handleToHome">Go Back</a-button>
      </div>
    </div>
  </div>
</template>

<script>
import types from './type'

export default {
  name: 'Exception',
  props: {
    type: {
      type: String,
      default: '404'
    }
  },
  data () {
    return {
      config: types
    }
  },
  methods: {
    handleToHome () {
      this.$router.push({ name: 'dashboard' })
    }
  }
}
</script>

<style lang="less" scoped>
  .exception {
    min-height: 500px;
    height: 80%;
    align-items: center;
    text-align: center;
    margin-top: 150px;
    .img {
      display: inline-block;
      padding-right: 52px;
      zoom: 1;
      img {
        height: 256px;
        max-width: 256px;
      }
    }
    .content {
      display: inline-block;
      flex: auto;
      h1 {
        color: #434e59;
        font-size: 72px;
        font-weight: 600;
        line-height: 72px;
        margin-bottom: 24px;
      }
      .desc {
        color: rgba(0, 0, 0, .45);
        font-size: 20px;
        line-height: 28px;
        margin-bottom: 16px;
      }
    }
  }

  .mobile {
    .exception {
      margin-top: 30px;
      .img {
        padding-right: unset;

        img {
          height: 40%;
          max-width: 80%;
        }
      }
    }
  }
</style>
