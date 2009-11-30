/*******************************************************************************

 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;


import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.maven.ide.eclipse.core.MavenConsole;
import org.maven.ide.eclipse.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.project.IMavenMarkerManager;
import org.maven.ide.eclipse.project.MavenProjectManager;

public abstract class AbstractDependencyConfigurator {

  public static final String ATTR_ID = "id";
  
  public static final String ATTR_PRIORITY = "priority";

  public static final String ATTR_NAME = "name";
  
  public static final String ATTR_CLASS = "class";
  
  private int priority;
  private String id;
  private String name;

  protected MavenProjectManager projectManager;
  protected MavenRuntimeManager runtimeManager;
  protected IMavenMarkerManager markerManager; 
  protected MavenConsole console;
  
  public void setProjectManager(MavenProjectManager projectManager) {
    this.projectManager = projectManager;
  }
  
  public void setRuntimeManager(MavenRuntimeManager runtimeManager) {
    this.runtimeManager = runtimeManager;
  }

  public void setMarkerManager(IMavenMarkerManager markerManager) {
    this.markerManager = markerManager;
  }
  
  public void setConsole(MavenConsole console) {
    this.console = console;
  }
  
  public abstract void configureDependency(MavenProject mavenProject, IProject mavenIProject, MavenProject dependencyProject, 
      IProject dependencyIProject, IProgressMonitor monitor) throws MarkedException;

  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    this.id = config.getAttribute(ATTR_ID);
    this.name = config.getAttribute(ATTR_NAME);
    String priorityString = config.getAttribute(ATTR_PRIORITY);
    try {
      priority = Integer.parseInt(priorityString);
    } catch (Exception ex) {
      priority = Integer.MAX_VALUE;
    }
  }

  protected void addNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
    if (!project.hasNature(natureId)) {
      IProjectDescription description = project.getDescription();
      String[] prevNatures = description.getNatureIds();
      String[] newNatures = new String[prevNatures.length + 1];
      System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
      newNatures[0] = natureId;
      description.setNatureIds(newNatures);
      project.setDescription(description, monitor);
    }
  }

  @Override
  public String toString() {
    return id + ":" + name + "(" + priority + ")";
  }

  public void init() {
    //do nothing here, extenders may override
  }
  
}
