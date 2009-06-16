/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.ejb.project.operations.IEjbFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.internal.ejb.project.operations.EjbFacetInstallDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;


/**
 * EjbProjectConfiguratorDelegate
 * 
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class EjbProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    if(facetedProject.hasProjectFacet(WTPProjectsUtil.EJB_FACET)) {
      try {
        facetedProject.modify(Collections.singleton(new IFacetedProject.Action(IFacetedProject.Action.Type.UNINSTALL,
            facetedProject.getInstalledVersion(WTPProjectsUtil.EJB_FACET), null)), monitor);
      } catch(Exception ex) {
        MavenLogger.log("Error removing EJB facet", ex);
      }
    }

    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    // WTP doesn't allow facet versions changes for JEE facets 
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EJB_FACET)) {
      // Configuring content directory, used by WTP to create META-INF/manifest.mf, ejb-jar.xml
      EjbPluginConfiguration config = new EjbPluginConfiguration(mavenProject);
      String contentDir = config.getEjbContentDirectory(project);
      
      IDataModel ejbModelCfg = DataModelFactory.createDataModel(new EjbFacetInstallDataModelProvider());
      ejbModelCfg.setProperty(IEjbFacetInstallDataModelProperties.CONFIG_FOLDER, contentDir);

      IProjectFacetVersion ejbFv = config.getEjbFacetVersion();
      
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, ejbFv, ejbModelCfg));
    }

    if(!actions.isEmpty()) {
      facetedProject.modify(actions, monitor);
    }

    removeTestFolderLinks(project, mavenProject, monitor, "/"); //XXX Doesn't work in certain -unidentified yet- circumstances!!!

    //Remove "library unavailable at runtime" warning.
    //addContainerAttribute(project, WTPClasspathConfigurator.NONDEPENDENCY_ATTRIBUTE, monitor);
}

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    // TODO check if there's anything to do!
  }

  public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // do nothing
  }

}
