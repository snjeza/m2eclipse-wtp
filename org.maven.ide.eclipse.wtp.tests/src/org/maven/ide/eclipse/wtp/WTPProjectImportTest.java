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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.maven.ide.eclipse.jdt.BuildPathManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;


public class WTPProjectImportTest extends AsbtractMavenProjectTestCase {

  public void testProjectImportDefault() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    IProject[] projects = importProjects("projects/MNGECLIPSE-20", new String[] {"pom.xml", "type/pom.xml",
        "app/pom.xml", "web/pom.xml", "ejb/pom.xml", "ear/pom.xml",}, configuration);

    waitForJobsToComplete();

    {
      IJavaProject javaProject = JavaCore.create(projects[0]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());

      IMarker[] markers = projects[0].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(projects[1]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-type/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = projects[1].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(projects[2]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-type", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("junit-3.8.1.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-app/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = projects[2].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(projects[3]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(1, classpathEntries.length);
      assertEquals("log4j-1.2.13.jar", classpathEntries[0].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.asList(rawClasspath).toString(), 5, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-web/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      IMarker[] markers = projects[3].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(projects[4]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[1].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.asList(rawClasspath).toString(), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/main/resources", rawClasspath[1].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[2].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[3].getPath().toString());

      IMarker[] markers = projects[4].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }

    {
      IJavaProject javaProject = JavaCore.create(projects[5]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(4, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-ejb", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-app", classpathEntries[1].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type", classpathEntries[2].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[3].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.asList(rawClasspath).toString(), 2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());

      IMarker[] markers = projects[5].findMarkers(null, true, IResource.DEPTH_INFINITE);
      assertEquals(toString(markers), 0, markers.length);
    }
  }

  public void testProjectImportNoWorkspaceResolution() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(false);
    configuration.setResolveWorkspaceProjects(false);
    configuration.setActiveProfiles("");

    IProject[] projects = importProjects("projects/MNGECLIPSE-20", 
        new String[] {
            "pom.xml", 
            "type/pom.xml",
            "app/pom.xml", 
            "web/pom.xml", 
            "ejb/pom.xml", 
            "ear/pom.xml"}, 
        configuration);

    waitForJobsToComplete();

    projects[0].refreshLocal(IResource.DEPTH_INFINITE, monitor);

    IResource res1 = projects[0].getFolder("ejb/target");
    IResource res2 = projects[4].getFolder("target");

    assertTrue(res1.exists());
    assertTrue(res2.exists());

    workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);

    {
      IJavaProject javaProject = JavaCore.create(projects[0]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());

      // IMarker[] markers = projects[0].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[0]);
      assertEquals(markers.toString(), 0, markers.size());
    }

    {
      // type
      IJavaProject javaProject = JavaCore.create(projects[1]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(0, classpathEntries.length);

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-type/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      // IMarker[] markers = projects[1].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[1]);
      assertEquals(markers.toString(), 0, markers.size());
    }

    {
      // app
      IJavaProject javaProject = JavaCore.create(projects[2]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(3, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("log4j-1.2.13.jar", classpathEntries[1].getPath().lastSegment());
      assertEquals("junit-3.8.1.jar", classpathEntries[2].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(3, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-app/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());

      // IMarker[] markers = projects[2].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[2]);
      assertEquals(markers.toString(), 3, markers.size());
    }

    {
      // web
      projects[3].build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
      IJavaProject javaProject = JavaCore.create(projects[3]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(2, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.toString(rawClasspath), 5, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-web/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[1].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[2].getPath().toString());
      assertEquals("org.eclipse.jst.j2ee.internal.web.container", rawClasspath[3].getPath().toString());
      assertEquals("org.eclipse.jst.j2ee.internal.module.container", rawClasspath[4].getPath().toString());

      // IMarker[] markers = projects[3].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[3]);
      assertEquals(markers.toString(), 4, markers.size());
    }

    {
      // ejb
      IJavaProject javaProject = JavaCore.create(projects[4]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(2, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-app-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());
      assertEquals("MNGECLIPSE-20-type-0.0.1-SNAPSHOT.jar", classpathEntries[1].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.asList(rawClasspath).toString(), 4, rawClasspath.length);
      assertEquals("/MNGECLIPSE-20-ejb/src/main/java", rawClasspath[0].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/src/main/resources", rawClasspath[1].getPath().toString());
      assertEquals("/MNGECLIPSE-20-ejb/target/classes", rawClasspath[1].getOutputLocation().toString());
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[2].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[3].getPath().toString());

      // IMarker[] markers = projects[4].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[4]);
      assertEquals(markers.toString(), 4, markers.size());
    }

    {
      // ear
      IJavaProject javaProject = JavaCore.create(projects[5]);
      IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
          .getClasspathEntries();
      assertEquals(1, classpathEntries.length);
      assertEquals("MNGECLIPSE-20-ejb-0.0.1-SNAPSHOT.jar", classpathEntries[0].getPath().lastSegment());

      IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
      assertEquals(Arrays.asList(rawClasspath).toString(), 2, rawClasspath.length);
      assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[0].getPath().toString());
      assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[1].getPath().toString());

      // IMarker[] markers = projects[5].findMarkers(null, true, IResource.DEPTH_INFINITE);
      List<IMarker> markers = findErrorMarkers(projects[4]);
      assertEquals(markers.toString(), 4, markers.size());
    }
  }

  public void testProjectImportWithModules() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    configuration.setResolveWorkspaceProjects(true);
    configuration.setActiveProfiles("");

    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals(Arrays.asList(classpathEntries).toString(), 2, classpathEntries.length);
    assertEquals("log4j-1.2.13.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", classpathEntries[1].getPath().lastSegment());

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    assertEquals(Arrays.asList(rawClasspath).toString(), 7, rawClasspath.length);
    assertEquals("/MNGECLIPSE-20/type/src/main/java", rawClasspath[0].getPath().toString());
    assertEquals("/MNGECLIPSE-20/type/target/classes", rawClasspath[0].getOutputLocation().toString());
    assertEquals("/MNGECLIPSE-20/app/src/main/java", rawClasspath[1].getPath().toString());
    assertEquals("/MNGECLIPSE-20/app/target/classes", rawClasspath[1].getOutputLocation().toString());
    assertEquals("/MNGECLIPSE-20/web/src/main/java", rawClasspath[2].getPath().toString());
    assertEquals("/MNGECLIPSE-20/web/target/classes", rawClasspath[2].getOutputLocation().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/java", rawClasspath[3].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/target/classes", rawClasspath[3].getOutputLocation().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/resources", rawClasspath[4].getPath().toString());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[5].getPath().toString());
    assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[6].getPath().toString());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testProjectImportWithModulesNoWorkspaceResolution() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    configuration.setResolveWorkspaceProjects(false);
    configuration.setActiveProfiles("");

    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("log4j-1.2.13.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", classpathEntries[1].getPath().lastSegment());

    IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
    assertEquals(7, rawClasspath.length);
    assertEquals("/MNGECLIPSE-20/type/src/main/java", rawClasspath[0].getPath().toString());
    assertEquals("/MNGECLIPSE-20/app/src/main/java", rawClasspath[1].getPath().toString());
    assertEquals("/MNGECLIPSE-20/web/src/main/java", rawClasspath[2].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/java", rawClasspath[3].getPath().toString());
    assertEquals("/MNGECLIPSE-20/ejb/src/main/resources", rawClasspath[4].getPath().toString());
    assertEquals("org.eclipse.jdt.launching.JRE_CONTAINER", rawClasspath[5].getPath().toString());
    assertEquals("org.maven.ide.eclipse.MAVEN2_CLASSPATH_CONTAINER", rawClasspath[6].getPath().toString());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }

  public void testUpdateClasspathContainerWithModulesNoWorkspace() throws Exception {
    deleteProject("MNGECLIPSE-20");
    deleteProject("MNGECLIPSE-20-app");
    deleteProject("MNGECLIPSE-20-ear");
    deleteProject("MNGECLIPSE-20-ejb");
    deleteProject("MNGECLIPSE-20-type");
    deleteProject("MNGECLIPSE-20-web");

    ResolverConfiguration configuration = new ResolverConfiguration();
    configuration.setIncludeModules(true);
    configuration.setResolveWorkspaceProjects(false);
    configuration.setActiveProfiles("");

    IProject project = importProject("projects/MNGECLIPSE-20/pom.xml", configuration);

    waitForJobsToComplete();

//    BuildPathManager buildpathManager = MavenPlugin.getDefault().getBuildpathManager();
//    buildpathManager.updateClasspathContainer(project, new NullProgressMonitor());

    IJavaProject javaProject = JavaCore.create(project);
    IClasspathEntry[] classpathEntries = BuildPathManager.getMaven2ClasspathContainer(javaProject)
        .getClasspathEntries();
    assertEquals("" + Arrays.asList(classpathEntries), 2, classpathEntries.length);
    assertEquals("log4j-1.2.13.jar", classpathEntries[0].getPath().lastSegment());
    assertEquals("junit-3.8.1.jar", classpathEntries[1].getPath().lastSegment());

    IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
    assertEquals(toString(markers), 0, markers.length);
  }
  
}

