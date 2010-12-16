/*
 * This file is part of NavalPlan
 *
 * Copyright (C) 2009-2010 Fundación para o Fomento da Calidade Industrial e
 *                         Desenvolvemento Tecnolóxico de Galicia
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

package org.navalplanner.web.reports;

import static org.navalplanner.web.I18nHelper._;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;

import org.navalplanner.business.labels.entities.Label;
import org.navalplanner.business.orders.entities.Order;
import org.navalplanner.business.resources.entities.Criterion;
import org.navalplanner.web.common.Util;
import org.navalplanner.web.common.components.ExtendedJasperreport;
import org.navalplanner.web.common.components.bandboxsearch.BandboxSearch;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Toolbarbutton;

/**
 * @author Lorenzo Tilve Álvaro <ltilve@igalia.com>
 * @author Susana Montes Pedreira <smontes@wirelessgalicia.com>
 */
public class OrderCostsPerResourceController extends GenericForwardComposer {

    private IOrderCostsPerResourceModel orderCostsPerResourceModel;

    private OrderCostsPerResourceReport orderCostsPerResourceReport;

    private Datebox startingDate;

    private Datebox endingDate;

    private ComboboxOutputFormat outputFormat;

    private Hbox URItext;

    private Toolbarbutton URIlink;

    private static final String HTML = "html";

    private Listbox lbOrders;

    private Listbox lbLabels;

    private Listbox lbCriterions;

    private BandboxSearch bdOrders;

    private BandboxSearch bdLabels;

    private BandboxSearch bdCriterions;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        comp.setVariable("controller", this, true);
        orderCostsPerResourceModel.init();
    }

    public void showReport(ExtendedJasperreport report) {
        final String type = outputFormat.getOutputFormat();

        orderCostsPerResourceReport = new OrderCostsPerResourceReport(report);
        orderCostsPerResourceReport.setDatasource(getDataSource());
        orderCostsPerResourceReport.setParameters(getParameters());

        String URI = orderCostsPerResourceReport.show(type);
        if (type.equals(HTML)) {
            URItext.setStyle("display: none");
            Executions.getCurrent().sendRedirect(URI, "_blank");
        } else {
            URItext.setStyle("display: inline");
            URIlink.setHref(URI);
        }

    }

    private JRDataSource getDataSource() {
        return orderCostsPerResourceModel.getOrderReport(getSelectedOrders(),
                getStartingDate(), getEndingDate(), getSelectedLabels(),
                getSelectedCriterions());
    }

    private Map<String, Object> getParameters() {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("startingDate", getStartingDate());
        result.put("endingDate", getEndingDate());

        return result;
    }

    public List<Order> getAllOrders() {
        return orderCostsPerResourceModel.getOrders();
    }

    public List<Order> getSelectedOrders() {
        return Collections.unmodifiableList(orderCostsPerResourceModel
                .getSelectedOrders());
    }

    public void onSelectOrder() {
        Order order = (Order) bdOrders.getSelectedElement();
        if (order == null) {
            throw new WrongValueException(bdOrders, _("please, select a project"));
        }
        boolean result = orderCostsPerResourceModel.addSelectedOrder(order);
        if (!result) {
            throw new WrongValueException(bdOrders,
                    _("This project has already been added."));
        } else {
            Util.reloadBindings(lbOrders);
        }
        bdOrders.clear();
    }

    public void onRemoveOrder(Order order) {
        orderCostsPerResourceModel.removeSelectedOrder(order);
        Util.reloadBindings(lbOrders);
    }

    private Date getStartingDate() {
         return startingDate.getValue();
    }

    private Date getEndingDate() {
        return endingDate.getValue();
    }

    public Constraint checkConstraintStartingDate() {
        return new Constraint() {
            @Override
            public void validate(Component comp, Object value)
                    throws WrongValueException {
                Date startDateLine = (Date) value;
                if ((startDateLine != null) && (getEndingDate() != null)
                        && (startDateLine.compareTo(getEndingDate()) > 0)) {
                    throw new WrongValueException(comp,
                            _("must be lower than finish date"));
                }
            }
        };
    }

    public Constraint checkConstraintEndingDate() {
        return new Constraint() {
            @Override
            public void validate(Component comp, Object value)
                    throws WrongValueException {
                Date endingDate = (Date) value;
                if ((endingDate != null) && (getStartingDate() != null)
                        && (endingDate.compareTo(getStartingDate()) < 0)) {
                    throw new WrongValueException(comp,
                            _("must be greater than finish date"));
                }
            }
        };
    }

    public List<Label> getAllLabels() {
        return orderCostsPerResourceModel.getAllLabels();
    }

    public void onSelectLabel() {
        Label label = (Label) bdLabels.getSelectedElement();
        if (label == null) {
            throw new WrongValueException(bdLabels, _("please, select a label"));
        }
        boolean result = orderCostsPerResourceModel.addSelectedLabel(label);
        if (!result) {
            throw new WrongValueException(bdLabels,
                    _("This label has already been added."));
        } else {
            Util.reloadBindings(lbLabels);
        }
        bdLabels.clear();
    }

    public void onRemoveLabel(Label label) {
        orderCostsPerResourceModel.removeSelectedLabel(label);
        Util.reloadBindings(lbLabels);
    }

    public List<Label> getSelectedLabels() {
        return orderCostsPerResourceModel.getSelectedLabels();
    }

    public List<Criterion> getSelectedCriterions() {
        return orderCostsPerResourceModel.getSelectedCriterions();
    }

    public List<Criterion> getAllCriterions() {
        return orderCostsPerResourceModel.getCriterions();
    }

    public void onSelectCriterion() {
        Criterion criterion = (Criterion) bdCriterions.getSelectedElement();
        if (criterion == null) {
            throw new WrongValueException(bdCriterions,
                    _("please, select a Criterion"));
        }
        boolean result = orderCostsPerResourceModel
                .addSelectedCriterion(criterion);
        if (!result) {
            throw new WrongValueException(bdCriterions,
                    _("This Criterion has already been added."));
        } else {
            Util.reloadBindings(lbCriterions);
        }
        bdCriterions.clear();
    }

    public void onRemoveCriterion(Criterion criterion) {
        orderCostsPerResourceModel.removeSelectedCriterion(criterion);
        Util.reloadBindings(lbCriterions);
    }

}
