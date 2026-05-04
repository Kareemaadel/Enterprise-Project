package com.example.WorkHub.controller;

import com.example.WorkHub.dto.ProjectCreateRequest;
import com.example.WorkHub.dto.TaskCreateRequest;
import com.example.WorkHub.model.Project;
import com.example.WorkHub.model.Task;
import com.example.WorkHub.service.ProjectService;
import com.example.WorkHub.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    public ProjectController(ProjectService projectService, TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
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
}
