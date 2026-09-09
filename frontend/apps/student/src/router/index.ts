// 域/模块: 平台底座/学生端应用
// 类型: 路由
// 职责: 学生端路由表——登录页与首页占位，业务页面随各域任务包落位
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../views/LoginView.vue";
import HomeView from "../views/HomeView.vue";
import { getAccessToken } from "../api/http";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/login" },
    { path: "/login", name: "login", component: LoginView, meta: { title: "登录" } },
    { path: "/home", name: "home", component: HomeView, meta: { title: "首页" } },
  ],
});

router.beforeEach((to) => {
  // 未持有 accessToken 禁入首页，避免占位页渲染出无主内容
  if (to.name === "home" && !getAccessToken()) {
    return { name: "login" };
  }
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · slate 学生端` : "slate 学生端";
});

export default router;
