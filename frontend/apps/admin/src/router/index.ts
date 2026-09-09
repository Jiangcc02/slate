// 域/模块: 平台底座/管理端应用
// 类型: 路由
// 职责: 管理端路由表——登录页与首页占位，业务页面随各域任务包落位
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../views/LoginView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/login" },
    { path: "/login", name: "login", component: LoginView, meta: { title: "登录" } },
  ],
});

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · slate 管理端` : "slate 管理端";
});

export default router;
