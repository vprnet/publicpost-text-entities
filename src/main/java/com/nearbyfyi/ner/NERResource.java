package com.nearbyfyi.ner;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.io.JsonStringEncoder;

/**
 * JAX-RS resource for the Stanford NER (Named Entity Recognition) service.
 *
 * @author jh  2012.11.10
 */
@Path("/classify")
public class NERResource
    {
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String classifyAsText(String sText)
        {
        if (sText == null || (sText = sText.trim()).length() == 0)
            {
            return "";
            }

        NERApplication app = NERApplication.ensureInstance();

        // obtain the default classifier
        AbstractSequenceClassifier classifier = app.ensureClassifier(null);

        // classify the text as inline XML
        sText = classifier.classifyToString(sText, "inlineXML", true);

        // extract the entities from the inline XML
        Map<String, Collection<String>> mapEntities = new HashMap<String, Collection<String>>();

        int      cch    = 0;
        String[] asText = sText.split("</[^>]+>");
        for (String sEntity : asText)
            {
            int iEntity = sEntity.indexOf("<");
            if (iEntity != -1)
                {
                sEntity = sEntity.substring(iEntity);
                iEntity = sEntity.indexOf('>');
                if (iEntity != -1)
                    {
                    String sType = sEntity.substring(1, iEntity);
                    if (!sType.isEmpty())
                        {
                        // encode type as CSV string
                        sType = escapeForCSV(sType.toLowerCase());

                        sEntity = sEntity.substring(iEntity + 1).trim();
                        if (!sEntity.isEmpty())
                            {
                            // encode entity as JSON string
                            sEntity = escapeForCSV(sEntity);

                            Collection<String> colEntities = mapEntities.get(sType);
                            if (colEntities == null)
                                {
                                colEntities = new ArrayList<String>();
                                mapEntities.put(sType, colEntities);
                                }
                            colEntities.add(sEntity);

                            cch += sType.length() + sEntity.length() + 7; // "...", "..."\n
                            }
                        }
                    }
                }
            }

        // construct the final CSV
        StringBuilder builder = new StringBuilder(cch);
        for (Map.Entry<String, Collection<String>> entry : mapEntities.entrySet())
            {
            String             sType       = entry.getKey();
            Collection<String> colEntities = entry.getValue();

            for (String sEntity : colEntities)
                {
                builder.append('"');
                builder.append(sType);
                builder.append("\", ");
                builder.append('"');
                builder.append(sEntity);
                builder.append('"');
                builder.append('\n');
                }
            }

        return builder.toString();
        }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public String classifyAsJSON(String sText)
        {
        if (sText == null || (sText = sText.trim()).length() == 0)
            {
            return "{\"entities\":{}}";
            }

        NERApplication    app     = NERApplication.ensureInstance();
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();

        // obtain the default classifier
        AbstractSequenceClassifier classifier = app.ensureClassifier(null);

        // classify the text as inline XML
        sText = classifier.classifyToString(sText, "inlineXML", true);

        // extract the entities from the inline XML
        Map<String, Collection<String>> mapEntities = new HashMap<String, Collection<String>>();

        int      cch    = 15; // {"entities":{}}
        String[] asText = sText.split("</[^>]+>");
        for (String sEntity : asText)
            {
            int iEntity = sEntity.indexOf("<");
            if (iEntity != -1)
                {
                sEntity = sEntity.substring(iEntity);
                iEntity = sEntity.indexOf('>');
                if (iEntity != -1)
                    {
                    String sType = sEntity.substring(1, iEntity);
                    if (!sType.isEmpty())
                        {
                        // encode type as JSON string
                        sType = String.valueOf(encoder.quoteAsString(sType.toLowerCase()));

                        sEntity = sEntity.substring(iEntity + 1).trim();
                        if (!sEntity.isEmpty())
                            {
                            // encode entity as JSON string
                            sEntity = String.valueOf(encoder.quoteAsString(sEntity));

                            Collection<String> colEntities = mapEntities.get(sType);
                            if (colEntities == null)
                                {
                                colEntities = new ArrayList<String>();
                                mapEntities.put(sType, colEntities);
                                cch += sType.length() + 3; // "...":
                                }

                            if (colEntities.size() == 1)
                                {
                                cch += 2; // [...]
                                }
                            if (colEntities.size() >= 1)
                                {
                                cch += 1; // comma between each entity
                                }
                            colEntities.add(sEntity);

                            cch += sEntity.length() + 2; // "..."
                            }
                        }
                    }
                }
            }

        // add space for a comma between each entity type
        cch += Math.max(0, mapEntities.size() - 1);

        // construct the final JSON
        StringBuilder builder = new StringBuilder(cch);
        builder.append("{\"entities\":{");
        for (Iterator<Map.Entry<String, Collection<String>>> iterTypes = mapEntities.entrySet().iterator(); ; )
            {
            Map.Entry<String, Collection<String>> entry = iterTypes.next();

            String             sType       = entry.getKey();
            Collection<String> colEntities = entry.getValue();

            builder.append('"');
            builder.append(sType);
            builder.append("\":");
            if (colEntities.size() > 1)
                {
                builder.append('[');
                }
            for (Iterator<String> iterEntities = colEntities.iterator(); ; )
                {
                builder.append('"');
                builder.append(iterEntities.next());
                builder.append('"');
                if (iterEntities.hasNext())
                    {
                    builder.append(',');
                    }
                else
                    {
                    break;
                    }
                }
            if (colEntities.size() > 1)
                {
                builder.append(']');
                }
            if (iterTypes.hasNext())
                {
                builder.append(',');
                }
            else
                {
                break;
                }
            }
        builder.append("}}");

        return builder.toString();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a version of the given string that is safe to use in a CSV file.
     *
     * @param sText  the string to analyze
     *
     * @return a CSV-safe version of the given string
     */
    protected String escapeForCSV(String sText)
        {
        return sText.replaceAll("\"", "\\\"");
        }
    }
