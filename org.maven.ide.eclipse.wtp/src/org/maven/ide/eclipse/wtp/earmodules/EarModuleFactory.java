/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.earmodules;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.maven.ide.eclipse.wtp.DomUtils.getBooleanChildValue;
import static org.maven.ide.eclipse.wtp.DomUtils.getChildValue;

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.maven.ide.eclipse.wtp.earmodules.output.FileNameMapping;
import org.maven.ide.eclipse.wtp.earmodules.output.FileNameMappingFactory;


/**
 * Builds an {@link EarModule} based on an <tt>Artifact</tt>.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 */
public final class EarModuleFactory {
  
  private ArtifactTypeMappingService artifactTypeMappingService;

  private FileNameMapping fileNameMapping;

  private ArtifactRepository artifactRepository;

  private EarModuleFactory(ArtifactTypeMappingService artifactTypeMappingService, FileNameMapping fileNameMapping,
      ArtifactRepository artifactRepository) {
    this.artifactTypeMappingService = artifactTypeMappingService;
    this.artifactRepository = artifactRepository;
    this.fileNameMapping = fileNameMapping;
  }

  public static EarModuleFactory createEarModuleFactory(ArtifactTypeMappingService artifactTypeMappingService,
      FileNameMapping fileNameMapping, String mainArtifactId, Set<Artifact> artifacts) throws EarPluginException {
    if(artifactTypeMappingService == null) {
      artifactTypeMappingService = new ArtifactTypeMappingService(null);
    }
    if(fileNameMapping == null) {
      fileNameMapping = FileNameMappingFactory.INSTANCE.getDefaultFileNameMapping();
    }
    ArtifactRepository artifactRepository = new ArtifactRepository(artifacts, mainArtifactId,
        artifactTypeMappingService);

    return new EarModuleFactory(artifactTypeMappingService, fileNameMapping, artifactRepository);
  }

  /**
   * Creates a new {@link EarModule} based on the specified {@link Artifact} and the specified execution configuration.
   * 
   * @param artifact the artifact
   * @param defaultLibBundleDir the default bundle dir for {@link JarModule}
   * @return an ear module for this artifact
   */
  public EarModule newEarModule(Artifact artifact, String defaultLibBundleDir) throws UnknownArtifactTypeException {
    // Get the standard artifact type based on default config and user-defined mapping(s)
    final String artifactType = artifactTypeMappingService.getStandardType(artifact.getType());
    AbstractEarModule earModule = null;
    if("jar".equals(artifactType)) {
      earModule = new JarModule(artifact);
      ((JarModule)earModule).setLibBundleDir(defaultLibBundleDir);
    } else if("ejb".equals(artifactType) || "ejb3".equals(artifactType)) {
      earModule  = new EjbModule(artifact);
    } else if("par".equals(artifactType)) {
      earModule  = new ParModule(artifact);
    } else if("ejb-client".equals(artifactType)) {
      earModule  = new EjbClientModule(artifact);
    } else if("rar".equals(artifactType)) {
      earModule  = new RarModule(artifact);
    } else if("war".equals(artifactType)) {
      earModule  = new WebModule(artifact);
    } else if("sar".equals(artifactType)) {
      earModule  = new SarModule(artifact);
    } else if("wsr".equals(artifactType)) {
      earModule  = new WsrModule(artifact);
    } else if("har".equals(artifactType)) {
      earModule  = new HarModule(artifact);
    } else {
      throw new IllegalStateException("Could not handle artifact type[" + artifactType + "]");
    }

    earModule.setBundleFileName(fileNameMapping.mapFileName(artifact));
    
    return earModule;

  }

  public EarModule newEarModule(Xpp3Dom domModule, String defaultLibBundleDir) throws EarPluginException {
    String artifactType = domModule.getName();
    String groupId      = getChildValue(domModule, "groupId");
    String artifactId   = getChildValue(domModule, "artifactId");
    String classifier   = getChildValue(domModule, "classifier");
    
    AbstractEarModule earModule = null;
    // Get the standard artifact type based on default config and user-defined mapping(s)
    if ( "jarModule".equals(artifactType) || "javaModule".equals(artifactType)) {
      JarModule jarModule = new JarModule();
      jarModule.setIncludeInApplicationXml(getBooleanChildValue(domModule, "includeInApplicationXml"));
      earModule = jarModule;
    } 
    else if ( "ejbModule".equals(artifactType) || "ejb3Module".equals(artifactType)) {
      earModule  = new EjbModule();
    } 
    else if ( "webModule".equals(artifactType)){
      WebModule webModule  = new WebModule();
      webModule.setContextRoot(getChildValue(domModule, "contextRoot"));
      earModule = webModule;
    }
    else if ( "parModule".equals(artifactType)){
      earModule = new ParModule();
    }
    else if ( "ejbClientModule".equals(artifactType)){
      earModule = new EjbClientModule();
    }
    else if ( "rarModule".equals(artifactType)){
      earModule = new RarModule();
    }
    else if ( "warModule".equals(artifactType)){
      earModule = new WebModule();
    }
    else if ( "sarModule".equals(artifactType)){
      earModule = new SarModule();
    }
    else if ( "wsrModule".equals(artifactType)){
      earModule = new WsrModule();
    }
    else if ( "harModule".equals(artifactType)){
      earModule = new HarModule();
    }
    else {
        throw new IllegalStateException( "Could not handle artifact type[" + artifactType + "]" );
    }
    
    Artifact artifact   = artifactRepository.resolveArtifact(groupId, artifactId, earModule.getType(), classifier);
    earModule.setArtifact(artifact);
    
    earModule.setBundleDir(getChildValue(domModule, "bundleDir"));
    //To this point, we're sure to have a valid earModule ...
    String bundleFileName  = getChildValue(domModule, "bundleFileName");
    if (null==bundleFileName){
      bundleFileName = fileNameMapping.mapFileName(artifact);
    }
    earModule.setBundleFileName(bundleFileName);
    earModule.setUri(getChildValue(domModule, "uri"));
    earModule.setExcluded(getBooleanChildValue(domModule, "excluded"));

    //The following will be ignored by WTP - so far
    String unpack = getChildValue(domModule, "unpack");
    earModule.setShouldUnpack(unpack==null?null:Boolean.valueOf(unpack));
    earModule.setAltDeploymentDescriptor(getChildValue(domModule, "altDeploymentDescriptor"));
    
    return earModule;
  }

}
