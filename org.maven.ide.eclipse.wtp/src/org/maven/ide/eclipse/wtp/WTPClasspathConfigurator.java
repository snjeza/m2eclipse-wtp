/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.project.configurator.AbstractClasspathConfigurator;


/**
 * @see http://wiki.eclipse.org/ClasspathEntriesPublishExportSupport
 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=128851
 * @author Igor Fedorenko
 */
class WTPClasspathConfigurator extends AbstractClasspathConfigurator {

  static final IClasspathAttribute NONDEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY, "");

  static final IClasspathAttribute DEPENDENCY_ATTRIBUTE = JavaCore.newClasspathAttribute(
      IClasspathDependencyConstants.CLASSPATH_COMPONENT_DEPENDENCY, "/WEB-INF/lib");

  static final Set<IClasspathAttribute> NONDEPENDENCY_ATTRIBUTES = Collections.singleton(NONDEPENDENCY_ATTRIBUTE);

  @Override
  public Set<IClasspathAttribute> getAttributes(Artifact artifact, int kind) {
    String scope = artifact.getScope();
    // Check the scope & set WTP non-dependency as appropriate
    if(Artifact.SCOPE_PROVIDED.equals(scope) || Artifact.SCOPE_TEST.equals(scope)
        || Artifact.SCOPE_SYSTEM.equals(scope)) {
      return NONDEPENDENCY_ATTRIBUTES;
    }
    return null;
  }

  @Override
  public void configureClasspath(Set<IClasspathEntry> entries) {
    // WTP 2.0 does not support workspace dependencies in third party classpath containers
    for(Iterator<IClasspathEntry> it = entries.iterator(); it.hasNext();) {
      IClasspathEntry entry = it.next();
      String scope = getAttributeValue(entry, IMavenConstants.SCOPE_ATTRIBUTE);
      if(IClasspathEntry.CPE_PROJECT == entry.getEntryKind() && Artifact.SCOPE_COMPILE.equals(scope)) {
        it.remove();
      }
    }
  }

  private String getAttributeValue(IClasspathEntry entry, String name) {
    for(IClasspathAttribute attribute : entry.getExtraAttributes()) {
      if (name.equals(attribute.getName())) {
        return attribute.getValue();
      }
    }
    return null;
  }
}
