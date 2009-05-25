/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.maven.ide.eclipse.project.MavenProjectUtils;

/**
 * See http://maven.apache.org/plugins/maven-ejb-plugin/ejb-mojo.html
 * 
 * @author Fred Bricon
 */
class EjbPluginConfiguration {
  
  /**
   * Maven defaults ejb version to 2.1
   */
  private static final IProjectFacetVersion DEFAULT_EJB_FACET_VERSION = IJ2EEFacetConstants.EJB_21;
  
  final Plugin plugin;
 
  final MavenProject ejbProject;
  
  public EjbPluginConfiguration(MavenProject mavenProject) {

    if (JEEPackaging.EJB != JEEPackaging.getValue(mavenProject.getPackaging()))
      throw new IllegalArgumentException("Maven project must have ejb packaging");
    
    this.ejbProject = mavenProject;
    
    Plugin ejbPlugin = null;
    for (Plugin plugin : mavenProject.getBuildPlugins()) {
      if ("org.apache.maven.plugins".equals(plugin.getGroupId()) && "maven-ejb-plugin".equals(plugin.getArtifactId())) {
        ejbPlugin = plugin;
        break;
      }
    }
    this.plugin = ejbPlugin;
  }

  /**
   * Gets EJB_FACET version of the project from pom.xml.<br/> 
   * @return  value of &lt;maven-ejb-plugin&gt;&lt;configuration&gt;&lt;ejbVersion&gt;. Default value is 2.1.
   */
  public IProjectFacetVersion getEjbFacetVersion() {
    if (plugin == null){
      return DEFAULT_EJB_FACET_VERSION; 
    }

    Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
    if (dom == null) {
      return DEFAULT_EJB_FACET_VERSION; 
    }
    
    Xpp3Dom domVersion = dom.getChild("ejbVersion");
    if (domVersion != null) {
      return WTPProjectsUtil.EJB_FACET.getVersion(domVersion.getValue());
    }
    return DEFAULT_EJB_FACET_VERSION; 
  }
  
  /**
   * @return the first resource location directory declared in pom.xml
   */
  public String getEjbContentDirectory(IProject project) {
    IPath[] resources = MavenProjectUtils.getResourceLocations(project, ejbProject.getResources());
    return resources[0].toPortableString();
  }
  
}
