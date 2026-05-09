package com.ainote.service;

import java.util.ArrayList;
import java.util.List;

public class PersonalChatReply {
    private String answer;
    private String contextSummary;
    private String model;
    private List<PersonalChatReference> references = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getContextSummary() {
        return contextSummary;
    }

    public void setContextSummary(String contextSummary) {
        this.contextSummary = contextSummary;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<PersonalChatReference> getReferences() {
        return references;
    }

    public void setReferences(List<PersonalChatReference> references) {
        this.references = references;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
