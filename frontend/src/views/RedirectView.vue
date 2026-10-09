<script setup lang="ts">
  import { onBeforeMount } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  const route = useRoute();
  const router = useRouter();

  onBeforeMount(() => {
    const { path } = route.params;
    router.replace({ path: `/${path}` });
  });
</script>

<template>
  <div />
</template>
