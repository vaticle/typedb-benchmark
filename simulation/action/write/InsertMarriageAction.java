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

package grakn.benchmark.simulation.action.write;

import grakn.benchmark.simulation.action.Action;
import grakn.benchmark.simulation.driver.Transaction;
import grakn.benchmark.simulation.world.World;

import java.util.ArrayList;

public abstract class InsertMarriageAction<TX extends Transaction, ACTION_RETURN_TYPE> extends Action<TX, ACTION_RETURN_TYPE> {
    protected final World.City worldCity;
    protected final int marriageIdentifier;
    protected final String wifeEmail;
    protected final String husbandEmail;

    public InsertMarriageAction(TX dbOperation, World.City city, int marriageIdentifier, String wifeEmail, String husbandEmail) {
        super(dbOperation);
        this.worldCity = city;
        this.marriageIdentifier = marriageIdentifier;
        this.wifeEmail = wifeEmail;
        this.husbandEmail = husbandEmail;
    }

    @Override
    protected ArrayList<Object> inputForReport() {
        return argsList(worldCity, marriageIdentifier, wifeEmail, husbandEmail);
    }

    public enum InsertMarriageActionField implements ComparableField {
        MARRIAGE_IDENTIFIER, WIFE_EMAIL, HUSBAND_EMAIL, CITY_NAME
    }

}
