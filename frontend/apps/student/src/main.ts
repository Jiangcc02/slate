// 域/模块: 平台底座/学生端应用
// 类型: 应用入口
// 职责: 装配学生端——Pinia、路由、Element Plus、根组件挂载
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.use(ElementPlus);
app.mount("#app");
