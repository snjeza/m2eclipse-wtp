/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.facets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;


/**
 * RAR uninstall delegate
 * 
 * @author Fred Bricon
 */
public class RarUninstallDelegate implements IDelegate {

  public void execute(IProject project, IProjectFacetVersion fv, Object config, IProgressMonitor monitor)
      throws CoreException {
    // do nothing
  }
}
