<template>
  <div class="login">
    <div class="auth-layout">
      <section class="auth-intro">
        <brand-logo />
        <span class="intro-kicker">INTERNSHIP ASSESSMENT</span>
        <h1>让学习、考核与成长记录在同一个工作台里</h1>
        <p>从注册审核到学习材料、正式考核和能力画像，统一管理实习培养全过程。</p>
        <div class="intro-facts"><span><b>全流程</b>培养记录</span><span><b>多角色</b>协同管理</span><span><b>可迁移</b>部署架构</span></div>
      </section>
      <section class="auth-panel">
        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
          <div class="panel-heading"><span class="eyebrow">欢迎回来</span><h3 class="title">账号登录</h3><p class="subtitle">使用系统账号进入学习考核工作台</p></div>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          placeholder="账号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          show-password
          auto-complete="off"
          placeholder="密码"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code">
          <img :src="codeUrl" @click="getCode" class="login-code-img"/>
        </div>
      </el-form-item>
      <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div class="login-links">
          <router-link class="link-type" :to="'/register'" v-if="register">注册实习生账号</router-link>
        </div>
      </el-form-item>
        </el-form>
        <section class="test-accounts" aria-label="测试账号">
      <div class="test-accounts__heading">
        <span>数据库测试账号</span>
        <span class="test-accounts__hint">点击填充，统一密码 admin123</span>
      </div>
      <div class="test-account-list">
        <button
          v-for="account in testAccounts"
          :key="account.username"
          type="button"
          class="test-account"
          @click="fillTestAccount(account)"
        >
          <span class="test-account__dot" :class="account.tone"></span>
          <span>
            <strong>{{ account.label }}</strong>
            <small>{{ account.username }}</small>
          </span>
        </button>
      </div>
        </section>
      </section>
    </div>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>实习生学习考核系统 · 本地开发环境</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from '@/utils/jsencrypt'
import BrandLogo from '@/components/BrandLogo'

export default {
  name: "Login",
  components: { BrandLogo },
  data() {
    return {
      codeUrl: "",
      loginForm: {
        username: "test_super_admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: false,
      // 注册开关
      register: true,
      testAccounts: [
        { label: "超级管理员", username: "test_super_admin", tone: "is-blue" },
        { label: "交付部门管理员", username: "test_dept_admin", tone: "is-green" },
        { label: "开发部门管理员", username: "test_dev_admin", tone: "is-green" },
        { label: "设计部门管理员", username: "test_design_admin", tone: "is-green" },
        { label: "质检部门管理员", username: "test_qa_admin", tone: "is-green" },
        { label: "建模部门管理员", username: "test_model_admin", tone: "is-green" },
        { label: "实施实习生（正式）", username: "test_impl_intern", tone: "is-purple" },
        { label: "开发实习生（预备）", username: "test_dev_intern", tone: "is-orange" },
        { label: "设计实习生（预备）", username: "test_design_intern", tone: "is-orange" },
        { label: "质检实习生（预备）", username: "test_qa_intern", tone: "is-orange" },
        { label: "建模实习生（预备）", username: "test_model_intern", tone: "is-orange" }
      ],
      redirect: undefined
    };
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  created() {
    this.getCode();
    this.getCookie();
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img;
          this.loginForm.uuid = res.uuid;
        }
      });
    },
    getCookie() {
      const username = Cookies.get("username");
      const password = Cookies.get("password");
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      };
    },
    fillTestAccount(account) {
      this.loginForm.username = account.username;
      this.loginForm.password = "admin123";
      this.loginForm.code = "";
      this.$message({ message: `${account.label}账号已填充`, type: "success", duration: 1600 });
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove('rememberMe');
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{});
          }).catch(() => {
            this.loading = false;
            if (this.captchaEnabled) {
              this.getCode();
            }
          });
        }
      });
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background: #f4f6f9;
}
.title {
  margin: 0 auto 6px;
  text-align: center;
  color: #172033;
  font-size: 23px;
}
.subtitle {
  margin: 0 0 28px;
  color: #8490a5;
  font-size: 13px;
  text-align: center;
}

