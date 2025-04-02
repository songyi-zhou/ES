package org.zhou.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhou.backend.model.response.ApiResponse;
import org.zhou.backend.repository.SquadGroupLeaderRepository;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SquadGroupLeaderController {
    
    private final SquadGroupLeaderRepository squadGroupLeaderRepository;

    @GetMapping("/squad-group-leaders")
    public ResponseEntity<ApiResponse> getAllLeaders() {
        return ResponseEntity.ok(
            ApiResponse.success(squadGroupLeaderRepository.findAll())
        );
    }
} 