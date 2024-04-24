/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.scans.similarity.impl.ms2deepscore;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.datamodel.Scan;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;

public class MS2DeepscoreModel extends EmbeddingBasedSimilarity {

  /**
   * Predicts the MS2Deepscore similarity
   */
  private final ZooModel<NDList, NDList> model;
  private final SettingsMS2Deepscore settings;
  private final SpectrumTensorizer spectrumTensorizer;

  public MS2DeepscoreModel(URI modelFilePath, URI settingsFilePath)
      throws ModelNotFoundException, MalformedModelException, IOException {
//        todo load settings as well.
    Criteria<NDList, NDList> criteria = Criteria.builder().setTypes(NDList.class, NDList.class)
        .optModelPath(Paths.get(modelFilePath))
        .optOption("mapLocation", "true") // this model requires mapLocation for GPU
        .optProgress(new ProgressBar()).build();
    this.model = criteria.loadModel();
    this.settings = loadSettings(settingsFilePath);
    if (!Arrays.deepToString(settings.additionalMetadata()).equals(
        "[[StandardScaler, {metadata_field=precursor_mz, mean=0.0, standard_deviation=1000.0}], [CategoricalToBinary, {metadata_field=ionmode, entries_becoming_one=positive, entries_becoming_zero=negative}]]")) {
      throw new RuntimeException(
          "The model uses an additional metadata format that is not supported. Please use the default MS2Deepscore model or ask the developers for support.");
    }
    this.spectrumTensorizer = new SpectrumTensorizer(settings);

  }

  private SettingsMS2Deepscore loadSettings(URI settingsFilePath) throws IOException {

    ObjectMapper mapper = new ObjectMapper();
    //    Allows skipping fields in json which are not in SettingsMS2Deepscore (the for us useless settings)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //    Makes sure that all the important settings were in the json (with the expected name)
    mapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    // JSON file to Java object
    return mapper.readValue(new File(settingsFilePath), SettingsMS2Deepscore.class);
  }

  public NDArray predictEmbeddingFromTensors(TensorizedSpectra tensorizedSpectra)
      throws TranslateException {
//    Todo This is an autoclosable object. Using a try block is suggested, but in that case the output is not available outside this function ... I am not sure about how to fix this.
    NDManager manager = NDManager.newBaseManager();
    Predictor<NDList, NDList> predictor = model.newPredictor();
    NDList predictions = predictor.predict(
        new NDList(manager.create(tensorizedSpectra.tensorizedFragments()),
            manager.create(tensorizedSpectra.tensorizedMetadata())));

    return predictions.getFirst();

  }

  public NDArray predictEmbedding(Scan[] scans) throws TranslateException {
    TensorizedSpectra tensorizedSepctra = spectrumTensorizer.tensorizeSpectra(scans);
    return predictEmbeddingFromTensors(tensorizedSepctra);
  }
}
