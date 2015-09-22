package com.xebialabs.jira.xlr.addons.workflow;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.google.common.base.Strings;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

import com.xebialabs.jira.xlr.client.TemplateNotFoundException;
import com.xebialabs.jira.xlr.client.XLReleaseClient;
import com.xebialabs.jira.xlr.client.XLReleaseClientException;
import com.xebialabs.jira.xlr.dto.Release;
import com.xebialabs.jira.xlr.dto.TemplateVariable;

import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_PASSWORD_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_PASSWORD_GLOBAL;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_RELEASE_TITLE_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_RELEASE_ID_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_TEMPLATE_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_URL_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_URL_GLOBAL;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_USERNAME_GLOBAL;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_USER_NAME_FIELD;
import static java.lang.String.format;

/**
 * This is the post-function class that gets executed at the end of the transition.
 * Any parameters that were saved in your factory class will be available in the transientVars Map.
 */
public class StartReleasePostFunction extends AbstractJiraFunctionProvider
{
    public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {
        MutableIssue issue = getIssue(transientVars);

        try {
            doExecute(args, issue);
        } catch (IllegalArgumentException e) {
            writeErrorAsComment(issue, "Start Release In XLR Post Function configuration error:\n" + e.getMessage());
        } catch (TemplateNotFoundException e) {
            writeErrorAsComment(issue, e.getMessage());
        } catch (XLReleaseClientException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("XLR runtime error occurred. Stack Trace :");
            e.printStackTrace(pw);
            pw.close();
            writeErrorAsComment(issue, sw.toString());
        }
    }

    private void writeErrorAsComment(Issue issue, String msg) {
        writeComment(issue, "{color:red}\n" + msg + "\n{color}");
    }

    private void writeComment(Issue issue, String msg) {
        CommentManager commentManager = ComponentAccessor.getCommentManager();
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getUser();
        commentManager.create(issue, user, msg, false);
    }

    private void doExecute(Map args, MutableIssue issue) throws XLReleaseClientException {
        IssueFieldMapper argsMapper = new IssueFieldMapper(args, issue);
        XLReleaseClient xlReleaseClient = initClient(argsMapper);

        String releaseId = argsMapper.getReleaseId();
        if (releaseId != null) {
            writeComment(issue, "Release already created with id " + releaseId + ". Will ignore transition.");
            return;
        }

        String xlrTemplate = argsMapper.getReleaseTemplateName();
        Release releaseTemplate = xlReleaseClient.findTemplateByTitle(xlrTemplate);
        List<TemplateVariable> variables = xlReleaseClient.getVariables(releaseTemplate.getPublicId());
        argsMapper.populateVariables(variables);

        String title = argsMapper.getOptionalCustomFieldValue(XLR_RELEASE_TITLE_FIELD);
        if (Strings.isNullOrEmpty(title)) {
            title = "Release Jira Issue " + issue.getKey();
        }

        Release release = xlReleaseClient.createRelease(releaseTemplate.getPublicId(), title, variables);
        issue.setCustomFieldValue(argsMapper.getReleaseIdField(), release.getPublicId());
        writeComment(issue, format("[%s|%s#/releases/%s] created.", title, argsMapper.getUrl(), release.getPublicId()));
        xlReleaseClient.startRelease(release.getPublicId());

    }

    private XLReleaseClient initClient(IssueFieldMapper argsMapper) {
        return new XLReleaseClient(argsMapper.getUrl(), argsMapper.getUsername(), argsMapper.getPassword());
    }


    private static class IssueFieldMapper {

        private final Map settings;
        private Issue issue;
        Map<String, CustomField> issueCustomFields;

        IssueFieldMapper(Map settings, Issue issue) {
            this.settings = settings;
            this.issue = issue;
            this.issueCustomFields = getCustomFieldsForIssueIndexedByFieldName(issue);
        }

        public String getReleaseTemplateName() {
            return getCustomRequiredFieldValue(XLR_TEMPLATE_FIELD);
        }

