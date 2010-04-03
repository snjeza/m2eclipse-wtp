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
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.application.WebModule;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.eclipse.jst.j2ee.componentcore.util.EARArtifactEdit;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.javaee.application.Application;
import org.eclipse.jst.javaee.application.Module;
import org.eclipse.jst.javaee.core.SecurityRole;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.internal.util.ComponentUtilities;
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
import org.maven.ide.eclipse.tests.common.AbstractMavenProjectTestCase;

/**
 * WTPProjectConfiguratorTest
 *
 * @author igor
 */
public class WTPProjectConfiguratorTest extends AbstractMavenProjectTestCase {
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
    assertEquals(projects[0], references[1].getReferencedComponent().getProject());
    assertEquals(projects[1], references[0].getReferencedComponent().getProject());
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


  public void testMNGECLIPSE1578_testRuntimeScopeDependency() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-1578", //
        new String[] {"war/pom.xml", "runtime-jar/pom.xml"}, new ResolverConfiguration());

    IProject war = projects[0];
    IProject runtimeJar = projects[1];
    assertMarkers(war, 0);
    assertMarkers(runtimeJar, 0);
    IVirtualComponent warComponent = ComponentCore.createComponent(projects[0]);
    assertEquals(3, warComponent.getReferences().length);
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


  public void testMNGECLIPSE1600_absoluteDirectories() throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-1600/", new String[] {"test/pom.xml", "testEAR/pom.xml"}, new ResolverConfiguration());
    
    IVirtualComponent warComponent = ComponentCore.createComponent(projects[0]);
    IVirtualFolder rootwar = warComponent.getRootFolder();
    IResource[] warResources = rootwar.getUnderlyingResources();
    assertEquals(1, warResources.length);
    assertEquals(projects[0].getFolder("/WebContent"), warResources[0]);

    IVirtualComponent earComponent = ComponentCore.createComponent(projects[1]);
    IVirtualFolder rootEar = earComponent.getRootFolder();
    IResource[] earResources = rootEar.getUnderlyingResources();
    assertEquals(1, earResources.length);
    assertEquals(projects[1].getFolder("/EarContent"), earResources[0]);
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
/*
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
*/
  public void testMNGECLIPSE688_NonDeployedDependencies () throws Exception {
    IProject[] projects = importProjects("projects/MNGECLIPSE-688", new String[]{"war-optional/pom.xml","core/pom.xml"}, new ResolverConfiguration());
    IProject war = projects[0]; 
    //IProject optionalJar = projects[1]; 
    assertMarkers(war, 0);
    //assertMarkers(optionalJar, 0);//MNGECLIPSE-1119 : optional projects shouldn't be deployed
    IClasspathEntry[] classpathEntries = getClassPathEntries(war);
    IClasspathEntry junit = classpathEntries[0];
    assertEquals("junit-3.8.1.jar", junit.getPath().lastSegment());
    assertNotDeployable(junit); //Junit is marked as <optional>true<optional>
    
    IVirtualComponent warComponent = ComponentCore.createComponent(war);
    assertEquals(4, warComponent.getReferences().length);
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
    //assertFalse(applicationXml.exists()); // application.xml is not mandatory for Java EE 5.0, hence not created
    assertTrue(applicationXml.exists()); // application.xml is created as maven-ear-plugin is configured as such by default
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference[] references = comp.getReferences();
    assertEquals(1, references.length);
    IVirtualReference junit = references[0];
    assertEquals("junit-3.8.1.jar", junit.getArchiveName());
    //MNGECLIPSE-1872 : check "/lib" is used as deployment directory
    assertEquals("/lib", junit.getRuntimePath().toPortableString());
  
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
    List<IMarker> warnings = findMarkers(core, IMarker.SEVERITY_WARNING);
    assertTrue(toString(warnings), warnings.isEmpty());
    
    assertMarkers(ejb, 0);
    warnings = findMarkers(ejb, IMarker.SEVERITY_WARNING);
    assertTrue(toString(warnings), warnings.isEmpty());

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
    // these projects can actually be deployed to JBoss
    // importing projects in unsorted order
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
        new String[] {"DWPMain/pom.xml", "DWPDependency/pom.xml", }, 
        new ResolverConfiguration());

    waitForJobsToComplete();
    assertEquals(2, projects.length);
    IProject dep = projects[1];
    IProject main = projects[0];
    
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
    // Exported filenames should be consistent when workspace resolution is on/off
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
    updateProject(project, "good_pom.xml");    
    assertMarkers(project, 0);    

    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(DEFAULT_WEB_VERSION, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    
  }

  // FIXME fails on the CI server.
  public void testMNGECLIPSE1045_TimestampedSnapshots() throws Exception {
    IProject ear = importProject("projects/MNGECLIPSE-1045/pom.xml", new ResolverConfiguration());

    assertMarkers(ear, 0);
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference[] references = comp.getReferences();
    assertEquals(1, references.length);
    IVirtualReference snapshot = references[0];
    assertEquals("MNGECLIPSE-1045-DEP-0.0.1-SNAPSHOT.jar", snapshot.getArchiveName());
  }
  
  public void testMNGECLIPSE1627_SkinnyWars() throws Exception {
    
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-1627/", //
        new String[] {"ear/pom.xml", "utility1/pom.xml", "utility2/pom.xml", "war-fullskinny/pom.xml", "war-mixedskinny/pom.xml", },
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[0];
    IProject utility1 = projects[1];
    IProject utility2 = projects[2];
    IProject fullskinnywar = projects[3];
    IProject mixedskinnywar = projects[4];
    
    assertMarkers(ear, 0);
    assertMarkers(utility1, 0);
    assertMarkers(utility2, 0);
    assertMarkers(fullskinnywar, 0);
    assertMarkers(mixedskinnywar, 0);
    
    IVirtualComponent comp = ComponentCore.createComponent(ear);
    
    IVirtualReference utilityRef1 = comp.getReference("MNGECLIPSE-1627-utility1");
    assertNotNull(utilityRef1);
    IVirtualReference utilityRef2 = comp.getReference("MNGECLIPSE-1627-utility2");
    assertNotNull(utilityRef2);
    
    ////////////
    //check the fullskinny war project
    ////////////
    IVirtualReference warRef1 = comp.getReference("MNGECLIPSE-1627-war-fullskinny");
    assertNotNull(warRef1);
    assertEquals("MNGECLIPSE-1627-war-fullskinny-0.0.1-SNAPSHOT.war",warRef1.getArchiveName());    
    
    //the fully skinny war contains to project refs whatsoever
    IVirtualComponent warComp1 = warRef1.getReferencedComponent();
    IVirtualReference[] fromWarRefs1 = warComp1.getReferences();
    assertEquals(4, fromWarRefs1.length);
    
    //check the component refs and their runtime path
    //TODO the reference ordering seems stable, but someone experienced should have a look
    assertEquals(utility1, fromWarRefs1[0].getReferencedComponent().getProject());
    assertEquals("/", fromWarRefs1[0].getRuntimePath().toString());
    assertEquals(utility2, fromWarRefs1[1].getReferencedComponent().getProject());
    assertEquals("/", fromWarRefs1[1].getRuntimePath().toString());    
    assertTrue(fromWarRefs1[2].getReferencedComponent().getDeployedName().endsWith("commons-lang-2.4.jar"));  
    assertEquals("/", fromWarRefs1[2].getRuntimePath().toString());  
    assertTrue(fromWarRefs1[3].getReferencedComponent().getDeployedName().endsWith("commons-collections-2.0.jar"));  
    assertEquals("/", fromWarRefs1[3].getRuntimePath().toString());  
    
    //check for all expected dependencies in the manifest
    IFile war1ManifestFile = ComponentUtilities.findFile(warComp1, new Path(J2EEConstants.MANIFEST_URI));
    Manifest mf1 = loadManifest(war1ManifestFile);

    //check that manifest classpath contains all dependencies
    String classpath = mf1.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
    assertTrue(classpath.contains(utilityRef1.getArchiveName()));
    assertTrue(classpath.contains(utilityRef2.getArchiveName()));
    assertTrue(classpath.contains("commons-lang-2.4.jar"));
    assertTrue(classpath.contains("commons-collections-2.0.jar"));
    //...but not junit, which is a test dependency
    assertFalse(classpath.contains("junit-3.8.1.jar"));
    
    //check that junit is in the maven classpath container instead
    IClasspathEntry[] mavenContainerEntries = getMavenContainerEntries(fullskinnywar);
    assertEquals(1, mavenContainerEntries.length);
    assertEquals("junit-3.8.1.jar", mavenContainerEntries[0].getPath().lastSegment());
    
    ////////////
    //check the mixedskinny war project
    ////////////
    IVirtualReference warRef2 = comp.getReference("MNGECLIPSE-1627-war-mixedskinny");
    assertNotNull(warRef2);
    assertEquals("MNGECLIPSE-1627-war-mixedskinny-0.0.1-SNAPSHOT.war",warRef2.getArchiveName());    
    
    IVirtualComponent warComp2 = warRef2.getReferencedComponent();
    IVirtualReference[] fromWarRefs2 = warComp2.getReferences();
    
    //check the component refs and their runtime path
    //TODO the reference ordering seems stable, but someone experienced should have a look
    //TODO the WEB-INF/lib located refs seem to come first
    assertEquals(utility2, fromWarRefs2[0].getReferencedComponent().getProject());
    assertEquals("/WEB-INF/lib", fromWarRefs2[0].getRuntimePath().toString());    
    assertTrue(fromWarRefs2[1].getReferencedComponent().getDeployedName().endsWith("commons-collections-2.0.jar"));  
    assertEquals("/WEB-INF/lib", fromWarRefs2[1].getRuntimePath().toString());  
    assertEquals(utility1, fromWarRefs2[2].getReferencedComponent().getProject());
    assertEquals("/", fromWarRefs2[2].getRuntimePath().toString());
    assertTrue(fromWarRefs2[3].getReferencedComponent().getDeployedName().endsWith("commons-lang-2.4.jar"));  
    assertEquals("/", fromWarRefs2[3].getRuntimePath().toString());  

    //check for all expected dependencies in the manifest
    IFile war2ManifestFile = ComponentUtilities.findFile(warComp2, new Path(J2EEConstants.MANIFEST_URI));
    Manifest mf2 = loadManifest(war2ManifestFile);
    
    //check that manifest classpath only contain utility1 and commons-lang
    classpath = mf2.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
    assertTrue(classpath.contains(utilityRef1.getArchiveName()));
    assertFalse(classpath.contains(utilityRef2.getArchiveName()));
    assertTrue(classpath.contains("commons-lang-2.4.jar"));
    assertFalse(classpath.contains("commons-collections-2.0.jar"));
    //...but not junit, which is a test dependency
    assertFalse(classpath.contains("junit-3.8.1.jar"));
    
    //check that junit and commons-collections is in the maven classpath container instead
    mavenContainerEntries = getMavenContainerEntries(mixedskinnywar);
    assertEquals(2, mavenContainerEntries.length);
    assertEquals("commons-collections-2.0.jar", mavenContainerEntries[0].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", mavenContainerEntries[1].getPath().lastSegment());
  }

  private Manifest loadManifest(IFile war1ManifestFile) throws CoreException, IOException {
    Manifest mf1;
    InputStream is = war1ManifestFile.getContents();
    try {
      mf1 = new Manifest(is);
    } finally {
      is.close();
    }
    return mf1;
  }

  //FIXME Test crashes on ear project update when WTPProjectConfiguratorTest tests are run as a whole. Works fine when run standalone.  
  public void XXXtestDeploymentDescriptorsJavaEE() throws Exception {
    
    deleteProject("pom");
    deleteProject("ear");
    deleteProject("ejb");
    deleteProject("war");
    deleteProject("core");
    
    IProject[] projects = importProjects(
        "projects/deployment-descriptors/", //
        new String[] {"javaEE/pom.xml", "javaEE/ear/pom.xml", "javaEE/core/pom.xml", "javaEE/ejb/pom.xml", "javaEE/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[1];
    IProject core = projects[2];
    IProject ejb = projects[3];
    IProject war = projects[4];
    
    assertMarkers(core, 0);
    assertMarkers(ejb, 0);
    assertMarkers(war, 0);
    assertMarkers(ear, 0);
   
    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(WebFacetUtils.WEB_24, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-0.0.1-SNAPSHOT.war",warRef.getArchiveName());
    IVirtualReference coreRef = comp.getReference("core");
    assertNotNull(coreRef);
    assertEquals("core-0.0.1-SNAPSHOT.jar",coreRef.getArchiveName());
    IVirtualReference ejbRef = comp.getReference("ejb");
    assertNotNull(ejbRef);
    assertEquals("ejb-0.0.1-SNAPSHOT.jar",ejbRef.getArchiveName());
    
    Application app = (Application)ModelProviderManager.getModelProvider(ear).getModelObject();
    assertEquals(3,app.getModules().size());
    Module webModule = app.getFirstModule(warRef.getArchiveName());
    assertNotNull("missing webmodule "+warRef.getArchiveName(),webModule);
    assertEquals("/dummy",webModule.getWeb().getContextRoot());

    Module coreModule = app.getFirstModule(coreRef.getArchiveName());
    assertNotNull("missing javaModule "+coreRef.getArchiveName(),coreModule);

    Module ejbModule = app.getFirstModule(ejbRef.getArchiveName());
    assertNotNull("missing ejbModule "+ejbRef.getArchiveName(),ejbModule);
    assertNull(ejbModule.getAltDd());
    
    List<SecurityRole> roles = app.getSecurityRoles();
    assertNotNull(roles);
    assertEquals(2, roles.size());

    updateProject(ear, "pom.step2.xml"); //FIXME crash cannot find pom/ear/target    
    
    assertEquals(2,app.getModules().size());
    coreModule = app.getFirstModule(coreRef.getArchiveName());
    assertNull(coreRef.getArchiveName()+" javamodule should be missing",coreModule);

    webModule = app.getFirstModule(warRef.getArchiveName());
    assertNotNull("missing webmodule "+warRef.getArchiveName(),webModule);
    assertEquals("/war-root",webModule.getWeb().getContextRoot());
    
    ejbModule = app.getFirstModule(ejbRef.getArchiveName());
    assertNotNull("missing ejbModule "+ejbRef.getArchiveName(),ejbModule);
    assertEquals("altdd-ejb.jar",ejbModule.getAltDd());

    roles = app.getSecurityRoles();
    assertEquals(4, roles.size());//TODO remove deleted roles
}

  //FIXME Test crashes on ear project update when WTPProjectConfiguratorTest tests are run as a whole. Works fine when run standalone.  
  public void XXXtestDeploymentDescriptorsJ2EE() throws Exception {
    deleteProject("pom");
    deleteProject("ear");
    deleteProject("ejb");
    deleteProject("war");
    deleteProject("core");

    IProject[] projects = importProjects(
        "projects/deployment-descriptors/", //
        new String[] {"J2EE/pom.xml", "J2EE/ear/pom.xml", "J2EE/core/pom.xml", "J2EE/ejb/pom.xml", "J2EE/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[1];
    IProject core = projects[2];
    IProject ejb = projects[3];
    IProject war = projects[4];
    
    assertMarkers(core, 0);
    assertMarkers(ejb, 0);
    assertMarkers(war, 0);
    assertMarkers(ear, 0);
   
    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(WebFacetUtils.WEB_24, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IVirtualComponent comp = ComponentCore.createComponent(ear);
    IVirtualReference warRef = comp.getReference("war");
    assertNotNull(warRef);
    assertEquals("war-0.0.1-SNAPSHOT.war",warRef.getArchiveName());
    IVirtualReference coreRef = comp.getReference("core");
    assertNotNull(coreRef);
    assertEquals("core-0.0.1-SNAPSHOT.jar",coreRef.getArchiveName());
    IVirtualReference ejbRef = comp.getReference("ejb");
    assertNotNull(ejbRef);
    assertEquals("ejb-0.0.1-SNAPSHOT.jar",ejbRef.getArchiveName());
    
    org.eclipse.jst.j2ee.application.Application app = (org.eclipse.jst.j2ee.application.Application)ModelProviderManager.getModelProvider(ear).getModelObject();
    assertEquals(3,app.getModules().size());
    org.eclipse.jst.j2ee.application.Module webModule = app.getFirstModule(warRef.getArchiveName());
    assertNotNull("missing webmodule "+warRef.getArchiveName(),webModule);
    assertTrue(webModule.isWebModule());
    assertEquals("/dummy",((WebModule)webModule).getContextRoot());

    org.eclipse.jst.j2ee.application.Module coreModule = app.getFirstModule(coreRef.getArchiveName());
    assertNotNull("missing javaModule "+coreRef.getArchiveName(),coreModule);

    org.eclipse.jst.j2ee.application.Module ejbModule = app.getFirstModule(ejbRef.getArchiveName());
    assertTrue(ejbModule.isEjbModule());
    assertNotNull("missing ejbModule "+ejbRef.getArchiveName(),ejbModule);
    assertNull(ejbModule.getAltDD());
    
    List<SecurityRole> roles = app.getSecurityRoles();
    assertNotNull(roles);
    assertEquals(2, roles.size());

    updateProject(ear, "pom.step2.xml"); //FIXME crash cannot find pom/ear/target    
    
    assertEquals(2,app.getModules().size());
    coreModule = app.getFirstModule(coreRef.getArchiveName());
    assertNull(coreRef.getArchiveName()+" javamodule should be missing",coreModule);

    webModule = app.getFirstModule(warRef.getArchiveName());
    assertNotNull("missing webmodule "+warRef.getArchiveName(),webModule);
    assertEquals("/war-root",((WebModule)webModule).getContextRoot());
    
    ejbModule = app.getFirstModule(ejbRef.getArchiveName());
    assertNotNull("missing ejbModule "+ejbRef.getArchiveName(),ejbModule);
    assertEquals("altdd-ejb.jar",ejbModule.getAltDD());

    roles = app.getSecurityRoles();
    assertEquals(4, roles.size());//TODO remove deleted roles
}

  public void testMNGECLIPSE1088_generateApplicationXml() throws Exception {

    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-1088/", //
        new String[] {"A/pom.xml", "B/pom.xml", "C/pom.xml", "D/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(4, projects.length);
    IProject ejb = projects[0];
    IProject war = projects[1];
    IProject ear1 = projects[2];
    IProject ear2 = projects[3];
    
    assertMarkers(war, 0);
    assertMarkers(ejb, 0);
    assertMarkers(ear1, 0);
    assertMarkers(ear2, 0);
   
    String applicationXmlRelativePath = "src/main/application/META-INF/application.xml";
    assertTrue(ear1.getFile(applicationXmlRelativePath).exists()); // application.xml is created as maven-ear-plugin is configured as such by default
    Application app1 = (Application)ModelProviderManager.getModelProvider(ear1).getModelObject();
    assertEquals(2,app1.getModules().size());
    assertNotNull("missing jarmodule for C",app1.getFirstModule("A-0.0.1-SNAPSHOT.jar"));
    assertNotNull("missing webmodule for C",app1.getFirstModule("website.war"));//MNGECLIPSE-2145 EAR should use finalName 

    Application app2 = (Application)ModelProviderManager.getModelProvider(ear2).getModelObject();
    assertEquals(2,app2.getModules().size());
    assertNotNull("missing jarmodule for D",app2.getFirstModule("A.jar"));
    assertNotNull("missing webmodule for D",app2.getFirstModule("B.war"));
    assertFalse(ear2.getFile(applicationXmlRelativePath).exists());// application.xml is not created as per maven-ear-plugin configuration 
    
}

  //Lars Ködderitzsch test case from https://issues.sonatype.org/browse/MNGECLIPSE-1644
  public void testMNGECLIPSE1644_contextRoot() throws Exception {
     
     IProject[] projects = importProjects(
         "projects/MNGECLIPSE-1644/", //
         new String[] {"ear/pom.xml", "war1/pom.xml", "war2/pom.xml", },
         new ResolverConfiguration());

     waitForJobsToComplete();
     
     assertEquals(3, projects.length);
     IProject ear = projects[0];
     IProject war1 = projects[1];
     IProject war2 = projects[2];
     
     assertMarkers(ear, 0);
     assertMarkers(war1, 0);
     assertMarkers(war2, 0);
     
     //check the context roots of the wars in the ear
     EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
     assertNotNull(edit);
     String war1ContextRoot = edit.getWebContextRoot(war1);
     String war2ContextRoot = edit.getWebContextRoot(war2);
     edit.dispose();
     
     assertEquals("/custom-context-root", war1ContextRoot);
     assertEquals("/MNGECLIPSE-1644-war2", war2ContextRoot);
  }

  public void testMNGECLIPSE2145_finalNames() throws Exception {
    
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-2145/testcase", //
        new String[] {"pom.xml", "ear/pom.xml", "war/pom.xml", "jar/pom.xml", },
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(4, projects.length);
    IProject pom = projects[0];
    IProject ear = projects[1];
    IProject war = projects[2];
    IProject jar = projects[3];
    
    assertMarkers(pom, 0);
    assertMarkers(ear, 0);
    assertMarkers(war, 0);
    assertMarkers(jar, 0);
    
    //check the context roots of the wars in the ear
    EARArtifactEdit edit = EARArtifactEdit.getEARArtifactEditForRead(ear);
    assertNotNull(edit);
    
    IVirtualComponent earComp = ComponentCore.createComponent(ear);
    
    IVirtualReference jarRef = earComp.getReference("jar");
    assertNotNull(jarRef);
    assertEquals("testcase-jar.jar",jarRef.getArchiveName());

    IVirtualReference warRef = earComp.getReference("war");
    assertNotNull(warRef);
    String uri = edit.getModuleURI(warRef.getReferencedComponent());
    assertEquals("testcase-war.war", uri);
    
 }

  
  public void testMNGECLIPSE1184_contextRootProperty() throws Exception {
    
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-1184/", //
        new String[] {"pom/ear/pom.xml", "pom/pom.xml", "pom/war/pom.xml", },
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(3, projects.length);
    IProject ear = projects[0];
    IProject pom = projects[1];
    IProject war = projects[2];
    
    assertMarkers(pom, 0);
    assertMarkers(ear, 0);
    assertMarkers(war, 0);
    
    //check the context root is the same as the one defined as a property in the parent pom
    Application app = (Application)ModelProviderManager.getModelProvider(ear).getModelObject();
    Module webModule = (Module)app.getModules().get(0);
    assertEquals("specialUri.war", webModule.getUri());
    assertEquals("/customContextRoot", webModule.getWeb().getContextRoot());
 }

  public void testMNGECLIPSE1121_pluginManagementSettings() throws Exception {
    //We check the pluginManagement settings are correctly interpreted from the different WTPProjectConfigurator delegates
    IProject[] projects = importProjects(
        "projects/MNGECLIPSE-1121/", //
        new String[] {"pom/pom.xml", "pom/ear/pom.xml", "pom/core/pom.xml", "pom/ejb/pom.xml", "pom/war/pom.xml"},
        new ResolverConfiguration());

    waitForJobsToComplete();
    
    assertEquals(5, projects.length);
    IProject ear = projects[1];
    IProject core = projects[2];
    IProject ejb = projects[3];
    IProject war = projects[4];
    
    assertMarkers(core, 0);
    assertMarkers(ejb, 0);
    assertMarkers(war, 0);
    assertMarkers(ear, 0);
   
    IFacetedProject fpWar = ProjectFacetsManager.create(war);
    assertNotNull(fpWar);
    assertTrue(fpWar.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(WebFacetUtils.WEB_25, fpWar.getInstalledVersion(WebFacetUtils.WEB_FACET));

    IFacetedProject fpCore = ProjectFacetsManager.create(core, false, monitor);
    assertNotNull(ProjectFacetsManager.getFacetedProjects().toString(), fpCore);  
    assertTrue(fpCore.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(UTILITY_10, fpCore.getInstalledVersion(UTILITY_FACET));

    IFacetedProject fpEjb = ProjectFacetsManager.create(ejb);
    assertNotNull(fpEjb);
    assertTrue(fpEjb.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    assertEquals(IJ2EEFacetConstants.EJB_30, fpEjb.getInstalledVersion(EJB_FACET));

    IFacetedProject fpEar = ProjectFacetsManager.create(ear);
    assertNotNull(fpEar);
    assertFalse(fpEar.hasProjectFacet(JavaFacetUtils.JAVA_FACET)); //Ears don't have java facet
    assertEquals(IJ2EEFacetConstants.ENTERPRISE_APPLICATION_50, fpEar.getInstalledVersion(EAR_FACET));
    IResource[] underlyingResources = getUnderlyingResources(ear);
    assertEquals(1, underlyingResources.length);
    assertEquals(ear.getFolder("/EarContent"), underlyingResources[0]);

    IFile applicationXml = ear.getFile("EarContent/META-INF/application.xml"); 
    assertTrue(applicationXml.exists());
  }

  // MNGECLIPSE-1878
  public void testPreserveClassPathContainersOnUpdate() throws Exception {
    deleteProject("MNGECLIPSE-1878-core");
    deleteProject("MNGECLIPSE-1878-ejb");
    deleteProject("MNGECLIPSE-1878-web");
    deleteProject("MNGECLIPSE-1878-ear");
    deleteProject("MNGECLIPSE-1878");

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/MNGECLIPSE-1878", new String[] {"pom.xml", "MNGECLIPSE-1878-core/pom.xml", 
        "MNGECLIPSE-1878-web/pom.xml", "MNGECLIPSE-1878-ejb/pom.xml", "MNGECLIPSE-1878-ear/pom.xml",}, configuration);

    waitForJobsToComplete();

    IProject web = projects[2];
    IProject ejb = projects[3];
    IProject ear = projects[4];
    //Building project as tests crashes, complaining about /MNGECLIPSE-1878/MNGECLIPSE-1878-ejb/target/classes later, on project update 
    ejb.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    waitForJobsToComplete();

    
    {
      IJavaProject webProject  = JavaCore.create(web); 
      IClasspathEntry[] rawClasspath = webProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 4, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.5", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.eclipse.jst.j2ee.internal.web.container", rawClasspath[2].getPath().toString());
      assertEquals("org.eclipse.jst.j2ee.internal.module.container", rawClasspath[3].getPath().toString());
    }
    {
      IJavaProject ejbProject  = JavaCore.create(ejb); 
      IClasspathEntry[] rawClasspath = ejbProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-1878-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.5", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());
      assertEquals("org.eclipse.jst.j2ee.internal.module.container", rawClasspath[3].getPath().toString());
    }
    {
      IFacetedProject fpEar = ProjectFacetsManager.create(ear);
      assertNotNull(fpEar);
      assertFalse(fpEar.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
    }
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    // update configuration
    configurationManager.updateProjectConfiguration(web, configuration, "", monitor);
    waitForJobsToComplete();

    {
      IJavaProject webProject  = JavaCore.create(web); 
      IClasspathEntry[] rawClasspath = webProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 4, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.5", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.eclipse.jst.j2ee.internal.web.container", rawClasspath[2].getPath().toString());
      assertEquals("org.eclipse.jst.j2ee.internal.module.container", rawClasspath[3].getPath().toString());

      IClasspathEntry[] entries = getWebLibClasspathContainer(webProject).getClasspathEntries();
      assertEquals(Arrays.toString(entries), 1, entries.length);
      assertEquals(IClasspathEntry.CPE_PROJECT, entries[0].getEntryKind());
      assertEquals("MNGECLIPSE-1878-core", entries[0].getPath().lastSegment());
    }

    configurationManager.updateProjectConfiguration(ejb, configuration, "", monitor);
    waitForJobsToComplete();

    {
      IJavaProject ejbProject  = JavaCore.create(ejb); 
      IClasspathEntry[] rawClasspath = ejbProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 5, rawClasspath.length);
      assertEquals("/MNGECLIPSE-1878-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-1878-ejb/src/main/resources", rawClasspath[1].getPath().toString());//TODO Resources folder appear after config update (WTP added MANIFEST.MF)
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/J2SE-1.5", rawClasspath[2].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[3].getPath().toString());
      assertEquals("org.eclipse.jst.j2ee.internal.module.container", rawClasspath[4].getPath().toString());
    }
    
  }

  private static IClasspathContainer getWebLibClasspathContainer(IJavaProject project) throws JavaModelException {
    IClasspathEntry[] entries = project.getRawClasspath();
    for(int i = 0; i < entries.length; i++ ) {
      IClasspathEntry entry = entries[i];
      if(entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER && "org.eclipse.jst.j2ee.internal.web.container".equals(entry.getPath().segment(0))) {
        return JavaCore.getClasspathContainer(entry.getPath(), project);
      }
    }
    return null;
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
    fail(message+ " is not a marker");
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
 
  /**
   * Replace the project pom.xml with a new one, triggers new build
   * @param project
   * @param newPomName
   * @throws Exception
   */
  private void updateProject(IProject project, String newPomName) throws Exception {
    
    copyContent(project, newPomName, "pom.xml");
    
    IProjectConfigurationManager configurationManager = MavenPlugin.getDefault().getProjectConfigurationManager();
    ResolverConfiguration configuration = new ResolverConfiguration();
    configurationManager.enableMavenNature(project, configuration, monitor);
    configurationManager.updateProjectConfiguration(project, configuration, mavenConfiguration.getGoalOnImport(), monitor);
    
    waitForJobsToComplete();
    project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    waitForJobsToComplete();
  }
}
