package com.xebialabs.jira.xlr.addons.workflow;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.sql.Timestamp;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.jira.project.version.Version;
import org.apache.commons.lang.StringUtils;
import com.atlassian.jira.user.DelegatingApplicationUser;
import com.atlassian.jira.issue.customfields.option.LazyLoadedOption;

import com.google.common.base.Strings;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;

import com.xebialabs.jira.xlr.client.TemplateNotFoundException;
import com.xebialabs.jira.xlr.client.XLReleaseClient;
import com.xebialabs.jira.xlr.client.XLReleaseClientException;
import com.xebialabs.jira.xlr.dto.Release;
import com.xebialabs.jira.xlr.dto.TemplateVariable;
import com.xebialabs.jira.xlr.dto.TemplateVariableV2;
import com.atlassian.jira.bc.project.component.ProjectComponent;

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
        String serverVersion = xlReleaseClient.getServerVersion();

        String releaseId = argsMapper.getReleaseId();
        if (releaseId != null) {
            writeComment(issue, "Release already created with id " + releaseId + ". Will ignore transition.");
            return;
        }

        String xlrTemplate = argsMapper.getReleaseTemplateName();
        
        
        if (xlrTemplate == null || "".equals(xlrTemplate) )
        {
            /*
            ** Code simply returns If it is unable to find a matching template name defined in XLRelease.
            ** This is done to byepass the mandatory check for Template name
            ** If a template name is not passed in Jira, Then XLRelease will not write back an error as 
            ** comment to the Jira issue
            */
            return;
        }

        Release releaseTemplate = xlReleaseClient.findTemplateByTitle(xlrTemplate);

        List variables;
		
		if (serverVersion.substring(0,3).equals("4.6") || serverVersion.substring(0,3).equals("4.7") ) {
            variables = xlReleaseClient.getVariables(releaseTemplate.getPublicId(serverVersion));
        } else {
            variables =  xlReleaseClient.getVariablesV2(releaseTemplate.getPublicId(serverVersion));
        }
        
        
      
        argsMapper.populateVariables(variables, serverVersion);

        String title = argsMapper.getOptionalCustomFieldValue(XLR_RELEASE_TITLE_FIELD);
        if (Strings.isNullOrEmpty(title)) {
            title = "Release Jira Issue " + issue.getKey();
        }

        Release release = xlReleaseClient.createRelease(releaseTemplate.getPrivateId(), title, variables);
        issue.setCustomFieldValue(argsMapper.getReleaseIdField(), release.getPrivateId());
        writeComment(issue, format("[%s|%s#/releases/%s] created.", title, argsMapper.getUrl(), release.getPrivateId()));
        xlReleaseClient.startRelease(release.getPrivateId());

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
        	if ( getCustomRequiredFieldValue(XLR_TEMPLATE_FIELD) == null || "".equals(getCustomRequiredFieldValue(XLR_TEMPLATE_FIELD)) )
        	{
        		return "";
        	}
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

        public void populateVariables(List<TemplateVariable> variables, String serverVersion) {

        	try{
            for (TemplateVariable variable : variables) {
                String key;

                if (serverVersion.substring(0,3).equals("4.6") || serverVersion.substring(0,3).equals("4.7") ) {
                    key = variable.getKey().substring(2, variable.getKey().length() - 1);
                } else {
                    key = variable.getKey();
                }

                if (key.equals("issue")) {
                    variable.setValue(issue.getKey());
                    }
                    else if (key.equals("components"))
                    {
                      if (this.issue.getComponentObjects() != null)
                      {
                        StringBuffer componentsCSV = new StringBuffer();
                        Collection<ProjectComponent> projectComponents = this.issue.getComponentObjects();
                        java.util.Iterator<ProjectComponent> compIter = projectComponents.iterator();
                        while (compIter.hasNext())
                        {
                          ProjectComponent pComp = (ProjectComponent)compIter.next();
                          componentsCSV.append(pComp.getName()).append(",");
                        }
                        variable.setValue(componentsCSV.toString().substring(0, componentsCSV.length() - 1));
                      }
                    }
                    else if (key.equals("duedate"))
                    {
                      if (this.issue.getDueDate() != null)
                      {
                        Timestamp dueDate = this.issue.getDueDate();
                        variable.setValue(dueDate.toString());
                      }
                    }
                    else if (key.equals("environment"))
                    {
                      if (this.issue.getEnvironment() != null) {
                        variable.setValue(this.issue.getEnvironment());
                      }
                    }
                    else if (key.equals("fixVersions"))
                    {
                      StringBuffer fixVersionCSV = new StringBuffer();
                      
                      Collection<Version> fixVersion = this.issue.getFixVersions();
                      Iterator<Version> fixVerIter = fixVersion.iterator();
                      while (fixVerIter.hasNext())
                      {
                        Version version = (Version)fixVerIter.next();
                        fixVersionCSV.append(version.getName()).append(",");
                      }
                      variable.setValue(fixVersionCSV.toString().substring(0, fixVersionCSV.length() - 1));
                    }
                    else if (this.issueCustomFields.containsKey(key))
                    {
                      try{
                    	  System.out.println("Key:" + key + " Value:" + this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)));
                      }catch(Exception ex)
                      {
                    	 ex.printStackTrace(); 
                      }
                      if ((this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)) instanceof ArrayList))
                      {
                        List<String> slist = (ArrayList)this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key));
                        variable.setValue(StringUtils.join(slist, ','));
                      }
                      else if ((this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)) instanceof String))
                      {
                        variable.setValue((String)this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)));
                      }
                      else if ((this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)) instanceof DelegatingApplicationUser))
                      {
                        DelegatingApplicationUser testManager = (DelegatingApplicationUser)this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key));
                        variable.setValue(testManager.getEmailAddress());
                      }
                      else if ((this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)) instanceof LazyLoadedOption))
                      {
                        LazyLoadedOption changeCategory = (LazyLoadedOption)this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key));
                        variable.setValue(changeCategory.getValue());
                      }
                      else if (this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)) != null)
                      {
                        System.out.println("Inside 3rd Else.." + this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)).getClass());
                        variable.setValue(this.issue.getCustomFieldValue((CustomField)this.issueCustomFields.get(key)).toString());
                      }
                    }
                }
            	}catch(Exception ex)
            	{
            		ex.printStackTrace();
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
			
            if (! name.equals(XLR_TEMPLATE_FIELD)) //Ignore mandatory value check for XLR Template field
            {
	            if (Strings.isNullOrEmpty(value)) {
	                String issueFieldName = resolveIssueFieldNameFromSettingsFieldName(name);
	                throw new IllegalArgumentException(format("Custom field '%s', referenced from post function argument %s, has empty value on issue %s.", issueFieldName, name, issue.getId()));
	            }
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