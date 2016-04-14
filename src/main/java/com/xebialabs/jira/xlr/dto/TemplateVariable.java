package com.xebialabs.jira.xlr.dto;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateVariable {

    private String key;
    private String value;

    public TemplateVariable() {
    }

    public TemplateVariable(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }


    public static Map<String, String> toMap(Collection<? extends TemplateVariable> variables) {
        Map<String, String> result = new HashMap<String, String>();
        for (TemplateVariable variable : variables) {
            result.put(variable.getKey(), variable.getValue());
        }
        return result;
    }

}