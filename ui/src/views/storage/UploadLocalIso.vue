<template>
  <div>
    TODO: upload ISO from local file component

    <a-upload
      name="avatar"
      listType="picture-card"
      class="avatar-uploader"
      :showUploadList="false"
      action="https://www.mocky.io/v2/5cc8019d300000980a055e76"
      :beforeUpload="beforeUpload"
      @change="handleChange"
    >
      <img v-if="imageUrl" :src="imageUrl" alt="avatar" />
      <div v-else>
        <a-icon :type="loading ? 'loading' : 'plus'" />
        <div class="ant-upload-text">Upload</div>
      </div>
    </a-upload>
  </div>
</template>

<script>

export default {
  name: '',
  components: {
  },
  data () {
    return {
    }
  },
  methods: {
  }
}
</script>

<style scoped>
</style>
