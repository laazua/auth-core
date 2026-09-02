import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from '@/router';
import App from '@/App.vue';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

import '@/styles/global.css';
import 'nprogress/nprogress.css';

import NProgress from 'nprogress';

NProgress.configure({ showSpinner: false });

router.beforeEach(() => {
  NProgress.start();
});

router.afterEach(() => {
  NProgress.done();
});

router.onError(() => {
  NProgress.done();
});

const app = createApp(App);

for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component);
}

app.use(createPinia());
app.use(router);

app.mount('#app');
