import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from '@/router';
import App from '@/App.vue';

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

app.use(createPinia());
app.use(router);

app.mount('#app');
