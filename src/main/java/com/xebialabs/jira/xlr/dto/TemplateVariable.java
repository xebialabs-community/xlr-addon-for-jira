package com.xebialabs.jira.xlr.dto;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateVariable 
{
    private String key;
    private String type;
    private Object value;

    private Boolean requiresValue;
    private Boolean showOnReleaseStart;

    public TemplateVariable() 
    {
    }

    public TemplateVariable(final String key, final String type, final String value) 
    {
        this.key = key;
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString()
    {
        return String.format("key:%s, type:%s, value_type:%s", this.key, this.type, this.valueType());
    }

    public String valueType()
    {
        return (this.value == null?"null":this.value.getClass().getCanonicalName());
    }

    public String getKey() 
    {
        return key;
    }

    public void setKey(final String key) 
    {
        this.key = key;
    }

    public String getType()
    {
        return this.type;
    }

    public void setType(final String type)
    {
        this.type = type;
    }

    public Object getValue() 
    {
        return value;
    }

    public void setValue(final Object value) 
    {
        this.value = value;
    }

    public Boolean getRequiresValue() 
    {
        return requiresValue;
    }

    public void setRequiresValue(final Boolean requiresValue) 
    {
        this.requiresValue = requiresValue;
    }

    public Boolean getShowOnReleaseStart() 
    {
        return showOnReleaseStart;
    }

    public void setShowOnReleaseStart(final Boolean showOnReleaseStart) 
    {
        this.showOnReleaseStart = showOnReleaseStart;
    }

    public static Map<String, Object> toMap(Collection<? extends TemplateVariable> variables) 
    {
        Map<String, Object> result = new HashMap<String, Object>();
        for (TemplateVariable variable : variables) {
            result.put(variable.getKey(), variable.getValue());
        }
        return result;
    }
}