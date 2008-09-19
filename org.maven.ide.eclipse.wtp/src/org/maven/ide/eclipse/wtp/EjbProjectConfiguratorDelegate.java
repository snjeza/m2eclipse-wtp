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
import org.eclipse.jst.j2ee.ejb.project.operations.IEjbFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.internal.ejb.project.operations.EjbFacetInstallDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;

/**
 * EjbProjectConfiguratorDelegate
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class EjbProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  public void configureProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {

    EjbPluginConfiguration config = new EjbPluginConfiguration(mavenProject);
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);
    IProjectFacetVersion ejbFv = config.getEjbFacetVersion();
    
    IDataModel ejbModelCfg = DataModelFactory.createDataModel(new EjbFacetInstallDataModelProvider());
    
    //Configuring content directory : used by WTP to create META-INF/manifest.mf, ejb-jar.xml
    String contentDir = config.getEjbContentDirectory(project);
    ejbModelCfg.setProperty(IEjbFacetInstallDataModelProperties.CONFIG_FOLDER, contentDir);

    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EJB_FACET)) {//WTP doesn't allow facet versions changes for JEE facets. 
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, ejbFv, ejbModelCfg));
    }
    
    if (!actions.isEmpty()) {
      facetedProject.modify(actions, monitor);  
    }
    removeTestFolderLinks(project, mavenProject, monitor, "/"); //XXX Doesn't work !!!
  }

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    // TODO check if there's anything to do!
  }

}
