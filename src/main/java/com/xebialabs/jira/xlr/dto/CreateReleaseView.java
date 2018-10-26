package com.xebialabs.jira.xlr.dto;

import java.util.List;
import java.util.Set;

import com.xebialabs.jira.xlr.client.VariableSerializer;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateReleaseView {

    private List<TemplateVariable> variables;
    private List<String> tags;
    private String templateId;
    private String title;
    private String dueDate;
    private String scheduledStartDate;
    private ScriptUsername scriptUsername;
    private String scriptUserPassword;

    public CreateReleaseView() {
    }

    public CreateReleaseView(final String templateId, final String title, final List<TemplateVariable> variables, final List<String> tags, final String dueDate, final String scheduledStartDate, final ScriptUsername scriptUsername, String scriptUserPassword) {
        this.variables = variables;
        this.tags = tags;
        this.templateId = templateId;
        this.title = title;
        this.dueDate = dueDate;
        this.scheduledStartDate = scheduledStartDate;
        this.scriptUsername = scriptUsername;
        this.scriptUserPassword = scriptUserPassword;
    }

    @JsonSerialize(contentUsing=VariableSerializer.class)
    public List<TemplateVariable> getVariables() {
        return variables;
    }

    public void setVariables(final List<TemplateVariable> variables) {
        this.variables = variables;
    }

    public List<String> getTags() {
        return this.tags;
    }
    public void setTags(List<String> tags) {
        this.tags = tags;
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

    public ScriptUsername getScriptUsername() {
        return scriptUsername;
    }

    public void setScriptUsername(ScriptUsername scriptUsername) {
        this.scriptUsername = scriptUsername;
    }

    public String getScriptUserPassword() {
        return scriptUserPassword;
    }
}