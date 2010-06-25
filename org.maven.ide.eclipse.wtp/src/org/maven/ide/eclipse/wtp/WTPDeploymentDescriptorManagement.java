/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.common.CommonFactory;
import org.eclipse.jst.j2ee.model.IEARModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.javaee.application.Application;
import org.eclipse.jst.javaee.application.ApplicationFactory;
import org.eclipse.jst.javaee.application.Module;
import org.eclipse.jst.javaee.core.JavaeeFactory;
import org.eclipse.jst.javaee.core.SecurityRole;
import org.eclipse.wst.common.componentcore.internal.StructureEdit;
import org.maven.ide.eclipse.wtp.earmodules.EarModule;
import org.maven.ide.eclipse.wtp.earmodules.JarModule;
import org.maven.ide.eclipse.wtp.earmodules.SecurityRoleKey;
import org.maven.ide.eclipse.wtp.earmodules.WebModule;

/**
 * Deployment Descriptor management based on WTP API.
 *
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class WTPDeploymentDescriptorManagement implements DeploymentDescriptorManagement {

  public void updateConfiguration(final IProject project, final  MavenProject mavenProject, final EarPluginConfiguration plugin, final
      IProgressMonitor monitor) throws CoreException {

      final Set<SecurityRoleKey> securityRoleKeys = plugin.getSecurityRoleKeys();
      final Set<EarModule> earModules = plugin.getEarModules();
      
      IEARModelProvider earModel = (IEARModelProvider)ModelProviderManager.getModelProvider(project);
      StructureEdit se = null;
      try {
        se = StructureEdit.getStructureEditForWrite(project);
        earModel.modify(new Runnable() {
          public void run() {
            Object modelObject = ModelProviderManager.getModelProvider(project).getModelObject();
            if (modelObject instanceof Application){
               configureJavaEE((Application)modelObject, securityRoleKeys, earModules);
            } else if (modelObject instanceof org.eclipse.jst.j2ee.application.Application){
               configureJ2EE((org.eclipse.jst.j2ee.application.Application)modelObject, securityRoleKeys, earModules);
            }
          }
       }, null);
      } finally {
        if (se != null) {
          se.saveIfNecessary(monitor);
          se.dispose();
        }
      }
    }

  /**
   * Configure a JavaEE Ear application.xml
   * @param app
   * @param securityRoleKeys
   * @param earModules
   */
  private void configureJavaEE(Application app, final Set<SecurityRoleKey> securityRoleKeys, final Set<EarModule> earModules) {
    //Configure security roles
    List<SecurityRole> securityRoles = app.getSecurityRoles();
    for (SecurityRoleKey srk : securityRoleKeys)
    {
      SecurityRole securityRole = findJavaEESecurityRole(srk.getRoleName(), securityRoles);
      if (securityRole == null){
        securityRole = JavaeeFactory.eINSTANCE.createSecurityRole();
        securityRoles.add(securityRole);  
        securityRole.setRoleName(srk.getRoleName());
      }
      securityRole.setId(srk.getId());
    }
    //TODO Remove roles unknown to maven ?

    //Update modules
    for (EarModule earModule : earModules)
    {
      Module module = getSanitizedJavaEEModule(app, earModule);
      if (module != null)
      {
        module.setAltDd(earModule.getAltDeploymentDescriptor());
        if (module.getWeb() != null && earModule instanceof WebModule)
        {
          module.getWeb().setContextRoot(((WebModule)earModule).getContextRoot());
        } else if (earModule instanceof JarModule){
          //Remove the JavaModule if needed as WTP has a tendency to automatically add a JavaModule
          //if a main class is defined in the jar's manifest
          JarModule jarModule = (JarModule)earModule;
          if (!jarModule.isIncludeInApplicationXml())
          {
            app.getModules().remove(module);
          }
        }
      } else if (earModule instanceof JarModule){
        JarModule jarModule = (JarModule)earModule;
        if (jarModule.isIncludeInApplicationXml())
        {
          //There's currently no JavaModule in application.xml but maven-ear-plugin tells us it must be there
          module = ApplicationFactory.eINSTANCE.createModule();
          module.setJava(earModule.getUri());
          module.setAltDd(earModule.getAltDeploymentDescriptor());
          app.getModules().add(module);
        }
      }
    }
  }
  
  /**
   * Configure a J2EE Ear application.xml
   * @param app
   * @param securityRoleKeys
   * @param earModules
   */
  private void configureJ2EE(org.eclipse.jst.j2ee.application.Application app, final Set<SecurityRoleKey> securityRoleKeys,
      final Set<EarModule> earModules) {
    //Code Duplication ... Boring!!!
    List<org.eclipse.jst.j2ee.common.SecurityRole> securityRoles = app.getSecurityRoles();
    for (SecurityRoleKey srk : securityRoleKeys)
    {
      org.eclipse.jst.j2ee.common.SecurityRole securityRole = findJ2EESecurityRole(srk.getRoleName(), securityRoles);
      if (securityRole == null){
        securityRole = CommonFactory.eINSTANCE.createSecurityRole();
        securityRoles.add(securityRole);  
        securityRole.setRoleName(srk.getRoleName());
      }
      securityRole.setDescription(srk.getDescription());
    }
    
    //Update modules
    for (EarModule earModule : earModules)
    {
      org.eclipse.jst.j2ee.application.Module module = getSanitizedJ2EEModule(app, earModule);
      if (module != null)
      {
        //Update Alt Deployment Descriptor 
        module.setAltDD(earModule.getAltDeploymentDescriptor());
        
        //Update context root if needed for web modules
        if (module.isWebModule() && earModule instanceof WebModule) {
          org.eclipse.jst.j2ee.application.WebModule webModule = (org.eclipse.jst.j2ee.application.WebModule)module;
          String newContextRoot = ((WebModule)earModule).getContextRoot(); 
          webModule.setContextRoot(newContextRoot);
        } //Remove from application.xml if needed (WTP adds deps as JavaModules if their Manifest.mf declares a Main-class (Java EE 5 specification page 167)) 
        else if (earModule instanceof JarModule){
          JarModule jarModule = (JarModule)earModule;
          if (!jarModule.isIncludeInApplicationXml())
          {
            app.getModules().remove(module);
          }
        }
      }//Add JavaModule as declared in maven-ear-plugin  
      else if (earModule instanceof JarModule){
        JarModule jarModule = (JarModule)earModule;
        if (jarModule.isIncludeInApplicationXml())
        {
          module = org.eclipse.jst.j2ee.application.ApplicationFactory.eINSTANCE.createJavaClientModule();
          module.setUri(earModule.getUri());
          module.setAltDD(earModule.getAltDeploymentDescriptor());
          app.getModules().add(module);
        }
      }
    }
  }


  /**
   * @param app
   * @param earModule
   * @return
   */
  @SuppressWarnings("unchecked")
  private static Module getSanitizedJavaEEModule(Application app, EarModule earModule) {
    
    String artifactPath = ArtifactHelper.getM2REPOVarPath(earModule.getArtifact());
    Module wtpModule = null;
    Module badWtpModule = null;
    String uri = earModule.getUri();
    for(Module module : (List<Module>)app.getModules())
    {
      if (uri.equals(module.getUri()))
      {
        wtpModule = module;
      } else
      //Baaad hack : iterate over remaining modules so we can workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=289038 
      if (artifactPath != null && module.getUri().contains(artifactPath)){
        badWtpModule = module;
      }
    }
    
    //Actual workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=289038 bad modules are removed if necessary, or correctly updated if needed 
    if (badWtpModule != null) {
      if (wtpModule == null)
      {
        if (badWtpModule.getWeb() != null){
          badWtpModule.getWeb().setWebUri(uri);
        } else if (badWtpModule.getEjb() != null) {
          badWtpModule.setEjb(uri);
        } else if (badWtpModule.getConnector() != null) {
          badWtpModule.setConnector(uri);
        } else {
          badWtpModule.setJava(uri);
        }
        wtpModule = badWtpModule;

      } else {
        app.getModules().remove(badWtpModule);  
      }
    }
    return wtpModule;
  }

  
  
  private static SecurityRole findJavaEESecurityRole(String roleName, List<SecurityRole> securityRoles) {
    if (roleName == null) return null;
    
    for (SecurityRole sRole : securityRoles) {
      if (roleName.equals(sRole.getRoleName())) {
        return sRole;
      }
    }
    return null;
  }

  
  private static org.eclipse.jst.j2ee.application.Module  getSanitizedJ2EEModule(org.eclipse.jst.j2ee.application.Application app, EarModule earModule) {
    String artifactPath = ArtifactHelper.getM2REPOVarPath(earModule.getArtifact());
    org.eclipse.jst.j2ee.application.Module  wtpModule = null;
    org.eclipse.jst.j2ee.application.Module  badWtpModule = null;

    String uri  = earModule.getUri();
    for (org.eclipse.jst.j2ee.application.Module module : (List<org.eclipse.jst.j2ee.application.Module>)app.getModules())
    {
      if (uri.equals(module.getUri()))
      {
        wtpModule = module;
      } else
      //Baaad hack : iterate over remaining modules so we can workaround https://bugs.eclipse.org/bugs/show_bug.cgi?id=289038 
      if (artifactPath != null && module.getUri().contains(artifactPath)){
        badWtpModule = module;
      }
    }
                
    //Actual workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=289038 bad modules are removed if necessary, or correctly updated if needed
    if (badWtpModule != null) {
      if (wtpModule == null)
      {
        badWtpModule.setUri(uri);
        wtpModule = badWtpModule;
      } else {
        app.getModules().remove(badWtpModule);  
      }
    }
    return wtpModule;
  }
  
  private static org.eclipse.jst.j2ee.common.SecurityRole findJ2EESecurityRole(String roleName, List<org.eclipse.jst.j2ee.common.SecurityRole> securityRoles) {
    if (roleName == null) return null;
    
    for (org.eclipse.jst.j2ee.common.SecurityRole sRole : securityRoles) {
      if (roleName.equals(sRole.getRoleName())) {
        return sRole;
      }
    }
    return null;
  }

}
