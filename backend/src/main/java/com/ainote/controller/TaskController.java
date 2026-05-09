package com.ainote.controller;

import com.ainote.common.Result;
import com.ainote.entity.Task;
import com.ainote.service.TaskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Result<List<Task>> list(@RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return Result.success(taskService.listByStatus(status));
        }
        return Result.success(taskService.list());
    }

    @GetMapping("/{id}")
    public Result<Task> get(@PathVariable Long id) {
        return Result.success(taskService.get(id));
    }

    @PostMapping
    public Result<Task> create(@RequestBody Task task) {
        return Result.success(taskService.create(task));
    }

    @PutMapping("/{id}")
    public Result<Task> update(@PathVariable Long id, @RequestBody Task task) {
        return Result.success(taskService.update(id, task));
    }

    @PutMapping("/{id}/complete")
    public Result<Task> complete(@PathVariable Long id) {
        return Result.success(taskService.complete(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return Result.success();
    }
}