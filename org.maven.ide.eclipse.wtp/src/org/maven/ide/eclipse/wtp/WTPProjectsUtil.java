
package org.maven.ide.eclipse.wtp;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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

  private static boolean javaEE6Available;

  static {
    try {
      IJ2EEFacetConstants.class.getField("ENTERPRISE_APPLICATION_60");
      javaEE6Available = true;
    } catch(Throwable t) {
      javaEE6Available = false;
    }
  }
  
  /**
   * @return Returns the javaEE6Available.
   */
  public static boolean isJavaEE6Available() {
    return javaEE6Available;
  }
  
  
  /**
   * Checks if a project has a class in its classpath 
   * @param project : the workspace project
   * @param className : the fully qualified name of the class to search for
   * @return true if fullyQualifiedName is found in the project's classpath (provided the project is a JavaProject and its classpath has been set.)   
   */
  public static boolean hasInClassPath(IProject project, String className) {
    boolean result = false;
    if (project != null){
      IJavaProject javaProject = JavaCore.create(project);
      try {
        if (javaProject!= null && javaProject.findType(className)!=null){
         result = true; 
        }
      } catch(JavaModelException ex) {
        //Ignore this
      }
    }
    return result;
  }

  
  /**
   * Checks if the project is one of Dynamic Web, EJB, Application client, EAR or JCA project.
   * @param project - the project to be checked.
   * @return true if the project is a JEE - or legacy J2EE - project (but not a utility project). 
   */
  public static boolean isJavaEEProject(IProject project) {
    return (J2EEProjectUtilities.isLegacyJ2EEProject(project) || J2EEProjectUtilities.isJEEProject(project)) && !JavaEEProjectUtilities.isUtilityProject(project); 
  }
  
}
