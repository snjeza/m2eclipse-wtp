/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.maven.ide.eclipse.core.MavenLogger;
import org.maven.ide.eclipse.wtp.earmodules.ArtifactTypeMappingService;
import org.maven.ide.eclipse.wtp.earmodules.EarModule;
import org.maven.ide.eclipse.wtp.earmodules.EarModuleFactory;
import org.maven.ide.eclipse.wtp.earmodules.EarPluginException;
import org.maven.ide.eclipse.wtp.earmodules.SecurityRoleKey;
import org.maven.ide.eclipse.wtp.earmodules.output.FileNameMapping;
import org.maven.ide.eclipse.wtp.earmodules.output.FileNameMappingFactory;


/**
 * EarPluginConfiguration used to read maven-ear-plugin configuration.
 * 
 * @see http://maven.apache.org/plugins/maven-ear-plugin/
 * @see http://maven.apache.org/plugins/maven-ear-plugin/modules.html
 * 
 * @author Fred Bricon
 */
class EarPluginConfiguration {

  //Careful : This has a different meaning from the default library directory (/lib)
  private static final String EAR_DEFAULT_BUNDLE_DIR = "/"; 

  private static final String EAR_DEFAULT_CONTENT_DIR = "src/main/application"; // J2EEConstants.EAR_DEFAULT_LIB_DIR

  // Default EAR version produced by the maven-ear-plugin
  private static final IProjectFacetVersion DEFAULT_EAR_FACET = IJ2EEFacetConstants.ENTERPRISE_APPLICATION_13;

  private final MavenProject mavenProject;

  private final Plugin plugin;

  /**
   * directory where jars will be deployed.
   */
  private String libDirectory;

  // private String contentDirectory;

  // XXX see if Lazy loading / caching the different factories and services is relevant.
  ArtifactTypeMappingService typeMappingService;

  public EarPluginConfiguration(MavenProject mavenProject) {
    if(JEEPackaging.EAR != JEEPackaging.getValue(mavenProject.getPackaging())) {
      throw new IllegalArgumentException("Maven project must have ear packaging");
    }

    this.mavenProject = mavenProject;
    this.plugin = findEarPlugin(mavenProject);
  }

  private Plugin findEarPlugin(MavenProject mavenProject) {
    for(Plugin plugin : mavenProject.getBuildPlugins()) {
      if("org.apache.maven.plugins".equals(plugin.getGroupId()) //
          && "maven-ear-plugin".equals(plugin.getArtifactId())) {
        return plugin;
      }
    }
    return null;
  }

  /**
   * @return ear plugin configuration or null.
   */
  private Xpp3Dom getConfiguration() {
    if(plugin == null) {
      return null;
    }
    return (Xpp3Dom) plugin.getConfiguration();
  }

  /**
   * Gets an IProjectFacetVersion version from maven-ear-plugin configuration.
   * 
   * @return the facet version of the project, Maven defaults to (Java EE) 1.3
   */
  public IProjectFacetVersion getEarFacetVersion() {
    Xpp3Dom config = getConfiguration();
    if(config == null) {
      return DEFAULT_EAR_FACET;
    }

    Xpp3Dom domVersion = config.getChild("version");
    if(domVersion != null) {
      String sVersion = domVersion.getValue();
      try {
        double version = Double.parseDouble(sVersion); // hack to transform version 5 to 5.0
        // IJ2EEFacetConstants.ENTERPRISE_APPLICATION_FACET.getVersion(String version) is available in WTP 3.x
        return WTPProjectsUtil.EAR_FACET.getVersion(Double.toString(version));
      } catch(NumberFormatException nfe) {
        MavenLogger.log("unable to read ear version : " + sVersion, nfe);
        return DEFAULT_EAR_FACET;
      }
    }
    return DEFAULT_EAR_FACET;
  }

  /**
   * Gets the ear content directory of the project from pom.xml configuration.
   * 
   * @return the first resource directory found in pom.xml.
   */
  public String getEarContentDirectory(IProject project) {
    Xpp3Dom config = getConfiguration();
    if(config != null) {
      Xpp3Dom contentDirDom = config.getChild("earSourceDirectory");
      if(contentDirDom != null && contentDirDom.getValue() != null) {
        String contentDir = contentDirDom.getValue().trim();
        
        //MNGECLIPSE-1600 fixed absolute earSourceDirectory
        if(project != null) {
          IPath projectLocationPath = project.getLocation();
          if(projectLocationPath != null) {
            String projectLocation = projectLocationPath.toOSString();
            if(contentDir.startsWith(projectLocation)) {
              return contentDir.substring(projectLocation.length());
            }
          }
        }
        contentDir = (contentDir.length() == 0) ? EAR_DEFAULT_CONTENT_DIR : contentDir;
        return contentDir;
      }
    }

    return EAR_DEFAULT_CONTENT_DIR;
  }

  /**
   * Return the default bundle directory, where jars will be deployed.
   */
  public String getDefaultBundleDirectory() {
    if(libDirectory == null) {
      Xpp3Dom config = getConfiguration();
      if(config != null) {
        Xpp3Dom libDom = config.getChild("defaultLibBundleDir");
        if(libDom != null) {
          String libDir = libDom.getValue().trim();
          libDirectory = (libDir == null || libDir.length() == 0) ? EAR_DEFAULT_BUNDLE_DIR : libDir;
        }
      }
      libDirectory = (libDirectory  == null)?EAR_DEFAULT_BUNDLE_DIR:libDirectory;
    }
    return libDirectory;
  }

