/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.configurator.AbstractClasspathConfigurator;
import org.maven.ide.eclipse.project.configurator.AbstractClasspathConfiguratorFactory;


/**
 * @author Igor Fedorenko
 */
public class WTPClasspathConfiguratorFactory extends AbstractClasspathConfiguratorFactory {

  @Override
  public AbstractClasspathConfigurator createConfigurator(MavenProjectFacade facade) {
    MavenProject mavenProject = facade.getMavenProject();
    if(WarPluginConfiguration.isWarProject(mavenProject)) {
      IPath path = facade.getProjectRelativePath(mavenProject.getBuild().getDirectory());
      IFolder target = facade.getProject().getFolder(path);
      return new WTPClasspathConfigurator(target.getLocation());
    }
    return null;
  }

}
