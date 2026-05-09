package com.ainote.service;

import java.time.LocalDateTime;

public class DailyReport {
    private Long id;
    private String date;
    private String content;
    private String taskSummary;
    private Integer completedTaskCount;
    private Integer totalTaskCount;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTaskSummary() { return taskSummary; }
    public void setTaskSummary(String taskSummary) { this.taskSummary = taskSummary; }
    public Integer getCompletedTaskCount() { return completedTaskCount; }
    public void setCompletedTaskCount(Integer completedTaskCount) { this.completedTaskCount = completedTaskCount; }
    public Integer getTotalTaskCount() { return totalTaskCount; }
    public void setTotalTaskCount(Integer totalTaskCount) { this.totalTaskCount = totalTaskCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}