/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.maven.ide.eclipse.core.MavenLogger;


/**
 * @author Eugene Kuleshov
 */
public class WebResourcesImages {

  // object images

  // public static final Image IMG_WEB_RESOURCES = createImage("web-resources.gif");
  
  // descriptors
  
  public static final ImageDescriptor WEB_RESOURCES = create("web-resources.gif");
  
  
  private static ImageDescriptor create(String key) {
    try {
      ImageRegistry imageRegistry = getImageRegistry();
      if(imageRegistry != null) {
        ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(key);
        if(imageDescriptor==null) {
          imageDescriptor = createDescriptor(key);
          imageRegistry.put(key, imageDescriptor);
        }
        return imageDescriptor;
      }
    } catch(Exception ex) {
      MavenLogger.log(key, ex);
    }
    return null;
  }

  private static Image createImage(String key) {
    create(key);
    ImageRegistry imageRegistry = getImageRegistry();
    return imageRegistry == null ? null : imageRegistry.get(key);
  }

  private static ImageRegistry getImageRegistry() {
    MavenWtpPlugin plugin = MavenWtpPlugin.getDefault();
    return plugin == null ? null : plugin.getImageRegistry();
  }

  private static ImageDescriptor createDescriptor(String image) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(MavenWtpPlugin.ID, "icons/" + image);
  }

}
