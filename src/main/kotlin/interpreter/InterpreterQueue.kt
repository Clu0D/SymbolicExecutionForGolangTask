package interpreter

import memory.State
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

interface InterpreterQueue<T> {
    fun size(): Int

    fun add(e: T)

    fun get(): T

    companion object {
        val rnd = Random

        fun <T: State> dfsQueue(): InterpreterQueue<T> =
            Standard(::priorityDfs)

        fun <T: State> bfsQueue(): InterpreterQueue<T> =
            Standard(::priorityBfs)

        fun <T: State> timeAfterNewCodeQueue(): InterpreterQueue<T> =
            Standard(::priorityTimeFromNewCodeFound)

        fun <T: State> randomQueue(): InterpreterQueue<T> =
            WeightedRandom(::priorityRandom)

        fun <T: State> randomTimeAfterNewCodeQueue(): InterpreterQueue<T> =
            WeightedRandom(::priorityTimeFromNewCodeFound)

        private var dfsCounter = 1.0
        private fun priorityDfs(state: State): Double {
            dfsCounter--
            return dfsCounter
        }

        private var bfsCounter = 1.0
        private fun priorityBfs(state: State): Double {
            bfsCounter++
            return bfsCounter
        }

        private fun priorityTimeFromNewCodeFound(state: State): Double {
            return max(100 - (state.time - state.newCodeTime + 1), 10).toDouble()
        }

        private fun priorityRandom(state: State): Double {
            return rnd.nextDouble() + 0.01
        }
    }

    class Standard<T>(val priorityF: (T) -> Double) : InterpreterQueue<T> {
        private val treeSet = TreeSet<Pair<T, Double>>(compareBy { it.second })

        override fun size(): Int =
            treeSet.size

        override fun add(e: T) {
            treeSet.add(e to priorityF(e))
        }

        override fun get(): T =
            treeSet.pollFirst()!!.first
    }

    class WeightedRandom<T>(val priorityF: (T) -> Double) : InterpreterQueue<T> {
        private val list = mutableListOf<Pair<T, Double>>()
        private var sumWeight = 0.0

        override fun size(): Int =
            list.size

        override fun add(e: T) {
            val weight = priorityF(e) + 0.01
            list.add(e to weight)
            sumWeight += weight
        }

        override fun get(): T {
            var randomWeight = rnd.nextDouble(sumWeight)
            while (true) {
                val (e, w) = list.removeFirst()
                randomWeight -= w
                if (randomWeight <= 0) {
                    sumWeight -= w
                    return e
                }
                list.add(e to w)
            }
        }
    }
}