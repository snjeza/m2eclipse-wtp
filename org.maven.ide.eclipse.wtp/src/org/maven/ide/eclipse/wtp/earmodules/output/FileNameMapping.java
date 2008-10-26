/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

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
import org.eclipse.jst.j2ee.application.internal.operations.IModuleExtensions;
import org.maven.ide.eclipse.wtp.ArtifactHelper;
import org.maven.ide.eclipse.wtp.JEEPackaging;


/**
 * Maps file name {@link Artifact}. <p/> TODO: it might be easier to use a token-based approach instead.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
@SuppressWarnings("restriction")
public abstract class FileNameMapping {

  /**
   * Returns the file name of the specified artifact.
   * 
   * @param a the artifact
   * @return the name of the file for the specified artifact
   */
  public abstract String mapFileName(final Artifact a);

  
  /**
   * Construct the exported project filename if the artifact is a workspace project. 
   * @param artifact
   * @return the project's exported filename using the pattern [artifactId]-[version].[extension] or 
   * null if the artifact is not a workspace project. 
   */
  protected String getProjectName(final Artifact artifact) {
    if(ArtifactHelper.getWorkspaceProject(artifact) == null) {
      return null;
    }
    StringBuilder name = new StringBuilder(artifact.getArtifactId()); //ArtifactIds contain no spaces, no need to .replace(' ', '_')
    name.append("-").append(artifact.getVersion());//MNGECLIPSE-967 add versions to project filenames
    JEEPackaging packaging = JEEPackaging.getValue(artifact.getType());
    switch(packaging) {
      case WAR:
        name.append(IModuleExtensions.DOT_WAR);
        break;
      case RAR:
        name.append(IModuleExtensions.DOT_RAR);
        break;
      default:
        name.append(IModuleExtensions.DOT_JAR);
    }
    return name.toString();
  }
}
