<template>
  <router-link class="sidebar-logo-link" to="/">
    <brand-logo :collapsed="collapse" />
  </router-link>
</template>

<script>
import BrandLogo from '@/components/BrandLogo'

export default {
  name: 'SidebarLogo',
  components: { BrandLogo },
  props: {
    collapse: {
      type: Boolean,
      required: true
    }
  },
  computed: {}
}
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: auto;
  min-height: 70px;
  background: #fff;
  text-align: left;
  overflow: hidden;
}
</style>
