<script setup lang="ts">
definePageMeta({
  layout: 'background-home',
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
    <div class="footer-container">
      <div class="button-container">
        <hr>
        <NuxtLink id="explore_LASLOG" to="/login">
          探索 LAS LOG
        </NuxtLink>
        <hr>
      </div>
      <p class="copyright">
        Copyright © 2026 Lazy Alien Server
      </p>
    </div>
  </div>
</template>

<style scoped lang="scss">
h1 {
  font-size: clamp(4.8rem, 8vw, 8rem);
  font-weight: 1000;
  color: #746aeb;
  letter-spacing: 0.18rem;
  line-height: 0.95;
  margin: 0px 0px 1.5rem;
  display: flex;
  gap: 1.5rem;
  span {
    margin: 0px;
  }
}

h2 {
  font-size: clamp(2.6rem, 4vw, 4rem);
  letter-spacing: 0.12rem;
  font-weight: 700;
  line-height: 1.12;
  color: #5c39a1;
  margin: 0px;
}

.h2-mobile {
  display: none;
}

h3 {
  max-width: 38rem;
  font-size: 1.2rem;
  font-weight: 500;
  color: #a263cf;
  margin: 0px;
  line-height: 1.7;
  letter-spacing: 0.08rem;
}

img {
  width: min(100%, 36rem);
  height: auto;
}

.content {
  display: flex;
  justify-content: space-between;
  align-items: stretch;
  flex-direction: column;
  min-height: 100vh;
  width: min(1280px, calc(100% - 5rem));
  margin: 0 auto;
  padding: clamp(3rem, 6vw, 6rem) 0 clamp(2rem, 4vw, 3rem);
  box-sizing: border-box;
}

.main-container {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: clamp(2rem, 5vw, 6rem);
  align-items: center;
  box-sizing: border-box;
  width: 100%;
}

.left-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.right-container {
  display: flex;
  justify-content: center;
  align-items: center;
}

.tag-container {
  display: flex;
  flex-wrap: wrap;
  flex-direction: row;
  gap: 0.9rem;
  margin: 0px;
}

.tag {
  font-size: 1rem;
  font-weight: 600;
  color: white;
  letter-spacing: 0.14rem;
  background-color: #5c39a1;
  border-radius: 2.5rem;
  padding: 0.15rem 0.9rem 0.2rem 1rem;
}

.footer-container {
  display: flex;
  flex-direction: column;
  gap: 1.2rem;
  width: 100%;
}

.button-container {
  letter-spacing: 0.08rem;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  gap: clamp(1rem, 2vw, 2rem);
  overflow: hidden;

  hr {
    width: 100%;
    border: 0;
    height: 2px;
    background-color: #746aeb;
    flex-shrink: 0;
    opacity: 0.9;
  }

  #explore_LASLOG {
    color: #746aeb;
    font-size: 1.45rem;
    font-weight: 600;
    text-decoration: none;
    white-space: nowrap;
    flex-shrink: 0;
    display: inline-flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.55rem 1.9rem;
    border: #746aeb 2px solid;
    border-radius: 0.7rem;
    transition: all 0.2s ease-in-out;
  }

  #explore_LASLOG::before {
    content: '→';
    font-size: 1.8rem;
    line-height: 1;
  }

  #explore_LASLOG:hover {
    background-color: rgba($color: #746aeb, $alpha: 0.1);
    transform: translateY(-1px);
  }
}

.copyright {
  margin: 0px;
  text-align: center;
  color: rgba(116, 106, 235, 0.75);
  font-size: 1.1rem;
  font-weight: 500;
  letter-spacing: 0.08rem;
}

@media (max-width: 1200px) {
  .content {
    width: min(100%, calc(100% - 3rem));
  }

  .main-container {
    grid-template-columns: minmax(0, 1fr) minmax(260px, 0.8fr);
    gap: 2rem;
  }
}

@media (max-width: 1024px) {
  .right-container {
    display: none;
  }

  h1 {
    display: flex;
    justify-content: center;
  }

  .h2-desktop {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
  }

  h3 {
    max-width: 100%;
  }

  .main-container {
    display: flex;
    justify-content: center;
  }

  .left-container {
    text-align: center;
    align-items: center;
  }

  .tag-container {
    justify-content: center;
  }

  .tag {
    font-weight: 500;
  }
}

@media (max-width: 768px) {
  h1 {
    gap: 0.8rem;
    letter-spacing: 0.05rem;
  }

  .button-container {
    gap: 0.75rem;
  }

  .button-container #explore_LASLOG {
    font-size: 1.2rem;
    padding: 0.55rem 1.4rem;
  }
}

@media (max-width: 480px) {
  h1 {
    font-size: 5rem;
    flex-direction: column;
    gap: 0;
    margin-bottom: 1rem;
  }

  .h2-desktop {
    display: none;
  }

  .h2-mobile {
    font-size: 2.8rem;
    display: flex;
    flex-direction: column;
    align-items: center;

    strong {
      font-size: 3rem;
      margin-top: -10px;
    }
  }

  h3 {
    font-size: 1rem;
    line-height: 1.6;
  }

  .tag {
    padding: 0px 0.7rem 0px 0.9em;
  }

  .button-container hr {
    display: none;
  }

  .button-container {
    justify-content: center;
  }

  .button-container #explore_LASLOG {
    width: 100%;
    justify-content: center;
  }

  .content {
    width: min(100%, calc(100% - 2rem));
    padding-top: 2.5rem;
    padding-bottom: 2rem;
  }

  .copyright {
    font-size: 0.9rem;
  }
}
</style>
