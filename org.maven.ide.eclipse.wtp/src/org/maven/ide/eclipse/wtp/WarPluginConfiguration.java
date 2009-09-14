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

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.j2ee.internal.J2EEVersionConstants;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.jst.jee.util.internal.JavaEEQuickPeek;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.maven.ide.eclipse.wtp.internal.StringUtils;


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

  private IProject project;

  public WarPluginConfiguration(MavenProject mavenProject, IProject project) {
    this.plugin = findWarPlugin(mavenProject);
    this.project = project;
  }

  private Plugin findWarPlugin(MavenProject mavenProject) {
    for(Plugin plugin : mavenProject.getBuildPlugins()) {
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

  public Xpp3Dom[] getWebResources() {
    if(plugin != null) {
      Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
      Xpp3Dom[] children = dom.getChildren("webResources");
      return children;
    }
    return null;
  }

  public String getValueForWebResource(Xpp3Dom dom, String value) {
    Xpp3Dom resource = dom.getChild("resource");
    if(resource != null) {
      Xpp3Dom child = resource.getChild(value);
      if(child != null) {
        return child.getValue();
      }
    }
    return null;
  }

  public String getDirectoryForWebResource(Xpp3Dom dom) {
    return getValueForWebResource(dom, "directory");
  }

  public String getTargetPathForWebResource(Xpp3Dom dom) {
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
      String dir = warSourceDirectory[0].getValue();
      //MNGECLIPSE-1600 fixed absolute warSourceDirectory thanks to Snjezana Peco's patch
      if(project != null) {
        IPath projectLocationPath = project.getLocation();
        if(projectLocationPath != null && dir != null) {
          String projectLocation = projectLocationPath.toOSString();
          if(dir.startsWith(projectLocation)) {
            return dir.substring(projectLocation.length());
          }
        }
      }
      return dir;
    }

    return WAR_SOURCE_FOLDER;
  }

  public String[] getPackagingExcludes() {
    if(plugin != null) {
      Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
      if(dom != null) {
        Xpp3Dom excl = dom.getChild("packagingExcludes");
        if(excl != null) {
          return StringUtils.tokenizeToStringArray(excl.getValue(), ",");
        }
      }
    }
    return new String[0];
  }

  public String[] getPackagingIncludes() {
    if(plugin != null) {
      Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
      if(dom != null) {
        Xpp3Dom incl = dom.getChild("packagingIncludes");
        if(incl != null && incl.getValue() != null) {
          return StringUtils.tokenizeToStringArray(incl.getValue(), ",");
        }
      }
    }
    return new String[0];
  }

  public boolean isAddManifestClasspath() {

    if(plugin != null) {
      Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
      if(dom != null) {
        Xpp3Dom arch = dom.getChild("archive");
        if(arch != null) {
          Xpp3Dom manifest = arch.getChild("manifest");
          if(manifest != null) {
            Xpp3Dom addToClp = manifest.getChild("addClasspath");
            if(addToClp != null) {
              return Boolean.valueOf(addToClp.getValue());
            }
          }
        }
      }
    }
    return false;
  }

  public String getManifestClasspathPrefix() {

    if(plugin != null) {
      Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
      if(dom != null) {
        Xpp3Dom arch = dom.getChild("archive");
        if(arch != null) {
          Xpp3Dom manifest = arch.getChild("manifest");
          if(manifest != null) {
            Xpp3Dom prefix = manifest.getChild("classpathPrefix");
            if(prefix != null) {
              return prefix.getValue();
            }
          }
        }
      }
    }
    return null;
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
