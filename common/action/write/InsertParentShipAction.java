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

package grakn.benchmark.common.action.write;

import grakn.benchmark.common.action.Action;
import grakn.benchmark.common.action.SpouseType;
import grakn.benchmark.common.driver.DbOperation;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class InsertParentShipAction<DB_OPERATION extends DbOperation, ACTION_RETURN_TYPE> extends Action<DB_OPERATION, ACTION_RETURN_TYPE> {
    protected final HashMap<SpouseType, String> marriage;
    protected final String childEmail;

    public InsertParentShipAction(DB_OPERATION dbOperation, HashMap<SpouseType, String> marriage, String childEmail) {
        super(dbOperation);
        this.marriage = marriage;
        this.childEmail = childEmail;
    }

    @Override
    protected ArrayList<Object> inputForReport() {
        return argsList(marriage, childEmail);
    }

    public enum InsertParentShipActionField implements ComparableField {
        HUSBAND_EMAIL, WIFE_EMAIL, CHILD_EMAIL
    }
}
