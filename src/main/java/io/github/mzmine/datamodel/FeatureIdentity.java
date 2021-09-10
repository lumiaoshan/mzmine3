/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel;

import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * This interface represents an identification result.
 */
public interface FeatureIdentity extends Cloneable {

  /**
   * These variables define standard properties. The PROPERTY_NAME must be present in all instances
   * of FeatureIdentity. It defines the value which is returned by the toString() method.
   */
  String PROPERTY_NAME = "Name";
  String PROPERTY_FORMULA = "Molecular formula";
  String PROPERTY_METHOD = "Identification method";
  String PROPERTY_ID = "ID";
  String PROPERTY_URL = "URL";
  String PROPERTY_SPECTRUM = "SPECTRUM";
  String PROPERTY_COMMENT = "Comment";
  String PROPERTY_ADDUCT ="Adduct";
  String PROPERTY_SMILES = "Smiles";
  
  /**
   * Returns the value of the PROPERTY_NAME property. This value must always be set. Same value is
   * returned by the toString() method.
   *
   * @return Name
   */
  @NotNull
  String getName();

  /**
   * Returns full, multi-line description of this identity, one property per line (key: value)
   *
   * @return Description
   */
  @NotNull
  String getDescription();

  /**
   * Returns the value for a
   *
   * @param property
   * @return Description
   */
  @NotNull
  String getPropertyValue(String property);

  /**
   * Returns all the properties in the form of a map key --> value
   *
   * @return Description
   */
  @NotNull
  Map<String, String> getAllProperties();

  @NotNull
  public Object clone();
}
