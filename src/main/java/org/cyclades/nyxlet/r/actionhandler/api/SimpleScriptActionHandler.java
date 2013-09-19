/*******************************************************************************
 * Copyright (c) 2012, THE BOARD OF TRUSTEES OF THE LELAND STANFORD JUNIOR UNIVERSITY
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *    Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *    Neither the name of the STANFORD UNIVERSITY nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.cyclades.nyxlet.r.actionhandler.api;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.xml.stream.XMLStreamWriter;
import org.cyclades.engine.MetaTypeEnum;
import org.cyclades.engine.NyxletSession;
import org.cyclades.engine.nyxlet.templates.stroma.STROMANyxlet;
import org.cyclades.engine.nyxlet.templates.stroma.actionhandler.ActionHandler;
import org.cyclades.engine.stroma.STROMAResponseWriter;
import org.cyclades.engine.util.TransactionIdentifier;
import org.cyclades.engine.validator.OneOf;
import org.cyclades.engine.validator.ParameterHasValue;
import org.cyclades.engine.validator.ParameterMatches;
import org.cyclades.io.ResourceRequestUtils;
import org.cyclades.nyxlet.r.util.MetaType;
import org.cyclades.nyxlet.r.util.PropertyUtils;
import org.math.R.Logger.Level;
import org.math.R.Rsession;

/**
 * Simple ActionHandler that can be extended to implement different script engine execution strategies with the same input/output
 * pattern.
 */
public abstract class SimpleScriptActionHandler extends ActionHandler {

    public SimpleScriptActionHandler (STROMANyxlet parentNyxlet) throws Exception {
        super(parentNyxlet);
        tid = new TransactionIdentifier(null);
    }
    
    public Object executeScripts(List<String> scriptList, Map<String, List<String>> baseParameters, 
            Object scriptInputObject, RsessionOutput rOutput, String guid) throws Exception {
        Rsession s = null;
        try {
            Object returnObject = null;
            s = Rsession.newInstanceTry(rOutput/*System.out*/, null);
            if (staticSessionPropertiesMap != null) s.set(staticSessionPropertiesMap);
            if (scriptInputObject != null) s.set(INPUT_PARAMETER, scriptInputObject);
            s.set(OUTPUT_PARAMETER, "output variable is not set");
            s.set(GUID_VAR, guid);
            s.set(RESTFS_VAR, restfs);
            for (String script : scriptList) s.voidEval(script, false);    
            if (baseParameters.containsKey(OUTPUT_NATIVE_JAVA_PARAMETER)) {
                returnObject = s.eval(OUTPUT_PARAMETER).asNativeJavaObject();
            } else if (baseParameters.containsKey(OUTPUT_LIST_STRING_PARAMETER)) {
                returnObject =  toListString(s.eval(OUTPUT_PARAMETER).asNativeJavaObject());
            } else {
                returnObject = s.asString(OUTPUT_PARAMETER);
            }
            if (baseParameters.containsKey(VALIDATE_OUTPUT_META_TYPE_PARAMETER)) 
                if (!MetaType.validateAsMetaType(MetaTypeEnum.valueOf(baseParameters.get(VALIDATE_OUTPUT_META_TYPE_PARAMETER).get(0).toUpperCase()),
                        returnObject)) throw new Exception("Response did not parse correctly for given meta data format: " + 
                                baseParameters.get(VALIDATE_OUTPUT_META_TYPE_PARAMETER).get(0));
            return returnObject;
        } finally {
            try { s.end(); } catch (Exception e) {}
        }
    }
    
    protected String guid (Map<String, List<String>> baseParameters) {
        return (baseParameters.containsKey(GUID_VAR)) ? baseParameters.get(GUID_VAR).get(0) : tid.getTransactionID();
    }
    
    @SuppressWarnings("rawtypes")
    private String toListString (Object structure) throws Exception {
        if(structure instanceof Object[]) {
            return Arrays.deepToString((Object[])structure);
        } else if(structure instanceof double[]) {
            return Arrays.toString(((double[])structure));
        } else if(structure instanceof int[]) {
            return Arrays.toString(((int[])structure));
        } else if (structure instanceof Vector) {
            return Arrays.deepToString(((Vector)structure).toArray());
        } else {
            return structure.toString();
        }
    }
        
