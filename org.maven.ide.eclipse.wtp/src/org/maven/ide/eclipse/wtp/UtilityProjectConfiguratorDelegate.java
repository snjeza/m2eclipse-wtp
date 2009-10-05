/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;


/**
 * Utility Project Configurator Delegate.  
 * @author Fred Bricon
 */
class UtilityProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    // Only (re)configure utility projects 
    if(facetedProject.hasProjectFacet(WTPProjectsUtil.UTILITY_FACET)) {
      Set<Action> actions = new LinkedHashSet<Action>();
      installJavaFacet(actions, project, facetedProject);
      if(!actions.isEmpty()) {
        facetedProject.modify(actions, monitor);
      }

      removeTestFolderLinks(project, mavenProject, monitor, "/"); 

      // Remove "library unavailable at runtime" warning.
      addContainerAttribute(project, NONDEPENDENCY_ATTRIBUTE, monitor);
    }
}

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    // do nothing
  }

  public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // do nothing
  }

}
