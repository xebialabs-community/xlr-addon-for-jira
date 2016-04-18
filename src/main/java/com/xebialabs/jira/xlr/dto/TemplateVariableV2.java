package com.xebialabs.jira.xlr.dto;

/**
 * Created by vanstoner on 13/04/2016.
 */
public class TemplateVariableV2 extends TemplateVariable {

    private Boolean requiresValue;
    private Boolean showOnReleaseStart;

    public Boolean getRequiresValue() {
        return requiresValue;
    }

    public void setRequiresValue(final Boolean requiresValue) {
        this.requiresValue = requiresValue;
    }

    public Boolean getShowOnReleaseStart() {
        return showOnReleaseStart;
    }

    public void setShowOnReleaseStart(final Boolean showOnReleaseStart) {
        this.showOnReleaseStart = showOnReleaseStart;
    }
}
