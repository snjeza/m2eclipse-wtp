/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.project.IMavenProjectFacade;

/**
 * 
 * Helper for Maven artifacts
 *
 * @author Fred Bricon
 */
//XXX Should probably be refactored to another Maven helper class.
@SuppressWarnings("restriction")
public class ArtifactHelper {

  private static String M2_REPO_PREFIX = VirtualArchiveComponent.VARARCHIVETYPE + IPath.SEPARATOR
  + BuildPathManager.M2_REPO + IPath.SEPARATOR;

  /**
   * Returns an artifact's path relative to the local repository
   */
  //XXX Does maven API provide that kind of feature? 
  public static IPath getLocalRepoRelativePath(Artifact artifact) {
    if (artifact == null) {
      throw new IllegalArgumentException("artifact must not be null");
    }
    /*FIXME the following is broken, as the workspaceEmbedder is cached at the MavenPlugin start during ITs, the wrong embedder is used for local repo determination. 
    IPath m2repo = JavaCore.getClasspathVariable(BuildPathManager.M2_REPO); //always set
    IPath absolutePath = new Path(artifact.getFile().getAbsolutePath());
    IPath relativePath = absolutePath.removeFirstSegments(m2repo.segmentCount()).makeRelative().setDevice(null);
    return relativePath;
    */

    //MNGECLIPSE-1045 : patch from jerr, use artifact.getBaseVersion() to handle timestamped snapshots
    String prefix = artifact.getGroupId().replace('.', IPath.SEPARATOR) + IPath.SEPARATOR + artifact.getArtifactId() + IPath.SEPARATOR + artifact.getBaseVersion();
    String fullPath = artifact.getFile().getAbsolutePath().replace('\\', IPath.SEPARATOR); //relocated artifact expose their new jar location
    return new Path(fullPath.substring(fullPath.lastIndexOf(prefix)));    //Should work for stupid repo locations like c:\junit\junit\3.8.1\localrepo\junit\junit\3.8.1\junit-3.8.1.jar
  }
  
  /**
   * Returns an IProject from a maven artifact
   * @param artifact
   * @return the project artifact or null
   */
  public static IProject getWorkspaceProject(Artifact artifact) {
    IMavenProjectFacade workspaceProject = MavenPlugin.getDefault().getMavenProjectManager()
    .getMavenProject(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());

    if(workspaceProject != null && workspaceProject.getFullPath(artifact.getFile()) != null) {
      return workspaceProject.getProject();
    }
    return null;
  }

  /**
   * Returns the M2_REPO variable path for an artifact. ex : var/M2_REPO/groupid/artifactid/version/filename
   * @param artifact
   * @return the M2_REPO variable path for the artifact, null if the artifact is a workspace project
   */
  public static String getM2REPOVarPath(Artifact artifact)
  {
    if (getWorkspaceProject(artifact) != null)
    {
     return null; 
    }
    return M2_REPO_PREFIX + ArtifactHelper.getLocalRepoRelativePath(artifact).toPortableString();
  }

  
}
