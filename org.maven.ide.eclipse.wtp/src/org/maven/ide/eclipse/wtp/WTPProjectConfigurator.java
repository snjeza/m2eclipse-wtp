/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.LinkedHashMap;

import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * @author Igor Fedorenko
 */
public class WTPProjectConfigurator extends AbstractProjectConfigurator {

  @Override
  public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = request.getMavenProject();
    if(WarPluginConfiguration.isWarProject(mavenProject)) {
      IProject project = request.getProject();
      configureWtpWar(project, monitor);
    }
  }

  private void configureWtpWar(IProject project, IProgressMonitor monitor) throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    IProjectFacetVersion javaFv = JavaFacetUtils.compilerLevelToFacet(JavaFacetUtils.getCompilerLevel(project));
    if(!facetedProject.hasProjectFacet(javaFv)) {
      facetedProject.installProjectFacet(javaFv, null, monitor);
    }

    IProjectFacetVersion webFv = WebFacetUtils.WEB_FACET.getVersion("2.4");
    if(!facetedProject.hasProjectFacet(webFv)) {
      IDataModel webModelCfg = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());
      facetedProject.installProjectFacet(webFv, webModelCfg, monitor);
    }

    // this is quite ugly, consider adding getContainerAttributes to ClasspathConfigurator
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();
    for(int i = 0; i < cp.length; i++ ) {
      if(IClasspathEntry.CPE_CONTAINER == cp[i].getEntryKind()
          && BuildPathManager.isMaven2ClasspathContainer(cp[i].getPath())) {
        LinkedHashMap<String, IClasspathAttribute> attrs = new LinkedHashMap<String, IClasspathAttribute>();
        for(IClasspathAttribute attr : cp[i].getExtraAttributes()) {
          attrs.put(attr.getName(), attr);
        }
        attrs.put(IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, JavaCore.newClasspathAttribute(
            IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, "/WEB-INF/lib"));
        cp[i] = JavaCore.newContainerEntry(cp[i].getPath(), cp[i].getAccessRules(), attrs.values().toArray(
            new IClasspathAttribute[attrs.size()]), cp[i].isExported());
        break;
      }
    }
    javaProject.setRawClasspath(cp, monitor);
  }

//	isWEB = facetedProject.hasProjectFacet(ProjectFacetsManager.getProjectFacet(IModuleConstants.JST_WEB_MODULE));

}
