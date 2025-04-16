package org.zhou.backend.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.SchoolClass;

import jakarta.servlet.http.HttpServletResponse;

public interface ClassService {
    List<String> getDistinctMajors();
    List<SchoolClass> findAll();
    Page<SchoolClass> findPage(PageRequest pageRequest);
    SchoolClass save(SchoolClass classEntity);
    SchoolClass update(SchoolClass classEntity);
    void delete(String id);
    void generateTemplate(HttpServletResponse response) throws IOException;
    int importClasses(MultipartFile file) throws Exception;
} 