package com.example.WorkHub.service;

import com.example.WorkHub.model.Project;
import com.example.WorkHub.model.Task;
import com.example.WorkHub.model.TaskStatus;
import com.example.WorkHub.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;

    public TaskService(TaskRepository taskRepository, ProjectService projectService) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
    }

    // @Transactional
    // I wasnt sure whether this should be transactional since technically we only
    // perform one write operation and that will
    // rollback if the project is not found, and since no other operation occurred
    // that is enough.
    public Task createTask(UUID projectId, String title) {
        Project project = projectService.getProjectById(projectId);
        Task task = new Task();
        task.setTitle(title);
        task.setStatus(TaskStatus.P);
        task.setProject(project);

        return taskRepository.save(task);
    }

    // Here we had to use the Transactional annotation to invoke the optimistic
    // locking mechanism implemented in the Task table
    @Transactional
    public Task updateTaskStatus(UUID taskId, TaskStatus newStatus) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new com.example.WorkHub.exception.ResourceNotFoundException(Task.class, taskId));

        TaskStatus currentStatus = task.getStatus();

        if (currentStatus == newStatus) {
            throw new org.springframework.orm.ObjectOptimisticLockingFailureException(Task.class, taskId);
        }

        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid transition: cannot move from " + currentStatus + " to " + newStatus);
        }

        task.setStatus(newStatus);
        return taskRepository.save(task);
    }
}
