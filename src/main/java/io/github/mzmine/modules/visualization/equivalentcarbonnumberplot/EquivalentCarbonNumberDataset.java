package io.github.mzmine.modules.visualization.equivalentcarbonnumberplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;

public class EquivalentCarbonNumberDataset extends AbstractXYDataset implements XYDataset {

  private double[] xValues;
  private double[] yValues;
  private final FeatureListRow[] lipidRows;
  private final List<FeatureListRow> selectedRows;
  private final List<MatchedLipid> matchedLipids = new ArrayList<>();
  private final ILipidClass selectedLipidClass;
  private final int selectedDBENumber;
  private List<MatchedLipid> lipidsForDBE;

  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();


  public EquivalentCarbonNumberDataset(List<FeatureListRow> selectedRows,
      FeatureListRow[] lipidRows, ILipidClass selectedLipidClass, int selectedDBENumber) {
    this.selectedRows = selectedRows;
    this.lipidRows = lipidRows;
    this.selectedLipidClass = selectedLipidClass;
    this.selectedDBENumber = selectedDBENumber;
    for (FeatureListRow featureListRow : lipidRows) {
      if (featureListRow instanceof ModularFeatureListRow) {
        matchedLipids.add(featureListRow.get(LipidMatchListType.class).get(0));
      }
    }
    initDataset();
  }

  private void initDataset() {
    Map<ILipidClass, Map<Integer, List<MatchedLipid>>> groupedLipids = matchedLipids.stream()
        .collect(
            Collectors.groupingBy(matchedLipid -> matchedLipid.getLipidAnnotation().getLipidClass(),
                Collectors.groupingBy(matchedLipid -> {
                  ILipidAnnotation lipidAnnotation = matchedLipid.getLipidAnnotation();
                  if (lipidAnnotation instanceof MolecularSpeciesLevelAnnotation molecularAnnotation) {
                    return MSMS_LIPID_TOOLS.getCarbonandDBEFromLipidAnnotaitonString(
                        molecularAnnotation.getAnnotation()).getValue();
                  } else if (lipidAnnotation instanceof SpeciesLevelAnnotation) {
                    return MSMS_LIPID_TOOLS.getCarbonandDBEFromLipidAnnotaitonString(
                        lipidAnnotation.getAnnotation()).getValue();
                  } else {
                    return -1;
                  }
                }, Collectors.toList())));

    Map<Integer, List<MatchedLipid>> lipidsOfClass = groupedLipids.get(selectedLipidClass);

    if (lipidsOfClass != null) {
      lipidsForDBE = lipidsOfClass.get(selectedDBENumber);

      if (lipidsForDBE != null) {
        xValues = new double[lipidsForDBE.size()];
        yValues = new double[lipidsForDBE.size()];

        for (int i = 0; i < lipidsForDBE.size(); i++) {
          // get number of Carbons
          ILipidAnnotation lipidAnnotation = lipidsForDBE.get(i).getLipidAnnotation();
          if (lipidAnnotation instanceof MolecularSpeciesLevelAnnotation molecularAnnotation) {
            yValues[i] = MSMS_LIPID_TOOLS.getCarbonandDBEFromLipidAnnotaitonString(
                molecularAnnotation.getAnnotation()).getKey();
          } else if (lipidAnnotation instanceof SpeciesLevelAnnotation) {
            yValues[i] = MSMS_LIPID_TOOLS.getCarbonandDBEFromLipidAnnotaitonString(
                lipidAnnotation.getAnnotation()).getKey();
          }
          for (FeatureListRow lipidRow : lipidRows) {
            List<MatchedLipid> featureLipids = lipidRow.get(LipidMatchListType.class);

            if (!featureLipids.isEmpty()) {
              MatchedLipid featureMatchedLipid = featureLipids.get(0);

              if (lipidsForDBE.get(i).equals(featureMatchedLipid)) {
                xValues[i] = lipidRow.getAverageRT();
                break;
              }
            }
          }

        }
      }
    }
  }


  @Override
  public int getSeriesCount() {
    return 1;
  }

  public Comparable<?> getRowKey(int row) {
    return selectedRows.get(row).toString();
  }

  @Override
  public Comparable getSeriesKey(int series) {
    return getRowKey(series);
  }


  public double[] getXValues() {
    return xValues;
  }

  @Override
  public int getItemCount(int series) {
    return xValues.length;
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }

  public MatchedLipid getMatchedLipid(int item) {
    return lipidsForDBE.get(item);
  }

}
