/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.datamodel.features.types.MaldiSpotType;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2Processor;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.CenterFunction;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class FeatureResolverTask extends AbstractTask {

  // Logger.
  private static final Logger logger = Logger.getLogger(FeatureResolverTask.class.getName());

  // Feature lists.
  private final MZmineProject project;
  private final FeatureList originalPeakList;
  // User parameters
  private final ParameterSet parameters;
  // function to find center mz of all feature data points
  private final CenterFunction mzCenterFunction;
  private FeatureList newPeakList;
  // Counters.
  private int processedRows;
  private int totalRows;
  private String errorMsg;
  private boolean setMSMSRange, setMSMSRT;
  private double msmsRange;
  private float RTRangeMSMS;
  private GroupMS2Processor groupMS2Task;

  /**
   * Create the task.
   *
   * @param storage
   * @param list         feature list to operate on.
   * @param parameterSet task parameters.
   */
  public FeatureResolverTask(final MZmineProject project, MemoryMapStorage storage,
      final FeatureList list, final ParameterSet parameterSet, CenterFunction mzCenterFunction,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    // Initialize.
    this.project = project;
    parameters = parameterSet;
    originalPeakList = list;
    newPeakList = null;
    processedRows = 0;
    totalRows = 0;
    this.mzCenterFunction = mzCenterFunction;
  }

  @Override
  public String getTaskDescription() {
    if (groupMS2Task != null) {
      return groupMS2Task.getTaskDescription();
    }
    return "Feature recognition on " + originalPeakList;
  }

  @Override
  public double getFinishedPercentage() {
    if (groupMS2Task != null) {
      return groupMS2Task.getFinishedPercentage();
    }
    return totalRows == 0 ? 0.0 : processedRows / (double) totalRows;
  }

  @Override
  public void run() {
    errorMsg = null;

    if (!isCanceled()) {

      setStatus(TaskStatus.PROCESSING);
      logger.info("Started feature resolving on " + originalPeakList);

      // Check raw data files.
      if (originalPeakList.getNumberOfRawDataFiles() > 1) {

        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Feature resolving can only be performed on feature lists with a single raw data file");
      } else {
        try {
          if (((GeneralResolverParameters) parameters).getResolver(parameters,
              (ModularFeatureList) originalPeakList) != null) {
            dimensionIndependentResolve((ModularFeatureList) originalPeakList);
          } else {
            legacyResolve();
          }
          // resolving finished

          // sort and reset IDs here to ahve the same sorting for every feature list
          FeatureListUtils.sortByDefaultRT(newPeakList, true);

          // group MS2 with features
          var groupMs2Param = parameters.getParameter(GeneralResolverParameters.groupMS2Parameters);
          if (groupMs2Param.getValue()) {
            GroupMS2SubParameters ms2params = groupMs2Param.getEmbeddedParameters();
            groupMS2Task = new GroupMS2Processor(this, newPeakList, ms2params);
            // group all features with MS/MS
            groupMS2Task.process();
            groupMs2Param = null; // clear progress
          }

          if (!isCanceled()) {
            // add new list and remove old if requested
            final var handleOriginal = parameters.getValue(
                GeneralResolverParameters.handleOriginal);
            final String suffix = parameters.getValue(GeneralResolverParameters.SUFFIX);
            handleOriginal.reflectNewFeatureListToProject(suffix, project, newPeakList,
                originalPeakList);

            setStatus(TaskStatus.FINISHED);
            logger.info("Finished feature resolving on " + originalPeakList);
          }
        } catch (Exception e) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage(e.getMessage());
          logger.log(Level.SEVERE, "Feature resolving error: " + e.getMessage(), e);
        }

        // Report error.
        if (errorMsg != null) {
          setErrorMessage(errorMsg);
          setStatus(TaskStatus.ERROR);
        }
      }
    }
  }

  /**
   * Used for compatibility with old {@link FeatureResolver}s. New methods should implement
   * {@link Resolver}. See
   * {@link
   * io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolver}
   * as an example implementation.
   *
   */
  @Deprecated
  private void legacyResolve() {
    final FeatureResolver resolver = ((GeneralResolverParameters) parameters).getResolver();

    // Resolve features.
    newPeakList = resolvePeaks((ModularFeatureList) originalPeakList);
  }

  private void dimensionIndependentResolve(ModularFeatureList originalFeatureList) {
    final Resolver resolver = ((GeneralResolverParameters) parameters).getResolver(parameters,
        originalFeatureList);
    if (resolver == null) {
      setErrorMessage("Resolver could not be initialised.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);
    final ModularFeatureList resolvedFeatureList = createNewFeatureList(originalFeatureList);

    final FeatureDataAccess access = EfficientDataAccess.of(originalFeatureList,
        EfficientDataAccess.FeatureDataType.INCLUDE_ZEROS, dataFile);

    processedRows = 0;
    totalRows = originalFeatureList.getNumberOfRows();
    int peakId = 1;

    int c = 0;

    while (access.hasNextFeature()) {
      final ModularFeature originalFeature = (ModularFeature) access.nextFeature();
      final List<IonTimeSeries<? extends Scan>> resolvedSeries = resolver.resolve(access,
          getMemoryMapStorage());

      for (IonTimeSeries<? extends Scan> resolved : resolvedSeries) {
        final ModularFeatureListRow newRow = new ModularFeatureListRow(resolvedFeatureList,
            peakId++);
        final ModularFeature f = new ModularFeature(resolvedFeatureList,
            originalFeature.getRawDataFile(), resolved, originalFeature.getFeatureStatus());

        if (originalFeature.getMobilityUnit() != null) {
          f.set(MobilityUnitType.class, originalFeature.getMobilityUnit());
        }
        if (originalFeature.get(ImageType.class) != null) {
          f.set(ImageType.class, true);
        }
        if(originalFeature.get(MaldiSpotType.class) != null) {
          f.set(MaldiSpotType.class, originalFeature.get(MaldiSpotType.class));
        }
        newRow.addFeature(originalFeature.getRawDataFile(), f);
        resolvedFeatureList.addRow(newRow);
        if (resolved.getSpectra().size() <= 3) {
          c++;
        }
      }
      processedRows++;
    }
    logger.info(c + "/" + resolvedFeatureList.getNumberOfRows()
        + " have less than 4 scans (frames for IMS data)");
    //    QualityParameters.calculateAndSetModularQualityParameters(resolvedFeatureList);

    resolvedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(resolver.getModuleClass(), parameters,
            getModuleCallDate()));

    newPeakList = resolvedFeatureList;
  }

  @Override
  public void cancel() {
    super.cancel();
  }

  /**
   * This method is kept around to keep compatibility with resolvers implementing the legacy
   * interface {@link FeatureResolver}. All new resolvers should implement {@link Resolver} or
   * {@link AbstractResolver} instead.
   */
  @Deprecated
  private FeatureList resolvePeaks(final ModularFeatureList originalFeatureList) {

    final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);
    final ModularFeatureList resolvedFeatureList = createNewFeatureList(originalFeatureList);

    final FeatureResolver resolver = ((GeneralResolverParameters) parameters).getResolver();

    processedRows = 0;
    totalRows = originalFeatureList.getNumberOfRows();
    int peakId = 1;
    final Integer minNumDp = parameters.getValue(
        GeneralResolverParameters.MIN_NUMBER_OF_DATAPOINTS);

    for (int i = 0; i < totalRows; i++) {
      final ModularFeatureListRow originalRow = (ModularFeatureListRow) originalFeatureList.getRow(
          i);
      final ModularFeature originalFeature = originalRow.getFeature(dataFile);

      final ResolvedPeak[] peaks = resolver.resolvePeaks(originalFeature, parameters,
          mzCenterFunction, msmsRange, RTRangeMSMS);

      for (final ResolvedPeak peak : peaks) {
        if (peak.getScanNumbers().length < minNumDp) {
          continue;
        }
        peak.setParentChromatogramRowID(originalRow.getID());
        final ModularFeatureListRow newRow = new ModularFeatureListRow(resolvedFeatureList,
            peakId++);
        final ModularFeature newFeature = FeatureConvertors.ResolvedPeakToMoularFeature(
            resolvedFeatureList, peak, originalFeature.getFeatureData());
        if (originalFeature.getMobilityUnit() != null) {
          newFeature.set(MobilityUnitType.class, originalFeature.getMobilityUnit());
        }

        newRow.addFeature(dataFile, newFeature);
        newRow.setFeatureInformation(peak.getPeakInformation());
        resolvedFeatureList.addRow(newRow);
      }
      processedRows++;
    }

    resolvedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(resolver.getModuleClass(), parameters,
            getModuleCallDate()));

    return resolvedFeatureList;
  }

  private ModularFeatureList createNewFeatureList(ModularFeatureList originalFeatureList) {
    if (originalFeatureList.getRawDataFiles().size() > 1) {
      throw new IllegalArgumentException("Resolving cannot be applied to aligned feature lists.");
    }
    final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);

    // create a new feature list and don't copy. Previous annotations of features are invalidated
    // during resolution
    final ModularFeatureList resolvedFeatureList = new ModularFeatureList(
        originalFeatureList.getName() + " " + parameters.getParameter(
            GeneralResolverParameters.SUFFIX).getValue(), storage, dataFile);

    //    DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedFeatureList);
    resolvedFeatureList.setSelectedScans(dataFile, originalFeatureList.getSeletedScans(dataFile));

    // since we dont create a copy, we have to copy manually
    originalFeatureList.getAppliedMethods()
        .forEach(m -> resolvedFeatureList.getAppliedMethods().add(m));
    // the new method is added later, since we don't know here which resolver module is used.

    // check the actual feature data. IMSRawDataFiles can also be built as classic lc-ms features
    final Feature exampleFeature =
        originalFeatureList.getNumberOfRows() > 0 ? originalFeatureList.getRow(0).getBestFeature()
            : null;

    boolean isImagingFile = (originalFeatureList.getRawDataFile(0) instanceof ImagingRawDataFile);
    if (exampleFeature != null
        && exampleFeature.getFeatureData() instanceof IonMobilogramTimeSeries) {
      DataTypeUtils.addDefaultIonMobilityTypeColumns(resolvedFeatureList);
    }
    if (originalFeatureList.hasRowType(RTType.class) && !isImagingFile) {
      DataTypeUtils.addDefaultChromatographicTypeColumns(resolvedFeatureList);
    }
    if (isImagingFile) {
      DataTypeUtils.addDefaultImagingTypeColumns(resolvedFeatureList);
    }

    return resolvedFeatureList;
  }
}
