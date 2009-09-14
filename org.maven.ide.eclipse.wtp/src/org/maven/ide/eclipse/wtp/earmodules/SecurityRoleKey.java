/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.wtp.earmodules;


/**
 * SecurityRoleKey
 *
 * @author Fred Bricon
 */
public class SecurityRoleKey {
  private String id;
  
  private String roleName;
  
  private String description;
  
  /**
   * @return Returns the id.
   */
  public String getId() {
    return id;
  }

  /**
   * @param id The id to set.
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * @return Returns the roleName.
   */
  public String getRoleName() {
    return roleName;
  }

  /**
   * @param roleName The roleName to set.
   */
  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  /**
   * @return Returns the description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description The description to set.
   */
  public void setDescription(String description) {
    this.description = description;
  }
  
}
