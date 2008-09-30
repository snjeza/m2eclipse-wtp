/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.earmodules;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.maven.ide.eclipse.core.IMavenConstants;


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

/**
 * The base exception of the EAR plugin.
 * 
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author Fred Bricon
 */
public class EarPluginException extends CoreException {
  private static final long serialVersionUID = -819727447130647982L;

  private static final String DEFAULT_MESSAGE = "Error in ear plugin configuration";

  public EarPluginException() {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, DEFAULT_MESSAGE));
  }

  public EarPluginException(String message) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, message));
  }

  public EarPluginException(Throwable cause) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, DEFAULT_MESSAGE, cause));
  }

  public EarPluginException(String message, Throwable cause) {
    super(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, message, cause));
  }
}
