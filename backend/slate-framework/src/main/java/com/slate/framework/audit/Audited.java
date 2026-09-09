// 域/模块: 平台底座/工程规范
// 类型: 审计注解
// 职责: 声明写操作审计（铁律 L8）——切面记录 谁/动作/目标/参数摘要/结果 并落 operation_log
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.framework.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 写操作审计：action 为动作标识（如 org.member.remove）；target 可选，描述操作对象 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    String action();
    String target() default "";
}
