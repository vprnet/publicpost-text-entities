package com.nearbyfyi.ner;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;

import edu.stanford.nlp.ie.crf.CRFClassifier;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.zip.GZIPInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import javax.ws.rs.ApplicationPath;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

/**
 * JAX-RS application for the Stanford NER (Named Entity Recognition) service.
 *
 * @author jh  2012.11.10
 */
@ApplicationPath("/")
public class NERApplication
        extends Application
    {
    // ----- Application methods --------------------------------------------

    @Override
    public Set<Class<?>> getClasses()
        {
        // register root resource
        Set<Class<?>> setClasses = new HashSet<Class<?>>();
        setClasses.add(NERResource.class);
        return setClasses;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the classifier with the specified name.
     *
     * @param sName  the name of the classifier to return; if null, the
     *               default classifier will be returned
     *
     * @return the classifier with the specified name, never null
     */
    public AbstractSequenceClassifier ensureClassifier(String sName)
        {
        if (sName == null)
            {
            sName = m_sDefaultClassifier;
            }

        AbstractSequenceClassifier classifier = m_mapClassifiers.get(sName);
        if (classifier == null)
            {
            throw new IllegalArgumentException("unknown classifier \"" + sName + '"');
            }
        return classifier;
        }

    /**
     * Return the ServletConfig for the hosting Servlet.
     *
     * @return the ServletConfig
     */
    public ServletConfig getServletConfig()
        {
        return s_cfg;
        }

    /**
     * Return the ServletConfig for the hosting Servlet.
     *
     * @return the ServletConfig, never null
     */
    public ServletConfig ensureServletConfig()
        {
        ServletConfig cfg = getServletConfig();
        if (cfg == null)
            {
            throw new IllegalStateException("ServletConfig not configured");
            }
        return cfg;
        }

    /**
     * Inject the ServletConfig for the hosting Servlet.
     *
     * @param cfg the ServletConfig
     */
    @Context
    public void setServletConfig(ServletConfig cfg)
            throws ServletException
        {
        if (cfg == null)
            {
            throw new IllegalArgumentException("null ServletConfig");
            }
        s_cfg = cfg;

        // check to see if the application was already configured
        if (s_app != null)
            {
            return;
            }

        ServletContext ctx = cfg.getServletContext();

        // configure the set of classifier names
        String[]    asClassifiers  = ensureInitParameter("ner-classifiers").split("\\s+");
        Set<String> setClassifiers = new HashSet<String>(asClassifiers.length);
        Collections.addAll(setClassifiers, asClassifiers);
        m_setClassifiers = setClassifiers;

        // configure default classifier name
        String sDefaultClassifier = ensureInitParameter("ner-default-classifier");
        if (!m_setClassifiers.contains(sDefaultClassifier))
            {
            throw new ServletException("unknown default classifier \""
                    + sDefaultClassifier + '"');
            }
        m_sDefaultClassifier = sDefaultClassifier;

        // configure the classifier map
        Map<String, AbstractSequenceClassifier> mapClassifiers =
                new HashMap<String, AbstractSequenceClassifier>(setClassifiers.size());
        for (String sName : setClassifiers)
            {
            String sFilename = ensureInitParameter(sName);

            // open the classifier file
            InputStream input = ctx.getResourceAsStream(sFilename);
            if (input == null)
                {
                throw new ServletException("no such classifier file \""
                        + sFilename + '"');
                }

            // create a new classifier
            try
                {
                if (sFilename.endsWith(".gz"))
                    {
                    input = new BufferedInputStream(new GZIPInputStream(input));
                    }
                else
                    {
                    input = new BufferedInputStream(input);
                    }

                mapClassifiers.put(sName, CRFClassifier.getClassifier(input));
                }
            catch (IOException e)
                {
                throw new ServletException("error reading classifier", e);
                }
            catch (ClassCastException e)
                {
                throw new ServletException("error creating classifier", e);
                }
            catch (ClassNotFoundException e)
                {
                throw new ServletException("missing classifier class", e);
                }
            finally
                {
                try
                    {
                    input.close();
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }
        m_mapClassifiers = mapClassifiers;

        // the application has been configured
        s_app = this;
        }

    /**
     * Return the singleton instance of this class.
     *
     * @return the singleton
     */
    public static NERApplication ensureInstance()
        {
        NERApplication app = s_app;
        if (app == null)
            {
            throw new IllegalStateException("application not configured");
            }
        return app;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the value of the specified Servlet initialization parameter.
     *
     * @param sName
     *
     * @return the value of the specified Servlet initialization parameter,
     *         never null or empty
     */
    protected String ensureInitParameter(String sName)
            throws ServletException
        {
        ServletConfig cfg = ensureServletConfig();

        // validate arguments
        if (sName == null)
            {
            throw new IllegalArgumentException(
                    "null initialization parameter name");
            }

        String sValue = cfg.getInitParameter(sName);
        if (sValue == null)
            {
            throw new ServletException(
                    "missing initialization parameter \"" + sName + '"');
            }

        sValue = sValue.trim();
        if (sValue.isEmpty())
            {
            throw new ServletException(
                    "empty initialization parameter: \"" + sName + '"');
            }

        return sValue;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The set of configured classifier names.
     */
    protected Set<String> m_setClassifiers;

    /**
     * The name of the default classifier.
     */
    protected String m_sDefaultClassifier;

    /**
     * The map of configured classifiers, keyed by name.
     */
    protected Map<String, AbstractSequenceClassifier> m_mapClassifiers;

    /**
     * The ServletConfig of the hosting Servlet.
     */
    protected static ServletConfig s_cfg;

    /**
     * The singleton instance of this class.
     */
    protected static volatile NERApplication s_app;
    }
