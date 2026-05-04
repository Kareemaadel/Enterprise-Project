package com.example.WorkHub.service;

import com.example.WorkHub.model.Project;
import com.example.WorkHub.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Project createProject(String name, String createdByEmail) {
        Project project = new Project();
        project.setName(name);
        project.setCreatedBy(createdByEmail);
        // get the current auth from security context
        // if its a tenant auth token, grab the tenant and set it on the project
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        if (projects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No projects were found");
        }
        return projects;
    }

    @Transactional(readOnly = true)
    public Project getProjectById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new com.example.WorkHub.exception.ResourceNotFoundException(Project.class, id));
    }
}
