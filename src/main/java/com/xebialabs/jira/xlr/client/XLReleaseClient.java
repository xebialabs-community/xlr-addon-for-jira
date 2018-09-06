package com.xebialabs.jira.xlr.client;


import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.core.MediaType;

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
import com.xebialabs.jira.xlr.dto.ScriptUsername;
import com.xebialabs.jira.xlr.dto.TemplateVariable;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XLReleaseClient {

    private String user;
    private String password;
    private String serverUrl;

    private static final Logger log = LoggerFactory.getLogger(XLReleaseClient.class);

    public XLReleaseClient(String serverUrl, String username, String password) {
        this.user=username;
        this.password=password;
        this.serverUrl=serverUrl;
    }

    public Release findTemplateByTitle(String templateTitle) throws TemplateNotFoundException 
    {
        log.info(String.format("[XLR] findTemplateByTitle '%s'", templateTitle));
        
        WebResource service = newWebResource().path("api").path("v1").path("releases").path("byTitle").queryParam("releaseTitle", templateTitle);
        GenericType<List<Release>> genericType = new GenericType<List<Release>>() {};
        List<Release> templateCandidates = service.accept(APPLICATION_JSON_TYPE).get(genericType);

        Release template = null;
        for (Release templateCandidate : templateCandidates) 
        {
            if ("TEMPLATE".equals(templateCandidate.getStatus())) 
            {
                if ( template != null ) 
                {
                    log.error(String.format("[XLR] findTemplateByTitle '%s' not unique", templateTitle));
                    throw new TemplateNotFoundException("Found more than 1 template that matches title '" +templateTitle+ "'");
                }
                template = templateCandidate;
            }
        }

        if (template == null) 
        {
            log.error(String.format("[XLR] findTemplateByTitle '%s' not found", templateTitle));
            throw new TemplateNotFoundException("Template with title '"+ templateTitle + "' not found");
        }

        return template;
    }

    public List<TemplateVariable> getVariables(String templateId) throws XLReleaseClientException 
    {
        log.info(String.format("[XLR] getVariables '%s'", templateId));

        // Maintaining compatibility with previous versions of XLRelease
        WebResource service = newWebResource().path("api").path("v1").path("releases").path(templateId).path("variables");

        GenericType<List<TemplateVariable>> genericType = new GenericType<List<TemplateVariable>>() {};

        try
        {
            return service.accept(MediaType.APPLICATION_JSON).get(genericType);
        } 
        catch(Exception ex)
        {
            log.error(String.format("[XLR] getVariables failed '%s'", ex.getMessage()));
            throw new XLReleaseClientException("Unable to retrieve template variables", ex);
        }
    }

    public Release createRelease(final String templateId, final String releaseTitle, final List<TemplateVariable> variables, final List<String> tags, final ScriptUsername scriptUsername, final String scriptUserPassword) throws XLReleaseClientException 
    {
        WebResource service = newWebResource();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String scheduledStartDate = format.format(Calendar.getInstance().getTime());

        Calendar dueDate = Calendar.getInstance();
        dueDate.add(Calendar.DATE, 1);
        String scheduledDueDate = format.format(dueDate.getTime());

        CreateReleaseView createReleaseView = new CreateReleaseView(templateId, releaseTitle, variables, tags, scheduledDueDate, scheduledStartDate, scriptUsername, scriptUserPassword);

        log.info("[XLR] createRelease request");
        ClientResponse response = service.path("releases").type(MediaType.APPLICATION_JSON).post(ClientResponse.class, createReleaseView);
        log.info(String.format("[XLR] createRelease response : '%s' status [%d]", response.getClientResponseStatus().name(), Integer.valueOf(response.getStatus())));

        if (response.getClientResponseStatus().getFamily() != SUCCESSFUL) 
        {
            String errorReason = response.getEntity(String.class);
            log.error(String.format("[XLR] createRelease ERROR: %s", errorReason));
            throw new XLReleaseClientException(errorReason);
        }

        GenericType<Release> genericType = new GenericType<Release>() {};
        Release release = response.getEntity(genericType);
        log.debug(String.format("[XLR] createRelease result '%s'", release.toString()));
        return release;
    }

    public void startRelease(final String releaseId) throws XLReleaseClientException {
        WebResource service = newWebResource().path("releases").path(releaseId).path("start");

        log.info("[XLR] startRelease request");
        ClientResponse response = service.type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
        log.info(String.format("[XLR] startRelease response : '%s' status [%d]", response.getClientResponseStatus().name(), Integer.valueOf(response.getStatus())));

        if (response.getClientResponseStatus().getFamily() != SUCCESSFUL) {
            String errorReason = response.getEntity(String.class);
            log.error(String.format("[XLR] startRelease ERROR: %s", errorReason));
            throw new XLReleaseClientException(errorReason);
        }
    }

    private WebResource newWebResource() 
    {
        log.info(String.format("[XLR] newWebResource for '%s'", serverUrl));

        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper((new ObjectMapperProvider()).getMapper());
        
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        config.getSingletons().add(jacksonProvider);

        Client client = Client.create(config);
        // Useful for debugging: client.addFilter( new LoggingFilter(System.out) );
        client.addFilter( new HTTPBasicAuthFilter(user, password) );

        WebResource service = client.resource(serverUrl);
        return service;
    }
}