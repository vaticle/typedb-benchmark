/*
 * Copyright (C) 2021 Grakn Labs
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

package grakn.benchmark.neo4j.action.read;

import grakn.benchmark.neo4j.driver.Neo4jTransaction;
import grakn.benchmark.simulation.action.read.CitiesInContinentAction;
import grakn.benchmark.simulation.common.World;
import org.neo4j.driver.Query;

import java.util.HashMap;
import java.util.List;

public class Neo4jCitiesInContinentAction extends CitiesInContinentAction<Neo4jTransaction> {

    public Neo4jCitiesInContinentAction(Neo4jTransaction tx, World.City city) {
        super(tx, city);
    }

    @Override
    public List<String> run() {
        HashMap<String, Object> parameters = new HashMap<String, Object>() {{
            put("continentName", city.country().continent().name());
            put("cityName", city.name());
        }};
        return tx.sortedExecute(new Query(query(), parameters), "city.locationName", null);
    }

    public static String query() {
        return "MATCH (city:City)-[:LOCATED_IN*2]->(continent:Continent {locationName: $continentName})\n" +
                "WHERE NOT city.locationName = $cityName\n" +
                "RETURN city.locationName";
    }
}