        public CustomField getReleaseIdField() {
            return getCustomField(XLR_RELEASE_ID_FIELD);
        }

        public String getReleaseId() {
            return getCustomFieldValue(XLR_RELEASE_ID_FIELD);
        }

        public String getUrl() {
            return resolveValueFromCustomIssueFieldOrGlobalSetting(XLR_URL_FIELD, XLR_URL_GLOBAL, "Url");
        }

        public String getPassword() {
            return resolveValueFromCustomIssueFieldOrGlobalSetting(XLR_PASSWORD_FIELD, XLR_PASSWORD_GLOBAL, "Password");
        }

        public String getUsername() {
            return resolveValueFromCustomIssueFieldOrGlobalSetting(XLR_USER_NAME_FIELD, XLR_USERNAME_GLOBAL, "Username");
        }

        public void populateVariables(List<TemplateVariable> variables) {
            for (TemplateVariable variable : variables) {
                String key = variable.getKey().substring(2, variable.getKey().length() - 1);
                if (key.equals("issue")) {
                    variable.setValue(issue.getKey());
                } else if (issueCustomFields.containsKey(key)) {
                    variable.setValue((String) issue.getCustomFieldValue(issueCustomFields.get(key)));
                }
            }
        }

        private String resolveValueFromCustomIssueFieldOrGlobalSetting(String settingsField, String globalSettingsField, String settingName) {
            String value = getOptionalCustomFieldValue(settingsField);
            if (Strings.isNullOrEmpty(value)) {
                value = (String) settings.get(globalSettingsField);
                if (Strings.isNullOrEmpty(value)) {
                    throw new IllegalArgumentException(format("%s is not defined on issue nor is it defined in post function arguments as a global setting.", settingName));
                }
            }
            return value;
        }

        private String getCustomRequiredFieldValue(String name) {
            String value = getCustomFieldValue(name);
            if (Strings.isNullOrEmpty(value)) {
                String issueFieldName = resolveIssueFieldNameFromSettingsFieldName(name);
                throw new IllegalArgumentException(format("Custom field '%s', referenced from post function argument %s, has empty value on issue %s.", issueFieldName, name, issue.getId()));
            }
            return value;
        }

        private String getOptionalCustomFieldValue(String name) {
            CustomField issueField = getOptionalCustomField(name);
            if (issueField == null) {
                return null;
            }
            return (String)issue.getCustomFieldValue(issueField);
        }

        private String getCustomFieldValue(String name) {
            CustomField issueField = getCustomField(name);
            return (String)issue.getCustomFieldValue(issueField);
        }

        private CustomField getCustomField(final String name) {
            CustomField issueField = getOptionalCustomField(name);
            if (issueField == null) {
                String issueFieldName = resolveIssueFieldNameFromSettingsFieldName(name);
                throw new IllegalArgumentException(format("Custom field '%s', referenced from post function argument %s, is not defined on issue.", issueFieldName, name));
            }
            return issueField;
        }

        private CustomField getOptionalCustomField(final String name) {
            String issueFieldName = resolveIssueFieldNameFromSettingsFieldName(name);
            return issueCustomFields.get(issueFieldName);
        }

        private String resolveIssueFieldNameFromSettingsFieldName(String settingsFieldName) {
            String fieldName = (String) settings.get(settingsFieldName);
            if (Strings.isNullOrEmpty(fieldName)) {
                throw new IllegalArgumentException(format("Argument '%s' is not defined.", settingsFieldName));
            }
            return fieldName;
        }

        private Map<String, CustomField> getCustomFieldsForIssueIndexedByFieldName(Issue issue) {
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
            List<CustomField> customFieldObjects = customFieldManager.getCustomFieldObjects(issue);
            Map<String, CustomField> index = new HashMap<String, CustomField>(customFieldObjects.size());
            for (CustomField customFieldObject : customFieldObjects) {
                index.put(customFieldObject.getFieldName(), customFieldObject);
            }
            return index;
        }
    }
}