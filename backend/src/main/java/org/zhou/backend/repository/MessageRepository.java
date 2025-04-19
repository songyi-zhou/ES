package org.zhou.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zhou.backend.model.Message;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m WHERE m.receiver = :receiver AND m.type = :type ORDER BY m.createTime DESC")
    Page<Message> findByReceiverAndType(@Param("receiver") String receiver, @Param("type") String type, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.receiver = :receiver ORDER BY m.createTime DESC")
    Page<Message> findByReceiver(@Param("receiver") String receiver, Pageable pageable);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :receiver AND m.isRead = false")
    Long countUnreadByReceiver(@Param("receiver") String receiver);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :receiver AND m.isRead = false AND m.type = :type")
    Long countUnreadByType(@Param("receiver") String receiver, @Param("type") String type);
    
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.id = :messageId AND m.receiver = :receiver")
    int markAsRead(@Param("messageId") Long messageId, @Param("receiver") String receiver);
    
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver = :receiver AND m.type = :type")
    int markAllAsRead(@Param("receiver") String receiver, @Param("type") String type);
    
    @Modifying
    @Query("DELETE FROM Message m WHERE m.id = :messageId AND m.receiver = :receiver")
    int deleteByIdAndReceiver(@Param("messageId") Long messageId, @Param("receiver") String receiver);
} 