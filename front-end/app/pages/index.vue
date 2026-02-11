<script setup lang="ts">
definePageMeta({
  layout: 'background',
})
</script>

<template>
  <div class="content">
    <div class="main-container">
      <div class="left-container">
        <h1><span>LAS</span><span>LOG</span></h1>
        <h2 class="h2-desktop">
          管理您在LAS的<strong>一切事务</strong>
        </h2>
        <div class="tag-container">
          <div v-for="value in [['交流', '#A263CF'], ['分享', '#9774DA'], ['资料', '#8C84E5'], ['归档', '#8194F0']]" :key="value[0]" class="tag" :style="{ backgroundColor: value[1] }">
            {{ value[0] }}
          </div>
        </div>
        <h2 class="h2-mobile">
          管理您在LAS的<strong>一切事务</strong>
        </h2>
        <h3>Lazy Alien Server 的服务器交流平台，集成白名单管理、作品归档、插件管理、信息交流等功能</h3>
      </div>
      <div class="right-container">
        <img src="/img/logo.svg" alt="LAS LOG Logo">
      </div>
    </div>
    <div class="button-container">
      <hr>
      <a href="/user">探索 LAS LOG</a>
      <hr>
    </div>
  </div>
</template>

<style scoped lang="scss">
h1 {
  font-size: 8rem;
  font-weight: 1000;
  color: #746aeb;
  letter-spacing: 0.25rem;
  margin: 0px 0px 2rem 0px;
  display: flex;
  gap: 2rem;
  span {
    margin: -3.8rem 0px;
  }
}

h2 {
  font-size: 3.5rem;
  letter-spacing: 0.25rem;
  font-weight: 500;
  color: #5c39a1;
  margin: 0px;
  white-space: nowrap;
}

.h2-mobile {
  display: none;
}

h3 {
  font-size: 1.1rem;
  font-weight: 500;
  color: #a263cf;
  margin: 0px;
  letter-spacing: 2px;
  padding-bottom: 2rem;
  padding-right: 15%;
}

img {
  width: 34rem;
  height: auto;
}

.content {
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  height: 90vh;
  width: 80%;
  margin-left: 10%;
}

.main-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-sizing: border-box;
  width: 100%;
}

.tag-container {
  display: flex;
  flex-wrap: wrap;
  flex-flow: row;
  gap: 1rem;
  margin: 1rem 0px 2rem;
}

.tag {
  font-size: 1.1rem;
  font-weight: 600;
  color: white;
  letter-spacing: 0.2rem;
  background-color: #5c39a1;
  border-radius: 2.5rem;
  padding: 0px 0.9rem 0px 1.1rem;
}

.button-container {
  letter-spacing: 2px;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  gap: 2.5rem;
  margin-top: 7vh;
  overflow: hidden;
  hr {
    width: 900px;
    border: 0;
    height: 2px;
    background-color: #746aeb;
    flex-shrink: 0;
  }
  a {
    color: #746aeb;
    font-size: 1.4rem;
    font-weight: 400;
    text-decoration: none;
    white-space: nowrap;
    flex-shrink: 0;
    padding: 0.25rem 2.8rem;
    border: #746aeb 2px solid;
    border-radius: 0.7rem;
    transition: all 0.2s ease-in-out;
  }
  a:hover {
    background-color: rgba($color: #746aeb, $alpha: 0.1);
  }
}

@media (max-width: 1680px) {
  .right-container {
    display: none;
  }
  h1 {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
  }
  .h2-desktop {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
  }
  h3 {
    padding-right: 0px;
  }
  .main-container {
    justify-content: center;
  }
  .left-container {
    text-align: center;
  }
  .tag-container {
    justify-content: center;
  }
  .tag {
    font-weight: 500;
  }
}

@media (max-width: 1200px) {
  h1 {
    letter-spacing: 0;
  }
  .content {
    width: 90%;
    margin-left: 5%;
  }
}

@media (max-width: 480px) {
  h1 {
    font-size: 8rem;
    flex-direction: column;
    margin-top: -1rem;
    margin-bottom: 3rem;
  }
  .h2-desktop {
    display: none;
  }
  .h2-mobile {
    display: block;
    font-size: 2.8rem;
    display: flex;
    flex-direction: column;
    strong {
      font-size: 3rem;
      margin-top: -10px;
    }
  }
  h3 {
    display: none;
  }
  .tag {
    padding: 0px 0.7rem 0px 0.9em;
  }
  hr {
    display: none;
  }
  .button-container {
    margin-top: 3vh;
  }
}
</style>
