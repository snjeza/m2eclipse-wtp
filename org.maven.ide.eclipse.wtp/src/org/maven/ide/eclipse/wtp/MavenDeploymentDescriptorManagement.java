/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.core.IMavenConstants;
import org.maven.ide.eclipse.embedder.IMaven;
import org.maven.ide.eclipse.project.IMavenProjectFacade;
import org.maven.ide.eclipse.project.MavenProjectManager;

/**
 * Deployment Descriptor Management based on maven-ear-plugin
 *
 * @author Fred Bricon
 */
public class MavenDeploymentDescriptorManagement implements DeploymentDescriptorManagement {

  /**
   * Executes ear:generate-application-xml goal to generate application.xml (and jboss-app.xml if needed).
   * Existing files will be overwritten. 
   * @throws CoreException 
   */
  public void updateConfiguration(IProject project, MavenProject mavenProject, EarPluginConfiguration plugin, IProgressMonitor monitor) throws CoreException {

      MavenProjectManager projectManager = MavenPlugin.getDefault().getMavenProjectManager();
    
      IMavenProjectFacade mavenFacade =  projectManager.getProject(project);
      MavenExecutionPlan executionPlan = mavenFacade.getExecutionPlan(monitor);
      MojoExecution genConfigMojo = getExecution(executionPlan, "maven-ear-plugin", "generate-application-xml");
      if (genConfigMojo == null)
      {
        //TODO Better error management
        return;
      }
      //Let's force the generated config files location
      Xpp3Dom configuration = genConfigMojo.getConfiguration();
      if (configuration == null)
      {
        configuration = new Xpp3Dom("configuration");
        genConfigMojo.setConfiguration(configuration);
      }
      IFolder metaInfFolder = project.getFolder(plugin.getEarContentDirectory(project)+"/META-INF/");
      String generatedDescriptorLocation = metaInfFolder.getRawLocation().toOSString();
      Xpp3Dom genDescriptorLocationDom = configuration.getChild("generatedDescriptorLocation");
      if (genDescriptorLocationDom == null)
      {
        genDescriptorLocationDom = new Xpp3Dom("generatedDescriptorLocation"); 
        configuration.addChild(genDescriptorLocationDom);
      }
      genDescriptorLocationDom.setValue(generatedDescriptorLocation);

      //Create a maven request + session
      IMaven maven = MavenPlugin.getDefault().getMaven();
      IFile pomResource = project.getFile(IMavenConstants.POM_FILE_NAME);  
      
      //TODO check offline behavior, profiles
      MavenExecutionRequest request = projectManager.createExecutionRequest(pomResource, mavenFacade.getResolverConfiguration(), monitor);
      MavenSession session = maven.createSession(request, mavenProject);
      
      //Execute our hacked mojo 
      maven.execute(session, genConfigMojo, monitor);
      
      //Refresh the generated config files directory
      metaInfFolder.refreshLocal( IResource.DEPTH_ONE, monitor);
    }

    private MojoExecution getExecution(MavenExecutionPlan executionPlan, String artifactId, String goal) {
      for(MojoExecution execution : executionPlan.getExecutions()) {
        if(artifactId.equals(execution.getArtifactId()) && goal.equals(execution.getGoal())) {
          return execution;
        }
      }
      return null;
    }

}
