package com.xebialabs.jira.xlr.dto;


import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class Release {
    private String id;
    private String title;
    private String status;
    private ScriptUsername scriptUsername;
    private String scriptUserPassword;

    private Map<String, String> variableValues;

    public Release() {
    }

    public Release(final String id, final String title, final ScriptUsername scriptUsername, final String scriptUserPassword, final Map<String, String> variableValues) {
        this.id = id;
        this.title = title;
        this.scriptUsername = scriptUsername;
        this.scriptUserPassword = scriptUserPassword;
        this.variableValues = variableValues;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public Map<String, String> getVariableValues() {
        return variableValues;
    }

    public void setVariableValues(final Map<String, String> variableValues) {
        this.variableValues = variableValues;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getPrivateId(){
        return (id != null) ? id.replace("Applications/", "") : null;
    }

    public String getPublicId(String serverVersion) {

        if (serverVersion.substring(0,3).equals("4.6") || serverVersion.substring(0,3).equals("4.7") ) {
            return (id != null) ? id.replace("Applications/", "") : null;
        } else {
            return id;
        }

    }

    @Override
    public String toString() {
        return "Release{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Release that = (Release) o;

        if (title != null ? !title.equals(that.title) : that.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
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

    public void setScriptUserPassword(String scriptUserPassword) {
        this.scriptUserPassword = scriptUserPassword;
    }
}