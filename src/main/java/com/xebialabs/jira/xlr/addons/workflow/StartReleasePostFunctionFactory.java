package com.xebialabs.jira.xlr.addons.workflow;

import java.util.HashMap;
import java.util.Map;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginFunctionFactory;
import com.atlassian.jira.workflow.WorkflowManager;
import com.google.common.base.Strings;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.FunctionDescriptor;

import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_PASSWORD_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_PASSWORD_FIELD_DEFAULT;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_PASSWORD_GLOBAL;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_PASSWORD_GLOBAL_DEFAULT;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_RELEASE_TITLE_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_RELEASE_TITLE_FIELD_DEFAULT;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_RELEASE_ID_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_RELEASE_ID_FIELD_DEFAULT;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_TEMPLATE_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_TEMPLATE_FIELD_DEFAULT;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_URL_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_URL_FIELD_DEFAULT;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_URL_GLOBAL;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_URL_GLOBAL_DEFAULT;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_USERNAME_GLOBAL;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_USERNAME_GLOBAL_DEFAULT;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_USER_NAME_FIELD;
import static com.xebialabs.jira.xlr.addons.workflow.FieldConstants.XLR_USER_NAME_FIELD_DEFAULT;

/**
 * This is the factory class responsible for dealing with the UI for the post-function.
 * This is typically where you put default values into the velocity context and where you store user input.
 */

public class StartReleasePostFunctionFactory extends AbstractWorkflowPluginFactory implements WorkflowPluginFunctionFactory
{

    private WorkflowManager workflowManager;

    public StartReleasePostFunctionFactory(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
    }

    @Override
    protected void getVelocityParamsForInput(Map<String, Object> velocityParams) {
        //the default message
        velocityParams.put(XLR_URL_GLOBAL, XLR_URL_GLOBAL_DEFAULT);
        velocityParams.put(XLR_USERNAME_GLOBAL, XLR_USERNAME_GLOBAL_DEFAULT);
        velocityParams.put(XLR_PASSWORD_GLOBAL, XLR_PASSWORD_GLOBAL_DEFAULT);
        velocityParams.put(XLR_URL_FIELD, XLR_URL_FIELD_DEFAULT);
        velocityParams.put(XLR_USER_NAME_FIELD, XLR_USER_NAME_FIELD_DEFAULT);
        velocityParams.put(XLR_PASSWORD_FIELD, XLR_PASSWORD_FIELD_DEFAULT);
        velocityParams.put(XLR_RELEASE_ID_FIELD, XLR_RELEASE_ID_FIELD_DEFAULT);
        velocityParams.put(XLR_TEMPLATE_FIELD, XLR_TEMPLATE_FIELD_DEFAULT);
        velocityParams.put(XLR_RELEASE_TITLE_FIELD, XLR_RELEASE_TITLE_FIELD_DEFAULT);
    }

    @Override
    protected void getVelocityParamsForEdit(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        getVelocityParamsForInput(velocityParams);
        getVelocityParamsForView(velocityParams, descriptor);
    }

    @Override
    protected void getVelocityParamsForView(Map<String, Object> velocityParams, AbstractDescriptor descriptor) {
        if (!(descriptor instanceof FunctionDescriptor)) {
            throw new IllegalArgumentException("Descriptor must be a FunctionDescriptor.");
        }

        FunctionDescriptor functionDescriptor = (FunctionDescriptor)descriptor;
        Map args = functionDescriptor.getArgs();
        extractParams(args, velocityParams);
    }

    @Override
    public Map<String,Object> getDescriptorParams(Map<String, Object> formParams) {
        Map<String, Object> params = new HashMap<String, Object>();
        extractParams(formParams, params);
        return params;
    }

    private void extractParams( Map<String, Object> source, Map<String, Object> target) {
        extractParam(XLR_URL_GLOBAL, "", source, target);
        extractParam(XLR_USERNAME_GLOBAL, "", source, target);
        extractParam(XLR_PASSWORD_GLOBAL, "", source, target);
        extractParam(XLR_URL_FIELD, XLR_URL_FIELD_DEFAULT, source, target);
        extractParam(XLR_USER_NAME_FIELD, XLR_USER_NAME_FIELD_DEFAULT, source, target);
        extractParam(XLR_PASSWORD_FIELD, XLR_PASSWORD_FIELD_DEFAULT, source, target);
        extractParam(XLR_RELEASE_ID_FIELD, XLR_RELEASE_ID_FIELD_DEFAULT, source, target);
        extractParam(XLR_TEMPLATE_FIELD, XLR_TEMPLATE_FIELD_DEFAULT, source, target);
        extractParam(XLR_RELEASE_TITLE_FIELD, XLR_RELEASE_TITLE_FIELD_DEFAULT, source, target);
    }

    private void extractParam(String name, String defaultValue, Map<String, Object> source, Map<String, Object> target) {
        Object argument = source.get(name);
        String value;
        if(argument instanceof String) {
            value = (String) source.get(name);
        } else {
            value = extractSingleParam(source, name);
        }

        if (Strings.isNullOrEmpty(value)) {
            value = defaultValue;
        }
        target.put(name, value);
    }
}