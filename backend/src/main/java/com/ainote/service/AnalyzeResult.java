package com.ainote.service;

public class AnalyzeResult {
    private String summary;
    private java.util.List<TaskInfo> tasks;
    private java.util.List<String> knowledgeTags;
    private String reminderTime;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public java.util.List<TaskInfo> getTasks() { return tasks; }
    public void setTasks(java.util.List<TaskInfo> tasks) { this.tasks = tasks; }
    public java.util.List<String> getKnowledgeTags() { return knowledgeTags; }
    public void setKnowledgeTags(java.util.List<String> knowledgeTags) { this.knowledgeTags = knowledgeTags; }
    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public static class TaskInfo {
        private String title;
        private String description;
        private String priority;
        private String reminderTime;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getReminderTime() { return reminderTime; }
        public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }
    }
}