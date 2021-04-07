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

package grakn.benchmark.grakn.driver;

import grakn.benchmark.simulation.driver.Session;

public class GraknSession extends Session<GraknTransaction> {

    private final grakn.client.api.GraknSession nativeSession;

    public GraknSession(grakn.client.api.GraknSession nativeSession) {
        this.nativeSession = nativeSession;
    }

    public grakn.client.api.GraknSession unpack() {
        return nativeSession;
    }

    @Override
    public GraknTransaction newTransaction(String tracker, long iteration, boolean trace) {
        return new GraknTransaction(nativeSession, tracker, iteration, trace);
    }

    @Override
    public void close() {
        nativeSession.close();
    }
}