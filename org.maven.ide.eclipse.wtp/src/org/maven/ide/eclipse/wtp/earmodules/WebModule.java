package org.maven.ide.eclipse.wtp.earmodules;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;

/**
 * The {@link EarModule} implementation for a Web application module.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public class WebModule
    extends AbstractEarModule
{
    protected static final String WEB_MODULE = "web";

    protected static final String WEB_URI_FIELD = "web-uri";

    protected static final String CONTEXT_ROOT_FIELD = "context-root";

    private String contextRoot;

    public WebModule( Artifact a, String bundleFileName )
    {
        super( a, bundleFileName );
    }

    /**
     * Returns the context root to use for the web module.
     * <p/>
     * Note that this might return <tt>null</tt> till the
     * artifact has been resolved.
     *
     * @return the context root
     */
    public String getContextRoot()
    {
        // Context root has not been customized - using default
        if ( contextRoot == null )
        {
            contextRoot = getDefaultContextRoot( getArtifact() );
        }
        return contextRoot;
    }

    public String getType()
    {
        return "war";
    }

    /**
     * Generates a default context root for the given artifact, based
     * on the <tt>artifactId</tt>.
     *
     * @param a the artifact
     * @return a context root for the artifact
     */
    private static String getDefaultContextRoot( Artifact a )
    {
        if ( a == null )
        {
            throw new NullPointerException( "Artifact could not be null." );
        }
        return "/" + a.getArtifactId();
    }

    public void setContextRoot(String contextRoot) {
      this.contextRoot = contextRoot;
    }
}
