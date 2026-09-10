// 域/模块: 平台底座/系统管理
// 类型: 控制器
// 职责: sys 前缀接口——字典/参数/登录日志/操作日志（契约见 detail/api/系统管理.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.sysadmin.controller;

import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.audit.Audited;
import com.slate.platform.internal.audit.entity.OperationLog;
import com.slate.platform.internal.auth.entity.LoginLog;
import com.slate.platform.internal.auth.security.RequirePermission;
import com.slate.platform.internal.sysadmin.entity.DictType;
import com.slate.platform.internal.sysadmin.entity.SysConfig;
import com.slate.platform.internal.sysadmin.service.SysService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/sys")
public class SysController {

    private final SysService sysService;

    public SysController(SysService sysService) {
        this.sysService = sysService;
    }

    // ── 字典（读公开给登录用户，写需权限）──

    @GetMapping("/dicts/{type}")
    public List<SysService.DictItemView> dictItems(@PathVariable String type) {
        return sysService.dictItems(type);
    }

    @GetMapping("/dicts")
    @RequirePermission("sys:dict:write")
    public List<DictType> dictTypes() {
        return sysService.dictTypes();
    }

    @PostMapping("/dicts")
    @RequirePermission("sys:dict:write")
    @Audited(action = "sys.dict.createType", target = "dict_type")
    public DictType createDictType(@Valid @RequestBody CreateDictTypeRequest request) {
        return sysService.createDictType(request.code(), request.name(), request.remark());
    }

    @PostMapping("/dicts/{type}/items")
    @RequirePermission("sys:dict:write")
    @Audited(action = "sys.dict.upsertItem", target = "dict_item")
    public void upsertDictItem(@PathVariable String type, @Valid @RequestBody UpsertDictItemRequest request) {
        sysService.upsertDictItem(type, request.value(), request.label(), request.sort());
    }

    @DeleteMapping("/dicts/items/{id}")
    @RequirePermission("sys:dict:write")
    @Audited(action = "sys.dict.deleteItem", target = "dict_item")
    public void deleteDictItem(@PathVariable Long id) {
        sysService.deleteDictItem(id);
    }

    public record CreateDictTypeRequest(@NotBlank @Size(max = 64) String code,
                                        @NotBlank @Size(max = 64) String name,
                                        @Size(max = 255) String remark) {
    }

    public record UpsertDictItemRequest(@NotBlank @Size(max = 64) String value,
                                        @NotBlank @Size(max = 64) String label,
                                        Integer sort) {
    }

    // ── 参数配置 ──

    @GetMapping("/configs/{key}")
    public Map<String, String> configValue(@PathVariable String key,
                                           @RequestParam(required = false)
                                           @Pattern(regexp = "GLOBAL|CAMPUS") String scope) {
        return Map.of("value", String.valueOf(sysService.configValue(key, scope)));
    }

    @GetMapping("/configs")
    @RequirePermission("sys:config:write")
    public List<SysConfig> configs() {
        return sysService.configs();
    }

    @PostMapping("/configs")
    @RequirePermission("sys:config:write")
    @Audited(action = "sys.config.upsert", target = "sys_config")
    public void upsertConfig(@Valid @RequestBody UpsertConfigRequest request) {
        sysService.upsertConfig(request.key(), request.value(), request.scope(), request.remark());
    }

    @DeleteMapping("/configs/{id}")
    @RequirePermission("sys:config:write")
    @Audited(action = "sys.config.delete", target = "sys_config")
    public void deleteConfig(@PathVariable Long id) {
        sysService.deleteConfig(id);
    }

    public record UpsertConfigRequest(@NotBlank @Size(max = 96) String key,
                                      @NotBlank @Size(max = 1024) String value,
                                      @Pattern(regexp = "GLOBAL|CAMPUS") String scope,
                                      @Size(max = 255) String remark) {
    }

    // ── 日志查询（只读）──

    @GetMapping("/login-logs")
    @RequirePermission("sys:log:read")
    public PageResult<LoginLog> loginLogs(@RequestParam(required = false) String username,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                          PageQuery query) {
        return sysService.loginLogs(username, from, to, query);
    }

    @GetMapping("/operation-logs")
    @RequirePermission("sys:log:read")
    public PageResult<OperationLog> operationLogs(@RequestParam(required = false) Long accountId,
                                                  @RequestParam(required = false) String traceId,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                  @RequestParam(required = false)
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                  PageQuery query) {
        return sysService.operationLogs(accountId, traceId, from, to, query);
    }
}
