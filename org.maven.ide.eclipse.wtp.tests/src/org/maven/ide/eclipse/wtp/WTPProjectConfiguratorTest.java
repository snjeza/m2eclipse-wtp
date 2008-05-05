/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.common.project.facet.JavaFacetUtils;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.project.ResolverConfiguration;
import org.maven.ide.eclipse.tests.AsbtractMavenProjectTestCase;

/**
 * WTPProjectConfiguratorTest
 *
 * @author igor
 */
public class WTPProjectConfiguratorTest extends AsbtractMavenProjectTestCase {

  public void testSimple01_import() throws IOException, CoreException {
    IProject project = importProject("projects/simple/p01/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertTrue(facetedProject.hasProjectFacet(WebFacetUtils.WEB_24));
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
  }

  public void testSimple02_import() throws IOException, CoreException {
    IProject project = importProject("projects/simple/p02/pom.xml", new ResolverConfiguration());
    IFacetedProject facetedProject = ProjectFacetsManager.create(project);
    assertNotNull(facetedProject);
    assertTrue(facetedProject.hasProjectFacet(WebFacetUtils.WEB_24));
    assertTrue(facetedProject.hasProjectFacet(JavaFacetUtils.JAVA_FACET));
  }
}
