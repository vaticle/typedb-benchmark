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

package grakn.benchmark.common.agent.region;

import grakn.benchmark.common.action.ActionFactory;
import grakn.benchmark.common.agent.base.Agent;
import grakn.benchmark.common.agent.base.SimulationContext;
import grakn.benchmark.common.driver.DbDriver;
import grakn.benchmark.common.driver.DbOperation;
import grakn.benchmark.common.world.World;

import java.util.List;
import java.util.Random;

import static java.util.stream.Collectors.toList;

public abstract class CountryAgent<DB_OPERATION extends DbOperation> extends Agent<World.Country, DB_OPERATION> {

    public CountryAgent(DbDriver<DB_OPERATION> dbDriver, ActionFactory<DB_OPERATION, ?> actionFactory, SimulationContext simulationContext) {
        super(dbDriver, actionFactory, simulationContext);
    }

    @Override
    protected List<World.Country> getRegions(World world) {
        return world.getCountries().collect(toList());
    }

    protected abstract class CountryRegion extends Region {
        public CountryRegion(int iteration, String tracker, Random random, boolean test) {
            super(iteration, tracker, random, test);
        }
    }
}