package org.zhou.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zhou.backend.entity.EvaluationMaterial;
import org.zhou.backend.entity.SchoolClass;
import org.zhou.backend.entity.User;
import org.zhou.backend.model.request.ReportRequest;
import org.zhou.backend.model.request.ReviewRequest;
import org.zhou.backend.repository.ClassGroupMemberRepository;
import org.zhou.backend.repository.ClassRepository;
import org.zhou.backend.repository.EvaluationMaterialRepository;
import org.zhou.backend.repository.GradeGroupLeaderRepository;
import org.zhou.backend.repository.QuestionMaterialRepository;
import org.zhou.backend.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class QuestionMaterialService {
    
    private static final Logger log = LoggerFactory.getLogger(QuestionMaterialService.class);
    
    @Autowired
    private QuestionMaterialRepository questionMaterialRepository;
    
    @Autowired
    private ClassGroupMemberRepository classGroupMemberRepository;
    
    @Autowired
    private GradeGroupLeaderRepository gradeGroupLeaderRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ClassRepository classRepository;
    
    @Autowired
    private EvaluationMaterialRepository materialRepository;
    
    @Autowired
    private EvaluationService evaluationService;  // 注入 EvaluationService
    
    public Page<EvaluationMaterial> getQuestionMaterials(
            String department,
            String squad,
            List<String> statuses,
            int page,
            int size,
            String keyword) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<EvaluationMaterial> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 添加院系条件
            if (department != null && !department.isEmpty()) {
                predicates.add(cb.equal(root.get("department"), department));
            }
            
            // 添加中队条件
            if (squad != null && !squad.isEmpty()) {
                predicates.add(cb.equal(root.get("squad"), squad));
            }
            
            // 添加状态条件
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            
            // 添加关键词搜索条件
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("title"), likePattern),
                    cb.like(root.get("description"), likePattern)
                ));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return materialRepository.findAll(spec, pageable);
    }
    
    public void reviewMaterial(ReviewRequest request) {
        EvaluationMaterial material = questionMaterialRepository.findById(request.getMaterialId())
            .orElseThrow(() -> new RuntimeException("材料不存在"));
            
        material.setStatus(request.getStatus());
        material.setReviewComment(request.getComment());
        
        // 如果审核通过，更新加分相关字段并调用加分函数
        if ("APPROVED".equals(request.getStatus())) {
            // 更新材料表中的加分信息
            material.setEvaluationType(request.getEvaluationType());
            material.setScore(request.getScore());
            
            try {
                evaluationService.updateTotalBonus(
                    material.getUserId(),
                    request.getEvaluationType(),
                    request.getScore(),
                    material.getId()
                );
            } catch (IllegalStateException e) {
                throw new RuntimeException(e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("加分操作失败：" + e.getMessage());
            }
        }
        
        questionMaterialRepository.save(material);
    }
    
    @Transactional
    public Map<String, Object> reportToInstructor(ReportRequest request) {
        // 1. 获取材料列表并记录日志
        List<EvaluationMaterial> materials = questionMaterialRepository.findAllById(request.getMaterialIds());
        log.info("Found {} materials to report", materials.size());
        
        // 2. 获取这些材料对应的班级信息
        Set<String> departments = materials.stream()
            .map(m -> classRepository.findById(m.getClassId())
                .map(SchoolClass::getDepartment)
                .orElse(null))
            .filter(dept -> dept != null)
            .collect(Collectors.toSet());
        
        Set<String> grades = materials.stream()
            .map(m -> m.getClassId().substring(0, 4))
            .collect(Collectors.toSet());
        
        // 3. 查询负责这些学院和年级的导员
        List<User> instructors = userRepository.findInstructorsByDepartmentsAndGrades(departments, grades);
        log.info("Found {} instructors", instructors.size());
        
        // 4. 更新材料状态
        for (EvaluationMaterial material : materials) {
            material.setStatus("REPORTED");
            material.setReviewComment(request.getNote());
            material.setReportedAt(LocalDateTime.now());
            // 设置第一个匹配的导员为审核人
            if (!instructors.isEmpty()) {
                material.setReviewerId(instructors.get(0).getId());
            }
        }
        materials = questionMaterialRepository.saveAll(materials);
        log.info("Updated {} materials", materials.size());
        
        // 5. 返回导员信息
        List<Map<String, Object>> instructorInfos = instructors.stream()
            .map(i -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", i.getId());
                map.put("name", i.getName());
                map.put("department", i.getDepartment());
                return map;
            })
            .collect(Collectors.toList());
        
        return Map.of(
            "success", true,
            "message", "材料已成功上报",
            "instructors", instructorInfos,
            "updatedMaterials", materials.size()
        );
    }
} 