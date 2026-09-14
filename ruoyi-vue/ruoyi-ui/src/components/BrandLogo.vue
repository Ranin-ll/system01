<template>
  <div class="brand" :class="{ 'brand--collapsed': collapsed }">
    <span class="brand-mark">rogu</span>
    <div v-if="!collapsed" class="brand-copy">
      <span class="brand-name">实习生学习考核系统</span>
      <span class="brand-sub">交互原型 · 本地开发环境</span>
    </div>
  </div>
</template>

<script>
export default {
  name: 'BrandLogo',
  props: {
    collapsed: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style lang="scss" scoped>
.brand {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 9px;
  padding: 4px 9px 18px;
}

.brand--collapsed {
  justify-content: center;
  padding: 4px 0 18px;
}

.brand-mark {
  flex: 0 0 auto;
  color: #1764f5;
  font-size: 25px;
  font-weight: 800;
  font-style: italic;
  letter-spacing: 0;
  line-height: 1;
}

.brand-copy {
  min-width: 0;
}

.brand-name,
.brand-sub {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-name {
  color: #20242c;
  font-size: 15px;
  font-weight: 700;
}

.brand-sub {
  margin-top: 3px;
  color: #8d96a3;
  font-size: 11px;
}
</style>
