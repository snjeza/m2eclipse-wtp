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
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.jca.project.facet.ConnectorFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.jca.project.facet.IConnectorFacetInstallDataModelProperties;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectUtils;

/**
 * ConnectorProjectConfiguratorDelegate
 *
 * @author Fred Bricon
 */
public class ConnectorProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate{

  public static final ArtifactFilter SCOPE_FILTER_RUNTIME = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.AbstractProjectConfiguratorDelegate#configure(org.eclipse.core.resources.IProject, org.apache.maven.project.MavenProject, org.eclipse.core.runtime.IProgressMonitor)
   */
  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    if(facetedProject.hasProjectFacet(WTPProjectsUtil.JCA_FACET)) {
      try {
        facetedProject.modify(Collections.singleton(new IFacetedProject.Action(IFacetedProject.Action.Type.UNINSTALL,
            facetedProject.getInstalledVersion(WTPProjectsUtil.JCA_FACET), null)), monitor);
      } catch(Exception ex) {
        MavenLogger.log("Error removing JCA facet", ex);
      }
    }
    
    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    RarPluginConfiguration config = new RarPluginConfiguration(mavenProject);
    // WTP doesn't allow facet versions changes for JEE facets 
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.JCA_FACET)) {
      // Configuring content directory, used by WTP to create META-INF/manifest.mf, ra.xml
      String contentDir = config.getRarContentDirectory(project);
      
      IDataModel rarModelCfg = DataModelFactory.createDataModel(new ConnectorFacetInstallDataModelProvider());
      rarModelCfg.setProperty(IConnectorFacetInstallDataModelProperties.CONFIG_FOLDER, contentDir);

      IProjectFacetVersion connectorFv = config.getConnectorFacetVersion(project);
      
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, connectorFv, rarModelCfg));
    }

    if(!actions.isEmpty()) {
      facetedProject.modify(actions, monitor);
    }

    if (!config.isJarIncluded()) {
      //project classes won't be jar'ed in the resulting rar.
      removeSourceLinks(project, mavenProject, monitor, "/");
    }
    removeTestFolderLinks(project, mavenProject, monitor, "/"); 
    
    String customRaXml = config.getCustomRaXml(project);
    removeRaXmlLink(project, monitor);
    if (customRaXml != null) {
      addRaXmlLink(project, monitor, customRaXml);
    }
    
    //Remove "library unavailable at runtime" warning. TODO is it relevant for connector projects?
    addContainerAttribute(project, NONDEPENDENCY_ATTRIBUTE, monitor);
  }

  /**
   * Remove custom ra.xml link, if exists.
   */
  private void removeRaXmlLink(IProject project, IProgressMonitor monitor) {
    //How do we remove links only knowing their runtimepath?
  }

  /**
   * @param monitor 
   * @param project 
   * @param customRaXml
   */
  private void addRaXmlLink(IProject project, IProgressMonitor monitor, String customRaXml) {
    IVirtualComponent component = ComponentCore.createComponent(project);
    if (component != null){
      IVirtualFolder root = component.getRootFolder().getFolder("/");
      Properties props = component.getMetaProperties();
      
      IPath path = new Path(customRaXml);
      try {
        root.createLink(path, 0, monitor);
      } catch(CoreException ex) {
        //ignore
        MavenLogger.log(ex);
      }
    }
  }

  private void removeSourceLinks(IProject project, MavenProject mavenProject, IProgressMonitor monitor, String folder) throws CoreException {
      IVirtualComponent component = ComponentCore.createComponent(project);
      if (component != null){
        IVirtualFolder jsrc = component.getRootFolder().getFolder(folder);
        for(IPath location : MavenProjectUtils.getSourceLocations(project, mavenProject.getCompileSourceRoots())) {
          jsrc.removeLink(location, 0, monitor);
        }
        for(IPath location : MavenProjectUtils.getResourceLocations(project, mavenProject.getResources())) {
          jsrc.removeLink(location, 0, monitor);
        }
      }
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.IProjectConfiguratorDelegate#configureClasspath(org.eclipse.core.resources.IProject, org.apache.maven.project.MavenProject, org.maven.ide.eclipse.jdt.IClasspathDescriptor, org.eclipse.core.runtime.IProgressMonitor)
   */
  public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {
    // Nothing to do
    
  }

  /* (non-Javadoc)
   * @see org.maven.ide.eclipse.wtp.IProjectConfiguratorDelegate#setModuleDependencies(org.eclipse.core.resources.IProject, org.apache.maven.project.MavenProject, org.eclipse.core.runtime.IProgressMonitor)
   */
  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {

    IVirtualComponent rarComponent = ComponentCore.createComponent(project);
    
    Set<IVirtualReference> newRefs = new LinkedHashSet<IVirtualReference>();
    
    Set<Artifact> artifacts =  mavenProject.getArtifacts();
    
    //Adding artifact references in .component. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=297777#c1
    for(Artifact artifact : artifacts) {
      //Don't deploy pom, non runtime or optional dependencies
      if("pom".equals(artifact.getType()) || !SCOPE_FILTER_RUNTIME.include(artifact) || artifact.isOptional()) {
        continue;
      }
      
      IMavenProjectFacade workspaceDependency = projectManager.getMavenProject(artifact.getGroupId(), artifact
          .getArtifactId(), artifact.getVersion());

      if(workspaceDependency != null && !workspaceDependency.getProject().equals(project)
          && workspaceDependency.getFullPath(artifact.getFile()) != null) {
        //artifact dependency is a workspace project
        IProject depProject = workspaceDependency.getProject();
        newRefs.add(createReference(rarComponent, depProject));
      } else {
        //artifact dependency should be added as a JEE module, referenced with M2_REPO variable 
        newRefs.add(createReference(rarComponent, artifact));
      }
    }

    IVirtualReference[] newRefsArray = new IVirtualReference[newRefs.size()];
    newRefs.toArray(newRefsArray);
    
    //Only change the preject references if they've changed
    if (hasChanged(rarComponent.getReferences(), newRefsArray)) {
      rarComponent.setReferences(newRefsArray);
    }
  }

  private IVirtualReference createReference(IVirtualComponent rarComponent, IProject depProject) {
    IVirtualComponent depComponent = ComponentCore.createComponent(depProject);
    return ComponentCore.createReference(rarComponent, depComponent);
  }
  private IVirtualReference createReference(IVirtualComponent rarComponent, Artifact artifact) {
      //Create dependency component, referenced from the local Repo.
      String artifactPath = ArtifactHelper.getM2REPOVarPath(artifact);
      IVirtualComponent depComponent = ComponentCore.createArchiveComponent(rarComponent.getProject(), artifactPath);
      return ComponentCore.createReference(rarComponent, depComponent);
  }


  private boolean hasChanged(IVirtualReference[] existingRefs, IVirtualReference[] refArray) {

    if (existingRefs==refArray) {
      return false;
    }
    if (existingRefs == null || existingRefs.length != refArray.length) {
      return true;
    }
    for (int i=0; i<existingRefs.length;i++){
      IVirtualReference existingRef = existingRefs[i];
      IVirtualReference newRef = refArray[i];
      if ((existingRef.getArchiveName() != null && !existingRef.getArchiveName().equals(newRef.getArchiveName())) ||
          !existingRef.getReferencedComponent().equals(newRef.getReferencedComponent())) {
        return true;  
      }
    }
    return false;    
  }

}