    @Override
    public void handle (NyxletSession nyxletSession, Map<String, List<String>> baseParameters, STROMAResponseWriter stromaResponseWriter) throws Exception {
        final String eLabel = "SimpleScriptActionHandler.handle: ";
        try {
            /********************************************************************/
            /*******                  START CODE BLOCK                    *******/
            /*******                                                      *******/
            /******* YOUR CODE GOES HERE...WITHIN THESE COMMENT BLOCKS.   *******/
            /******* MODIFYING ANYTHING OUTSIDE OF THESE BLOCKS WITHIN    *******/
            /******* THIS METHOD MAY EFFECT THE STROMA COMPATIBILITY      *******/
            /******* OF THIS ACTION HANDLER.                              *******/
            /********************************************************************/
            Object scriptInputObject = (nyxletSession.containsMapChannelKey(INPUT_PARAMETER)) ? 
                    nyxletSession.getMapChannelObject(INPUT_PARAMETER) : 
                        baseParameters.containsKey(INPUT_PARAMETER) ? baseParameters.get(INPUT_PARAMETER).toArray() : null;
            List<String> scriptList = new ArrayList<String>();
            if (baseParameters.containsKey(SCRIPT_URI_PARAMETER)) {
                for (String scriptURI : baseParameters.get(SCRIPT_URI_PARAMETER)) 
                    scriptList.add(new String(ResourceRequestUtils.getData(scriptURI, null), "UTF-8"));
            }
            if (baseParameters.containsKey(SCRIPT_PARAMETER)) {
                if (parameterAsBoolean(RUN_SCRIPT_FIRST_PARAMETER, baseParameters, false)) {
                    scriptList.addAll(0, baseParameters.get(SCRIPT_PARAMETER));
                } else {
                    scriptList.addAll(baseParameters.get(SCRIPT_PARAMETER));
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String guid = guid(baseParameters);
            if (!parameterAsBoolean(NO_GUID_OUT_PARAMETER, baseParameters, false)) 
                stromaResponseWriter.addResponseParameter(GUID_VAR, guid);
            Object out = executeScripts(scriptList, baseParameters, scriptInputObject, new RsessionOutput(Level.ERROR, baos), guid);
            if (parameterAsBoolean(R_LOG_OUTPUT_PARAMETER, baseParameters, false)) stromaResponseWriter.addResponseParameter(
                    R_LOG_OUTPUT_PARAMETER, new String(baos.toByteArray(), "UTF-8"));
            if (parameterAsBoolean(USE_MAP_CHANNEL_PARAMETER, baseParameters, false)) {
                nyxletSession.putMapChannelObject(OUTPUT_PARAMETER, out);
            } else {
                XMLStreamWriter streamWriter = stromaResponseWriter.getXMLStreamWriter();
                streamWriter.writeStartElement(OUTPUT_PARAMETER);
                streamWriter.writeCharacters((out == null) ? "null" : out.toString());
                streamWriter.writeEndElement();
            }
            /********************************************************************/
            /*******                  END CODE BLOCK                      *******/
            /********************************************************************/
        } catch (Exception e) {
            getParentNyxlet().logStackTrace(e);
            handleException(nyxletSession, stromaResponseWriter, eLabel, e);
        } finally {
            stromaResponseWriter.done();
        }
    }

    @Override
    public boolean isHealthy () throws Exception {
        final String eLabel = "SimpleScriptActionHandler.isHealthy: ";
        if (healthCheckCommand == null) return true;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Map<String, List<String>> dummyBaseParameters = new HashMap<String, List<String>>();
            Object returnObject = executeScripts(Arrays.asList(healthCheckCommand), new HashMap<String, List<String>>(), null, 
                    new RsessionOutput(Level.ERROR, baos), guid(dummyBaseParameters));
            if (returnObject.toString().indexOf(healthCheckValidationTerm) < 0) 
                throw new Exception(eLabel + "Term not found: [" + healthCheckValidationTerm + "] in [" + returnObject + "]");
            return true;
        } catch (Exception e) {
            getParentNyxlet().logError(new String(baos.toByteArray(), "UTF-8"), e);
            return false;
        }
    }

    @Override
    public void init () throws Exception {
        initCore();
        getFieldValidators()
        .add(new OneOf()
            .add(new ParameterHasValue(SCRIPT_PARAMETER))
            .add(new ParameterHasValue(SCRIPT_URI_PARAMETER)));
    }
    
    protected void initCore () throws Exception {
        if (getParentNyxlet().getExternalProperties().containsKey(PASSWORD)) {
            getFieldValidators().add(new ParameterMatches(
                    PASSWORD, getParentNyxlet().getExternalProperties().getProperty(PASSWORD)).showValues(false));
        }
        if (getParentNyxlet().getExternalProperties().containsKey(R_HOME_PROPERTY))
            System.setProperty("R_HOME", getParentNyxlet().getExternalProperties().getProperty(R_HOME_PROPERTY));
        restfs = getParentNyxlet().getExternalProperties().getPropertyOrError(RESTFS_VAR);
        rLogLevel = Level.valueOf(getParentNyxlet().getExternalProperties().getPropertyOrError(R_LOG_LEVEL_PROPERTY));
        healthCheckCommand = (getParentNyxlet().getExternalProperties().containsKey(HEALTH_CHECK_COMMAND_PROPERTY)) ? 
                getParentNyxlet().getExternalProperties().getProperty(HEALTH_CHECK_COMMAND_PROPERTY) : null;
        if (healthCheckCommand != null) healthCheckValidationTerm = 
                getParentNyxlet().getExternalProperties().getPropertyOrError(HEALTH_CHECK_VALIDATION_TERM_PROPERTY);
        staticSessionPropertiesMap = (getParentNyxlet().getExternalProperties().containsKey(STATIC_SESSION_PROPERTIES_PROPERTY)) ?
                PropertyUtils.propertiesAsMap(getParentNyxlet().getExternalProperties().getProperty(
                        STATIC_SESSION_PROPERTIES_PROPERTY)) : null;
    }

    @Override
    public void destroy () throws Exception {
        // your destruction code here, if any
    }
        
    public static final String SCRIPT_PARAMETER                         = "script";
    public static final String SCRIPT_URI_PARAMETER                     = "script-uri";
    public static final String RUN_SCRIPT_FIRST_PARAMETER               = "script-first";
    public static final String INPUT_PARAMETER                          = "input";
    public static final String OUTPUT_PARAMETER                         = "output";
    public static final String USE_MAP_CHANNEL_PARAMETER                = "use-map-channel";
    public static final String PASSWORD                                 = "password";
    public static final String OUTPUT_NATIVE_JAVA_PARAMETER             = "output-native-java";
    public static final String R_HOME_PROPERTY                          = "r_home";
    public static final String GUID_VAR                                 = "guid";
    public static final String NO_GUID_OUT_PARAMETER                    = "no-guid-out";
    public static final String RESTFS_VAR                               = "restfs";
    public static final String R_LOG_LEVEL_PROPERTY                     = "r_log_level";
    public static final String R_LOG_OUTPUT_PARAMETER                   = "r-log-out";
    public static final String HEALTH_CHECK_COMMAND_PROPERTY            = "health_check_command";
    public static final String HEALTH_CHECK_VALIDATION_TERM_PROPERTY    = "health_check_validation_term";
    public static final String STATIC_SESSION_PROPERTIES_PROPERTY       = "static_session_properties";
    public static final String OUTPUT_LIST_STRING_PARAMETER             = "output-list-string";
    public static final String VALIDATE_OUTPUT_META_TYPE_PARAMETER      = "validate-output-meta-type";
    
    protected TransactionIdentifier tid;
    protected String restfs;
    protected Level rLogLevel;
    protected String healthCheckCommand;
    protected String healthCheckValidationTerm;
    protected Map<String, Object> staticSessionPropertiesMap;
    
}
