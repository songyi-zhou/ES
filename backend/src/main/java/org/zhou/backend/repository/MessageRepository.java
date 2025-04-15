package org.zhou.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.zhou.backend.model.Message;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface MessageRepository extends JpaRepository<Message, Long> {
    
    Page<Message> findByType(String type, Pageable pageable);
    
    List<Message> findByTypeAndReadFalse(String type);
    
    List<Message> findByReadFalse();
    
    @Query("SELECT m.type, COUNT(m) FROM Message m WHERE m.read = false GROUP BY m.type")
    List<Object[]> countUnreadByType();
    
    default Map<String, Integer> getUnreadCountMap() {
        return countUnreadByType().stream()
            .collect(Collectors.toMap(
                arr -> (String) arr[0],
                arr -> ((Number) arr[1]).intValue()
            ));
    }
} 