/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

/**
 * WebResourcesLabelProvider
 *
 * @author Eugene Kuleshov
 */
public class WebResourcesLabelProvider extends WorkbenchLabelProvider implements ICommonLabelProvider {

  public void init(ICommonContentExtensionSite config) {
  }

  public void restoreState(IMemento memento) {
  }

  public void saveState(IMemento memento) {
  }

  public String getDescription(Object element) {
    if(element instanceof WebResourcesNode) {
      return "Web Resources";
    }
    return null;
  }
  
  protected String decorateText(String input, Object element) {
    // TODO Auto-generated method decorateText
    return super.decorateText(input, element);
  }
  
  protected ImageDescriptor decorateImage(ImageDescriptor input, Object element) {
    // TODO Auto-generated method decorateImage
    return super.decorateImage(input, element);
  }

}
