package com.xebialabs.jira.xlr.dto;

import java.util.List;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateReleaseView {

    private List<TemplateVariable> variables;
    private String templateId;
    private String title;
    private String dueDate;
    private String scheduledStartDate;

    public CreateReleaseView() {
    }

    public CreateReleaseView(final String templateId, final String title, final List<TemplateVariable> variables, final String dueDate, final String scheduledStartDate) {
        this.variables = variables;
        this.templateId = templateId;
        this.title = title;
        this.dueDate = dueDate;
        this.scheduledStartDate = scheduledStartDate;
    }

    public List<TemplateVariable> getVariables() {
        return variables;
    }

    public void setVariables(final List<TemplateVariable> variables) {
        this.variables = variables;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final String templateId) {
        this.templateId = templateId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getScheduledStartDate() {
        return scheduledStartDate;
    }

    public void setScheduledStartDate(String scheduledStartDate) {
        this.scheduledStartDate = scheduledStartDate;
    }
}