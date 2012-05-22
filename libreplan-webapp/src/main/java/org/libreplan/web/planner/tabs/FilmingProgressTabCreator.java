/*
 * This file is part of LibrePlan
 *
 * Copyright (C) 2012 WirelessGalicia, S.L.
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
package org.libreplan.web.planner.tabs;

import static org.libreplan.web.I18nHelper._;
import static org.libreplan.web.planner.tabs.MultipleTabsPlannerController.BREADCRUMBS_SEPARATOR;
import static org.libreplan.web.planner.tabs.MultipleTabsPlannerController.getSchedulingLabel;

import java.util.HashMap;
import java.util.Map;

import org.libreplan.business.common.IAdHocTransactionService;
import org.libreplan.business.common.IOnTransaction;
import org.libreplan.business.common.Registry;
import org.libreplan.business.orders.entities.Order;
import org.libreplan.web.planner.filming.progress.FilmingProgressController;
import org.libreplan.web.planner.order.ISaveCommand;
import org.libreplan.web.planner.order.PlanningStateCreator;
import org.libreplan.web.planner.order.PlanningStateCreator.PlanningState;
import org.libreplan.web.planner.tabs.CreatedOnDemandTab.IComponentCreator;
import org.zkoss.ganttz.extensions.ITab;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;

/**
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public class FilmingProgressTabCreator {

    public static ITab create(Mode mode, PlanningStateCreator planningStateCreator,
            FilmingProgressController filmingProgressController, Component breadcrumbs) {
        return new FilmingProgressTabCreator(mode, planningStateCreator, filmingProgressController,
                breadcrumbs).build();
    }

    private final PlanningStateCreator planningStateCreator;
    private final Mode mode;
    private final FilmingProgressController filmingProgressController;
    private final Component breadcrumbs;

    private FilmingProgressTabCreator(Mode mode, PlanningStateCreator planningStateCreator,
            FilmingProgressController filmingProgressController, Component breadcrumbs) {
        this.mode = mode;
        this.planningStateCreator = planningStateCreator;
        this.filmingProgressController = filmingProgressController;
        this.breadcrumbs = breadcrumbs;
    }

    private ITab build() {
        return TabOnModeType.forMode(mode).forType(ModeType.GLOBAL, createFilmingProgressTab())
                .forType(ModeType.ORDER, createFilmingProgressTab()).create();
    }

    private ITab createFilmingProgressTab() {
        IComponentCreator componentCreator = new IComponentCreator() {

            @Override
            public org.zkoss.zk.ui.Component create(org.zkoss.zk.ui.Component parent) {
                Map<String, Object> arguments = new HashMap<String, Object>();
                arguments.put("filmingProgressController", filmingProgressController);
                return Executions.createComponents("filming_progress.zul", parent,
                        arguments);
            }

        };
        return new CreatedOnDemandTab(_("Filming progress"), "order-dashboard", componentCreator) {

            @Override
            protected void afterShowAction() {
                PlanningState planningState = getPlanningState(mode.getOrder(), getDesktop());
                Order currentOrder = planningState.getOrder();
                ISaveCommand saveCommand = planningState.getSaveCommand();
                filmingProgressController.init(currentOrder,
                        saveCommand);
                breadcrumbs.getChildren().clear();
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(getSchedulingLabel()));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(_("Filming progress")));
                breadcrumbs.appendChild(new Image(BREADCRUMBS_SEPARATOR));
                breadcrumbs.appendChild(new Label(currentOrder.getName()));
            }
        };
    }

    PlanningState getPlanningState(final Order order, final Desktop desktop) {
        IAdHocTransactionService transactionService = Registry.getTransactionService();
        return transactionService.runOnTransaction(new IOnTransaction<PlanningState>() {
            public PlanningState execute() {
                return planningStateCreator.retrieveOrCreate(desktop, order);
            }
        });
    }
}