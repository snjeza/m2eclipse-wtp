/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class DomUtils {

  /**
   * Return the value of a child node.
   * @param parent - the parent node.
   * @param childName - the child node name.
   * @return the child node value or null if it doesn't exist.
   */
  public static final String getChildValue(Xpp3Dom parent, String childName) {
    String result = null;
    if (parent != null && childName != null) {
      Xpp3Dom dom = parent.getChild(childName);
      if (dom != null) {
        result = dom.getValue().trim();
      }
    }
    return result;
  }

  public static final boolean getBooleanChildValue(Xpp3Dom parent, String childName) {
    return Boolean.valueOf(getChildValue(parent, childName));
  }
}
