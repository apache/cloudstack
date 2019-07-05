<template>
  <div class="home">
    <div class="banner">
      <img style="width: 64px; height: 64px" src="../assets/logo.png">
      <h3 style="margin-top: 1rem">Welcome to Primate App</h3>
    </div>
  </div>
</template>

<script>

export default {
  name: 'Home',
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
  .home {
    width: 900px;
    margin: 0 auto;
    padding: 25px 0;
  }
  .home > .banner {
    text-align: center;
    padding: 25px 0;
    margin: 25px 0;
  }
</style>
