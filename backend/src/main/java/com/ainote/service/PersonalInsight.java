package com.ainote.service;

import java.util.ArrayList;
import java.util.List;

public class PersonalInsight {
    private String headline;
    private String understanding;
    private String rhythm;
    private int todayPlanned;
    private int todayCompleted;
    private int pendingTasks;
    private List<String> suggestions = new ArrayList<>();
    private List<String> focusTags = new ArrayList<>();

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getUnderstanding() { return understanding; }
    public void setUnderstanding(String understanding) { this.understanding = understanding; }
    public String getRhythm() { return rhythm; }
    public void setRhythm(String rhythm) { this.rhythm = rhythm; }
    public int getTodayPlanned() { return todayPlanned; }
    public void setTodayPlanned(int todayPlanned) { this.todayPlanned = todayPlanned; }
    public int getTodayCompleted() { return todayCompleted; }
    public void setTodayCompleted(int todayCompleted) { this.todayCompleted = todayCompleted; }
    public int getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(int pendingTasks) { this.pendingTasks = pendingTasks; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public List<String> getFocusTags() { return focusTags; }
    public void setFocusTags(List<String> focusTags) { this.focusTags = focusTags; }
}
