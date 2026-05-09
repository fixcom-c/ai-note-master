package com.ainote.service;

import com.ainote.common.BusinessException;
import com.ainote.entity.Task;
import com.ainote.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserContextService userContextService;

    public TaskService(TaskRepository taskRepository, UserContextService userContextService) {
        this.taskRepository = taskRepository;
        this.userContextService = userContextService;
    }

    public List<Task> list() {
        Long userId = userContextService.getCurrentUserId();
        return taskRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
    }

    public List<Task> listByStatus(String status) {
        Long userId = userContextService.getCurrentUserId();
        return taskRepository.findByUserIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(userId, status);
    }

    public Task get(Long id) {
        Long userId = userContextService.getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权限访问此任务");
        }
        return task;
    }

    @Transactional
    public Task create(Task task) {
        validateTask(task);
        Long userId = userContextService.getCurrentUserId();
        task.setUserId(userId);
        task.setStatus("pending");
        task.setTitle(task.getTitle().trim());
        if (task.getPriority() == null || task.getPriority().isBlank()) {
            task.setPriority("medium");
        }
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(Long id, Task taskUpdate) {
        validateTask(taskUpdate);
        Long userId = userContextService.getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权限修改此任务");
        }
        task.setTitle(taskUpdate.getTitle().trim());
        task.setDescription(taskUpdate.getDescription());
        task.setPriority(taskUpdate.getPriority() == null || taskUpdate.getPriority().isBlank() ? "medium" : taskUpdate.getPriority());
        task.setReminderTime(taskUpdate.getReminderTime());
        return taskRepository.save(task);
    }

    @Transactional
    public Task complete(Long id) {
        Long userId = userContextService.getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作此任务");
        }
        task.setStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Transactional
    public void delete(Long id) {
        Long userId = userContextService.getCurrentUserId();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("任务不存在"));
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException("无权限删除此任务");
        }
        task.setIsDeleted(true);
        taskRepository.save(task);
    }

    public List<Task> getTodayTasks() {
        Long userId = userContextService.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        return taskRepository.findByUserIdAndCreatedAtBetweenAndIsDeletedFalse(userId, startOfDay, endOfDay);
    }

    private void validateTask(Task task) {
        if (task == null || task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new BusinessException("任务标题不能为空");
        }
    }
}
