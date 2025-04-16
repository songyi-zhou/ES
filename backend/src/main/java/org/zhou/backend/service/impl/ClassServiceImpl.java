package org.zhou.backend.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.SchoolClass;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.service.ClassService;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {
    
    private final ClassRepository classRepository;
    
    @Override
    public List<String> getDistinctMajors() {
        return classRepository.findAll().stream()
            .map(schoolClass -> schoolClass.getMajor())
            .filter(major -> major != null && !major.isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<SchoolClass> findAll() {
        return classRepository.findAll();
    }

    @Override
    public Page<SchoolClass> findPage(PageRequest pageRequest) {
        return classRepository.findAll(pageRequest);
    }

    @Override
    public SchoolClass save(SchoolClass classEntity) {
        return classRepository.save(classEntity);
    }

    @Override
    public SchoolClass update(SchoolClass classEntity) {
        // 先获取原有实体
        SchoolClass existingClass = classRepository.findById(classEntity.getId())
                .orElseThrow(() -> new RuntimeException("班级不存在"));
        // 保留原有的创建时间
        classEntity.setCreatedAt(existingClass.getCreatedAt());
        return classRepository.save(classEntity);
    }

    @Override
    public void delete(String id) {
        classRepository.deleteById(id);
    }

    @Override
    public void generateTemplate(HttpServletResponse response) throws IOException {
        // 创建工作簿和工作表
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("班级导入模板");
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        String[] headers = {"班级ID", "班级名称", "学院", "专业", "年级"};
        
        // 设置列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, 20 * 256);
        }
        
        // 创建标题单元格样式
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        
        
        // 写入标题
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        Row exampleRow = sheet.createRow(1);
        exampleRow.createCell(0).setCellValue("CS2101");
        exampleRow.createCell(1).setCellValue("计算机科学与技术2021-1");
        exampleRow.createCell(2).setCellValue("信息科学技术学院");
        exampleRow.createCell(3).setCellValue("计算机科学与技术");
        exampleRow.createCell(4).setCellValue("2021");
        
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=class_template.xlsx");
        
        // 写入响应流
        ServletOutputStream outputStream = response.getOutputStream();
        workbook.write(outputStream);
        workbook.close();
        outputStream.flush();
    }

    @Override
    public int importClasses(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        
        List<SchoolClass> classes = new ArrayList<>();
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        
        // 从第二行开始读取数据（跳过标题行）
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            // 检查必填字段
            String id = getCellValueAsString(row.getCell(0));
            String name = getCellValueAsString(row.getCell(1));
            if (id.isEmpty() || name.isEmpty()) {
                continue;
            }
            
            SchoolClass classEntity = new SchoolClass();
            classEntity.setId(id);
            classEntity.setName(name);
            classEntity.setDepartment(getCellValueAsString(row.getCell(2)));
            classEntity.setMajor(getCellValueAsString(row.getCell(3)));
            classEntity.setGrade(getCellValueAsString(row.getCell(4)));
            
            classes.add(classEntity);
        }
        
        workbook.close();
        
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("没有有效的数据行");
        }
        
        // 批量保存
        classRepository.saveAll(classes);
        return classes.size();
    }
    
    // 辅助方法：获取单元格的字符串值
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
} 