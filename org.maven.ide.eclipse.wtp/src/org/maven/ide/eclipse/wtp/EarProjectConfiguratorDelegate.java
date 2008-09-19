/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.j2ee.application.internal.operations.AddComponentToEnterpriseApplicationDataModelProvider;
import org.eclipse.jst.j2ee.application.internal.operations.RemoveComponentFromEnterpriseApplicationDataModelProvider;
import org.eclipse.jst.j2ee.earcreation.IEarFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.internal.common.classpath.J2EEComponentClasspathUpdater;
import org.eclipse.jst.j2ee.internal.earcreation.EarFacetInstallDataModelProvider;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.properties.ICreateReferenceComponentsDataModelProperties;
import org.eclipse.wst.common.componentcore.internal.resources.VirtualArchiveComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelProvider;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.wtp.earmodules.EarModule;


/**
 * Configures Ear projects from maven-ear-plugin.
 * 
 * @see org.eclipse.jst.j2ee.ui.AddModulestoEARPropertiesPage
 * 
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
class EarProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  private static String M2_REPO_PREFIX = VirtualArchiveComponent.VARARCHIVETYPE + IPath.SEPARATOR
      + IMavenConstants.M2_REPO + IPath.SEPARATOR;

  private static final IStatus OK_STATUS = IDataModelProvider.OK_STATUS;

  public void configureProject(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {

    EarPluginConfiguration config = new EarPluginConfiguration(mavenProject);
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);

    Set<Action> actions = new LinkedHashSet<Action>();

    IDataModel earModelCfg = DataModelFactory.createDataModel(new EarFacetInstallDataModelProvider());

    //Configuring content directory
    String contentDir = config.getEarContentDirectory(project);
    earModelCfg.setProperty(IEarFacetInstallDataModelProperties.CONTENT_DIR, contentDir);

    //Adding EAR Facet
    IProjectFacetVersion earFv = config.getEarFacetVersion();

    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET)) {//WTP doesn't allow facet versions changes for JEE facets. 
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, earFv, earModelCfg));
    }

    if(!actions.isEmpty()) {
      facetedProject.modify(actions, monitor);
    }
    //FIXME Sometimes, test folders are still added to org.eclipse.wst.common.component
    removeTestFolderLinks(project, mavenProject, monitor, "/");  

    //XXX updating libdir?
  }

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    if(!facetedProject.hasProjectFacet(WTPProjectsUtil.EAR_FACET)) {
      return;
    }

    EarPluginConfiguration config = new EarPluginConfiguration(mavenProject);
    //Retrieving all ear module configuration from maven-ear-plugin : User defined modules + artifacts dependencies.
    Set<EarModule> earModules = config.getEarModules();
    String libBundleDir = config.getDefaultLibDirectory();

    //FB : I consider the delegate to be stateless - maybe I'm wrong -
    //hence we wrap all the interesting attributes of our new ear in an inner class, to stay close to AddModulestoEARPropertiesPage. 
    EarComponentWrapper earComponentWrp = new EarComponentWrapper(project, libBundleDir);

    for(EarModule earModule : earModules) {
      if(earModule.isExcluded()) {
        continue;
      }

      Artifact artifact = earModule.getArtifact();
      IMavenProjectFacade workspaceDependency = projectManager.getMavenProject(artifact.getGroupId(), artifact
          .getArtifactId(), artifact.getVersion());

      if(workspaceDependency != null && !workspaceDependency.getProject().equals(project)
          && workspaceDependency.getFullPath(artifact.getFile()) != null) {
        //artifact dependency is a workspace project
        IProject depProject = workspaceDependency.getProject();
        configureDependencyProject(workspaceDependency, monitor);
        earComponentWrp.addProject(depProject, earModule);
      } else {
        //artifact dependency should be added as a JEE module, referenced with M2_REPO variable 
        earComponentWrp.addReference(earModule);
      }
    }

    removeComponentsFromEAR(earComponentWrp, monitor);
    addComponentsToEAR(earComponentWrp, monitor);
    //XXX how do we set security roles using wtp api?
    //XXX how do we set alt-dds using wtp api?
    //XXX generating Deployment Descriptor ? operation exists in WTP 3.0.0 
  }

  private void configureDependencyProject(IMavenProjectFacade mavenProjectFacade, IProgressMonitor monitor)
      throws CoreException {
    // TODO Check what to do w/ the following datamodel
    /*
    IDataModel migrationdm = DataModelFactory.createDataModel(new JavaProjectMigrationDataModelProvider());
    migrationdm.setProperty(IJavaProjectMigrationDataModelProperties.PROJECT_NAME, project.getName());
    try{
    migrationdm.getDefaultOperation().execute(monitor, null);
    } catch (ExecutionException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, "Unable to configure dependent project",e));
    } 
    */

    //XXX Refactor the following w/ 
    IProject project = mavenProjectFacade.getProject();
    String depPackaging = mavenProjectFacade.getPackaging();
    //jee dependency has not been configured yet - i.e. it has no JEE facet-
    if(JEEPackaging.isJEEPackaging(depPackaging) && !WTPProjectsUtil.isJavaEEProject(project)) {
      IProjectConfiguratorDelegate delegate = ProjectConfiguratorDelegateFactory
          .getProjectConfiguratorDelegate(mavenProjectFacade.getPackaging());
      if(delegate != null) {
        //Lets install the proper facets
        delegate.configureProject(project, mavenProjectFacade.getMavenProject(monitor), monitor);
      }
    } else {
      //XXX Probably should create a UtilProjectConfiguratorDelegate
      configureWtpUtil(project, monitor);
    }
  }

  private void addComponentsToEAR(EarComponentWrapper earComponentWrapper, IProgressMonitor monitor)
      throws CoreException {
    //XXX adding components is probably the most complex part. look closely at AddModulestoEARPropertiesPage.java.
    //We need to handle java projects, dependency components, if jeeVersion >=5 : java project in lib dir and dependency components in lib dir  
    //I try a slightly different approach than what's done in AddModulestoEARPropertiesPage. Not sure if it's gonna make it though.
    //The thing is maven-ear-plugin lets you define as many bundle dir as you want
    if(earComponentWrapper == null) {
      return;
    }
    IVirtualComponent earComponent = earComponentWrapper.getEarComponent();
    Map<String, Map<IVirtualComponent, String>> compMap = earComponentWrapper.getComponentsToAddMap();
    if(compMap != null && !compMap.isEmpty()) {
      for(String bundleDir : compMap.keySet()) {
        execAddOp(monitor, earComponent, compMap.get(bundleDir), bundleDir);
      }
    }
  }

  private void removeComponentsFromEAR(EarComponentWrapper earComponentWrapper, IProgressMonitor monitor)
      throws CoreException {
    if(earComponentWrapper == null) {
      return;
    }
    IVirtualComponent earComponent = earComponentWrapper.getEarComponent();
    Map<String, Set<IVirtualComponent>> compMap = earComponentWrapper.getComponentsToRemove();
    if(compMap != null && !compMap.isEmpty()) {
      for(String bundleDir : compMap.keySet()) {
        execRemoveOp(monitor, earComponent, compMap.get(bundleDir), bundleDir);
      }
      J2EEComponentClasspathUpdater.getInstance().queueUpdateEAR(earComponent.getProject());
      //XXX At this point WTP normally update project manifests 
    }
  }

  /**
   * Execute addComponent operation on the ear component.
   * 
   * @param monitor - the monitor.
   * @param earComponent - the ear component.
   * @param uriMap - the components set to be added to the ear.
   * @param deployPath - the deploy path the components will be added to.
   * @throws CoreException
   */

  @SuppressWarnings("unchecked")
  private void execAddOp(IProgressMonitor monitor, IVirtualComponent earComponent,
      Map<IVirtualComponent, String> uriMap, String deployPath) throws CoreException {
    if(uriMap == null || uriMap.isEmpty()) {
      return;
    }

    List<IVirtualComponent> components = new ArrayList<IVirtualComponent>(uriMap.keySet());
    IDataModel dm = DataModelFactory.createDataModel(new AddComponentToEnterpriseApplicationDataModelProvider());

    dm.setProperty(ICreateReferenceComponentsDataModelProperties.SOURCE_COMPONENT, earComponent);
    List<IVirtualComponent> modHandlesList = (List<IVirtualComponent>) dm
        .getProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST);
    modHandlesList.addAll(components);
    dm.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST, components);
    dm.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENTS_DEPLOY_PATH, deployPath);
    dm.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENTS_TO_URI_MAP, uriMap);

    IStatus stat = dm.validateProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST);
    if(stat != OK_STATUS) {
      throw new CoreException(stat);
    }
    try {
      dm.getDefaultOperation().execute(monitor, null);
    } catch(ExecutionException e) {
      throw new CoreException(
          new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, "Unable to add components to ear", e));
    }
  }

  @SuppressWarnings("unchecked")
  private void execRemoveOp(IProgressMonitor monitor, IVirtualComponent earComponent,
      Set<IVirtualComponent> compToRemove, String deployPath) throws CoreException {
    if(compToRemove == null || compToRemove.isEmpty()) {
      return;
    }

    IDataModel dm = DataModelFactory.createDataModel(new RemoveComponentFromEnterpriseApplicationDataModelProvider());
    dm.setProperty(ICreateReferenceComponentsDataModelProperties.SOURCE_COMPONENT, earComponent);
    List<IVirtualComponent> modHandlesList = (List<IVirtualComponent>) dm
        .getProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST);
    modHandlesList.addAll(compToRemove);
    dm.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENT_LIST, modHandlesList);
    dm.setProperty(ICreateReferenceComponentsDataModelProperties.TARGET_COMPONENTS_DEPLOY_PATH, deployPath);

    try {
      dm.getDefaultOperation().execute(null, null);
    } catch(ExecutionException e) {
      throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID,
          "Unable to remove components from ear", e));
    }
  }

  /**
   * EarComponentWrapper stores ear component informations and its dependency components.
   * 
   * @author Fred Bricon.
   */
  //XXX Refactor to top level class if private fields accessibility is an issue.
  private class EarComponentWrapper {

    /**
     * Map containing Sets of components to add per deployement dir.
     */
    private Map<String, Map<IVirtualComponent, String>> componentsToAddMap;

    /**
     * Map containing Sets of components per deployement dir.
     */
    private Map<String, Set<IVirtualComponent>> allComponentsMap;

    //private boolean isJeeVersion = false;

    private IVirtualComponent earComponent;

    private String defaultLibBundleDir;

    private EarComponentWrapper(IProject project, String libBundleDir) {
      earComponent = ComponentCore.createComponent(project);
      componentsToAddMap = new HashMap<String, Map<IVirtualComponent, String>>();
      allComponentsMap = new HashMap<String, Set<IVirtualComponent>>();
      this.defaultLibBundleDir = libBundleDir == null || "".equals(libBundleDir.trim()) ? "/" : libBundleDir;
    }

    void addProject(IProject project, EarModule earModule) {
      IVirtualComponent projectComponent = ComponentCore.createComponent(project);
      addToAllComponentsMap(projectComponent, earModule.getBundleDir());
      if(!inEARAlready(projectComponent)) {
        addToComponentUriMap(projectComponent, earModule.getUri(), earModule.getBundleDir());
      }
    }

    private boolean inEARAlready(IVirtualComponent component) {
      IVirtualReference refs[] = earComponent.getReferences();
      for(IVirtualReference ref : refs) {
        if(ref.getReferencedComponent().equals(component)) {
          return true;
        }
      }
      return false;
    }

    void addReference(EarModule earModule) {
      //Create dependency component, referenced from the local Repo.
      String artifactPath = M2_REPO_PREFIX
          + ArtifactHelper.getLocalRepoRelativePath(earModule.getArtifact()).toPortableString();
      IVirtualComponent depComponent = ComponentCore.createArchiveComponent(earComponent.getProject(), artifactPath);
      
      addToAllComponentsMap(depComponent, earModule.getBundleDir());

      IVirtualReference newRef = ComponentCore.createReference(earComponent, depComponent);
      //Check duplicates for dependency component
      IVirtualReference[] existingRefs = earComponent.getReferences();
      String defaultArchiveName = new Path(newRef.getReferencedComponent().getName()).lastSegment();

      boolean dupeArchiveName = false;
      //check for duplicates
      for(IVirtualReference existingRef : existingRefs) {
        if(existingRef.getReferencedComponent().getName().equals(newRef.getReferencedComponent().getName())) {
          return; //same exact component already referenced
        } else if(existingRef.getArchiveName().equals(defaultArchiveName)) {
          dupeArchiveName = true; //different archive with same archive name
        }
      }

      for(int j = 1; dupeArchiveName; j++ ) { //ensure it doesn't have the runtime path
        int lastDotIndex = defaultArchiveName.lastIndexOf('.');
        String newArchiveName = null;
        String increment = "_" + j;
        if(lastDotIndex != -1) {
          newArchiveName = defaultArchiveName.substring(0, lastDotIndex) + increment
              + defaultArchiveName.substring(lastDotIndex);
        } else {
          newArchiveName = defaultArchiveName.substring(0) + increment;
        }

        int k = 0;
        for(; k < existingRefs.length; k++ ) {
          if(existingRefs[k].getArchiveName().equals(newArchiveName)) {
            break;
          }
        }
        if(k == existingRefs.length) {
          dupeArchiveName = false;
          newRef.setArchiveName(newArchiveName);
        }
      }
      //newRef.setRuntimePath(new Path(earModule.getBundleDir()));
      //earComponent.addReferences(new IVirtualReference[] {newRef });
      if(!dupeArchiveName) {
        addToComponentUriMap(depComponent, earModule.getUri(), earModule.getBundleDir());
      }
    }

    private void addToComponentUriMap(IVirtualComponent component, String uri, String bundleDir) {
      //To this point, bundleDir can not be null
      if(bundleDir == null || "".equals(bundleDir)) {
        bundleDir = "/";
      }

      Map<IVirtualComponent, String> componentsUriMap = componentsToAddMap.get(bundleDir);
      if(componentsUriMap == null) {
        componentsUriMap = new HashMap<IVirtualComponent, String>();
      }
      componentsUriMap.put(component, uri);
      componentsToAddMap.put(bundleDir, componentsUriMap);
    }

    private void addToAllComponentsMap(IVirtualComponent component, String bundleDir) {
      //To this point, bundleDir can not be null
      if(bundleDir == null || "".equals(bundleDir)) {
        bundleDir = "/";
      }

      Set<IVirtualComponent> componentsInDir = allComponentsMap.get(bundleDir);
      if(componentsInDir == null) {
        componentsInDir = new HashSet<IVirtualComponent>();
      }
      componentsInDir.add(component);
      allComponentsMap.put(bundleDir, componentsInDir);
    }

    private Map<String, Set<IVirtualComponent>> getComponentsToRemove() {
      Map<String, Set<IVirtualComponent>> compToRemoveMap = new HashMap<String, Set<IVirtualComponent>>(0);

      IVirtualReference[] oldrefs = getEarComponent().getReferences();

      for(IVirtualReference ref : oldrefs) {
        //1° get the old reference component
        IVirtualComponent handle = ref.getReferencedComponent();
        String oldDir = ref.getRuntimePath().toString();
        //2° get the new components defined for the old reference deployment 
        Set<IVirtualComponent> compInDir = getComponentsInDir(oldDir);

        //If the component doesn't exists anymore, or its deployPath has changed, we remove it
        if(!compInDir.contains(handle)) {
          Set<IVirtualComponent> compToRemoveSet = compToRemoveMap.get(oldDir);
          if(compToRemoveSet == null) {
            compToRemoveSet = new HashSet<IVirtualComponent>();
          }
          compToRemoveSet.add(handle);
          compToRemoveMap.put(oldDir, compToRemoveSet);
        }
      }

      return compToRemoveMap;
    }

    private Set<IVirtualComponent> getComponentsInDir(String dir) {
      if(allComponentsMap == null && allComponentsMap.isEmpty() || allComponentsMap.get(dir) == null) {
        return new HashSet<IVirtualComponent>(0);
      }
      return allComponentsMap.get(dir);
    }

    IVirtualComponent getEarComponent() {
      return earComponent;
    }

    String getDefaultLibBundleDir() {
      return defaultLibBundleDir;
    }

    Map<String, Map<IVirtualComponent, String>> getComponentsToAddMap() {
      return componentsToAddMap;
    }

  }

}
