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
import org.eclipse.wst.common.componentcore.ModuleCoreNature;


/**
 * See http://maven.apache.org/plugins/maven-war-plugin/war-mojo.html
 * 
 * @author Igor Fedorenko
 */
class WarPluginConfiguration {

  private static final String WAR_PACKAGING = "war";

  static boolean isWarProject(MavenProject mavenProject) {
    return WAR_PACKAGING.equals(mavenProject.getPackaging());
  }

  static boolean isWTPProject(IProject project) {
    return ModuleCoreNature.getModuleCoreNature(project) != null;
  }
}
