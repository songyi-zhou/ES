package org.zhou.backend.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zhou.backend.entity.User;
import org.zhou.backend.entity.ImportLog;
import org.zhou.backend.model.request.UserImportRequest;
import org.zhou.backend.model.response.ImportResult;
import java.io.IOException;
import java.util.List;

@Service
public interface UserImportService {
    ImportResult importUsersFromExcel(MultipartFile file) throws IOException;
    User addUser(UserImportRequest request);
    void generateTemplate(HttpServletResponse response) throws IOException;
    List<ImportLog> getImportLogs();
}