package com.batchhawk.controller;

import com.batchhawk.data.repository.AgentRunRepository;
import com.batchhawk.data.response.AgentRunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping(value = "/api/admin/runs", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AdminAgentRunController {

    private final AgentRunRepository agentRunRepository;

    @GetMapping
    public Page<AgentRunResponse> list(
        @PageableDefault(size = 25, sort = "startedAt", direction = Sort.Direction.DESC) final Pageable pageable
    ) {
        return agentRunRepository.findRecentRuns(pageable).map(AgentRunResponse::from);
    }
}
