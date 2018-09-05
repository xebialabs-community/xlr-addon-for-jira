package com.xebialabs.jira.xlr.client;

import java.io.IOException;
import java.util.ArrayList;

import com.atlassian.jira.issue.customfields.option.Option;
import com.xebialabs.jira.xlr.dto.TemplateVariable;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Map JIRA custom field types to XL Release variable types.
 */
public class VariableSerializer extends JsonSerializer<TemplateVariable>
{
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
        // otherwise we'll just let jackson do it's own conversion
        else
        {
            jgen.writeObjectField("value", var.getValue());
        }

        jgen.writeEndObject();
    }
}