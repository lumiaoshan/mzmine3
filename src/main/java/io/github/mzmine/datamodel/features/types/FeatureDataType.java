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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ListRowBinding;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureDataType extends
    DataType<ObjectProperty<IonTimeSeries<? extends Scan>>> implements NoTextColumn,
    NullColumnType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "feature_data";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Feature data";
  }

  @Override
  public ObjectProperty<IonTimeSeries<? extends Scan>> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @NotNull
  @Override
  public List<RowBinding> createDefaultRowBindings() {
    // listen to changes in DataPointsType for all ModularFeatures
    return List.of(new ListRowBinding(new FeatureShapeType(), this));
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if (!(value instanceof IonTimeSeries series)) {
      return;
    }
    if (file == null) {
      throw new IllegalArgumentException("Cannot save feature data for file = null");
    }
    final List<? extends Scan> selectedScans = flist.getSeletedScans(file);
    if (selectedScans == null) {
      throw new IllegalArgumentException("Cannot find selected scans.");
    }

    writer.writeStartElement(getUniqueID());
    series.saveValueToXML(writer, selectedScans);
    writer.writeEndElement();
  }

  @Override
  public Object loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    return super.loadFromXML(reader, flist, row, feature, file);
  }
}
