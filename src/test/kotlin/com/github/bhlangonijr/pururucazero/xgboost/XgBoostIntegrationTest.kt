package com.github.bhlangonijr.pururucazero.xgboost

import com.github.bhlangonijr.pururucazero.ml.PgnConverter.Companion.pgnToDataSet
import ml.dmlc.xgboost4j.java.DMatrix
import ml.dmlc.xgboost4j.java.XGBoost
import org.junit.Test

class XgBoostIntegrationTest {

    @Test
    fun testLearningPgnXgBoost() {

        val data = pgnToDMatrix("src/test/resources/Stockfish_DD_64-bit_4CPU.pgn")
        val test = pgnToDMatrix("src/test/resources/pt54.pgn")

        println(data.rowNum())
        println(test.rowNum())

        val params = HashMap<String, Any>()
        params["booster"] = "dart"
        params["eta"] = 0.3
        params["max_depth"] = 3
        params["nthread"] = 5
        params["num_class"] = 3
        params["verbosity"] = 3

        //params["eval_metric"] = "mlogloss"
        params["objective"] = "multi:softprob"

        val watches = HashMap<String, DMatrix>()
        watches["train"] = data
        watches["test"] = test

        val round = 50
        println("Training")
        val booster = XGBoost.train(data, params, round, watches, null, null)

        val predicts = booster.predict(test)

        predicts.forEachIndexed { i1, floats ->
            floats.forEachIndexed { i2, m ->
                println("$i1,$i2 = $m [" + test.label[i1] + "]")
            }
        }
    }
}

fun pgnToDMatrix(name: String): DMatrix {

    val data = pgnToDataSet(name)
    val matrix = DMatrix(data.rowHeaders, data.colIndex,
            data.features, DMatrix.SparseType.CSR, data.labels.size)
    matrix.label = data.labels
    return matrix
}