/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.maven.ide.eclipse.project.configurator.AbstractClasspathConfigurator;


/**
 * See http://wiki.eclipse.org/ClasspathEntriesPublishExportSupport See
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=128851
 * 
 * @author Igor Fedorenko
 */
class WTPClasspathConfigurator extends AbstractClasspathConfigurator {

  public static final String PACKAGING_WAR = "war";

  WTPClasspathConfigurator() {
  }

  @Override
  public Set getAttributes(Artifact artifact, int kind) {
    Set<IClasspathAttribute> attributes = new HashSet<IClasspathAttribute>();

    String scope = artifact.getScope();
    // Check the scope & set WTP non-dependency as appropriate
    if(Artifact.SCOPE_PROVIDED.equals(scope) || Artifact.SCOPE_TEST.equals(scope)
        || Artifact.SCOPE_SYSTEM.equals(scope)) {
      attributes.add(JavaCore.newClasspathAttribute(IClasspathDependencyConstants.CLASSPATH_COMPONENT_NON_DEPENDENCY,
          ""));
    }

    return attributes;
  }

  @Override
  public void configureClasspath(Map entries) {
    // WTP 2.0 does not support workspace dependencies in thirdparty classpath containers
    for(Iterator<IClasspathEntry> it = entries.values().iterator(); it.hasNext();) {
      IClasspathEntry entry = it.next();
      if(IClasspathEntry.CPE_PROJECT == entry.getEntryKind()) {
        it.remove();
      }
    }
  }
}
