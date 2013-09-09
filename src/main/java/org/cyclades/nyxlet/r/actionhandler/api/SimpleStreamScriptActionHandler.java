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
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamWriter;
import org.cyclades.engine.NyxletSession;
import org.cyclades.engine.nyxlet.templates.stroma.STROMANyxlet;
import org.cyclades.engine.stroma.STROMAResponseWriter;
import org.cyclades.engine.validator.ParameterMatches;
import org.cyclades.io.ResourceRequestUtils;
import org.cyclades.io.StreamUtils;
import org.math.R.Logger.Level;

/**
 * Simple ActionHandler that can be extended to implement different script engine execution strategies with the same input/output
 * pattern. This ActionHandler is *not* STROMA compatible and is intended to upload scripts via RESTful POST.
 */
public abstract class SimpleStreamScriptActionHandler extends SimpleScriptActionHandler {

    public SimpleStreamScriptActionHandler (STROMANyxlet parentNyxlet) throws Exception {
        super(parentNyxlet);
    }
    
    @Override
    public boolean ignoreSTROMAParameters () {
        return true;
    }
    
    @Override
    public void handle (NyxletSession nyxletSession, Map<String, List<String>> baseParameters, STROMAResponseWriter stromaResponseWriter) throws Exception {
        final String eLabel = "SimpleStreamScriptActionHandler.handle: ";
        boolean binaryResponse = false;
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
            Boolean haveScript = false;
            // Add URI source scripts
            if (baseParameters.containsKey(SCRIPT_URI_PARAMETER)) {
                for (String scriptURI : baseParameters.get(SCRIPT_URI_PARAMETER)) 
                    scriptList.add(new String(ResourceRequestUtils.getData(scriptURI, null), "UTF-8"));
                haveScript = true;
            }
            // Add InputStream source scripts
            if (nyxletSession.getInputStream() != null) {
                String script = new String(StreamUtils.toByteArray(nyxletSession.getInputStream()), "UTF-8");
                if (!script.trim().isEmpty()) {
                    scriptList.add(script);
                    haveScript = true;
                }
            }
            // Add URL payload source scripts
            if (baseParameters.containsKey(SCRIPT_PARAMETER)) {
                if (parameterAsBoolean(RUN_SCRIPT_FIRST_PARAMETER, baseParameters, false)) {
                    scriptList.addAll(0, baseParameters.get(SCRIPT_PARAMETER));
                } else {
                    scriptList.addAll(baseParameters.get(SCRIPT_PARAMETER));
                }
                haveScript = true;
            }
            if (!haveScript) throw new Exception("No input script found to execute.");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();            
            Object out = executeScripts(scriptList, baseParameters, scriptInputObject, new RsessionOutput(Level.ERROR, baos));
            if (parameterAsBoolean(BINARY_RESPONSE_PARAMETER, baseParameters, false)) {
                nyxletSession.getOutputStream().write((byte[])out);
                binaryResponse = true;
                return;
            }
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
            if (!binaryResponse) stromaResponseWriter.done();
        }
    }
    
    @Override
    public void init () throws Exception {
        initCore();
        if (getParentNyxlet().getExternalProperties().containsKey(PASSWORD)) {
            getFieldValidators().add(new ParameterMatches(
                    PASSWORD, getParentNyxlet().getExternalProperties().getProperty(PASSWORD)).showValues(false));
        }
    }
    
    public static final String BINARY_RESPONSE_PARAMETER = "binary-response";
    
}
