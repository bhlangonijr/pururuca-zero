package com.github.bhlangonijr.pururucazero.uci

import com.github.bhlangonijr.pururucazero.*

class Uci constructor(private val search: Search) {

    fun exec(cmd: String): Boolean {

        val tokens = cmd.split(" ")
        val command = tokens[0]
        return when (command) {
            "uci" -> handleUci()
            "ucinewgame" -> handleUciNewGame()
            "isready" -> handleIsReady()
            "position" -> handlePosition(tokens)
            "go" -> handleGo(tokens)
            "stop" -> handleStop()
            "quit" -> handleQuit()
            "setoption" -> handleSetoption(tokens)
            else -> handleUnknownCommand(cmd)
        }
    }

    private fun handleUci(): Boolean {

        println("classId name $NAME $VERSION")
        println("classId author $AUTHOR")
        println("uciok")
        return true
    }

    private fun handleUciNewGame(): Boolean {

        search.stop()
        search.reset()
        return true
    }

    private fun handleIsReady(): Boolean {

        //do stuff
        println("readyok")
        return true
    }

    private fun handlePosition(tokens: List<String>): Boolean {

        val positionType = tokens[1]
        when (positionType) {
            "fen" -> {
                val moves = mergeTokens(tokens, "moves", " ").trim()
                val part = getString(tokens, "fen", "")
                val state = if (moves.isNotBlank())
                    mergeTokens(tokens, part, "moves", " ")
                else
                    mergeTokens(tokens, part, " ")
                val fen = "$part $state".trim()
                search.setupPosition(fen, moves)
            }
            "startpos" -> {
                val moves = mergeTokens(tokens, "moves", " ").trim()
                search.setupPosition(moves)
            }
            else -> println("info string ignoring malformed uci command")

        }
        return true
    }

    private fun handleGo(tokens: List<String>): Boolean {

        val params = SearchParams(
                whiteTime = getLong(tokens, "wtime", "6000000"),
                blackTime = getLong(tokens, "btime", "6000000"),
                whiteIncrement = getLong(tokens, "winc", "1000"),
                blackIncrement = getLong(tokens, "binc", "1000"),
                moveTime = getLong(tokens, "movetime", "0"),
                movesToGo = getInt(tokens, "movestogo", "1"),
                depth = getInt(tokens, "depth", "100"),
                nodes = getLong(tokens, "nodes", "50000000"),
                infinite = getBoolean(tokens, "infinite", "false"),
                ponder = getBoolean(tokens, "ponder", "false"),
                searchMoves = getString(tokens, "movestogo", ""),
                threads = search.threads
        )

        return search.start(params)
    }

    private fun handleSetoption(tokens: List<String>): Boolean {

        val option = tokens[2]
        val value = tokens[4]

        when (option) {
            "fen" -> {
                search.threads = value.toInt()
            }
            else -> println("info string ignoring unsupported uci option")

        }

        return true
    }

    private fun handleStop(): Boolean = search.stop()

    private fun handleQuit(): Boolean {

        println("bye")
        return false
    }

    private fun handleUnknownCommand(cmd: String): Boolean {

        println("info string unknown command: $cmd")
        return true
    }

}