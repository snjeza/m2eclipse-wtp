/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.internal;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Maven WTP plugin
 *
 * @author Eugene Kuleshov
 */
public class MavenWtpPlugin extends AbstractUIPlugin {

  public static String ID = "org.maven.ide.eclipse.wtp";
  
  private static MavenWtpPlugin instance;

  public MavenWtpPlugin() {
    instance = this;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
  }
  
  public static MavenWtpPlugin getDefault() {
    return instance;
  }
  
}
