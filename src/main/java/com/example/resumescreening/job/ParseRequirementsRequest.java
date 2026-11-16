package com.example.resumescreening.job;

public class ParseRequirementsRequest {
    private String customText;

    public ParseRequirementsRequest() {}

    public ParseRequirementsRequest(String customText) {
        this.customText = customText;
    }

    public String getCustomText() { return customText; }
    public void setCustomText(String customText) { this.customText = customText; }
}
