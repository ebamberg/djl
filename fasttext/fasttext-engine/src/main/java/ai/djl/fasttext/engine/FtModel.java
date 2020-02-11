/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package ai.djl.fasttext.engine;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Block;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.translate.Translator;
import ai.djl.util.PairList;
import com.github.jfasttext.FastTextWrapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * {@code FtModel} is the fastText implementation of {@link Model}.
 *
 * <p>FtModel contains all the methods in Model to load and process a model.
 */
public class FtModel implements Model {

    FastTextWrapper.FastTextApi fta;

    private Path modelDir;
    private String modelName;
    private Map<String, String> properties;

    /** Constructs a new Model. */
    FtModel() {
        fta = new FastTextWrapper.FastTextApi();
        properties = new ConcurrentHashMap<>();
    }

    /**
     * Loads the fastText model from a specified location.
     *
     * @param modelPath the directory of the model
     * @param modelName the name of the model
     * @param options load model options, see documentation for the specific engine
     * @throws IOException Exception for file loading
     */
    @Override
    public void load(Path modelPath, String modelName, Map<String, String> options)
            throws IOException, MalformedModelException {
        if (Files.notExists(modelPath)) {
            throw new FileNotFoundException(
                    "Model directory doesn't exist: " + modelPath.toAbsolutePath());
        }
        modelDir = modelPath.toAbsolutePath();
        this.modelName = modelName;

        Path modelFile = modelDir.resolve(modelName);
        if (Files.notExists(modelFile)) {
            if (modelName.endsWith(".ftz") || modelName.endsWith(".bin")) {
                throw new FileNotFoundException(
                        "Model file doesn't exist: " + modelFile.toAbsolutePath());
            }
            modelFile = modelDir.resolve(modelName + ".ftz");
            if (Files.notExists(modelFile)) {
                modelFile = modelDir.resolve(modelName + ".ftz");
                if (Files.notExists(modelFile)) {
                    throw new FileNotFoundException(
                            "Model " + modelName + " not found in directory " + modelDir);
                }
            }
        }

        String modelFilePath = modelFile.toString();
        if (!fta.checkModel(modelFilePath)) {
            throw new MalformedModelException("Malformed FastText model file:" + modelFilePath);
        }
        fta.loadModel(modelFilePath);

        properties.put("model-type", fta.getModelName().getString());
    }

    /** {@inheritDoc} */
    @Override
    public void save(Path modelDir, String modelName) {}

    /** {@inheritDoc} */
    @Override
    public Block getBlock() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setBlock(Block block) {
        throw new UnsupportedOperationException("Fasttext doesn't support Block.");
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return modelName;
    }

    /** {@inheritDoc} */
    @Override
    public Trainer newTrainer(TrainingConfig trainingConfig) {
        return new FtTrainer(this, trainingConfig);
    }

    /** {@inheritDoc} */
    @Override
    public <I, O> Predictor<I, O> newPredictor(Translator<I, O> translator) {
        return new FtPredictor<>(this, translator);
    }

    /** {@inheritDoc} */
    @Override
    public void setDataType(DataType dataType) {}

    /** {@inheritDoc} */
    @Override
    public DataType getDataType() {
        return DataType.UNKNOWN;
    }

    /** {@inheritDoc} */
    @Override
    public void cast(DataType dataType) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /** {@inheritDoc} */
    @Override
    public PairList<String, Shape> describeInput() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public PairList<String, Shape> describeOutput() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getArtifactNames() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public <T> T getArtifact(String name, Function<InputStream, T> function) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public URL getArtifact(String artifactName) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getArtifactAsStream(String name) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public NDManager getNDManager() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    void setModelFile(Path modelFile) {
        this.modelDir = modelFile;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        fta.unloadModel();
        fta.close();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("Model (\n\tName: ").append(modelName);
        if (modelDir != null) {
            sb.append("\n\tModel location: ").append(modelDir.toAbsolutePath());
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append("\n\t").append(entry.getKey()).append(": ").append(entry.getValue());
        }
        sb.append("\n)");
        return sb.toString();
    }
}