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
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.componentcore.resources.IVirtualResource;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.project.IMavenProjectFacade;


/**
 * WebProjectConfiguratorDelegate
 * 
 * @author Igor Fedorenko
 * @author Fred Bricon
 */
class WebProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  public void configureProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    if(facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      try {
        facetedProject.modify(Collections.singleton(new IFacetedProject.Action(IFacetedProject.Action.Type.UNINSTALL,
            facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET), null)), monitor);
      } catch(Exception ex) {
        MavenLogger.log("Error removing WEB facet", ex);
      }
    }

    // make sure to update the main deployment folder
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject);
    String warSourceDirectory = config.getWarSourceDirectory();
    
    IVirtualComponent component = ComponentCore.createComponent(project);
    if(component != null) {
      component.create(IVirtualResource.NONE, monitor);
      component.getRootFolder().createLink(new Path("/" + warSourceDirectory), IVirtualResource.NONE, monitor);
    }
    
    Set<Action> actions = new LinkedHashSet<Action>();

    installJavaFacet(actions, project, facetedProject);

    if(!facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      IDataModel webModelCfg = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());

      webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER, warSourceDirectory);

      IProjectFacetVersion webFv = config.getWebFacetVersion(project);

      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, webFv, webModelCfg));
    }
    // WTP 2.0.2/3.0M6 does not allow to change WEB_FACET version
    // else if (!facetedProject.hasProjectFacet(webFv)) {
    //   actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, webFv, webModelCfg));
    // }

    facetedProject.modify(actions, monitor);

    // MNGECLIPSE-632 remove test sources/resources from WEB-INF/classes
    removeTestFolderLinks(project, mavenProject, monitor, "/WEB-INF/classes");

    addContainerAttribute(project, WTPClasspathConfigurator.DEPENDENCY_ATTRIBUTE, monitor);
  }

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IVirtualComponent component = ComponentCore.createComponent(project);

    Set<IVirtualReference> references = new LinkedHashSet<IVirtualReference>();
    for(IMavenProjectFacade dependency : getWorkspaceDependencies(project, mavenProject)) {
      String depPackaging = dependency.getPackaging();
      //jee dependency has not been configured yet - i.e. has not JEE facet-
      if(JEEPackaging.isJEEPackaging(depPackaging) && !WTPProjectsUtil.isJavaEEProject(project)) {
        IProjectConfiguratorDelegate delegate = ProjectConfiguratorDelegateFactory
            .getProjectConfiguratorDelegate(depPackaging);
        if(delegate != null) {
          delegate.configureProject(dependency.getProject(), dependency.getMavenProject(monitor), monitor);
        }
      } else {
        // standard jar project
        configureWtpUtil(dependency.getProject(), monitor);
      }
      IVirtualComponent depComponent = ComponentCore.createComponent(dependency.getProject());
      IVirtualReference reference = ComponentCore.createReference(component, depComponent);
      reference.setRuntimePath(new Path("/WEB-INF/lib"));
      references.add(reference);
    }

    component.setReferences(references.toArray(new IVirtualReference[references.size()]));
  }

}
