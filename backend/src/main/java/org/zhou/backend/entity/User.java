package org.zhou.backend.entity;

import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String name;
    
    private String department;
    private String major;
    private String className;
    
    @Column(name = "class_id")
    private String classId;
    
    @Column(name = "role_level")
    private Integer roleLevel;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    private Set<String> roles;
    
    @Column(name = "grade")
    private String grade;

    @Column(name = "squad")
    private String squad;  // 对于学生是所属中队号，对于导员是负责的中队列表

    @Column(name = "phone")
    private String phone;  // 联系电话

    @Column(name = "email")
    private String email;  // 邮箱

    @Column(name = "avatar_url")
    private String avatarUrl;  // 头像URL

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
} 