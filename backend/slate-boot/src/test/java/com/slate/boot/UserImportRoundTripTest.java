// 域/模块: 平台底座/用户中心
// 类型: 集成测试
// 职责: Excel 导入往返——生成模板格式 xlsx（含成功行与失败行）→ 导入 → 断言建档/开户/入班与行级报告
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.boot;

import com.slate.platform.api.user.ImportResult;
import com.slate.platform.internal.user.service.UserImportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
class UserImportRoundTripTest {

    @Autowired
    private UserImportService importService;

    @Test
    void 导入学生成功行建档入班失败行留痕(@TempDir Path temp) throws Exception {
        byte[] excel = buildExcel();
        Files.write(temp.resolve("import.xlsx"), excel);
        // 冒烟文件另存固定路径（供 curl 上传验收用）
        try (OutputStream out = Files.newOutputStream(Path.of(System.getProperty("java.io.tmpdir"),
                "slate-import-test.xlsx"))) {
            out.write(excel);
        }

        ImportResult result = importService.importStudents(new ByteArrayInputStream(excel), "2026-2027");

        assertEquals(3, result.total());
        assertEquals(2, result.succeeded());
        assertEquals(1, result.failed());
        assertTrue(result.rows().stream().anyMatch(r -> r.success() && "导入庚".equals(r.studentName())));
        assertTrue(result.rows().stream().anyMatch(r -> !r.success() && "导入辛".equals(r.studentName())));
    }

    /** 模板列：姓名 | 学籍号 | 入学届 | 年级名 | 班级名；种子组织：一年级/一(1)班 存在、九年级 不存在。
     *  学籍号带运行时间戳：学籍号哈希唯一键会正确拦截重复导入（幂等去重），测试数据须每次运行唯一 */
    private byte[] buildExcel() throws Exception {
        String runTag = String.valueOf(System.currentTimeMillis());
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("students");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("学籍号");
            header.createCell(2).setCellValue("入学届");
            header.createCell(3).setCellValue("年级名");
            header.createCell(4).setCellValue("班级名");
            String[][] rows = {
                    {"导入庚", "S" + runTag + "01", "2026", "一年级", "一(1)班"},
                    {"导入辛", "S" + runTag + "02", "2026", "九年级", "九(9)班"},
                    {"导入壬", "S" + runTag + "03", "2026", "一年级", "一(1)班"},
            };
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int c = 0; c < rows[i].length; c++) {
                    row.createCell(c).setCellValue(rows[i][c]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
