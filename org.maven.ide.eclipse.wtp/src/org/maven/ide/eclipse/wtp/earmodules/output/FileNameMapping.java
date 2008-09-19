package org.maven.ide.eclipse.wtp.earmodules.output;

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
import org.eclipse.core.resources.IProject;
import org.eclipse.jst.j2ee.application.internal.operations.IModuleExtensions;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.maven.ide.eclipse.wtp.ArtifactHelper;

/**
 * Maps file name {@link Artifact}.
 * <p/>
 * TODO: it might be easier to use a token-based approach instead.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
@SuppressWarnings("restriction")
public abstract class FileNameMapping
{

    /**
     * Returns the file name of the specified artifact.
     *
     * @param a the artifact
     * @return the name of the file for the specified artifact
     */
    public abstract String mapFileName(final Artifact a);

    @SuppressWarnings("deprecation")
    protected String getProjectName(final Artifact a){
      IProject project =ArtifactHelper.getWorkspaceProject(a);
      if (project == null) {
        return null;
      }
      String name = project.getName().replace( ' ', '_');
      if (J2EEProjectUtilities.isDynamicWebProject(project)) {
        name += IModuleExtensions.DOT_WAR;
      } else if (J2EEProjectUtilities.isJCAProject(project)) {
        name += IModuleExtensions.DOT_RAR;
      } else {
        name += IModuleExtensions.DOT_JAR;
      }
     return name;
    }
}
