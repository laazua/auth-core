import 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    title?: string;
    icon?: string;
    requiresAuth?: boolean;
    public?: boolean;
    roles?: string[];
    permissions?: string[];
    keepAlive?: boolean;
    hidden?: boolean;
    affix?: boolean;
    breadcrumb?: boolean;
    activeMenu?: string;
  }
}

export interface RouteMeta {
  title?: string;
  icon?: string;
  requiresAuth?: boolean;
  public?: boolean;
  roles?: string[];
  permissions?: string[];
  keepAlive?: boolean;
  hidden?: boolean;
  affix?: boolean;
  breadcrumb?: boolean;
  activeMenu?: string;
}

export interface AppRouteRecordRaw {
  path: string;
  name?: string;
  component?: unknown;
  redirect?: string;
  children?: AppRouteRecordRaw[];
  meta?: RouteMeta;
  alias?: string | string[];
  beforeEnter?: unknown;
  props?: unknown;
}
