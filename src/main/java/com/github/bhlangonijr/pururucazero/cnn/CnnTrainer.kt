package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.numberOfPlanes
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.deeplearning4j.nn.api.Model
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.ConvolutionMode
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.BatchNormalization
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.api.BaseTrainingListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.evaluation.regression.RegressionEvaluation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer
import org.nd4j.linalg.learning.config.Adam
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File

class CnnTrainer(
    private val modelPath: String
) {
    fun train(
        trainDatasetIterator: PgnDatasetIterator,
        testDatasetIterator: PgnDatasetIterator,
        epochs: Int = 50,
        seed: Long = 123L
    ) {

        val outputNum = trainDatasetIterator.labelNames.size
        trainDatasetIterator.reset()
        testDatasetIterator.reset()

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
                    .nOut(64)
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
                    .nOut(256)
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
                DenseLayer.Builder()
                    .nOut(256)
                    .activation(Activation.RELU)
                    .build()
            )
            .layer(
                OutputLayer.Builder(LossFunctions.LossFunction.MSE)
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


        val modelPath = File(modelPath)
        ModelSerializer.writeModel(model, modelPath, true)
        println("Model has been saved in ${modelPath.path}")
        testDatasetIterator.reset()
        val eval = RegressionEvaluation()

        while (testDatasetIterator.hasNext()) {
            val dataSet = testDatasetIterator.next()
            val predicts = model.output(dataSet.features)
            val labels = dataSet.labels
            eval.eval( dataSet.labels, model.output(dataSet.features, false))
            println("eval: ${eval.stats()}")
            println("Predicts: $predicts")
            println("Labels: $labels")

        }

        val eval2: Evaluation = model.evaluate(testDatasetIterator)
        println("Eval2: ${eval2.stats()}")
    }
}