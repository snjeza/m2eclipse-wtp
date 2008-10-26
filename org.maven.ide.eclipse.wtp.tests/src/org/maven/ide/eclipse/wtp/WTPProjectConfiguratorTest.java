/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.project.IProjectConfigurationManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;

/**
 * WTPProjectConfiguratorTest
 *
 * @author igor
 */
public class WTPProjectConfiguratorTest extends AsbtractMavenProjectTestCase {
  private static final IProjectFacetVersion DEFAULT_WEB_VERSION = WebFacetUtils.WEB_FACET.getVersion("2.5");
  public static final IProjectFacet EJB_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.EJB); 
  public static final IProjectFacet UTILITY_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.UTILITY);
  public static final IProjectFacetVersion UTILITY_10 = UTILITY_FACET.getVersion("1.0");
  public static final IProjectFacet EAR_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.ENTERPRISE_APPLICATION);
  private static final IProjectFacetVersion DEFAULT_EAR_FACET = IJ2EEFacetConstants.ENTERPRISE_APPLICATION_13;

  public void testSimple01_import() throws Exception {
    IProject project = importProject("projects/simple/p01/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_23, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));

    IResource[] underlyingResources = getUnderlyingResources(project);
    assertEquals(1, underlyingResources.length);
    assertEquals(project.getFolder("/src/main/webapp"), underlyingResources[0]);
  }

  public void testSimple02_import() throws Exception {
    IProject project = importProject("projects/simple/p02/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_23, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
  }

  public void testSimple03_import() throws Exception {
    IProject project = importProject("projects/simple/p03/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(DEFAULT_WEB_VERSION, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
  }

  public void testMNGECLIPSE631() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-631", //
        new String[] {"common/pom.xml", "core/pom.xml", "project1/pom.xml"}, new ResolverConfiguration());

    IVirtualComponent component = ComponentCore.createComponent(projects[2]);
    IVirtualReference[] references = component.getReferences();
    assertEquals(2, references.length);
    assertEquals(projects[0], references[0].getReferencedComponent().getProject());
    assertEquals(projects[1], references[1].getReferencedComponent().getProject());
  }

  public void testSimple04_testScopeDependency() throws Exception {
    IProject[] projects = importProjects("projects/simple", //
        new String[] {"t01/pom.xml", "p04/pom.xml"}, new ResolverConfiguration());

    IVirtualComponent component = ComponentCore.createComponent(projects[1]);
    assertEquals(0, component.getReferences().length);

    IJavaProject javaProject = JavaCore.create(projects[1]);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();
    assertEquals(1, cp.length);
    assertEquals(projects[0].getFullPath(), cp[0].getPath());
  }

  public void testSimple05_sourceFolders() throws Exception {
    IProject project = importProject("projects/simple/p05/pom.xml", new ResolverConfiguration());
    
    IVirtualComponent component = ComponentCore.createComponent(project);
    IVirtualFolder root = component.getRootFolder();
    IVirtualFolder folder = root.getFolder("/WEB-INF/classes");
    IResource[] underlyingResources = folder.getUnderlyingResources();
    assertEquals(2, underlyingResources.length);
    assertEquals(project.getFolder("/src/main/java"), underlyingResources[0]);
    assertEquals(project.getFolder("/src/main/resources"), underlyingResources[1]);
  }

  public void testNonDefaultWarSourceDirectory() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-627/TestWar/pom.xml", new ResolverConfiguration());
    
    IVirtualComponent component = ComponentCore.createComponent(project);
    IVirtualFolder root = component.getRootFolder();
    IResource[] underlyingResources = root.getUnderlyingResources();
    assertEquals(1, underlyingResources.length);
    assertEquals(project.getFolder("/webapp"), underlyingResources[0]);
  }

  public void testSameArtifactId() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-679/pom.xml", new ResolverConfiguration());
    
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    IClasspathEntry[] cp = container.getClasspathEntries();

    assertEquals(2, cp.length);
    assertEquals("junit-junit-3.8.1.jar", cp[0].getPath().lastSegment());
    assertEquals("test-junit-3.8.1.jar", cp[1].getPath().lastSegment());
  }

  public void testLooseBuildDirectory() throws Exception {
    // import should not fail for projects with output folders located outside of project's basedir
    importProject("projects/MNGECLIPSE-767/pom.xml", new ResolverConfiguration());
  }

  public void testEnableMavenNature() throws Exception {
    IProject project = createExisting("test.project.MNGECLIPSE-629", "projects/MNGECLIPSE-629");

    IFacetedProject facetedProject;

    facetedProject = ProjectFacetsManager.create(project);
    assertNull(facetedProject);

    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    configurationManager.enableMavenNature(project, new ResolverConfiguration(), monitor);

    facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
  }
  
  public void testMNGECLIPSE793() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-793", //
        new String[] {"common/pom.xml", "core/pom.xml", "project1/pom.xml"}, new ResolverConfiguration());

    IVirtualComponent core = ComponentCore.createComponent(projects[1]); //core
    IVirtualReference[] references = core.getReferences();
    assertTrue(references == null || references.length == 0);
  }
  
  
  public void testMNGECLIPSE688_defaultEjb() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-688/ejb1/pom.xml", new ResolverConfiguration());

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    //Defaut ejb project should have 2.1 project facet
    assertEquals(IJ2EEFacetConstants.EJB_21, facetedProject.getInstalledVersion(EJB_FACET));
    
    IFile ejbJar = project.getFile("src/main/resources/META-INF/ejb-jar.xml");
    assertTrue(ejbJar.exists());
    //TODO check DTD
  }

  public void testMNGECLIPSE688_Ejb_30() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-688/ejb2/pom.xml", new ResolverConfiguration());

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(JavaFacetUtils.JAVA_50, facetedProject.getInstalledVersion(JavaFacetUtils.JAVA_FACET));
    assertEquals(IJ2EEFacetConstants.EJB_30, facetedProject.getInstalledVersion(EJB_FACET));

    IFolder ejbModuleFolder = project.getFolder("ejbModule"); 
    assertTrue(ejbModuleFolder.exists());

    //ejb-jar file should not have been created in the custom resources directory, as it's not mandatory according to the Java EE 5 specs
    IFile ejbJar = project.getFile("ejbModule/META-INF/ejb-jar.xml"); 
    assertFalse(ejbJar.exists());
    //TODO check DTD
  }

  public void testMNGECLIPSE688_NonDeployedDependencies () throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-688/war-optional/pom.xml", new ResolverConfiguration());

    IClasspathEntry[] classpathEntries = getClassPathEntries(project);
    IClasspathEntry junit = classpathEntries[0];
    assertEquals("junit-3.8.1.jar", junit.getPath().lastSegment());
    assertNotDeployable(junit); //Junit is marked as <optional>true<optional>
  }

  public void testMNGECLIPSE688_CustomEarContent () throws Exception {
    IProject ear = importProject("projects/MNGECLIPSE-688/ear21-1/pom.xml", new ResolverConfiguration());

    IFacetedProject fpEar = ProjectFacetsManager.create(ear);
    assertNotNull(fpEar);
    assertFalse(fpEar.hasProjectFacet(JavaFacetUtils.JAVA_FACET)); //Ears don't have java facet
    assertEquals(DEFAULT_EAR_FACET, fpEar.getInstalledVersion(EAR_FACET));

    IResource[] underlyingResources = getUnderlyingResources(ear);
    assertEquals(1, underlyingResources.length);
    assertEquals(ear.getFolder("/CustomEarSourceDirectory"), underlyingResources[0]);

    IFile applicationXml = ear.getFile("CustomEarSourceDirectory/META-INF/application.xml"); 
    assertTrue(applicationXml.exists());
  }

  public void testMNGECLIPSE688_Ear50 () throws Exception {
    IProject ear = importProject("projects/MNGECLIPSE-688/ear50-1/pom.xml", new ResolverConfiguration());

    assertMarkers(ear, 0);
    
    IFacetedProject fpEar = ProjectFacetsManager.create(ear);
    assertNotNull(fpEar);
    assertFalse(fpEar.hasProjectFacet(JavaFacetUtils.JAVA_FACET)); //Ears don't have java facet
    assertEquals(IJ2EEFacetConstants.ENTERPRISE_APPLICATION_50, fpEar.getInstalledVersion(EAR_FACET));

    IResource[] underlyingResources = getUnderlyingResources(ear);
    assertEquals(1, underlyingResources.length);
    assertEquals(ear.getFolder("/src/main/application"), underlyingResources[0]);

    IFile applicationXml = ear.getFile("src/main/application/META-INF/application.xml"); 
    assertFalse(applicationXml.exists()); // application.xml is not mandatory for Java EE 5.0, hence not created
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference[] references = comp.getReferences();
    assertEquals(1, references.length);
    IVirtualReference junit = references[0];
    assertEquals("junit-3.8.1.jar", junit.getArchiveName());
    assertEquals("lib/", junit.getRuntimePath().toPortableString());
  
  }

  
  public void testMNGECLIPSE688_Jee1() throws Exception {
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-688/", //
        new String[] {"jee1/pom.xml", "jee1/core/pom.xml", "jee1/ejb/pom.xml", "jee1/war/pom.xml", "jee1/ear/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject core = projects[1];
    IProject ejb = projects[2];
    IProject war = projects[3];
    IProject ear = projects[4];
    
    assertMarkers(core, 0);
    assertMarkers(ejb, 0);
    assertMarkers(war, 0);
    assertMarkers(ear, 0);
    
    IFacetedProject fpCore = ProjectFacetsManager.create(core, false, monitor);
    assertNotNull(ProjectFacetsManager.getFacetedProjects().toString(), fpCore);  
    assertTrue(fpCore.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(UTILITY_10, fpCore.getInstalledVersion(UTILITY_FACET));

    IFacetedProject fpEjb = ProjectFacetsManager.create(ejb);
    assertNotNull(fpEjb);
    assertTrue(fpEjb.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(IJ2EEFacetConstants.EJB_21, fpEjb.getInstalledVersion(EJB_FACET));

    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(WebFacetUtils.WEB_24, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IFacetedProject fpEar = ProjectFacetsManager.create(ear);
    assertNotNull(fpEar);
    assertFalse(fpEar.hasProjectFacet(JavaFacetUtils.JAVA_FACET)); //Ears don't have java facet
    assertEquals(DEFAULT_EAR_FACET, fpEar.getInstalledVersion(EAR_FACET));

    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-0.0.1-SNAPSHOT.war",warRef.getArchiveName());    
}

  public void XXXtestMNGECLIPSE688_Pom14_1() throws Exception {
    //These project can actually be deployed to JBoss
    //Trying to import projects in unsorted order
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-688/", //
        new String[] {"pom14-1/pom.xml", "pom14-1/ear14-1/pom.xml", "pom14-1/war23-1/pom.xml", "pom14-1/war23-2/pom.xml", "pom14-1/ejb21-1/pom.xml", "pom14-1/core-1/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(6, projects.length);
    IProject ear = projects[1];
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference[] references = comp.getReferences();
    assertEquals(toString(references), 5, references.length);
    // The reference order changes between imports, so can't rely on references indexes
    assertNotNull(comp.getReference("core-1"));
    assertNotNull(comp.getReference("ejb21-1"));
    assertNotNull(comp.getReference("war23-1"));
    assertNotNull(comp.getReference("war23-2"));
    assertNotNull(comp.getReference("var/M2_REPO/commons-lang/commons-lang/2.4/commons-lang-2.4.jar"));
    
    // checked provided dependencies won't be deployed in the ear
    IProject war1 = projects[2];
    IClasspathEntry[] war1CP = getClassPathEntries(war1);
    assertEquals(Arrays.asList(war1CP).toString(), 6, war1CP.length);
    // war23-1 pom.xml states that no dependencies should be deployed (in WEB-INF/lib)
    for (IClasspathEntry entry : war1CP){
      assertNotDeployable(entry);
    }
  }

  
  public void testMNGECLIPSE597() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-597", 
        new String[] {"DWPDependency/pom.xml", "DWPMain/pom.xml"}, 
        new ResolverConfiguration());

    waitForJobsToComplete();
    assertEquals(2, projects.length);
    IProject dep = projects[0];
    IProject main = projects[1];
    
    assertMarkers(main, 0);
    assertMarkers(dep, 0);

    IFacetedProject mainWar = ProjectFacetsManager.create(main);
    assertNotNull(mainWar);
    assertTrue(mainWar.hasProjectFacet(WebFacetUtils.WEB_FACET));

    IFacetedProject depWar = ProjectFacetsManager.create(dep);
    assertNotNull(depWar);
    assertTrue(depWar.hasProjectFacet(WebFacetUtils.WEB_FACET));

    IVirtualComponent comp = ComponentCore.createComponent(main);
    IVirtualReference[] references = comp.getReferences();
    IVirtualReference depRef = references[0];
    assertEquals(dep, depRef.getReferencedComponent().getProject());
  }


  public void testMNGECLIPSE965_fileNames() throws Exception {
    //Exported filenames should be consistent when workspace resolution is on/off
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-965/", //
        new String[] {"ear-standardFileNames/pom.xml", "ear-fullFileNames/pom.xml", "testFileNameWar/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(3, projects.length);
    IProject earStandardFN = projects[0];
    IProject earFullFN = projects[1];
    IProject war = projects[2];
    
    assertMarkers(earStandardFN, 0);
    assertMarkers(earFullFN, 0);
    assertMarkers(war, 0);

    //Check standard file name mapping
    IVirtualComponent earStandardFNcomp = ComponentCore.createComponent(earStandardFN);
    IVirtualReference warRef = earStandardFNcomp.getReference("testFileNameWar");
    assertNotNull(warRef);
    assertEquals("testFileNameWar-0.0.1-SNAPSHOT.war",warRef.getArchiveName());

    //Check full file name mapping
    IVirtualComponent earFullFNcomp = ComponentCore.createComponent(earFullFN);
    warRef = earFullFNcomp.getReference("testFileNameWar");
    assertNotNull(warRef);
    assertEquals("foo-bar-testFileNameWar-0.0.1-SNAPSHOT.war",warRef.getArchiveName());


    /* FIXME FullFileNameMapping doesn't work for non project refs. Need to fix that 
    IVirtualReference junitRef = comp.getReference("var/M2_REPO/junit/junit/3.8.1/junit-3.8.1.jar");
    assertNotNull(junitRef);
    assertEquals("junit-junit-3.8.1.jar",junitRef.getArchiveName());
    */
  }  
  
  public void testMNGECLIPSE984_errorMarkers() throws Exception {
    IProject project = importProject("projects/MNGECLIPSE-984/pom.xml", new ResolverConfiguration());
    waitForJobsToComplete();

    //Web Project configuration failed because Web 2.5 projects need Java 1.5 
    List<IMarker> markers = findErrorMarkers(project);
    assertEquals(2, markers.size());
    assertHasMarker("One or more constraints have not been satisfied.", markers);
    assertHasMarker("Dynamic Web Module 2.5 requires Java 5.0 or newer.", markers);

    //Markers disappear when the compiler level is set to 1.5
    /* can't get it to work for now
    copyContent(project, "good_pom.xml", "pom.xml");
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
    assertMarkers(project, 0);    

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(DEFAULT_WEB_VERSION, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    */
  }

  
  private String toString(IVirtualReference[] references) {
    StringBuilder sb = new StringBuilder("[");
    
    String sep = "";
    for(IVirtualReference reference : references) {
      IVirtualComponent component = reference.getReferencedComponent();
      sb.append(sep).append(reference.getRuntimePath() + " - ");
      sb.append(component.getName());
      sb.append(" " + component.getMetaProperties());
      sep = ", ";
    }
    
    return sb.append(']').toString();
  }

  private void assertHasMarker(String message, List<IMarker> markers) throws CoreException {
    for (IMarker marker : markers) {
      if (marker.getAttribute(IMarker.MESSAGE).equals(message)) {
        return ;
      }
    }
    fail("Markers doesn't contain " + message);
  }

  private void assertMarkers(IProject project, int expected) throws CoreException {
    // IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    List<IMarker> markers = findErrorMarkers(project);
    assertEquals(project.getName() + " : " + toString(markers.toArray(new IMarker[markers.size()])), //
        expected, markers.size());
  }

  private void  assertNotDeployable(IClasspathEntry entry){
    assertDeployable(entry, false);
  }
  
  private void  assertDeployable(IClasspathEntry entry, boolean expectedDeploymentStatus){
    assertEquals(entry.toString() + " " + IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, expectedDeploymentStatus,      hasExtraAttribute(entry, IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY));
    assertEquals(entry.toString() + " " + IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, !expectedDeploymentStatus, hasExtraAttribute(entry, IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY));
  }

  private static boolean  hasExtraAttribute (IClasspathEntry entry, String expectedAttribute){
    for (IClasspathAttribute cpa : entry.getExtraAttributes()) {
      if (expectedAttribute.equals(cpa.getName())){
        return true;
      }
    }
    return false;
  }

  private static IClasspathEntry[] getClassPathEntries(IProject project) throws Exception {
    IJavaProject javaProject = JavaCore.create(project);
    IClasspathContainer container = BuildPathManager.getMaven2ClasspathContainer(javaProject);
    return container.getClasspathEntries();
  }
  
  
  private static IResource[] getUnderlyingResources(IProject project) {
    IVirtualComponent component = ComponentCore.createComponent(project);
    IVirtualFolder root = component.getRootFolder();
    IResource[] underlyingResources = root.getUnderlyingResources();
    return underlyingResources;
  }
 
}
