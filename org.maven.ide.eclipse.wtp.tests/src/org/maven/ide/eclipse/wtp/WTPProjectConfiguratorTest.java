/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.project.BuildPathManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;

/**
 * WTPProjectConfiguratorTest
 *
 * @author igor
 */
public class WTPProjectConfiguratorTest extends AsbtractMavenProjectTestCase {
  private static final IProjectFacetVersion DEFAULT_WEB_VERSION = WebFacetUtils.WEB_23;

  public void testSimple01_import() throws Exception {
    IProject project = importProject("projects/simple/p01/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_23, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
  }

  public void testSimple02_import() throws Exception {
    IProject project = importProject("projects/simple/p02/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertEquals(WebFacetUtils.WEB_24, facetedProject.getInstalledVersion(WebFacetUtils.WEB_FACET));
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
    IProject[] projects = importProjects("projects/MNGECLIPSE-631", new String[] {"common/pom.xml", "core/pom.xml", "project1/pom.xml"}, new ResolverConfiguration());

    IVirtualComponent component = ComponentCore.createComponent(projects[2]);
    IVirtualReference[] references = component.getReferences();
    assertEquals(2, references.length);
    assertEquals(projects[0], references[0].getReferencedComponent().getProject());
    assertEquals(projects[1], references[1].getReferencedComponent().getProject());
  }

  public void testSimple04_testScopeDependency() throws Exception {
    IProject[] projects = importProjects("projects/simple", new String[] {"t01/pom.xml", "p04/pom.xml"}, new ResolverConfiguration());

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
}
