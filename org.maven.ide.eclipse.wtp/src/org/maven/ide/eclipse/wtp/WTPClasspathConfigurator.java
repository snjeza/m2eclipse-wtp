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
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.j2ee.classpathdep.IClasspathDependencyConstants;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.jdt.AbstractClasspathConfigurator;


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

  private final String targetDir;

  public WTPClasspathConfigurator(String targetDir) {
    this.targetDir = targetDir;
  }

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
  public Set<IClasspathEntry> configureClasspath(Set<IClasspathEntry> cp0) {
    Set<IClasspathEntry> cp1 = new LinkedHashSet<IClasspathEntry>();

    Set<String> dups = new LinkedHashSet<String>();
    Set<String> names = new HashSet<String>();
    for(IClasspathEntry entry : cp0) {
      // WTP 2.0 does not support workspace dependencies in third party classpath containers
      String scope = getAttributeValue(entry, IMavenConstants.SCOPE_ATTRIBUTE);
      if(IClasspathEntry.CPE_PROJECT == entry.getEntryKind() && Artifact.SCOPE_COMPILE.equals(scope)) {
        continue;
      }

      if (!names.add(entry.getPath().lastSegment())) {
        dups.add(entry.getPath().lastSegment());
      }

      cp1.add(entry);
    }

    // WTP does not let remapping of individual container entries
    Set<IClasspathEntry> cp2 = new LinkedHashSet<IClasspathEntry>();
    for(IClasspathEntry entry : cp1) {
      IClasspathEntry newEntry = entry;
      if (dups.contains(entry.getPath().lastSegment())) {
        File src = new File(entry.getPath().toOSString());
        String groupId = getAttributeValue(entry, IMavenConstants.GROUP_ID_ATTRIBUTE);
        File dst = new File(targetDir, groupId + "-" + entry.getPath().lastSegment());
        try {
          if (src.canRead()) {
            if (isDifferent(src, dst)) { // uses lastModified
              FileUtils.copyFile(src, dst);
              dst.setLastModified(src.lastModified());
            }
            newEntry = JavaCore.newLibraryEntry(Path.fromOSString(dst.getCanonicalPath()),
                entry.getSourceAttachmentPath(),
                entry.getSourceAttachmentRootPath(),
                entry.getAccessRules(),
                entry.getExtraAttributes(),
                entry.isExported());
          }
        } catch(IOException ex) {
          MavenLogger.log("File copy failed", ex);
        }
      }
      cp2.add(newEntry);
    }

    return cp2;
  }

  private boolean isDifferent(File src, File dst) {
    if (!dst.exists()) {
      return true;
    }

    return src.length() != dst.length() 
        || src.lastModified() != dst.lastModified();
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
