package com.tech.arno.coroutine.flow

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

class FlowLearn {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = runBlocking {
            val list = listOf(1, 2, 3, 4)
            val list2 = listOf(1, 2)
            val almostSameList = listOf(1, 1, 2, 3, 3, 4)
            testFold(list)
            testThrottling(list)
            testBuffer(list)
            testCombine(list, list2)
            testDistinct(almostSameList)
        }


        //region 测试累加
        private suspend fun testFold(list: List<Int>) {
            testReduceOperator(list)
            testFoldOperator(list)
        }

        //endregion
        //region 测试不同间隔下,节流触发效果不同
        private suspend fun testThrottling(list: List<Int>) {
            testDebounceOperator(list, 200, 500)
            testDebounceOperator(list, 500, 200)

            testSampleOperator(list, 500, 200)
            testSampleOperator(list, 200, 500)

        }

        //endregion
        //region 测试缓冲
        private suspend fun testBuffer(list: List<Int>) {
            testNoBufferOperator(list)
            testBufferOperator(list)
            testBufferOperator(list, 2)
        }
        //endregion

        //region 测试组合
        private suspend fun testCombine(list: List<Int>, list2: List<Int>) {
            testFlatMergeOperator(list, list2)
            testCombineOperator(list, list2)
            testZipOperator(list, list2)
        }
        //endregion

        private suspend fun testDistinct(list: List<Int>) {
            testDistinctUntilChangedOperator(list)
        }

        /**
         * 测试reduce操作符
         * 累加，如果为空则抛出异常
         */
        private suspend fun testReduceOperator(list: List<Int>) {
            println("========>")
            println("测试reduce数据为：${list.joinToString()}")
            val result = list.asFlow()
                .onEach { println("发射 $it") }
                .catch {
                    println("捕获异常：$it")
                }
                .reduce { accumulator, value ->
                    println("执行reduce操作 accumulator：$accumulator + value: $value")
                    accumulator + value
                }
            println("测试reduce完成合并为一个，结果为： $result")
            println("<========")
        }

        /**
         * 测试reduce操作符
         * 累加 并映射成其他数据,fold带有初始值
         */
        private suspend fun <E> testFoldOperator(list: List<E>) {
            println("========>")
            println("测试Fold数据为：${list.joinToString()}")
            val result = list.asFlow()
                .onEach { println("发射 $it") }
                .fold("init") { accumulator, value ->
                    println("执行fold操作 accumulator：$accumulator + value: $value")
                    "$accumulator,$value"
                }
            println("测试fold完成合并为一个String，结果为： $result")
            println("<========")
        }

        /**
         * 测试debounce操作符,限流
         * 限流达到指定时间后才发出数据，最后一个数据一定会发出，如果间隔内收到数据，则重置计时器
         *
         * @param E
         * @param list
         * @param emitDelay
         * @param debounceDelay
         */
        @OptIn(FlowPreview::class)
        private suspend fun <E> testDebounceOperator(list: List<E>, emitDelay: Long, debounceDelay: Long) {
            println("========>")
            println("测试debounce数据为：${list.joinToString()},发射间隔为$emitDelay，debounce 间隔为$debounceDelay")
            flow {
                list.forEach {
                    emit(it)
                    kotlinx.coroutines.delay(emitDelay)
                }
            }.onEach { println("发射 $it") }
                .debounce(debounceDelay)
                .collect {
                    println("测试debounce完成，间隔$debounceDelay 内触发： $it")
                }
            println("<========")
        }

        /**
         * 测试sample 操作符，限流
         * 规定时间内只能发送一个数据，最后一个数据不一定发送
         * @param E
         * @param list
         * @param emitDelay
         * @param debounceDelay
         */
        private suspend fun <E> testSampleOperator(list: List<E>, emitDelay: Long, debounceDelay: Long) {
            println("========>")
            println("测试sample数据为：${list.joinToString()},发射间隔为$emitDelay，debounce 间隔为$debounceDelay")
            flow {
                list.forEach {
                    emit(it)
                    kotlinx.coroutines.delay(emitDelay)
                }
            }.onEach { println("发射 $it") }
                .sample(debounceDelay)
                .collect {
                    println("测试sample完成，间隔$debounceDelay 内触发： $it")
                }
            println("<========")
        }

        /**
         * 测试乘积两个流合并（全排列—铺平）
         * 线程安全，可以并行执行
         * @param T
         * @param R
         * @param list1
         * @param list2
         */
        @OptIn(FlowPreview::class)
        private suspend fun <T, R> testFlatMergeOperator(list1: List<T>, list2: List<R>) {
            println("========>")
            println("测试flatMerge数据为：list1 = ${list1.joinToString()}")
            println("测试flatMerge数据为：list2 = ${list1.joinToString()}")
            list1.asFlow().flatMapMerge { l1 ->
                list2.map { l2 ->
                    l1 to l2
                }.asFlow()
            }.collect {
                println("测试flatMerge阶乘，结果 $it")
            }
            println("<========")
        }

        /**
         * 测试缓冲操作符
         *
         * @param T
         * @param list
         * @param bufferSize
         */
        private suspend fun <T> testBufferOperator(list: List<T>, bufferSize: Int = Channel.BUFFERED) {
            println("========>")
            println("测试buffer数据为：list = ${list.joinToString()}")
            list.asFlow()
                .onEach {
                    println("发射 $it")
                }.buffer(bufferSize)
                .collect {
                    println("测试buffer，结果 $it")
                }
            println("<========")
        }

        private suspend fun <T> testNoBufferOperator(list: List<T>) {
            println("========>")
            println("测试NoBuffer数据为：list = ${list.joinToString()}")
            list.asFlow()
                .onEach {
                    println("发射NoBuffer $it")
                }
                .collect {
                    println("测试NoBuffer，结果 $it")
                }
            println("<========")
        }

        /**
         * 合并两个 flow
         * 长的一方会持续接受到短的一方的最后一个数据，直到结束
         *
         * @param T
         * @param R
         * @param list1
         * @param list2
         */
        private suspend fun <T, R> testCombineOperator(list1: List<T>, list2: List<R>) {
            println("========>")
            println("测试combine数据为：list1 = ${list1.joinToString()}")
            println("测试combine数据为：list2 = ${list2.joinToString()}")
            list1.asFlow().combine(list2.asFlow()) { l1, l2 ->
                l1 to l2
            }.collect {
                println("测试combine，结果 $it")
            }
            println("<========")
        }

        /**
         * 合并两个 flow
         * 只会跟短的对齐
         *
         * @param T
         * @param R
         * @param list1
         * @param list2
         */
        private suspend fun <T, R> testZipOperator(list1: List<T>, list2: List<R>) {
            println("========>")
            println("测试zip数据为：list1 = ${list1.joinToString()}")
            println("测试zip数据为：list2 = ${list2.joinToString()}")
            list1.asFlow().zip(list2.asFlow()) { l1, l2 ->
                l1 to l2
            }.collect {
                println("测试zip，结果 $it")
            }
            println("<========")
        }

        /**
         * 去重直到与前一个不一样时候才能收到数据
         *
         * @param T
         * @param list
         */
        private suspend fun <T> testDistinctUntilChangedOperator(list: List<T>) {
            println("========>")
            println("测试distinctUntilChanged数据为：list1 = ${list.joinToString()}")
            list.asFlow().distinctUntilChanged().collect {
                println("测试distinctUntilChanged，结果 $it")
            }
            println("<========")
        }
    }
}

