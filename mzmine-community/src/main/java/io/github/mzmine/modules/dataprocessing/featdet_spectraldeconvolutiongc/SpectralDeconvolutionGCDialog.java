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

package io.github.mzmine.modules.dataprocessing.featdet_spectraldeconvolutiongc;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.pseudospectrumvisualizer.PseudoSpectrumVisualizerPane;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.ComboComponent;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.fx.interaction.ChartMouseEventFX;
import org.jfree.chart.fx.interaction.ChartMouseListenerFX;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;

public class SpectralDeconvolutionGCDialog extends ParameterSetupDialog {

  private static final Color DOMAIN_MARKER_COLOR = new Color(200, 200, 255, 100);
  private static final Color TOLERANCE_MARKER_COLOR = new Color(255, 128, 0, 100);

  private final ParameterSet parameters;
  private final SplitPane paramPreviewSplit;
  private final SplitPane clusteringSelectedFeatureSplit;
  private final BorderPane previewWrapperPane;
  private final BorderPane pseudoSpectrumPaneWrapper;
  private final StackPane scatterPlotStackPane;
  private final Button updateButton;
  private final ComboComponent<Feature> deconvolutedFeaturesComboBox;
  private final Label numberOfCompoundsLabel;
  private final Label selectedFeatureGroupLabel;
  private final Label preparingPreviewLabel;
  private final SpectralDeconvolutionPreviewPlot scatterPlot;

  private FeatureList featureList;
  private SpectralDeconvolutionAlgorithm spectralDeconvolutionAlgorithm;
  private RTTolerance rtTolerance;
  private Integer minNumberOfSignals;
  private List<Range<Double>> mzValuesToIgnore;
  private PseudoSpectrumVisualizerPane pseudoSpectrumVisualizerPane;
  private List<ModularFeature> allFeatures;
  private List<List<ModularFeature>> groupedFeatures;
  private Feature closestFeatureGroup;

  public SpectralDeconvolutionGCDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    this.parameters = parameters;
    setMinWidth(1000);
    setMinHeight(1000);

    paramPreviewSplit = new SplitPane();
    paramPreviewSplit.getItems().add(getParamPane());
    paramPreviewSplit.setOrientation(Orientation.HORIZONTAL);
    paramPreviewSplit.setDividerPositions(0.4);
    mainPane.setCenter(paramPreviewSplit);

    previewWrapperPane = new BorderPane();
    pseudoSpectrumPaneWrapper = new BorderPane();
    updateButton = new Button("Update preview");
    updateButton.setOnAction(_ -> updatePreview());

    scatterPlot = new SpectralDeconvolutionPreviewPlot("Spectral Deconvolution", "Retention Time",
        "m/z");
    scatterPlotStackPane = new StackPane();
    scatterPlotStackPane.getChildren().add(scatterPlot);
    clusteringSelectedFeatureSplit = new SplitPane();
    clusteringSelectedFeatureSplit.getItems().add(scatterPlotStackPane);
    clusteringSelectedFeatureSplit.setOrientation(Orientation.VERTICAL);
    previewWrapperPane.setCenter(clusteringSelectedFeatureSplit);

    deconvolutedFeaturesComboBox = new ComboComponent<>(FXCollections.observableArrayList());
    HBox buttonBox = new HBox(updateButton, deconvolutedFeaturesComboBox);
    deconvolutedFeaturesComboBox.setOnAction(_ -> updateSelectedFeature());
    previewWrapperPane.setBottom(buttonBox);

    paramPreviewSplit.getItems().add(previewWrapperPane);
    numberOfCompoundsLabel = new Label("Number of compounds: ");
    selectedFeatureGroupLabel = new Label("Selected rt group: ");
    VBox labelVBox = new VBox(numberOfCompoundsLabel, selectedFeatureGroupLabel);
    buttonBox.getChildren().add(labelVBox);

    preparingPreviewLabel = new Label("Preparing preview");
    preparingPreviewLabel.setStyle("-fx-font-size: 24px;");
    preparingPreviewLabel.setVisible(false);
    scatterPlotStackPane.getChildren().add(preparingPreviewLabel);

