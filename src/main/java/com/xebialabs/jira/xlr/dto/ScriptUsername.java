package com.xebialabs.jira.xlr.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScriptUsername {
    private String username;
    private String fullName;

    public ScriptUsername(){}

    public ScriptUsername(String username){
        this.username = username;
    }

    public ScriptUsername(String username, String fullName){
        this.username = username;
        this.fullName = fullName;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
