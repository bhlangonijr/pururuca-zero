package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.numberOfPlanes
import org.deeplearning4j.nn.api.Model
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.ConvolutionMode
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.BatchNormalization
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.api.BaseTrainingListener
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
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File

class CnnTrainer(
    private val modelPath: String,
    private val normalizerPath: String?
) {

    private val scaler: DataNormalization = NormalizerMinMaxScaler()
    fun train(
        trainDatasetIterator: PgnDatasetIterator,
        testDatasetIterator: PgnDatasetIterator,
        epochs: Int = 20,
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
            .updater(Adam())
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .list()
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(3, 3)
                    .stride(1, 1)
                    .activation(Activation.RELU)
                    .nIn(numberOfPlanes)
                    .nOut(256)
                    .convolutionMode(ConvolutionMode.Same)
                    .build()
            )
            .layer(BatchNormalization())
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(3, 3)
                    .stride(1, 1)
                    .activation(Activation.RELU)
                    .nOut(256)
                    .build()
            )
            .layer(BatchNormalization())
            .layer(
                ConvolutionLayer.Builder()
                    .kernelSize(3, 3)
                    .stride(1, 1)
                    .activation(Activation.RELU)
                    .nOut(512)
                    .build()
            )
            .layer(BatchNormalization())
            .layer(
                DenseLayer.Builder()
                    .nOut(512)
                    .activation(Activation.RELU)
                    .build()
            )
            .layer(
                OutputLayer.Builder(LossFunctions.LossFunction.XENT)
                    .name("output")
                    .nOut(outputNum)
                    .activation(Activation.SIGMOID)
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
            //ScoreIterationListener(1),
            object: BaseTrainingListener() {
                override fun onEpochStart(model: Model?) {
                    println("Epoch start: ${model?.score()}")
                    super.onEpochStart(model)
                }

                override fun onEpochEnd(model: Model?) {
                    println("Epoch end: ${model?.score()}")
                    super.onEpochEnd(model)
                }

                override fun iterationDone(model: Model?, iteration: Int, epoch: Int) {
                    println("Iteration done: ${model?.score()}, $iteration, $epoch")
                    super.iterationDone(model, iteration, epoch)
                }
            }
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