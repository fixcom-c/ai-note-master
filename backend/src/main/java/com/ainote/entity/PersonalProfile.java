package com.ainote.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "personal_profile")
public class PersonalProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "identity_summary", columnDefinition = "TEXT")
    private String identitySummary;

    @Column(name = "current_focus", columnDefinition = "TEXT")
    private String currentFocus;

    @Column(name = "long_term_goals", columnDefinition = "TEXT")
    private String longTermGoals;

    @Column(name = "work_style", columnDefinition = "TEXT")
    private String workStyle;

    @Column(name = "life_areas", columnDefinition = "TEXT")
    private String lifeAreas;

    @Column(name = "ai_preference", columnDefinition = "TEXT")
    private String aiPreference;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getIdentitySummary() { return identitySummary; }
    public void setIdentitySummary(String identitySummary) { this.identitySummary = identitySummary; }
    public String getCurrentFocus() { return currentFocus; }
    public void setCurrentFocus(String currentFocus) { this.currentFocus = currentFocus; }
    public String getLongTermGoals() { return longTermGoals; }
    public void setLongTermGoals(String longTermGoals) { this.longTermGoals = longTermGoals; }
    public String getWorkStyle() { return workStyle; }
    public void setWorkStyle(String workStyle) { this.workStyle = workStyle; }
    public String getLifeAreas() { return lifeAreas; }
    public void setLifeAreas(String lifeAreas) { this.lifeAreas = lifeAreas; }
    public String getAiPreference() { return aiPreference; }
    public void setAiPreference(String aiPreference) { this.aiPreference = aiPreference; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
