/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;


/**
 * See http://maven.apache.org/plugins/maven-war-plugin/war-mojo.html
 * 
 * @author Igor Fedorenko
 */
class WarPluginConfiguration {

  private static final String WAR_SOURCE_FOLDER = "/src/main/webapp";
  private static final String WAR_PACKAGING = "war";

  final Plugin plugin;

  public WarPluginConfiguration(MavenProject mavenProject) {
    Plugin war = null;
    for (Plugin plugin : (List<Plugin>) mavenProject.getBuildPlugins()) {
      if ("org.apache.maven.plugins".equals(plugin.getGroupId()) && "maven-war-plugin".equals(plugin.getArtifactId())) {
        war = plugin;
        break;
      }
    }
    this.plugin = war;
  }

  static boolean isWarProject(MavenProject mavenProject) {
    return WAR_PACKAGING.equals(mavenProject.getPackaging());
  }

  public String getWarSourceDirectory() {
    if (plugin == null) {
      return WAR_SOURCE_FOLDER;
    }

    Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
    if (dom == null) {
      return WAR_SOURCE_FOLDER;
    }
    
    Xpp3Dom[] warSourceDirectory = dom.getChildren("warSourceDirectory");
    if (warSourceDirectory != null && warSourceDirectory.length > 0) {
      // first one wins
      return warSourceDirectory[0].getValue();
    }

    return WAR_SOURCE_FOLDER;
  }
}
