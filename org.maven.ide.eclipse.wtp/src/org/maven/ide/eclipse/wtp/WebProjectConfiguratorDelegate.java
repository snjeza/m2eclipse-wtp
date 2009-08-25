/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
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
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.IClasspathDescriptor;
import org.maven.ide.eclipse.jdt.IClasspathEntryDescriptor;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.wtp.internal.ExtensionReader;


/**
 * WebProjectConfiguratorDelegate
 * 
 * @author Igor Fedorenko
 * @author Fred Bricon
 */
class WebProjectConfiguratorDelegate extends AbstractProjectConfiguratorDelegate {

  static final IClasspathAttribute NONDEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, "");

  static final IClasspathAttribute DEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, "/WEB-INF/lib");

  protected void configure(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
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

    addContainerAttribute(project, DEPENDENCY_ATTRIBUTE, monitor);
  }

  public void setModuleDependencies(IProject project, MavenProject mavenProject, IProgressMonitor monitor)
      throws CoreException {
    IVirtualComponent component = ComponentCore.createComponent(project);
    //if the attempt to create dependencies happens before the project is actually created, abort. 
    //this will be created again when the project exists.
    if(component == null){
      return;
    }
    List<AbstractDependencyConfigurator> depConfigurators = ExtensionReader.readDependencyConfiguratorExtensions(projectManager, 
        MavenPlugin.getDefault().getMavenRuntimeManager(), mavenMarkerManager, 
        MavenPlugin.getDefault().getConsole());
    
    Set<IVirtualReference> references = new LinkedHashSet<IVirtualReference>();
    for(IMavenProjectFacade dependency : getWorkspaceDependencies(project, mavenProject)) {
      String depPackaging = dependency.getPackaging();
      MavenProject depMavenProject =  dependency.getMavenProject(monitor);
      //jee dependency has not been configured yet - i.e. has not JEE facet-
      if(JEEPackaging.isJEEPackaging(depPackaging) && !WTPProjectsUtil.isJavaEEProject(dependency.getProject())) {
        IProjectConfiguratorDelegate delegate = ProjectConfiguratorDelegateFactory
            .getProjectConfiguratorDelegate(depPackaging);
        if(delegate != null) {
          try {
            delegate.configureProject(dependency.getProject(), depMavenProject, monitor);
          } catch(MarkedException ex) {
            //Markers already have been created for this exception 
          }        
        }
      } else {
        // standard jar project
        configureWtpUtil(dependency.getProject(), depMavenProject, monitor);
      }
      IVirtualComponent depComponent = ComponentCore.createComponent(dependency.getProject());
      IVirtualReference reference = ComponentCore.createReference(component, depComponent);
      reference.setRuntimePath(new Path("/WEB-INF/lib"));
      references.add(reference);
    }

    component.setReferences(references.toArray(new IVirtualReference[references.size()]));

    for(IMavenProjectFacade dependency : getWorkspaceDependencies(project, mavenProject)) {
      MavenProject depMavenProject =  dependency.getMavenProject(monitor);
      Iterator<AbstractDependencyConfigurator> configurators = depConfigurators.iterator();
      while (configurators.hasNext()) {
        try {
          configurators.next().configureDependency(mavenProject, project, depMavenProject, dependency.getProject(), monitor);
        } catch(MarkedException ex) {
          //XXX handle this
        }
      }
    }
  }

  public void configureClasspath(IProject project, MavenProject mavenProject, IClasspathDescriptor classpath,
      IProgressMonitor monitor) throws CoreException {

    /*
     * Need to take care of three separate cases
     * 
     * 1. remove any project dependencies (they are represented as J2EE module dependencies)
     * 2. add non-dependency attribute for entries originated by artifacts with
     *    runtime, system, test scopes or optional dependencies (not sure about the last one)
     * 3. make sure all dependency JAR files have unique file names, i.e. artifactId/version collisions
     */

    Set<String> dups = new LinkedHashSet<String>();
    Set<String> names = new HashSet<String>();

    // first pass removes projects, adds non-dependency attribute and collects colliding filenames
    Iterator<IClasspathEntryDescriptor> iter = classpath.getEntryDescriptors().iterator();
    while (iter.hasNext()) {
      IClasspathEntryDescriptor descriptor = iter.next();
      IClasspathEntry entry = descriptor.getClasspathEntry();
      String scope = descriptor.getScope();

      // remove project dependencies
      if (IClasspathEntry.CPE_PROJECT == entry.getEntryKind() && Artifact.SCOPE_COMPILE.equals(scope)) {
        iter.remove();
        continue;
      }

      // add non-dependency attribute
      // Check the scope & set WTP non-dependency as appropriate
      // Optional artifact shouldn't be deployed
      if(Artifact.SCOPE_PROVIDED.equals(scope) || Artifact.SCOPE_TEST.equals(scope)
          || Artifact.SCOPE_SYSTEM.equals(scope) || descriptor.isOptionalDependency()) {
        descriptor.addClasspathAttribute(NONDEPENDENCY_ATTRIBUTE);
      }

      // collect duplicate file names 
      if (!names.add(entry.getPath().lastSegment())) {
        dups.add(entry.getPath().lastSegment());
      }
    }

    String targetDir = mavenProject.getBuild().getDirectory();

    // second pass disambiguates colliding entry file names
    iter = classpath.getEntryDescriptors().iterator();
    while (iter.hasNext()) {
      IClasspathEntryDescriptor descriptor = iter.next();
      IClasspathEntry entry = descriptor.getClasspathEntry();

      if (dups.contains(entry.getPath().lastSegment())) {
        File src = new File(entry.getPath().toOSString());
        String groupId = descriptor.getGroupId();
        File dst = new File(targetDir, groupId + "-" + entry.getPath().lastSegment());
        try {
          if (src.canRead()) {
            if (isDifferent(src, dst)) { // uses lastModified
              FileUtils.copyFile(src, dst);
              dst.setLastModified(src.lastModified());
            }
            descriptor.setClasspathEntry(JavaCore.newLibraryEntry(Path.fromOSString(dst.getCanonicalPath()), //
                entry.getSourceAttachmentPath(), //
                entry.getSourceAttachmentRootPath(), //
                entry.getAccessRules(), //
                entry.getExtraAttributes(), //
                entry.isExported()));
          }
        } catch(IOException ex) {
          MavenLogger.log("File copy failed", ex);
        }
      }
      
    }
  }

  private static boolean isDifferent(File src, File dst) {
    if (!dst.exists()) {
      return true;
    }

    return src.length() != dst.length() 
        || src.lastModified() != dst.lastModified();
  }

}
