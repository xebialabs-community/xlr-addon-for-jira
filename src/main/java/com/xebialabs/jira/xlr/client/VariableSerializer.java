package com.xebialabs.jira.xlr.client;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;

import com.atlassian.jira.issue.customfields.option.Option;
import com.xebialabs.jira.xlr.dto.TemplateVariable;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map JIRA custom field types to XL Release variable types.
 */
public class VariableSerializer extends JsonSerializer<TemplateVariable>
{
    private static final Logger log = LoggerFactory.getLogger(VariableSerializer.class);

    @Override
    public void serialize(TemplateVariable var, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException 
    {
        jgen.writeStartObject();
        jgen.writeStringField("key", var.getKey());

        // deal with String things as they can undergo conversions
        // JIRA options are a bit weird but deep down, they're still Strings
        if ( var.getValue() instanceof Option || var.getValue() instanceof String )
        {
            String val = null;
            if ( var.getValue() instanceof Option )
            {
                val = ((Option) var.getValue()).getValue();
            }
            else
            {
                val = (String) var.getValue();
            }

            // now we convert to what XLR is expecting
            // boolean
            if ( var.getType().equals("xlrelease.BooleanVariable") )
            {
                if ( "yes|true".indexOf(val.toLowerCase()) >= 0 )
                {
                    jgen.writeBooleanField("value", true);
                }
                else
                {
                    jgen.writeBooleanField("value", false);
                }
            }
            else if ( var.getType().equals("xlrelease.MapStringStringVariable") )
            {
                // parse to get list of items; either cr/lf if JIRA multiline text box or at comma
                String delim = ( val.indexOf("\r\n") > -1 ? "\r\n" : "," );
                String[] vals = val.split(delim);

                // parse at = to separate key and value
                jgen.writeFieldName("value");
                jgen.writeStartObject();
                for ( String kv : vals )
                {
                    if ( kv.indexOf("=") == -1 )
                    {
                        throw new IllegalArgumentException(String.format("The value for customField '%s' is must be of the form 'key=value'.  Instead we see '%s'", var.getKey(), kv));
                    }

                    String[] pair = kv.split("=");
                    jgen.writeStringField(pair[0].trim(), pair[1].trim());
                }
                jgen.writeEndObject();
            }
            else if ( var.getType().equals("xlrelease.SetStringVariable") )
            {
                // parse to get list of items; either cr/lf if JIRA multiline text box or at comma
                String delim = ( val.indexOf("\r\n") > -1 ? "\r\n" : "," );
                String[] vals = val.split(delim);

                jgen.writeFieldName("value");
                jgen.writeStartArray();
                for ( String v : vals )
                {
                    jgen.writeString(v);
                }
                jgen.writeEndArray();
            }
            else if ( var.getType().equals("xlrelease.StringVariable") )
            {
                jgen.writeStringField("value", val);
            }
        }
        else if ( var.getValue() instanceof ArrayList )
        {
            jgen.writeFieldName("value");
            jgen.writeStartArray();
            for ( Object item : ((ArrayList<?>)var.getValue()) )
            {
                if ( item instanceof Option )
                {
                    jgen.writeString(((Option) item).getValue());
                }
                else
                {
                    System.out.println(String.format("[WARN:XLR Plugin] custom field '%s' has unserializable item '%s'.  Skipping.", var.getKey(), item.toString()));
                    jgen.writeString("skipped");
                }
            }
            jgen.writeEndArray();
        }
        else if ( "xlrelease.DateVariable".equals(var.getType()) )
        {
            // JIRA passes as Timestamp.  Convert to ISO
            if ( var.getValue() instanceof Timestamp )
            {
                Timestamp ts = (Timestamp) var.getValue();
                ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ts.getTime()), ZoneId.systemDefault());
                String tsiso = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                log.debug(String.format("[XLR] writing date variable '%s' as '%s'", var.getKey(), tsiso));
                jgen.writeObjectField("value", tsiso);
            }
            else
            {
                // write as is
                log.debug(String.format("[XLR] writing date variable '%s' as is", var.getKey()));
                jgen.writeObjectField("value", var.getValue());
            }
        }
        // otherwise we'll just let jackson do it's own conversion
        else
        {
            log.debug(String.format("[XLR] writing unconverted type '%s' for variable '%s'", var.getType(), var.getKey()));
            jgen.writeObjectField("value", var.getValue());
        }

        jgen.writeEndObject();
    }
}