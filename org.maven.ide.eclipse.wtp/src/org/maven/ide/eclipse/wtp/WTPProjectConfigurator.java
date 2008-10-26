/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * Project configurator for WTP projects. Specific project configuration is delegated to the
 * IProjectConfiguratorDelegate bound to a maven packaging type.
 * 
 * @author Igor Fedorenko
 */
public class WTPProjectConfigurator extends AbstractProjectConfigurator {

  @Override
  public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = request.getMavenProject();
    //Lookup the project configurator 
    IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
        .getProjectConfiguratorDelegate(mavenProject.getPackaging());
    if(configuratorDelegate != null) {
      IProject project = request.getProject();
      try {
        configuratorDelegate.configureProject(project, mavenProject, monitor);
        configuratorDelegate.setModuleDependencies(project, mavenProject, monitor);
      } catch(MarkedException ex) {
        MavenLogger.log(ex.getMessage(), ex);
      }
    }
  }

  @Override
  protected void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    IMavenProjectFacade facade = event.getMavenProject();
    if(facade != null) {
      IProject project = facade.getProject();
      if(isWTPProject(project)) {
        MavenProject mavenProject = facade.getMavenProject(monitor);
        IProjectConfiguratorDelegate configuratorDelegate = ProjectConfiguratorDelegateFactory
            .getProjectConfiguratorDelegate(mavenProject.getPackaging());
        if(configuratorDelegate != null) {
          configuratorDelegate.setModuleDependencies(project, mavenProject, monitor);
        }
      }
    }
  }

  static boolean isWTPProject(IProject project) {
    return ModuleCoreNature.getModuleCoreNature(project) != null;
  }

}
