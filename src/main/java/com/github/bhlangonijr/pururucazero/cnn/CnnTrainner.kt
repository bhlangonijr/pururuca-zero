package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.numberOfPlanes
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.BatchNormalization
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.api.InvocationType
import org.deeplearning4j.optimize.listeners.EvaluativeListener
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer
import org.nd4j.linalg.learning.config.AdaDelta
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File

class CnnTrainner(
    private val modelPath: String,
    private val normalizerPath: String?
) {

    private val scaler: DataNormalization = NormalizerMinMaxScaler()
    fun train(
        trainDatasetIterator: PgnDatasetIterator,
        testDatasetIterator: PgnDatasetIterator,
        epochs: Int = 100,
        seed: Long = 123L
    ): Pair<Array<FloatArray>, FloatArray> {
        val outputNum = trainDatasetIterator.labelNames.size
        trainDatasetIterator.reset()
        testDatasetIterator.reset()
        normalizerPath?.let {
            scaler.fit(trainDatasetIterator)
            scaler.fit(testDatasetIterator)
            trainDatasetIterator.setPreProcessor(scaler)
            testDatasetIterator.setPreProcessor(scaler)
        }

        println("Building convolutional network...")
        val conf = NeuralNetConfiguration.Builder()
            .seed(seed)
            .updater(AdaDelta())
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .list()
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(5, 5)
                    .stride(1, 1)
                    .padding(1, 1)
                    .activation(Activation.LEAKYRELU)
                    .nIn(numberOfPlanes)
                    .nOut(64)
                    .build()
            )
            .layer(BatchNormalization())
            .layer(
                SubsamplingLayer.Builder()
                    .kernelSize(2, 2)
                    .stride(2, 2)
                    .poolingType(SubsamplingLayer.PoolingType.MAX)
                    .build()
            )
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(1, 1)
                    .stride(1, 1)
                    .padding(1, 1)
                    .activation(Activation.LEAKYRELU)
                    .nOut(32).build()
            )
            .layer(BatchNormalization())
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(5, 5)
                    .stride(1, 1)
                    .padding(1, 1)
                    .activation(Activation.LEAKYRELU)
                    .nOut(64).build()
            )
            .layer(BatchNormalization())
            .layer(
                SubsamplingLayer.Builder()
                    .kernelSize(2, 2)
                    .stride(2, 2)
                    .poolingType(SubsamplingLayer.PoolingType.MAX)
                    .build()
            )
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(1, 1)
                    .stride(1, 1)
                    .padding(1, 1)
                    .activation(Activation.LEAKYRELU)
                    .nOut(32).build()
            )
            .layer(BatchNormalization())
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(5, 5)
                    .stride(1, 1)
                    .padding(1, 1)
                    .activation(Activation.LEAKYRELU)
                    .nOut(256).build()
            )
            .layer(BatchNormalization())
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(1, 1)
                    .stride(1, 1)
                    .padding(1, 1)
                    .activation(Activation.LEAKYRELU)
                    .nOut(128).build()
            )
            .layer(BatchNormalization())
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(1, 1)
                    .stride(1, 1)
                    .padding(1, 1)
                    .activation(Activation.LEAKYRELU)
                    .nOut(outputNum).build()
            )
            .layer(BatchNormalization())
            .layer(
                SubsamplingLayer.Builder()
                    .kernelSize(2, 2)
                    .stride(2, 2)
                    .poolingType(SubsamplingLayer.PoolingType.AVG)
                    .build()
            )
            .layer(
                OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                    .name("output")
                    .nOut(outputNum)
                    .dropOut(0.8)
                    .activation(Activation.SOFTMAX)
                    .build()
            )
            .setInputType(
                InputType.convolutional(
                    Nd4jEncoder.size.toLong(),
                    Nd4jEncoder.size.toLong(),
                    numberOfPlanes.toLong()
                )
            )
            .build()
        val model = MultiLayerNetwork(conf)
        model.init()
        model.setListeners(
            ScoreIterationListener(1),
            EvaluativeListener(testDatasetIterator, 1, InvocationType.EPOCH_END)
        )
        println("Total num of params: ${model.numParams()}")
        model.fit(trainDatasetIterator, epochs)

        testDatasetIterator.reset()
        val eval: Evaluation = model.evaluate(testDatasetIterator)
        println(eval.stats())

        val modelPath = File(modelPath)
        ModelSerializer.writeModel(model, modelPath, true)
        println("Model has been saved in ${modelPath.path}")
        normalizerPath?.let {
            NormalizerSerializer.getDefault().write(scaler, it)
            println("Normalizer has been saved in $it")
        }
        testDatasetIterator.reset()
        val dataSet = testDatasetIterator.next()
        val predicts = model.output(dataSet.features).toFloatMatrix()

        val matrix = dataSet.labels.toFloatMatrix()
        val labels = FloatArray(matrix.size) { idx ->
            matrix[idx].indexOfFirst { it == 1f }.toFloat()
        }
        return Pair(
            predicts,
            labels
        )

    }
}