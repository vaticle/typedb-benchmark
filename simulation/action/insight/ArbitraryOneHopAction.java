/*
 * Copyright (C) 2020 Grakn Labs
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

package grakn.benchmark.simulation.action.insight;

import grakn.benchmark.simulation.driver.Transaction;

public abstract class ArbitraryOneHopAction<TX extends Transaction> extends WorldwideInsightAction<TX, Integer> {

    protected static String PERSON_EMAIL_FOR_QUERY = "email/0/World:Europe:United Kingdom:London/0";

    public ArbitraryOneHopAction(TX tx) {
        super(tx);
    }

}