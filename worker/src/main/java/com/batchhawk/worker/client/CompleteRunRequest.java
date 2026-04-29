package com.batchhawk.worker.client;

import java.util.List;

public record CompleteRunRequest(
        String status,
        List<String> fieldsAttempted,
        List<String> fieldsFound,
        String feedbackNotes,
        String checkoutNotes
) {}
