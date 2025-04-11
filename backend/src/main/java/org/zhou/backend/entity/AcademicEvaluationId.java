package org.zhou.backend.entity;

import java.io.Serializable;
import java.util.Objects;

public class AcademicEvaluationId implements Serializable {
    private String academicYear;
    private Integer semester;
    private String studentId;

    // 默认构造函数
    public AcademicEvaluationId() {}

    // 带参数的构造函数
    public AcademicEvaluationId(String academicYear, Integer semester, String studentId) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.studentId = studentId;
    }

    // equals 和 hashCode 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcademicEvaluationId that = (AcademicEvaluationId) o;
        return Objects.equals(academicYear, that.academicYear) &&
               Objects.equals(semester, that.semester) &&
               Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(academicYear, semester, studentId);
    }
} 