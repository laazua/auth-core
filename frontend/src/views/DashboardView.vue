<script setup lang="ts">
  import { ref } from 'vue';
  import BaseCard from '@/components/BaseCard.vue';

  const stats = ref([
    { label: '总用户数', value: '1,234', trend: '+12%', trendType: 'success', icon: 'User', gradient: 'var(--gradient-primary)' },
    { label: '总角色数', value: '56', trend: '+5%', trendType: 'success', icon: 'SwitchButton', gradient: 'var(--gradient-success)' },
    { label: '总权限数', value: '234', trend: '+8%', trendType: 'success', icon: 'Key', gradient: 'var(--gradient-info)' },
    { label: '总模块数', value: '12', trend: '-2%', trendType: 'warning', icon: 'Cpu', gradient: 'var(--gradient-warning)' },
  ]);

  const recentActivities = ref([
    {
      time: '2024-01-15 10:30',
      user: 'admin',
      action: '创建了用户',
      target: 'zhangsan',
      type: 'success',
    },
    {
      time: '2024-01-15 09:45',
      user: 'admin',
      action: '分配了角色',
      target: 'lisi -> 管理员',
      type: 'info',
    },
    {
      time: '2024-01-15 08:20',
      user: 'admin',
      action: '修改了权限',
      target: '用户管理模块',
      type: 'warning',
    },
    {
      time: '2024-01-14 17:10',
      user: 'admin',
      action: '新增了模块',
      target: '报表中心',
      type: 'success',
    },
    {
      time: '2024-01-14 15:30',
      user: 'admin',
      action: '删除了角色',
      target: '测试角色',
      type: 'danger',
    },
  ]);
</script>

<template>
  <div class="dashboard">
    <div class="dashboard__header">
      <h1 class="dashboard__title gradient-text">仪表盘</h1>
      <p class="dashboard__subtitle">欢迎回来，这里是系统概览</p>
    </div>

    <div class="dashboard__stats">
      <BaseCard
        v-for="stat in stats"
        :key="stat.label"
        class="dashboard__stat-card"
        :bordered="true"
        :hoverable="true"
        :gradient="true"
      >
        <div class="stat__content">
          <div class="stat__icon" :style="{ background: stat.gradient }">
            <component :is="stat.icon" />
          </div>
          <div class="stat__label">{{ stat.label }}</div>
          <div class="stat__value">{{ stat.value }}</div>
          <div class="stat__trend" :class="['stat__trend--' + stat.trendType]">
            <span class="stat__trend-icon">
              <svg
                v-if="stat.trendType === 'success'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="23 6 13.5 15.5 8.5 10.5" />
                <path d="M17 18a5 5 0 0 1-10 0" />
              </svg>
              <svg
                v-else
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              >
                <polyline points="17 18 12 13 7 18" />
                <path d="M7 6a5 5 0 0 1 10 0" />
              </svg>
            </span>
            <span>{{ stat.trend }}</span>
          </div>
        </div>
      </BaseCard>
    </div>

    <div class="dashboard__content">
      <BaseCard title="近期活动" class="dashboard__activity-card" :gradient="true">
        <div class="activity__list">
          <div v-for="activity in recentActivities" :key="activity.time" class="activity__item">
            <div class="activity__time">{{ activity.time }}</div>
            <div class="activity__info">
              <span class="activity__user">{{ activity.user }}</span>
              <span class="activity__action">{{ activity.action }}</span>
              <span class="activity__target">{{ activity.target }}</span>
            </div>
            <div class="activity__type" :class="['activity__type--' + activity.type]">
              <span class="activity__dot" />
            </div>
          </div>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<style scoped lang="scss">
  .dashboard {
    padding: 0;
    position: relative;
    z-index: 1;
  }

  .dashboard__header {
    margin-bottom: 28px;
    padding-bottom: 20px;
    border-bottom: 1px solid var(--color-border-light);
  }

  .dashboard__title {
    margin: 0 0 6px;
    font-size: var(--font-size-3xl);
    font-weight: var(--font-weight-bold);
    line-height: 1.2;
    letter-spacing: -0.5px;
  }

  .dashboard__subtitle {
    margin: 0;
    font-size: var(--font-size-base);
    color: var(--color-text-secondary);
  }

  .dashboard__stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 28px;
  }

  .stat__content {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .stat__icon {
    width: 48px;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: var(--color-border-radius-lg);
    color: white;
    font-size: 22px;
    margin-bottom: 4px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

    svg {
      width: 22px;
      height: 22px;
    }
  }

  .stat__label {
    font-size: 13px;
    color: var(--color-text-secondary);
    font-weight: var(--font-weight-medium);
  }

  .stat__value {
    font-size: var(--font-size-3xl);
    font-weight: var(--font-weight-bold);
    color: var(--color-text-primary);
    line-height: 1.1;
    letter-spacing: -1px;
  }

  .stat__trend {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    font-weight: var(--font-weight-semibold);
    padding: 4px 10px;
    border-radius: 20px;
    width: fit-content;

    &--success {
      color: var(--color-success);
      background: rgba(0, 168, 107, 0.08);
    }

    &--warning {
      color: var(--color-warning);
      background: rgba(217, 119, 6, 0.08);
    }

    &--danger {
      color: var(--color-error);
      background: rgba(220, 38, 38, 0.08);
    }
  }

  .stat__trend-icon {
    display: flex;
    width: 14px;
    height: 14px;
    flex-shrink: 0;

    svg {
      width: 100%;
      height: 100%;
    }
  }

  .dashboard__content {
    display: grid;
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .activity__list {
    display: flex;
    flex-direction: column;
  }

  .activity__item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px 0;
    border-bottom: 1px solid var(--color-border-lighter);
    transition: background var(--transition-duration-base) var(--transition-timing);

    &:last-child {
      border-bottom: none;
    }

    &:hover {
      background: var(--color-bg-hover);
      margin: 0 calc(var(--card-padding) * -1);
      padding-left: calc(var(--card-padding));
      padding-right: calc(var(--card-padding));
      border-radius: var(--color-border-radius-sm);
    }
  }

  .activity__time {
    width: 140px;
    flex-shrink: 0;
    font-size: 13px;
    color: var(--color-text-secondary);
    font-weight: var(--font-weight-medium);
  }

  .activity__info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  .activity__user {
    font-size: 14px;
    font-weight: var(--font-weight-semibold);
    color: var(--color-text-primary);
  }

  .activity__action {
    font-size: 13px;
    color: var(--color-text-regular);
  }

  .activity__target {
    font-size: 12px;
    color: var(--color-text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .activity__type {
    width: 12px;
    height: 12px;
    flex-shrink: 0;
    border-radius: 50%;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      inset: -4px;
      border-radius: 50%;
      opacity: 0.2;
    }

    &--success {
      background: var(--color-success);

      &::before {
        background: var(--color-success);
      }
    }

    &--info {
      background: var(--color-info);

      &::before {
        background: var(--color-info);
      }
    }

    &--warning {
      background: var(--color-warning);

      &::before {
        background: var(--color-warning);
      }
    }

    &--danger {
      background: var(--color-error);

      &::before {
        background: var(--color-error);
      }
    }
  }

  .activity__dot {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: currentColor;
  }

  @media (max-width: 1200px) {
    .dashboard__stats {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  @media (max-width: 768px) {
    .dashboard__stats {
      grid-template-columns: 1fr;
    }

    .activity__time {
      width: auto;
      flex-shrink: 0;
    }

    .dashboard__title {
      font-size: var(--font-size-2xl);
    }
  }
</style>