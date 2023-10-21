package com.github.bhlangonijr.pururucazero.cnn

import com.github.bhlangonijr.pururucazero.cnn.Nd4jEncoder.numberOfPlanes
import org.deeplearning4j.nn.api.Model
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.*
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex
import org.deeplearning4j.nn.conf.inputs.InputType
import org.deeplearning4j.nn.conf.layers.*
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor
import org.deeplearning4j.nn.graph.ComputationGraph
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.api.BaseTrainingListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.evaluation.classification.Evaluation
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.learning.config.Sgd
import org.nd4j.linalg.lossfunctions.LossFunctions
import java.io.File

class ResNetTrainer(
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
            .updater(Sgd())
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.LECUN_NORMAL)
            .graphBuilder()
            .setInputTypes(InputType.convolutional(8, 8, numberOfPlanes.toLong()))

        val input = "in"
        val initBlock = "init"
        val numResidualBlocks = 20
        conf.addInputs(input)
        val convOut: String = addConvBatchNormBlock(conf, initBlock, input, numberOfPlanes, true)
        val towerOut: String = addResidualTower(conf, numResidualBlocks, convOut)
        //val policyOut: String = addPolicyHead(conf, towerOut, true)
        val valueOut: String = addValueHead(conf, towerOut)
        conf.setOutputs(/*policyOut, */valueOut)

        val model = ComputationGraph(conf.build())
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
        //val eval = RegressionEvaluation()

        while (testDatasetIterator.hasNext()) {
            val dataSet = testDatasetIterator.next()
            val predicts = model.output(dataSet.features)
            val labels = dataSet.labels
//            eval.eval( dataSet.labels, model.output(dataSet.features))
//            println("eval: ${eval.stats()}")
            println("Predicts: $predicts")
            println("Labels: $labels")

        }

        val eval2: Evaluation = model.evaluate(testDatasetIterator)
        println("Eval2: ${eval2.stats()}")
    }

    /**Building block for AGZ residual blocks.
     * conv2d -> batch norm -> ReLU
     */
    fun addConvBatchNormBlock(
        conf: ComputationGraphConfiguration.GraphBuilder,
        blockName: String,
        inName: String?,
        nIn: Int,
        useActivation: Boolean,
        kernelSize: IntArray = intArrayOf(3, 3),
        strides: IntArray = intArrayOf(1, 1),
        convolutionMode: ConvolutionMode = ConvolutionMode.Same
    ): String {
        val convName = "conv_$blockName"
        val bnName = "batch_norm_$blockName"
        val actName = "relu_$blockName"
        conf.addLayer(
            convName, ConvolutionLayer.Builder().kernelSize(*kernelSize)
                .stride(*strides).convolutionMode(convolutionMode).nIn(nIn).nOut(256).build(), inName
        )
        conf.addLayer(bnName, BatchNormalization.Builder().nOut(256).build(), convName)
        return if (useActivation) {
            conf.addLayer(actName, ActivationLayer.Builder().activation(Activation.RELU).build(), bnName)
            actName
        } else bnName
    }

    private fun addResidualBlock(
        conf: ComputationGraphConfiguration.GraphBuilder,
        blockNumber: Int, inName: String?): String? {
        val firstBlock = "residual_1_$blockNumber"
        val firstOut = "relu_residual_1_$blockNumber"
        val secondBlock = "residual_2_$blockNumber"
        val mergeBlock = "add_$blockNumber"
        val actBlock = "relu_$blockNumber"
        val firstBnOut = addConvBatchNormBlock(conf, firstBlock, inName, 256, true)
        val secondBnOut = addConvBatchNormBlock(conf, secondBlock, firstOut, 256, false)
        conf.addVertex(mergeBlock, ElementWiseVertex(ElementWiseVertex.Op.Add), firstBnOut, secondBnOut)
        conf.addLayer(actBlock, ActivationLayer.Builder().activation(Activation.RELU).build(), mergeBlock)
        return actBlock
    }

    private fun addResidualTower(
        conf: ComputationGraphConfiguration.GraphBuilder,
        numBlocks: Int,
        inName: String?
    ): String {
        var name = inName
        for (i in 0 until numBlocks) {
            name = addResidualBlock(conf, i, name)
        }
        return name ?: ""
    }

    private fun addConvolutionalTower(
        conf: ComputationGraphConfiguration.GraphBuilder,
        numBlocks: Int, inName: String?): String {
        var name = inName
        for (i in 0 until numBlocks) {
            name = addConvBatchNormBlock(conf, i.toString(), name, 256, true)
        }
        return name ?: ""
    }

    private fun addValueHead(
        conf: ComputationGraphConfiguration.GraphBuilder,
        inName: String?,
        kernelSize: IntArray = intArrayOf(3, 3),
        strides: IntArray = intArrayOf(1, 1),
        convolutionMode: ConvolutionMode = ConvolutionMode.Same
        ): String {
        val convName = "value_head_conv_"
        val bnName = "value_head_batch_norm_"
        val actName = "value_head_relu_"
        val denseName = "value_head_dense_"
        val outputName = "value_head_output_"
        conf.addLayer(
            convName, ConvolutionLayer.Builder().kernelSize(*kernelSize).stride(*strides)
                .convolutionMode(convolutionMode).nOut(numberOfPlanes).nIn(256).build(), inName
        )
        conf.addLayer(bnName, BatchNormalization.Builder().nOut(numberOfPlanes).build(), convName)
        conf.addLayer(actName, ActivationLayer.Builder().activation(Activation.RELU).build(), bnName)
        conf.addLayer(denseName, DenseLayer.Builder().nIn(8 * 8 * numberOfPlanes).nOut(256).build(), actName)
        val preProcessorMap: MutableMap<String, InputPreProcessor> = HashMap()
        preProcessorMap[denseName] = CnnToFeedForwardPreProcessor(8, 8, numberOfPlanes.toLong())
        conf.inputPreProcessors = preProcessorMap
        conf.addLayer(outputName,
            OutputLayer.Builder(LossFunctions.LossFunction.XENT).activation(Activation.SIGMOID).nIn(256).nOut(1)
                .build(),
            denseName
        )
        return outputName
    }

}