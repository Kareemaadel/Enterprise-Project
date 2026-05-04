package com.example.WorkHub.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final MeterRegistry meterRegistry;

    public ReportService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void triggerReportGeneration(String tenantId) {
        // Business logic for generating report would go here
        
        // Increment the custom metric
        Counter.builder("workhub.reports.generated")
                .tag("tenantId", tenantId)
                .description("Number of reports generated")
                .register(meterRegistry)
                .increment();
    }
}
