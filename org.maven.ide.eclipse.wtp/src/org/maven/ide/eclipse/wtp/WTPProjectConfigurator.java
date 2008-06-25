/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEModuleFacetInstallDataModelProperties;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetInstallDataModelProvider;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.jee.util.internal.JavaEEQuickPeek;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.ModuleCoreNature;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.IFacetedProject.Action;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.MavenProjectChangedEvent;
import org.maven.ide.eclipse.project.MavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;
import org.maven.ide.eclipse.project.MavenProjectUtils;
import org.maven.ide.eclipse.project.configurator.AbstractProjectConfigurator;
import org.maven.ide.eclipse.project.configurator.ProjectConfigurationRequest;


/**
 * @author Igor Fedorenko
 */
@SuppressWarnings("restriction")
public class WTPProjectConfigurator extends AbstractProjectConfigurator {

  // XXX move to WarPluginConfoguration
  private static final String WEB_XML = "WEB-INF/web.xml";

  // WTP 2.0 does not seem to have any public API to access Utility JAR facet
  private static final IProjectFacet UTILITY_FACET = ProjectFacetsManager.getProjectFacet("jst.utility");
  private static final IProjectFacetVersion UTILITY_10 = UTILITY_FACET.getVersion("1.0");

  private final MavenProjectManager projectManager;

  public WTPProjectConfigurator() {
    this.projectManager = MavenPlugin.getDefault().getMavenProjectManager();
  }

  @Override
  public void configure(MavenEmbedder embedder, ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    MavenProject mavenProject = request.getMavenProject();
    if(WarPluginConfiguration.isWarProject(mavenProject)) {
      IProject project = request.getProject();
      configureWtpWar(project, mavenProject, monitor);
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
    Set<IProject> projects = new HashSet<IProject>();
    List<MavenProjectFacade> dependencies = new ArrayList<MavenProjectFacade>();
    @SuppressWarnings("unchecked")
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    for(Artifact artifact : artifacts) {
      MavenProjectFacade dependency = projectManager.getMavenProject(artifact);
      if(Artifact.SCOPE_COMPILE.equals(artifact.getScope())
          && dependency != null && !dependency.getProject().equals(project)
          && dependency.getFullPath(artifact.getFile()) != null
          && projects.add(dependency.getProject())) 
      {
        dependencies.add(dependency);
      }
    }
    return dependencies;
  }

  private void configureWtpUtil(IProject project, IProgressMonitor monitor) throws CoreException {
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    if(!facetedProject.hasProjectFacet(UTILITY_FACET)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, UTILITY_10, null));
    } else if (!facetedProject.hasProjectFacet(UTILITY_10)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, UTILITY_10, null));
    }

    facetedProject.modify(actions, monitor);
  }

  private void configureWtpWar(IProject project, MavenProject mavenProject, IProgressMonitor monitor) throws CoreException {
    WarPluginConfiguration config = new WarPluginConfiguration(mavenProject);
    
    IFacetedProject facetedProject = ProjectFacetsManager.create(project, true, monitor);
    Set<Action> actions = new LinkedHashSet<Action>();
    installJavaFacet(actions, project, facetedProject);

    String warSourceDirectory = config.getWarSourceDirectory();
    IProjectFacetVersion webFv = getWebFacetVersion(project, warSourceDirectory);
    IDataModel webModelCfg = DataModelFactory.createDataModel(new WebFacetInstallDataModelProvider());
    webModelCfg.setProperty(IJ2EEModuleFacetInstallDataModelProperties.CONFIG_FOLDER, warSourceDirectory);
    if(!facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, webFv, webModelCfg));
    }
    // WTP 2.0.2/3.0M6 does not allow to change WEB_FACET version
    //else if (!facetedProject.hasProjectFacet(webFv)) {
    //  actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, webFv, webModelCfg));
    //}

    facetedProject.modify(actions, monitor);

    // MNGECLIPSE-632 remove test sources/resources from WEB-INF/classes
    IVirtualComponent component = ComponentCore.createComponent(project);
    IVirtualFolder jsrc = component.getRootFolder().getFolder("/WEB-INF/classes");
    for (IPath location : getTestRoots(project, mavenProject)) {
      jsrc.removeLink(location, 0, monitor);
    }

    addContainerAttribute(project, WTPClasspathConfigurator.DEPENDENCY_ATTRIBUTE, monitor);
  }

  private Set<IPath> getTestRoots(IProject project, MavenProject mavenProject) {
    Set<IPath> testRoots = new HashSet<IPath>();
    testRoots.addAll(Arrays.asList(MavenProjectUtils.getSourceLocations(project, mavenProject.getTestCompileSourceRoots())));
    testRoots.addAll(Arrays.asList(MavenProjectUtils.getResourceLocations(project, mavenProject.getTestResources())));
    return testRoots;
  }

  private IProjectFacetVersion getWebFacetVersion(IProject project, String warSourceFolder) {
      IFile webXml = project.getFolder(warSourceFolder).getFile(WEB_XML);
      if (webXml.isAccessible()) {
        try {
          InputStream is = webXml.getContents();
          try {
            JavaEEQuickPeek jqp = new JavaEEQuickPeek(is);
            switch (jqp.getVersion()) {
              case J2EEVersionConstants.WEB_2_2_ID: return WebFacetUtils.WEB_22;
              case J2EEVersionConstants.WEB_2_3_ID: return WebFacetUtils.WEB_23;
              case J2EEVersionConstants.WEB_2_4_ID: return WebFacetUtils.WEB_24;
              case J2EEVersionConstants.WEB_2_5_ID: return WebFacetUtils.WEB_FACET.getVersion("2.5");
            }
          } finally {
            is.close();
          }
        } catch (IOException ex) {
          // expected
        } catch(CoreException ex) {
          // expected
        }
    }
    return WebFacetUtils.WEB_23;
  }

  private void installJavaFacet(Set<Action> actions, IProject project, IFacetedProject facetedProject) {
    IProjectFacetVersion javaFv = JavaFacetUtils.compilerLevelToFacet(JavaFacetUtils.getCompilerLevel(project));
    if(!facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.INSTALL, javaFv, null));
    } else if (!facetedProject.hasProjectFacet(javaFv)) {
      actions.add(new IFacetedProject.Action(IFacetedProject.Action.Type.VERSION_CHANGE, javaFv, null));
    }
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
