/*
 * This file is part of ###PROJECT_NAME###
 *
 * Copyright (C) 2009 Fundación para o Fomento da Calidade Industrial e
 *                    Desenvolvemento Tecnolóxico de Galicia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.navalplanner.business.planner.entities;

import java.util.Date;

import org.apache.commons.lang.Validate;

/**
 * Component class that encapsulates a {@link StartConstraintType} and its
 * associated constraint date <br />
 * @author Óscar González Fernández <ogonzalez@igalia.com>
 */
public class TaskStartConstraint {

    private StartConstraintType startConstraintType = StartConstraintType.AS_SOON_AS_POSSIBLE;

    private Date constraintDate = null;

    public TaskStartConstraint() {
    }

    public StartConstraintType getStartConstraintType() {
        return startConstraintType != null ? startConstraintType
                : StartConstraintType.AS_SOON_AS_POSSIBLE;
    }

    public void explicityMovedTo(Date date) {
        Validate.notNull(date);
        startConstraintType = startConstraintType.newTypeAfterMoved();
        constraintDate = new Date(date.getTime());
    }

    public Date getConstraintDate() {
        return constraintDate != null ? new Date(constraintDate.getTime())
                : null;
    }

    public void notEarlierThan(Date date) {
        Validate.notNull(date);
        this.constraintDate = date;
        this.startConstraintType = StartConstraintType.START_NOT_EARLIER_THAN;
    }

}
