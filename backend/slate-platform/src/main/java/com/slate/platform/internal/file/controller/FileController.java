// 域/模块: 平台底座/文件服务
// 类型: 控制器
// 职责: files 前缀接口——预签名/登记/下载 URL/检索/删除（契约见 detail/api/文件服务.md）
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.file.controller;

import com.slate.common.result.PageQuery;
import com.slate.common.result.PageResult;
import com.slate.framework.audit.Audited;
import com.slate.platform.api.file.FileDto;
import com.slate.platform.api.file.UploadCredentials;
import com.slate.platform.internal.auth.security.LoginUser;
import com.slate.platform.internal.auth.security.RequirePermission;
import com.slate.platform.internal.file.service.FileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload-credentials")
    public UploadCredentials issue(@Valid @RequestBody UploadCredentials.Request request) {
        return fileService.issueUploadCredentials(request);
    }

    @PostMapping
    @Audited(action = "file.register", target = "file_object")
    public FileDto register(@Valid @RequestBody UploadCredentials.RegisterRequest request,
                            @AuthenticationPrincipal LoginUser user) {
        return fileService.register(request, user.userId());
    }

    @GetMapping("/{id}/download-url")
    public Map<String, String> downloadUrl(@PathVariable Long id) {
        return Map.of("url", fileService.downloadUrl(id));
    }

    @GetMapping
    @RequirePermission("file:manage")
    public PageResult<FileDto> search(@RequestParam(required = false) String bizType,
                                      @RequestParam(required = false) Long bizId,
                                      PageQuery query) {
        return fileService.search(bizType, bizId, query);
    }

    @DeleteMapping("/{id}")
    @Audited(action = "file.delete", target = "file_object")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal LoginUser user) {
        boolean manager = user.hasPermission("file:manage");
        fileService.delete(id, user.userId(), manager);
    }
}
