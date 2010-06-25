/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jst.j2ee.web.project.facet.WebFacetUtils;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.core.MavenLogger;

/**
 * Web resources content provider
 *
 * @author Eugene Kuleshov
 */
public class WebResourcesContentProvider extends BaseWorkbenchContentProvider implements ICommonContentProvider, IPipelinedTreeContentProvider {

  // ICommonContentProvider
  
  public void init(ICommonContentExtensionSite config) {
  }

  public void restoreState(IMemento memento) {
  }

  public void saveState(IMemento memento) {
  }

  public Object[] getChildren(Object element) {
    if(element instanceof WebResourcesNode) {
      return ((WebResourcesNode) element).getResources();
    }
    return super.getChildren(element);
  }

  // IPipelinedTreeContentProvider

  @SuppressWarnings("rawtypes")
  public void getPipelinedElements(Object element, Set currentElements) {
  }
  
  @SuppressWarnings("unchecked")
  public void getPipelinedChildren(Object parent, Set currentChildren) {
    if (parent instanceof IProject) {
      IProject project = (IProject) parent;
      if(project.isAccessible()) {
        try {
          IFacetedProject facetedProject = ProjectFacetsManager.create(project);//MNGECLIPSE-1992 there's no reason to actually create a ProjectFacet at this point
          if(facetedProject != null && facetedProject.hasProjectFacet(WebFacetUtils.WEB_FACET)) {
            List newChildren = new ArrayList<Object>();
            newChildren.add(new WebResourcesNode(project));
            newChildren.addAll(currentChildren);
            currentChildren.clear();
            currentChildren.addAll(newChildren);
          }
        } catch(CoreException ex) {
          MavenLogger.log(ex);
        }
      }
    }
  }

  public Object getPipelinedParent(Object element, Object suggestedParent) {
    return suggestedParent;
  }

  public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
    return false;
  }
  
  public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
    return false;
  }
  
  public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {
    return addModification;
  }

  public PipelinedShapeModification interceptRemove(PipelinedShapeModification removeModification) {
    return removeModification;
  }
  
}

