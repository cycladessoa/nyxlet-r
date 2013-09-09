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
package org.cyclades.nyxlet.r.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.cyclades.engine.api.Nyxlet;
import org.cyclades.io.ResourceRequestUtils;

public class PropertyUtils {
    
    /**
     * Load one or more property files into a properties set returned as a Map<String, Object>
     * 
     * @param path  One or more paths to a properties file, comma separated. These can start with "http:".
     * @return      A Map<String, Object> representation of the loaded properties
     * @throws Exception
     */
    public static Map<String, Object> propertiesAsMap (String path) throws Exception {
        Map<String, Object> propertiesMap = new HashMap<String, Object>();
        Properties properties = new Properties();
        loadProperties(properties, path);
        for (Map.Entry<Object, Object> entry : properties.entrySet()) 
            propertiesMap.put(entry.getKey().toString(), entry.getValue());
        return propertiesMap;
    }
    
    /**
     * Load one or more properties files into given properties
     * 
     * @param properties    The properties to add to
     * @param path          One or more paths to a properties file, comma separated. These can start with "http:".
     * @throws Exception
     */
    public static void loadProperties (Properties properties, String path) throws Exception {
        loadProperties(properties, path.split("[,]"));
    }
    
    /**
     * Load one or more properties files into given properties
     * 
     * @param properties    The properties to add to
     * @param propPaths     Array of paths to property files. These can start with "http".
     * @throws Exception
     */
    public static void loadProperties (Properties properties, String[] propPaths) throws Exception {
        InputStream is = null;
        for (String propPath : propPaths) {
            try {
                is = ResourceRequestUtils.getInputStream(Nyxlet.getEngineContext().getCanonicalEngineDirectoryPath(propPath.trim()), 
                        null);
                properties.load(is);
                // XXX - This is flexible for linking properties files...however someone can introduce an infinite loop.
                if (properties.containsKey(SUPPLEMENTAL_PROPERTIES)) 
                    loadProperties(properties, (String)properties.remove(SUPPLEMENTAL_PROPERTIES));
                is.close();
            } finally {
                try { is.close(); } catch (Exception ignore ) { }
            }
        }
    }
    
    public static final String SUPPLEMENTAL_PROPERTIES  = "supplementalProperties";
    
}