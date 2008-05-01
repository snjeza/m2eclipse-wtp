/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * @author Igor Fedorenko
 */
public class WTPProjectConfigurator extends AbstractProjectConfigurator {

  // WTP 2.0 does not seem to have any public API to access Utility JAR facet
  private static final IProjectFacet UTILITY_FACET = ProjectFacetsManager.getProjectFacet("jst.utility");

  private final MavenProjectManager projectManager;

  public WTPProjectConfigurator() {
    this.projectManager = MavenPlugin.getDefault().getMavenProjectManager();
  }

  @Override
  public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor)
      throws CoreException {
    MavenProject mavenProject = request.getMavenProject();
    if(WarPluginConfiguration.isWarProject(mavenProject)) {
      IProject project = request.getProject();
      configureWtpWar(project, monitor);
      setModuleDependencies(project, mavenProject, monitor);
    }
  }
  
  @Override
  protected void mavenProjectChanged(MavenProjectChangedEvent event, IProgressMonitor monitor) throws CoreException {
    MavenProjectFacade facade = event.getMavenProject();
    if(facade != null) {
      IProject project = facade.getProject();
      if(isWTPProject(project)) {
        setModuleDependencies(project, facade.getMavenProject(), monitor);
      }
    }
  }

  private List<MavenProjectFacade> getWorkspaceDependencies(IProject project, MavenProject mavenProject) {
    List<MavenProjectFacade> dependencies = new ArrayList<MavenProjectFacade>();
    @SuppressWarnings("unchecked")
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      MavenProjectFacade dependency = projectManager.getMavenProject(artifact);
      if(dependency != null && !dependency.getProject().equals(project)
          && dependency.getFullPath(artifact.getFile()) != null) {
        dependencies.add(dependency);
      }
    }
    return dependencies;
  }

  private void configureWtpUtil(IProject project, IProgressMonitor monitor) throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    IProjectFacetVersion javaFv = JavaFacetUtils.compilerLevelToFacet(JavaFacetUtils.getCompilerLevel(project));
    if(!facetedProject.hasProjectFacet(javaFv)) {
      facetedProject.installProjectFacet(javaFv, null, monitor);
    }

    IProjectFacetVersion utilFv = UTILITY_FACET.getVersion("1.0");
    if(!facetedProject.hasProjectFacet(utilFv)) {
      facetedProject.installProjectFacet(utilFv, null, monitor);
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
      webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER, "/src/main/webapp");
      facetedProject.installProjectFacet(webFv, webModelCfg, monitor);
    }

    addContainerAttribute(project, WTPClasspathConfigurator.DEPENDENCY_ATTRIBUTE, monitor);
  }

  // XXX consider adding getContainerAttributes to ClasspathConfigurator
  private void addContainerAttribute(IProject project, IClasspathAttribute attribute, IProgressMonitor monitor)
      throws JavaModelException {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] cp = javaProject.getRawClasspath();
    for(int i = 0; i < cp.length; i++ ) {
      if(IClasspathEntry.CPE_CONTAINER == cp[i].getEntryKind()
          && BuildPathManager.isMaven2ClasspathContainer(cp[i].getPath())) {
        LinkedHashMap<String, IClasspathAttribute> attrs = new LinkedHashMap<String, IClasspathAttribute>();
        for(IClasspathAttribute attr : cp[i].getExtraAttributes()) {
          attrs.put(attr.getName(), attr);
        }
        attrs.put(attribute.getName(), attribute);
        IClasspathAttribute[] newAttrs = attrs.values().toArray(new IClasspathAttribute[attrs.size()]);
        cp[i] = JavaCore.newContainerEntry(cp[i].getPath(), cp[i].getAccessRules(), newAttrs, cp[i].isExported());
        break;
      }
    }
    javaProject.setRawClasspath(cp, monitor);
  }

  private void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IVirtualComponent component = ComponentCore.createComponent(project);

    Set<IVirtualReference> references = new LinkedHashSet<IVirtualReference>();
    for(MavenProjectFacade dependency : getWorkspaceDependencies(project, mavenProject)) {
      configureWtpUtil(dependency.getProject(), monitor);
      IVirtualComponent depComponent = ComponentCore.createComponent(dependency.getProject());
      IVirtualReference reference = ComponentCore.createReference(component, depComponent);
      reference.setRuntimePath(new Path("/WEB-INF/lib"));
      references.add(reference);
    }

    component.setReferences(references.toArray(new IVirtualReference[references.size()]));
  }

  //	isWEB = facetedProject.hasProjectFacet(ProjectFacetsManager.getProjectFacet(IModuleConstants.JST_WEB_MODULE));

//  @SuppressWarnings("restriction")
//  private void removeRefernce(IVirtualComponent component, IVirtualReference reference) {
//    // no public API to remove references?
//    ((VirtualComponent) component).removeReference(reference);
//  }

  static boolean isWTPProject(IProject project) {
    return ModuleCoreNature.getModuleCoreNature(project) != null;
  }

}
