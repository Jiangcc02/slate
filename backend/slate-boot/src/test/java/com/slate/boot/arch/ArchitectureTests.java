// 域/模块: 平台底座/CI 防线
// 类型: 架构测试（ArchUnit）
// 职责: 机械执行铁律 L7——模块依赖单向、internal 包对外封闭、分层不跨层、模块间无环
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.boot.arch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构防线：违规 = 构建失败（CI 拒绝合并）。
 * 新业务域 module 落地时：① 加入下方 layer 定义；② 为它补一条 internal 封闭规则。
 */
@AnalyzeClasses(packages = "com.slate", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTests {

    // ── L7：模块依赖方向（common ← framework ← platform ← boot，单向） ──

    @ArchTest
    static final ArchRule 模块依赖方向_自底向上 = layeredArchitecture()
            .consideringAllDependencies()
            .layer("boot").definedBy("com.slate.boot..")
            .layer("platform").definedBy("com.slate.platform..")
            .layer("framework").definedBy("com.slate.framework..")
            .layer("common").definedBy("com.slate.common..")
            .whereLayer("boot").mayNotBeAccessedByAnyLayer()
            .whereLayer("platform").mayOnlyBeAccessedByLayers("boot")
            .whereLayer("framework").mayOnlyBeAccessedByLayers("platform", "boot")
            .whereLayer("common").mayOnlyBeAccessedByLayers("framework", "platform", "boot");

    // ── L7：模块间只能引用对方暴露的 api 包 ──

    @ArchTest
    static final ArchRule 底座internal包对外封闭 = noClasses()
            .that().resideOutsideOfPackage("com.slate.platform..")
            .should().dependOnClassesThat().resideInAPackage("com.slate.platform.internal..")
            .because("跨模块只能引用 com.slate.platform.api（铁律 L7）");

    // ── L7：模块内分层单向（Controller → Service → Repository） ──
    // allowEmptyShould：骨架阶段尚无分层类，规则在首个业务域 module 落地后自动生效

    @ArchTest
    static final ArchRule controller不得直连repository = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule service不得依赖controller = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .allowEmptyShould(true);

    // ── L7：依赖图无环 ──

    @ArchTest
    static final ArchRule 模块间无循环依赖 = slices()
            .matching("com.slate.(*)..")
            .should().beFreeOfCycles();
}
