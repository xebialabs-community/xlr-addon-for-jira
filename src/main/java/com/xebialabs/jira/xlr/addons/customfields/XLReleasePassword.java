package com.xebialabs.jira.xlr.addons.customfields;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;

public class XLReleasePassword extends GenericTextCFType {

    public XLReleasePassword(CustomFieldValuePersister customFieldValuePersister, GenericConfigManager genericConfigManager) {
        super(customFieldValuePersister, genericConfigManager);
    }

}