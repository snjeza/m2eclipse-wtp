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
import java.util.List;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.jee.util.internal.JavaEEQuickPeek;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;


/**
 * See http://maven.apache.org/plugins/maven-war-plugin/war-mojo.html
 * 
 * @author Igor Fedorenko
 */
@SuppressWarnings("restriction")
class WarPluginConfiguration {
  private static final String WAR_SOURCE_FOLDER = "/src/main/webapp";

  private static final String WAR_PACKAGING = "war";

  private static final String WEB_XML = "WEB-INF/web.xml";

  final Plugin plugin;

  public WarPluginConfiguration(MavenProject mavenProject) {
    this.plugin = findWarPlugin(mavenProject);
  }

  @SuppressWarnings("unchecked")
  private Plugin findWarPlugin(MavenProject mavenProject) {
    for(Plugin plugin : (List<Plugin>) mavenProject.getBuildPlugins()) {
      if("org.apache.maven.plugins".equals(plugin.getGroupId()) //
          && "maven-war-plugin".equals(plugin.getArtifactId())) {
        return plugin;
      }
    }
    return null;
  }

  static boolean isWarProject(MavenProject mavenProject) {
    return WAR_PACKAGING.equals(mavenProject.getPackaging());
  }

  public Xpp3Dom[] getWebResources(){
    if(plugin != null){
      Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
      Xpp3Dom[] children = dom.getChildren("webResources");
      return children;
    }
    return null;
  }
  
  public String getValueForWebResource(Xpp3Dom dom, String value){
    Xpp3Dom resource = dom.getChild("resource");
    if(resource != null){
      Xpp3Dom child = resource.getChild(value);
      if(child != null){
        return child.getValue();
      }
    }
    return null;
  }
  
  public String getDirectoryForWebResource(Xpp3Dom dom){
    return getValueForWebResource(dom, "directory");
  }
  
  public String getTargetPathForWebResource(Xpp3Dom dom){
    return getValueForWebResource(dom, "targetPath");
  }
  public String getWarSourceDirectory() {
    if(plugin == null) {
      return WAR_SOURCE_FOLDER;
    }

    Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
    if(dom == null) {
      return WAR_SOURCE_FOLDER;
    }

    Xpp3Dom[] warSourceDirectory = dom.getChildren("warSourceDirectory");
    if(warSourceDirectory != null && warSourceDirectory.length > 0) {
      // first one wins
      return warSourceDirectory[0].getValue();
    }

    return WAR_SOURCE_FOLDER;
  }

  public IProjectFacetVersion getWebFacetVersion(IProject project) {
    IFile webXml = project.getFolder(getWarSourceDirectory()).getFile(WEB_XML);
    if(webXml.isAccessible()) {
      try {
        InputStream is = webXml.getContents();
        try {
          JavaEEQuickPeek jqp = new JavaEEQuickPeek(is);
          switch(jqp.getVersion()) {
            case J2EEVersionConstants.WEB_2_2_ID:
              return WebFacetUtils.WEB_22;
            case J2EEVersionConstants.WEB_2_3_ID:
              return WebFacetUtils.WEB_23;
            case J2EEVersionConstants.WEB_2_4_ID:
              return WebFacetUtils.WEB_24;
            case J2EEVersionConstants.WEB_2_5_ID:
              return WebFacetUtils.WEB_FACET.getVersion("2.5");
          }
        } finally {
          is.close();
        }
      } catch(IOException ex) {
        // expected
      } catch(CoreException ex) {
        // expected
      }
    }
    //MNGECLIPSE-984 web.xml is optional for 2.5 Web Projects 
    return WTPProjectsUtil.DEFAULT_WEB_FACET;
  }
}
