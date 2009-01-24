/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.internal;

import java.text.Collator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * WebResourcesSorter
 *
 * @author Eugene Kuleshov
 */
public class WebResourcesSorter extends ViewerSorter {

  public WebResourcesSorter() {
  }

  public WebResourcesSorter(Collator collator) {
    super(collator);
  }

  @Override
  public int category(Object element) {
    if(element instanceof WebResourcesNode) {
      return 0;
    }
    return 1;
  }
  
  public int compare(Viewer viewer, Object e1, Object e2) {
//    if(e1 instanceof WebResourcesNode) {
//      if(e2 instanceof IResource) {
//        return 1;
//      }
//    } else if(e2 instanceof WebResourcesNode) {
//      if(e2 instanceof IResource) {
//        return -1;
//      }
//    }
    return super.compare(viewer, e1, e2);
  }
  
}
