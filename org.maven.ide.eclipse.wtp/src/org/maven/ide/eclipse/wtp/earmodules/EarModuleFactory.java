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
public final class EarModuleFactory
{
    private ArtifactTypeMappingService artifactTypeMappingService;
    private FileNameMapping fileNameMapping;
    private ArtifactRepository artifactRepository;
    
    private EarModuleFactory(ArtifactTypeMappingService artifactTypeMappingService, FileNameMapping fileNameMapping, ArtifactRepository artifactRepository) {
      this.artifactTypeMappingService = artifactTypeMappingService;
      this.artifactRepository = artifactRepository;
      this.fileNameMapping = fileNameMapping;
    }
    
    public static EarModuleFactory createEarModuleFactory(ArtifactTypeMappingService artifactTypeMappingService, FileNameMapping fileNameMapping, String mainArtifactId, Set<Artifact> artifacts) throws EarPluginException {
      if (artifactTypeMappingService == null) {
        artifactTypeMappingService = new ArtifactTypeMappingService(null);
      }
      if (fileNameMapping == null) {
        fileNameMapping = FileNameMappingFactory.INSTANCE.getDefaultFileNameMapping(); 
      }
      ArtifactRepository artifactRepository = new ArtifactRepository(artifacts, mainArtifactId, artifactTypeMappingService);
      
      return new EarModuleFactory(artifactTypeMappingService, fileNameMapping, artifactRepository );
    }
    
    /**
     * Creates a new {@link EarModule} based on the
     * specified {@link Artifact} and the specified
     * execution configuration.
     *
     * @param artifact             the artifact
     * @param defaultLibBundleDir the default bundle dir for {@link JarModule}
     * @return an ear module for this artifact
     */
    public EarModule newEarModule( Artifact artifact, String defaultLibBundleDir)
        throws UnknownArtifactTypeException
    {
        // Get the standard artifact type based on default config and user-defined mapping(s)
        final String artifactType = artifactTypeMappingService.getStandardType( artifact.getType());
        String bundleFileName = fileNameMapping.mapFileName( artifact );

        if ( "jar".equals( artifactType ) )
        {
            return new JarModule( artifact, defaultLibBundleDir, bundleFileName );
        }
        else if ( "ejb".equals( artifactType ) )
        {
            return new EjbModule( artifact,  bundleFileName);
        }
        else if ( "ejb3".equals( artifactType ) )
        {
            return new Ejb3Module( artifact,  bundleFileName );
        }
        else if ( "par".equals( artifactType ) )
        {
            return new ParModule( artifact,  bundleFileName );
        }
        else if ( "ejb-client".equals( artifactType ) )
        {
            return new EjbClientModule( artifact, null, bundleFileName );
        }
        else if ( "rar".equals( artifactType ) )
        {
            return new RarModule( artifact,  bundleFileName );
        }
        else if ( "war".equals( artifactType ) )
        {
            return new WebModule( artifact,  bundleFileName );
        }
        else if ( "sar".equals( artifactType ) )
        {
            return new SarModule( artifact,  bundleFileName );
        }
        else if ( "wsr".equals( artifactType ) )
        {
            return new WsrModule( artifact,  bundleFileName );
        }
        else if ( "har".equals( artifactType ) )
        {
            return new HarModule( artifact,  bundleFileName );
        }
        else
        {
            throw new IllegalStateException( "Could not handle artifact type[" + artifactType + "]" );
        }
    }

    //XXX needs to be implemented
    public EarModule newEarModule(Xpp3Dom domModule, String defaultLib) throws EarPluginException {
      String groupId= getChildValue(domModule, "groupId");
      String artifactId= getChildValue(domModule, "artifactId");
      String type= getChildValue(domModule, "type");
      String classifier = getChildValue(domModule, "classifier");

      Artifact artifact = artifactRepository.resolveArtifact(groupId, artifactId, type, classifier);
   
      // Get the standard artifact type based on default config and user-defined mapping(s)
      //XXX need to create earmodules from xpp3dom
      /*
      if ( "jarModule".equals( artifactType ) || "javaModule".equals( artifactType ))
      {
          return new JarModule( artifact, defaultLib);
      }
      else if ( "ejbModule".equals( artifactType ) )
      {
          return new EjbModule( artifact );
      }
      else if ( "ejb3Module".equals( artifactType ) )
      {
          return new Ejb3Module( artifact );
      }
      else if ( "warModule".equals( artifactType ) )
      {
          return new WebModule( artifact );
      }
      else if ( "parModule".equals( artifactType ) )
      {
          return new ParModule( artifact );
      }
      else if ( "ejbClientModule".equals( artifactType ) )
      {
          return new EjbClientModule( artifact, null );
      }
      else if ( "rarModule".equals( artifactType ) )
      {
          return new RarModule( artifact );
      }
      else if ( "warModule".equals( artifactType ) )
      {
          return new WebModule( artifact );
      }
      else if ( "sarModule".equals( artifactType ) )
      {
          return new SarModule( artifact );
      }
      else if ( "wsrModule".equals( artifactType ) )
      {
          return new WsrModule( artifact );
      }
      else if ( "harModule".equals( artifactType ) )
      {
          return new HarModule( artifact );
      }
      else
      {
          throw new IllegalStateException( "Could not handle artifact type[" + artifactType + "]" );
      }
      */
      
      return newEarModule(artifact, defaultLib);
   }
}
