package com.batchhawk.controller;

import com.batchhawk.common.CompleteRunRequest;
import com.batchhawk.common.NextJobResponse;
import com.batchhawk.service.WorkerJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/worker")
@RequiredArgsConstructor
public class InternalWorkerController {

    private final WorkerJobService workerJobService;

    @PostMapping("/jobs")
    public ResponseEntity<NextJobResponse> claimNextJob() {
        try {
            return workerJobService.claimNextJob()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.noContent().build());
        } catch (DataIntegrityViolationException e) {
            // Lost a concurrent claim race — treat as nothing available
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping("/runs/{runId}/complete")
    public ResponseEntity<Void> completeRun(
            @PathVariable final Long runId,
            @RequestBody final CompleteRunRequest request) {
        workerJobService.completeRun(runId, request);
        return ResponseEntity.ok().build();
    }
}
