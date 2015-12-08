package com.xebialabs.jira.xlr.client;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;


import com.xebialabs.jira.xlr.dto.CreateReleaseView;
import com.xebialabs.jira.xlr.dto.Release;
import com.xebialabs.jira.xlr.dto.TemplateVariable;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class XLReleaseClient {

    private String user;
    private String password;
    private String serverUrl;

    public XLReleaseClient(String serverUrl, String username, String password) {
        this.user=username;
        this.password=password;
        this.serverUrl=serverUrl;
    }

    public List<TemplateVariable> getVariables(String templateId) {
        WebResource service = newWebResource().path("api").path("v1").path("releases").path(templateId).path("variables");
        GenericType<List<TemplateVariable>> genericType = new GenericType<List<TemplateVariable>>() {};
        return service.accept(MediaType.APPLICATION_JSON).get(genericType);
    }

    public Release findTemplateByTitle(String templateTitle) throws TemplateNotFoundException {
        WebResource service = newWebResource().path("api").path("v1").path("releases").path("byTitle")
                .queryParam("releaseTitle", templateTitle);
        GenericType<List<Release>> genericType = new GenericType<List<Release>>() {};
        List<Release> templateCandidates = service.accept(APPLICATION_JSON_TYPE).get(genericType);

        Release template = null;
        for (Release templateCandidate : templateCandidates) {
            if ("TEMPLATE".equals(templateCandidate.getStatus())) {
                if (template != null) {
                    throw new TemplateNotFoundException("Found more than 1 template that matches title '" +templateTitle+ "'");
                }
                template = templateCandidate;
            }
        }

        if (template == null) {
            throw new TemplateNotFoundException("Template with title '"+ templateTitle + "' not found");
        }

        return template;
    }

    public Release createRelease(final String templateId, final String releaseTitle, final List<TemplateVariable> variables) throws XLReleaseClientException {
        WebResource service = newWebResource();
        GenericType<Release> genericType = new GenericType<Release>() {};

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String scheduledStartDate = format.format(Calendar.getInstance().getTime());

        Calendar dueDate = Calendar.getInstance();
        dueDate.add(Calendar.DATE, 1);
        String scheduledDueDate = format.format(dueDate.getTime());
        CreateReleaseView createReleaseView = new CreateReleaseView(templateId, releaseTitle, variables, scheduledDueDate, scheduledStartDate);

        ClientResponse response = service.path("releases").type(MediaType.APPLICATION_JSON).post(ClientResponse.class, createReleaseView);
        if (response.getClientResponseStatus().getFamily() != SUCCESSFUL) {
            String errorReason = response.getEntity(String.class);
            throw new XLReleaseClientException(errorReason);
        }

        return response.getEntity(genericType);
    }

    public void startRelease(final String releaseId) throws XLReleaseClientException {
        Client client = newRestClient();
        WebResource service = client.resource(serverUrl).path("releases").path(releaseId).path("start");

        ClientResponse response = service.type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        if (response.getClientResponseStatus().getFamily() != SUCCESSFUL) {
            String errorReason = response.getEntity(String.class);
            throw new XLReleaseClientException(errorReason);
        }
    }

    private WebResource newWebResource() {
        Client client = newRestClient();
        WebResource service = client.resource(serverUrl);
        return service;
    }

    private Client newRestClient() {
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper((new ObjectMapperProvider()).getMapper());
        config.getSingletons().add(jacksonProvider);
        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        return client;
    }

}