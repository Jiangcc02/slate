// 域/模块: 平台底座/用户中心
// 类型: 服务
// 职责: 学生 Excel 批量导入——逐行建档+开户+入班（复用名单服务），返回行级结果报告
// 设计文档: docs/design/平台底座/design.md
// 维护者: 协调者 / agent-fffabc
package com.slate.platform.internal.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.slate.platform.api.user.ImportResult;
import com.slate.platform.api.user.UserDto;
import com.slate.platform.internal.org.entity.OrgUnit;
import com.slate.platform.internal.org.mapper.OrgUnitMapper;
import com.slate.platform.internal.org.service.MemberService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** 模板列：姓名 | 学籍号 | 入学届 | 年级名 | 班级名（表头行跳过） */
@Service
public class UserImportService {

    private static final Logger log = LoggerFactory.getLogger(UserImportService.class);
    /** 单次导入行数上限：同步导入的耗时与事务规模保护（异步任务化随规模需求升级） */
    private static final int MAX_ROWS = 2000;

    private final UserService userService;
    private final OrgUnitMapper orgUnitMapper;
    private final MemberService memberService;
    private final TransactionTemplate transactionTemplate;

    public UserImportService(UserService userService, OrgUnitMapper orgUnitMapper, MemberService memberService,
                             TransactionTemplate transactionTemplate) {
        this.userService = userService;
        this.orgUnitMapper = orgUnitMapper;
        this.memberService = memberService;
        this.transactionTemplate = transactionTemplate;
    }

    public ImportResult importStudents(InputStream excel, String academicYear) {
        List<ImportResult.RowResult> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(excel)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_ROWS) {
                throw new com.slate.common.error.BusinessException(
                        com.slate.platform.internal.user.error.UserErrorCode.USER_005,
                        "单次导入不超过 " + MAX_ROWS + " 行");
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {   // 0=表头
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                rows.add(importRow(i + 1, row, academicYear));
            }
        } catch (com.slate.common.error.BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("导入解析失败: {}", e.getMessage());
            throw new com.slate.common.error.BusinessException(
                    com.slate.platform.internal.user.error.UserErrorCode.USER_005, "文件无法解析为 xlsx");
        }
        int failed = (int) rows.stream().filter(r -> !r.success()).count();
        return new ImportResult(rows.size(), rows.size() - failed, failed, rows);
    }

    /** 单行 = 一个事务：建档+开户+入班原子成功或整体回滚（不留半行孤儿数据）；
     *  重放同一文件时学籍号哈希唯一键使重复行报 USER-007，不产生重复建档（导入幂等） */
    private ImportResult.RowResult importRow(int rowNo, Row row, String academicYear) {
        String name = text(row, 0);
        try {
            ImportResult.RowResult result = transactionTemplate.execute(tx ->
                    doImportRow(rowNo, row, academicYear));
            return result;
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ImportResult.RowResult(rowNo, name, false, message);
        }
    }

    private ImportResult.RowResult doImportRow(int rowNo, Row row, String academicYear) {
        String name = text(row, 0);
        if (name == null || name.isBlank()) {
            throw rowError("姓名为空");
        }
        String studentNo = text(row, 1);
        String gradeEntry = text(row, 2);
        String gradeName = text(row, 3);
        String className = text(row, 4);
        Long classId = resolveClass(gradeName, className);
        UserDto created = userService.create(new UserDto.CreateRequest(
                "STUDENT", name, null, studentNo, gradeEntry, null, null, null, null));
        memberService.enroll(classId, List.of(created.id()), academicYear);
        return new ImportResult.RowResult(rowNo, name, true, "ok:" + created.username());
    }

    private Long resolveClass(String gradeName, String className) {
        OrgUnit grade = orgUnitMapper.selectOne(new LambdaQueryWrapper<OrgUnit>()
                .eq(OrgUnit::getType, OrgUnit.GRADE).eq(OrgUnit::getName, gradeName).last("LIMIT 1"));
        if (grade == null) {
            throw rowError("年级不存在: " + gradeName);
        }
        OrgUnit classUnit = orgUnitMapper.selectOne(new LambdaQueryWrapper<OrgUnit>()
                .eq(OrgUnit::getType, OrgUnit.CLASS).eq(OrgUnit::getName, className)
                .eq(OrgUnit::getParentId, grade.getId()).last("LIMIT 1"));
        if (classUnit == null) {
            throw rowError("班级不存在: " + gradeName + "/" + className);
        }
        return classUnit.getId();
    }

    private String text(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (value == Math.floor(value)) {
                return String.valueOf((long) value);
            }
            return String.valueOf(value);
        }
        return cell.getStringCellValue() == null ? null : cell.getStringCellValue().trim();
    }

    /** 行级校验失败（USER-006，明细消息随行返回） */
    private static com.slate.common.error.BusinessException rowError(String message) {
        return new com.slate.common.error.BusinessException(
                com.slate.platform.internal.user.error.UserErrorCode.USER_006, message);
    }
}