.login-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 30px 30px 8px;
  .el-input {
    height: 38px;
    input {
      height: 38px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}
.login-links {
  min-height: 20px;
  text-align: right;
}
.test-accounts {
  width: 400px;
  margin-top: 14px;
  padding: 16px 18px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-sizing: border-box;
}
.test-accounts__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 11px;
  color: #344054;
  font-size: 13px;
  font-weight: 600;
}
.test-accounts__hint {
  color: #98a2b3;
  font-size: 12px;
  font-weight: 400;
}
.test-account-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  max-height: 218px;
  overflow-y: auto;
  padding-right: 4px;
}
.test-account {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid #e5eaf2;
  border-radius: 6px;
  background: #fff;
  color: #344054;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.test-account:hover {
  border-color: #93b4f4;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.12);
}
.test-account strong,
.test-account small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.test-account strong {
  font-size: 12px;
  font-weight: 600;
}
.test-account small {
  margin-top: 2px;
  color: #98a2b3;
  font-size: 11px;
}
.test-account__dot {
  flex: 0 0 auto;
  width: 7px;
  height: 7px;
  margin-right: 8px;
  border-radius: 50%;
}
.is-blue { background: #2563eb; }
.is-green { background: #12b76a; }
.is-orange { background: #f79009; }
.is-purple { background: #7f56d9; }
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 38px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 0;
}
@media screen and (max-width: 520px) {
  .login-form,
  .test-accounts {
    width: calc(100vw - 32px);
  }
  .test-account-list {
    grid-template-columns: 1fr;
  }
}
.login-code-img {
  height: 38px;
}

.login {
  min-height: 100%;
  height: auto;
  flex-direction: column;
  padding: 32px 20px 72px;
  background: #f4f6f9;
}
.auth-layout {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) 420px;
  width: min(960px, 100%);
  min-height: 560px;
  overflow: hidden;
  border: 1px solid #e7ecf3;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 16px 42px rgba(31, 49, 78, .08);
}
.auth-intro {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 52px;
  background: #edf4ff;
}
.auth-intro .brand { padding: 0 0 58px; }
.intro-kicker, .eyebrow { color: #1764f5; font-size: 12px; letter-spacing: 0; }
.auth-intro h1 { max-width: 380px; margin: 16px 0 14px; color: #1d2129; font-size: 30px; line-height: 1.35; }
.auth-intro p { max-width: 390px; margin: 0; color: #667085; line-height: 1.8; }
.intro-facts { display: flex; gap: 26px; margin-top: 50px; color: #667085; font-size: 12px; }
.intro-facts span { display: flex; flex-direction: column; gap: 5px; }
.intro-facts b { color: #1d2129; font-size: 14px; }
.auth-panel { display: flex; flex-direction: column; justify-content: center; padding: 44px 42px; }
.panel-heading { margin-bottom: 26px; }
.panel-heading .title { margin: 8px 0 5px; color: #1d2129; font-size: 24px; }
.panel-heading .subtitle { margin: 0; color: #8490a5; font-size: 13px; }
.login-form { width: 100%; padding: 0; }
.login-form .el-form-item { margin-bottom: 20px; }
.login-form .el-button { height: 40px; border-radius: 6px; }
.test-accounts { width: 100%; margin-top: 20px; padding: 14px 0 0; border: 0; border-top: 1px solid #e7ecf3; border-radius: 0; background: transparent; }
.el-login-footer {
  position: static;
  flex: 0 0 auto;
  margin-top: 16px;
  color: #98a2b3;
}
@media screen and (max-width: 760px) {
  .auth-layout { grid-template-columns: 1fr; min-height: 0; }
  .auth-intro { padding: 28px; }
  .auth-intro .brand { padding-bottom: 26px; }
  .auth-intro h1 { margin-top: 12px; font-size: 24px; }
  .intro-facts { margin-top: 26px; }
  .auth-panel { padding: 32px 28px; }
}
</style>