  /**
   * Reads maven-ear-plugin configuration to build a set of EarModule.
   * 
   * @see org.apache.maven.plugin.ear.AbstractEarMojo
   * @return an unmodifiable set of EarModule
   */
  public Set<EarModule> getEarModules() throws EarPluginException {
    Set<Artifact> artifacts = mavenProject.getArtifacts();
    if(artifacts == null || artifacts.isEmpty()) {
      return Collections.<EarModule> emptySet();
    }

    Set<EarModule> earModules = new HashSet<EarModule>(artifacts.size());
    String defaultBundleDir = getDefaultBundleDirectory();
    EarModuleFactory earModuleFactory = EarModuleFactory.createEarModuleFactory(getArtifactTypeMappingService(),
        getFileNameMapping(), getMainArtifactId(), artifacts);

    //Resolve Ear modules from plugin config
    earModules.addAll(getEarModulesFromConfig(earModuleFactory, defaultBundleDir)); 

    ScopeArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME);

    //next, add remaining modules from maven project dependencies
    for(Artifact artifact : artifacts) {

      // If the artifact's type is POM, ignore and continue
      // since it's used for transitive deps only.
      if("pom".equals(artifact.getType())) {
        continue;
      }

      // Artifact is not yet registered and it has neither test, nor a
      // provided scope, nor is it optional
      if(!isArtifactRegistered(artifact, earModules) && filter.include(artifact) && !artifact.isOptional()) {
        EarModule module = earModuleFactory.newEarModule(artifact, defaultBundleDir);
        if(module != null) {
          earModules.add(module);
        }
      }
    }
    
    //Remove excluded artifacts 
    Iterator<EarModule> modulesIterator = earModules.iterator();
    while (modulesIterator.hasNext())
    {
      EarModule module = modulesIterator.next();
      if (module.isExcluded())
      {
        modulesIterator.remove();  
      }
    }

    return Collections.unmodifiableSet(earModules);
  }

  private String getMainArtifactId() {
    // TODO read xml config
    return "none";
  }

  private ArtifactTypeMappingService getArtifactTypeMappingService() throws EarPluginException {
    if(typeMappingService == null) {
      typeMappingService = new ArtifactTypeMappingService(getConfiguration());
    }
    return typeMappingService;
  }

  private FileNameMapping getFileNameMapping() {

    Xpp3Dom config = getConfiguration();
    if(config == null) {
      return FileNameMappingFactory.INSTANCE.getDefaultFileNameMapping();
    }

    Xpp3Dom fileNameMappingDom = config.getChild("fileNameMapping");
    if(fileNameMappingDom != null) {
      String fileNameMappingName = fileNameMappingDom.getValue().trim();
      return FileNameMappingFactory.INSTANCE.getFileNameMapping(fileNameMappingName);
    }
    return FileNameMappingFactory.INSTANCE.getDefaultFileNameMapping();
  }

  /**
   * Return a set of ear modules defined in maven-ear-plugin configuration.
   * 
   * @param earModuleFactory
   */
  private Set<EarModule> getEarModulesFromConfig(EarModuleFactory earModuleFactory, String defaultBundleDir) throws EarPluginException {
    Set<EarModule> earModules = new HashSet<EarModule>();
    Xpp3Dom configuration = getConfiguration();
    if(configuration == null) {
      return earModules;
    }
    Xpp3Dom modulesNode = configuration.getChild("modules");

    if(modulesNode == null) {
      return earModules;
    }

    Xpp3Dom[] domModules = modulesNode.getChildren();
    if(domModules == null || domModules.length == 0) {
      return earModules;
    }
    
    for(Xpp3Dom domModule : domModules) {
      EarModule earModule = earModuleFactory.newEarModule(domModule, defaultBundleDir);
      if(earModule != null) {
        earModules.add(earModule);
      }
    }
    
    return earModules;
  }

  private static boolean isArtifactRegistered(Artifact a, Set<EarModule> modules) {
    for(EarModule module : modules) {
      if(module.getArtifact().equals(a)) {
        return true;
      }
    }
    return false;
  }

  public boolean isGenerateApplicationXml()  {
    Xpp3Dom configuration = getConfiguration();
    if(configuration == null) {
      return true;
    }
    Xpp3Dom generateApplicationXmlNode = configuration.getChild("generateApplicationXml");
    return (generateApplicationXmlNode == null) || Boolean.parseBoolean(generateApplicationXmlNode.getValue());
  }
  
  public Set<SecurityRoleKey>  getSecurityRoleKeys() {
    Set<SecurityRoleKey> securityRoles = new HashSet<SecurityRoleKey>();
    Xpp3Dom configuration = getConfiguration();
    if(configuration == null) {
      return securityRoles;
    }
    Xpp3Dom securityNode = configuration.getChild("security");

    if(securityNode == null) {
      return securityRoles;
    }

    Xpp3Dom[] secRoles = securityNode.getChildren("security-role");
    if(secRoles == null || secRoles.length == 0) {
      return securityRoles;
    }
    
    for(Xpp3Dom domSecRole : secRoles) {
      String id = domSecRole.getAttribute("id");
      String description = DomUtils.getChildValue(domSecRole, "description");
      String roleName = DomUtils.getChildValue(domSecRole, "role-name");
      if (roleName != null)
      {
        SecurityRoleKey srk = new SecurityRoleKey();
        srk.setId(id);
        srk.setRoleName(roleName);
        srk.setDescription(description);
        securityRoles.add(srk);
      }
    }
    
    return securityRoles;
  }

}
