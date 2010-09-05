/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * ProjectUtils
 *
 * @author fbricon
 */
public class ProjectUtils {

  /**
   * Transform an absolute path into a relative path to a project, if possible
   * @param project
   * @param absolutePath
   * @return
   */
  public static String getRelativePath(IProject project, String absolutePath){
    if(project != null && absolutePath != null) {
      IPath projectLocationPath = project.getLocation();
      if(projectLocationPath != null) {
        IPath path = new Path(absolutePath);
        return path.makeRelativeTo(projectLocationPath).toOSString();
      }
    }
    return absolutePath;
  }
}
