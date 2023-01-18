/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.vaticle.typedb.benchmark.common.seed

import com.vaticle.typedb.benchmark.common.concept.City
import com.vaticle.typedb.benchmark.common.concept.Gender.Companion.of
import com.vaticle.typedb.common.collection.Collections
import com.vaticle.typedb.common.collection.Pair
import java.util.Random
import java.util.function.Consumer

class RandomSource(seed: Long) {
    private val random = Random(seed)

    fun nextSource(): RandomSource {
        return RandomSource(random.nextLong())
    }

    fun nextBoolean(): Boolean {
        return random.nextBoolean()
    }

    fun <T> choose(list: List<T>): T {
        return list[random.nextInt(list.size)]
    }

    fun nextInt(): Int {
        return random.nextInt(Int.MAX_VALUE)
    }

    fun address(city: City): String {
        val houseNumber = random.nextInt(1000)
        val streetName = choose(city.country!!.continent!!.commonFirstNames(of(nextBoolean())))
        val zipCode = random.nextInt(10000)
        return "$houseNumber $streetName Street, ${city.name}, $zipCode ${city.country.name}"
    }

    fun <T> randomPairs(list: List<T>, pairsPerElement: Int): List<Pair<T, T>> {
        val pairs = mutableListOf<Pair<T, T>>()
        for (i in list.indices) {
            for (j in 0 until pairsPerElement) {
                var other = random.nextInt(list.size - 1)
                if (other >= i) other++
                pairs.add(Collections.pair(list[i], list[other]))
            }
        }
        return pairs
    }

    fun <T> randomPairs(list1: List<T>, list2: MutableList<T?>): List<Pair<T, T?>> {
        val numPairs = list1.size.coerceAtMost(list2.size)
        list2.shuffle(random)
        return (0 until numPairs).map { i -> Pair(list1[i], list2[i]) }
    }

    fun <RECIPIENT, RESOURCE> randomAllocation(
        recipients: List<RECIPIENT>,
        resources: List<RESOURCE>
    ): List<Pair<RECIPIENT, RESOURCE>> {
        return when (recipients.size) {
            0 -> emptyList()
            1 -> resources.map { Pair(recipients[0], it) }
            else -> resources.map { Pair(recipients[random.nextInt(recipients.size - 1)], it) }
        }
    }
}
