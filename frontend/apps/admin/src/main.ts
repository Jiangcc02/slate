// 域/模块: 平台底座/管理端应用
// 类型: 应用入口
// 职责: 装配管理端——Pinia、路由、样式入口、根组件挂载
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import { createApp } from "vue";
import { createPinia } from "pinia";
import "./assets/main.css";
import App from "./App.vue";
import router from "./router";

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.mount("#app");