    addMouseClickListenerToScatterPlot();
  }

  private void addMouseClickListenerToScatterPlot() {
    ChartViewer chartViewer = scatterPlot;
    chartViewer.addChartMouseListener(new ChartMouseListenerFX() {
      @Override
      public void chartMouseClicked(ChartMouseEventFX event) {
        XYPlot plot = (XYPlot) chartViewer.getChart().getPlot();
        double crosshairXValue = plot.getDomainCrosshairValue();
        double crosshairYValue = plot.getRangeCrosshairValue();
        handleCrosshairClick(crosshairXValue, crosshairYValue);
      }

      @Override
      public void chartMouseMoved(ChartMouseEventFX event) {
      }
    });
  }

  private void handleCrosshairClick(double rtValue, double mzValue) {
    Feature closestFeatureGroupNew = findClosestFeatureGroup(rtValue, mzValue);
    if (closestFeatureGroupNew != null && closestFeatureGroup != closestFeatureGroupNew) {
      closestFeatureGroup = closestFeatureGroupNew;
      deconvolutedFeaturesComboBox.getSelectionModel().select(closestFeatureGroup);
      updateSelectedFeature();
    }
  }

  private Feature findClosestFeatureGroup(double rtValue, double mzValue) {
    Feature closestFeature = allFeatures.stream().min(Comparator.comparingDouble(
            feature -> calculateEuclideanDistance(feature, rtValue, mzValue)))
        .orElse(null);

    if (closestFeature != null) {
      for (List<ModularFeature> features : groupedFeatures) {
        for (ModularFeature feature : features) {
          if (feature.equals(closestFeature)) {
            return SpectralDeconvolutionTools.getMainFeature(features, mzValuesToIgnore);
          }
        }
      }
    }
    return null;
  }

  private double calculateEuclideanDistance(Feature feature, double rtValue, double mzValue) {
    double rtDiff = feature.getRT() - rtValue;
    double mzDiff = feature.getMZ() - mzValue;
    return Math.sqrt(rtDiff * rtDiff + mzDiff * mzDiff);
  }

  private void updateSelectedFeature() {
    ModularFeature selectedFeature = (ModularFeature) deconvolutedFeaturesComboBox.getSelectionModel()
        .getSelectedItem();
    if (selectedFeature != null) {
      pseudoSpectrumVisualizerPane = new PseudoSpectrumVisualizerPane(selectedFeature);
      pseudoSpectrumPaneWrapper.setCenter(pseudoSpectrumVisualizerPane);
      scatterPlot.getChart().getXYPlot().clearDomainMarkers();
      scatterPlot.addIntervalMarker(selectedFeature.getRawDataPointsRTRange(), DOMAIN_MARKER_COLOR);
      scatterPlot.addIntervalMarker(rtTolerance.getToleranceRange(selectedFeature.getRT()),
          TOLERANCE_MARKER_COLOR);
      selectedFeatureGroupLabel.setText(
          "Selected rt group: " + MZmineCore.getConfiguration().getRTFormat()
              .format(selectedFeature.getRT()) + " min");
    }
  }

  private void updatePreview() {
    if (parameters.getValue(SpectralDeconvolutionGCParameters.FEATURE_LISTS)
        .getMatchingFeatureLists().length > 0) {

      initializeParameters();

      updateButton.setDisable(true);
      preparingPreviewLabel.setVisible(true);
      scatterPlot.setVisible(false);

      Task<List<List<ModularFeature>>> groupFeaturesTask = new Task<>() {
        @Override
        protected List<List<ModularFeature>> call() {
          return SpectralDeconvolutionTools.groupFeatures(spectralDeconvolutionAlgorithm,
              allFeatures, rtTolerance, minNumberOfSignals);
        }
      };

      groupFeaturesTask.setOnSucceeded(_ -> {
        groupedFeatures = groupFeaturesTask.getValue();
        populateScatterPlot();
        updateFeatureComboBox();
        updateSelectedFeature();
        updateButton.setDisable(false);
        preparingPreviewLabel.setVisible(false);
        scatterPlot.setVisible(true);
      });

      groupFeaturesTask.setOnFailed(_ -> {
        Throwable throwable = groupFeaturesTask.getException();
        MZmineCore.getDesktop()
            .displayErrorMessage("Error grouping features: " + throwable.getMessage());
        updateButton.setDisable(false);
        preparingPreviewLabel.setVisible(false);
        scatterPlot.setVisible(true);
      });

      Thread groupFeaturesThread = new Thread(groupFeaturesTask);
      groupFeaturesThread.setDaemon(true);
      groupFeaturesThread.start();
    } else {
      MZmineCore.getDesktop().displayMessage("No feature list selected. Cannot show preview");
    }
  }

  private void initializeParameters() {
    featureList = parameters.getParameter(SpectralDeconvolutionGCParameters.FEATURE_LISTS)
        .getValue().getMatchingFeatureLists()[0];
    allFeatures = featureList.getFeatures(featureList.getRawDataFile(0));
    spectralDeconvolutionAlgorithm = parameters.getValue(
        SpectralDeconvolutionGCParameters.SPECTRAL_DECONVOLUTION_ALGORITHM);
    rtTolerance = parameters.getValue(SpectralDeconvolutionGCParameters.RT_TOLERANCE);
    minNumberOfSignals = parameters.getValue(
        SpectralDeconvolutionGCParameters.MIN_NUMBER_OF_SIGNALS);
    if (parameters.getParameter(SpectralDeconvolutionGCParameters.MZ_VALUES_TO_IGNORE).getValue()) {
      mzValuesToIgnore = parameters.getParameter(
          SpectralDeconvolutionGCParameters.MZ_VALUES_TO_IGNORE).getEmbeddedParameter().getValue();
    } else {
      mzValuesToIgnore = null;
    }
  }

  private void populateScatterPlot() {
    Platform.runLater(() -> {
      scatterPlot.clearDatasets();
      Random random = new Random();
      for (List<ModularFeature> group : groupedFeatures) {
        XYSeries series = new XYSeries("Group " + MZmineCore.getConfiguration().getRTFormat()
            .format(group.getFirst().getRT()));
        for (ModularFeature feature : group) {
          series.add(feature.getRT(), feature.getMZ());
        }
        Color color = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
        scatterPlot.addDataset(series, color);
      }
    });
  }

  private void updateFeatureComboBox() {
    Task<ObservableList<Feature>> pseudoSpectraTask = new Task<>() {
      @Override
      protected ObservableList<Feature> call() {
        List<FeatureListRow> featureListRows = SpectralDeconvolutionTools.generatePseudoSpectra(
            allFeatures, featureList, rtTolerance, minNumberOfSignals,
            spectralDeconvolutionAlgorithm, mzValuesToIgnore);
        return featureListRows.stream().map(row -> row.getFeature(featureList.getRawDataFile(0)))
            .filter(Objects::nonNull).sorted(Comparator.comparingDouble(Feature::getRT))
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
      }
    };

    pseudoSpectraTask.setOnSucceeded(_ -> {
      ObservableList<Feature> resultFeatures = pseudoSpectraTask.getValue();
      Platform.runLater(() -> {
        deconvolutedFeaturesComboBox.setItems(resultFeatures);
        if (!resultFeatures.isEmpty()) {
          deconvolutedFeaturesComboBox.setValue(resultFeatures.getFirst());
        }
        if (!clusteringSelectedFeatureSplit.getItems().contains(pseudoSpectrumPaneWrapper)) {
          clusteringSelectedFeatureSplit.getItems().add(pseudoSpectrumPaneWrapper);
        }
        numberOfCompoundsLabel.setText("Number of compounds: " + resultFeatures.size());
      });
    });

    pseudoSpectraTask.setOnFailed(_ -> {
      Throwable throwable = pseudoSpectraTask.getException();
      Platform.runLater(() -> MZmineCore.getDesktop()
          .displayErrorMessage("Error generating pseudo spectra: " + throwable.getMessage()));
    });

    Thread pseudoSpectraThread = new Thread(pseudoSpectraTask);
    pseudoSpectraThread.setDaemon(true);
    pseudoSpectraThread.start();
  }

  @Override
  protected void parametersChanged() {
    updateParameterSetFromComponents();
    super.parametersChanged();
  }
}
