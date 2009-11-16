
package org.maven.ide.eclipse.wtp;

import org.eclipse.core.resources.IProject;
import org.eclipse.jst.j2ee.internal.project.J2EEProjectUtilities;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.jst.j2ee.project.facet.IJ2EEFacetConstants;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * Utility class for WTP projects.
 * 
 * @author Fred Bricon
 */
@SuppressWarnings("restriction")
public class WTPProjectsUtil {

  public static final IProjectFacet UTILITY_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.UTILITY);

  public static final IProjectFacetVersion UTILITY_10 = UTILITY_FACET.getVersion("1.0");

  public static final IProjectFacet EJB_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.EJB);

  public static final IProjectFacet JCA_FACET = ProjectFacetsManager.getProjectFacet(IJ2EEFacetConstants.JCA);

  public static final IProjectFacet DYNAMIC_WEB_FACET = ProjectFacetsManager
      .getProjectFacet(IJ2EEFacetConstants.DYNAMIC_WEB);

  /**
   * Defaults Web facet version to 2.5
   */
  public static final IProjectFacetVersion DEFAULT_WEB_FACET = DYNAMIC_WEB_FACET.getVersion("2.5");

  public static final IProjectFacet EAR_FACET = ProjectFacetsManager
      .getProjectFacet(IJ2EEFacetConstants.ENTERPRISE_APPLICATION);

  /**
   * Checks if the project is one of Dynamic Web, EJB, Application client, EAR or JCA project.
   * @param project - the project to be checked.
   * @return true if the project is a JEE - or legacy J2EE - project (but not a utility project). 
   */
  public static boolean isJavaEEProject(IProject project) {
    return (J2EEProjectUtilities.isLegacyJ2EEProject(project) || J2EEProjectUtilities.isJEEProject(project)) && !JavaEEProjectUtilities.isUtilityProject(project); 
  }
  
}
