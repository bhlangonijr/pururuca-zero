package com.github.bhlangonijr.chesslib

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
            else -> handleUnknownCommand(cmd)
        }
    }

    private fun handleUci(): Boolean {

        println("id name $NAME $VERSION")
        println("id author $AUTHOR")
        return true
    }

    private fun handleUciNewGame(): Boolean {

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
                val fen = mergeTokens(tokens, "fen", "moves", "").trim()
                val moves = mergeTokens(tokens, "moves", " ").trim()
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
                whiteTime = getArg(tokens, "wtime")?.toLong(),
                blackTime = getArg(tokens, "btime")?.toLong(),
                whiteIncrement = getArg(tokens, "winc")?.toLong(),
                blackIncrement = getArg(tokens, "binc")?.toLong(),
                moveTime = getArg(tokens, "movetime")?.toLong(),
                movesToGo = getArg(tokens, "movestogo")?.toInt(),
                depth = getArg(tokens, "depth")?.toInt(),
                nodes = getArg(tokens, "nodes")?.toLong(),
                infinite = getArg(tokens, "infinite")?.toBoolean(),
                ponder = getArg(tokens, "ponder")?.toBoolean()
                )

        return search.start(params)
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
