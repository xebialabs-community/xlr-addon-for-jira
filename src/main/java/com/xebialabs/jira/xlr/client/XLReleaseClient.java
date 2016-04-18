package com.xebialabs.jira.xlr.client;


import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.xebialabs.jira.xlr.dto.TemplateVariableV2;
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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class XLReleaseClient {

    private String user;
    private String password;
    private String serverUrl;

    public String getServerVersion() {
        return serverVersion;
    }

    private String serverVersion;

    public XLReleaseClient(String serverUrl, String username, String password) {
        this.user=username;
        this.password=password;
        this.serverUrl=serverUrl;
        this.serverVersion=determineServerVersion();
    }

    public String determineServerVersion() {

        String defaultVersion = "4.6";
        String foundVersion;
        WebResource service = newWebResource().path("server").path("info");

        ClientResponse response = service.accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
        if (response.getClientResponseStatus().getFamily() != SUCCESSFUL) {
            String errorReason = response.getEntity(String.class);
            return defaultVersion;
        }

        String xml = response.getEntity(String.class);
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = null;
        Document xmlDocument = null;
        try {
            builder = builderFactory.newDocumentBuilder();
            xmlDocument = builder.parse(new InputSource(new StringReader(xml)));
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            return xPath.compile("//version/text()").evaluate(xmlDocument);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return defaultVersion;
        }


    }



    public List<TemplateVariableV2> getVariablesV2(String templateId) {

        // Maintaining compatibility with previous versions of XLRelease
        WebResource service = null;

        service = newWebResource().path("api").path("v1").path("releases").path(templateId).path("variables");


        GenericType<List<TemplateVariableV2>> genericType = new GenericType<List<TemplateVariableV2>>() {};
        return service.accept(MediaType.APPLICATION_JSON).get(genericType);
    }

    public List<TemplateVariable> getVariables(String templateId) {

        // Maintaining compatibility with previous versions of XLRelease
        WebResource service = null;

        service = newWebResource().path("releases").path(templateId).path("updatable-variables");

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