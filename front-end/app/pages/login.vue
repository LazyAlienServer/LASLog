<script setup lang="ts">
definePageMeta({
  layout: 'background',
})
</script>

<template>
  <div id="login_card">
    <NuxtImg id="login_illustration" preload src="/img/login-illustration.jpg" alt="login illustration" />
    <div id="login_form">
      <h1>
        账户验证
      </h1>
      <UInput
        type="text"
        icon="i-custom-login-username"
        placeholder="用户名"
        :autofocus="true"
        :ui="{ root: 'relative input_wrapper w-8/10',
               base: 'ps-10 bg-transparent text-[#383838] font-thin text-lg ring-0 focus-visible:ring-0',
               leadingIcon: 'size-6' }"
      />
      <UInput
        type="password"
        icon="i-custom-login-password"
        placeholder="密码"
        :ui="{ root: 'relative input_wrapper w-8/10',
               base: 'ps-10 bg-transparent text-[#383838] font-thin text-lg ring-0 focus-visible:ring-0' }"
      />
      <UButton
        :ui="{
          base: 'mt-8 w-7/10 h-11 justify-center button_wrapper text-white text-xl tracking-[1rem] hover:opacity-90',
        }"
      >
        登录
      </UButton>
      <div class="w-10/12 flex flex-row-reverse">
        <ULink to="/" class="active_account hover:opacity-85">
          激活账号
        </ULink>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
$gradient-blue-purple-soft: linear-gradient(to right, #8295ff, #c97cfc);

#login_card {
  position: relative;
  display: flex;
  flex-direction: row;
  background-color: white;
  border-radius: 1rem;
  top: 30vh;
  left: 27vw;
  width: 46vw;
  height: 44vh;
  overflow: hidden;
}

@media (max-width: 480px) {
  #login_card {
    width: 100%;
    height: 100%;
    top: 0;
    left: 0;
    border-radius: 0;
  }
}

#login_illustration {
  position: relative;
  top: 0;
  left: 0;
  height: 100%;
  width: 40%;
  object-fit: cover;
}

@media (max-width: 1200px) {
  #login_illustration {
    display: none;
  }
}

#login_form {
  position: relative;
  top: 0;
  left: 0;
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-around;
}

#login_form h1 {
  color: transparent;
  background-image: $gradient-blue-purple-soft;
  background-clip: text;
  border: none;
  text-shadow: none;
  font-size: 1.875rem;
  letter-spacing: 0.8rem;
}

.input_wrapper::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background-image: $gradient-blue-purple-soft;
}

#login_form {
  :deep(.button_wrapper) {
    position: relative;
    background-image: linear-gradient(to right, #8295ff, #cfd6ff, #e2b8ff); /* ✅ 必需：遮挡内容区 */
    border-radius: 8px;
    z-index: 1;
    cursor: pointer;
  }

  :deep(.button_wrapper)::after {
    content: '';
    position: absolute;
    inset: 1px;
    background-image: linear-gradient(to right, #e0d1eb, #e2b8ff);
    border-radius: inherit;
    z-index: -1;
    pointer-events: none;
  }
}

.active_account {
  color: #c175f9;
  position: relative;
  padding-right: 0.5rem;
  width: auto;
  line-height: 1rem;
  text-align: center;
  font-weight: 200;
}
.active_account::after {
  content: '';
  position: absolute;
  bottom: 0;
  right: 0;
  background-color: #cc88fc;
  width: 0.2rem;
  height: 100%;
}
</style>
