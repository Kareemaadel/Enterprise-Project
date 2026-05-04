package com.example.WorkHub.controller;

import com.example.WorkHub.dto.ProjectCreateRequest;
import com.example.WorkHub.dto.TaskCreateRequest;
import com.example.WorkHub.dto.JobResponse;
import com.example.WorkHub.model.Job;
import com.example.WorkHub.model.Project;
import com.example.WorkHub.model.Task;
import com.example.WorkHub.service.ProjectService;
import com.example.WorkHub.service.ReportService;
import com.example.WorkHub.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final ReportService reportService;

    public ProjectController(ProjectService projectService,
                             TaskService taskService,
                             ReportService reportService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.reportService = reportService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public ResponseEntity<Project> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        Project project = projectService.createProject(request.name(), request.createdBy());
        return new ResponseEntity<>(project, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable UUID id) {
        Project project = projectService.getProjectById(id);
        return new ResponseEntity<>(project, HttpStatus.OK);
    }

    @PostMapping("/{id}/tasks")
    public ResponseEntity<Task> addTaskToProject(
            @PathVariable UUID id,
            @Valid @RequestBody TaskCreateRequest request) {

        Task task = taskService.createTask(id, request.title());
        return new ResponseEntity<>(task, HttpStatus.CREATED);
    }

    /**
     * POST /projects/{id}/generate-report
     * Enqueues an async report generation job via Kafka.
     * Returns the Job entity with PENDING status immediately.
     */
    @PostMapping("/{id}/generate-report")
    public ResponseEntity<Map<String, Object>> generateReport(@PathVariable UUID id) {
        Job job = reportService.enqueueReportGeneration(id);
        return new ResponseEntity<>(Map.of(
                "message", "Report generation enqueued",
                "jobId", job.getId(),
                "status", job.getStatus()
        ), HttpStatus.ACCEPTED);
    }

    /**
     * GET /projects/{id}/reports
     * Lists all report jobs for a project (allows checking completion status).
     */
    @GetMapping("/{id}/reports")
    public ResponseEntity<List<JobResponse>> getProjectReports(@PathVariable UUID id) {
        List<Job> reports = reportService.getReportsByProjectId(id);
        List<JobResponse> response = reports.stream()
                .map(j -> new JobResponse(
                        j.getId(),
                        j.getStatus(),
                        j.getProject() != null ? j.getProject().getId() : null,
                        j.getTenant() != null ? j.getTenant().getId() : null
                ))
                .toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
