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

package org.navalplanner.business.planner.limiting.entities;

import static org.navalplanner.business.workingday.EffortDuration.hours;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.joda.time.LocalDate;
import org.navalplanner.business.calendars.entities.ResourceCalendar;
import org.navalplanner.business.planner.entities.DayAssignment;
import org.navalplanner.business.planner.entities.GenericDayAssignment;
import org.navalplanner.business.planner.entities.GenericResourceAllocation;
import org.navalplanner.business.planner.entities.ResourceAllocation;
import org.navalplanner.business.planner.entities.SpecificDayAssignment;
import org.navalplanner.business.planner.entities.SpecificResourceAllocation;
import org.navalplanner.business.resources.entities.LimitingResourceQueue;
import org.navalplanner.business.resources.entities.Resource;
import org.navalplanner.business.workingday.ResourcesPerDay;

/**
 * Handles all the logic related to allocation of
 * {@link LimitingResourceQueueElement} into {@link LimitingResourceQueue}
 *
 * The class does not do the allocation itself but provides methods:
 * <em>getFirstValidGap</em>, <em>calculateStartAndEndTime</em> or
 * <em>generateDayAssignments</em>, needed to do the allocation of
 * {@link LimitingResourceQueueElement}
 *
 * @author Diego Pino Garcia <dpino@igalia.com>
 *
 */
public class LimitingResourceAllocator {

    private final static ResourcesPerDay ONE_RESOURCE_PER_DAY = ResourcesPerDay
            .amount(new BigDecimal(1));

    /**
     * Returns first valid gap in queue for element
     *
     * Returns null if there is not a valid gap. This case can only happen on
     * trying to allocate an element related to a generic resource allocation.
     * It is possible that queue.resource does not hold element.criteria at any
     * interval of time
     *
     * @param queue search gap inside queue
     * @param element element to fit into queue
     * @return
     */
    public static Gap getFirstValidGap(
            LimitingResourceQueue queue, LimitingResourceQueueElement element) {

        final Resource resource = queue.getResource();
        final List<LimitingResourceQueueElement> elements = new LinkedList<LimitingResourceQueueElement>(
                queue.getLimitingResourceQueueElements());
        final int size = elements.size();
        final DateAndHour startTime = getStartTimeBecauseOfGantt(element);

        int pos = 0;

        // Iterate through queue elements
        while (pos <= size) {
            Gap gap = getGapInQueueAtPosition(
                    resource, elements, startTime, pos++);

            if (gap != null) {
                List<Gap> subgaps = getFittingSubgaps(
                        element, gap, resource);
                if (!subgaps.isEmpty()) {
                    return subgaps.get(0);
                }
            }
        }

        // The queue cannot hold this element (queue.resource
        // doesn't meet element.criteria)
        return null;
    }

    private static List<Gap> getFittingSubgaps(
            LimitingResourceQueueElement element,
            final Gap gap, final Resource resource) {

        List<Gap> result = new ArrayList<Gap>();

        if (isSpecific(element) && gap.canFit(element)) {
            result.add(gap);
        } else if (isGeneric(element)) {
            final List<Gap> gaps = gap.splitIntoGapsSatisfyingCriteria(
                    resource, element.getCriteria());
            for (Gap subgap : gaps) {
                if (subgap.canFit(element)) {
                    result.add(subgap);
                }
            }
        }
        return result;
    }

    public static Gap getFirstValidGapSince(
            LimitingResourceQueueElement element, LimitingResourceQueue queue,
            DateAndHour since) {
        List<Gap> gaps = getValidGapsForElementSince(element, queue, since);
        return (!gaps.isEmpty()) ? gaps.get(0) : null;
    }

    public static List<Gap> getValidGapsForElementSince(
            LimitingResourceQueueElement element, LimitingResourceQueue queue,
            DateAndHour since) {

        List<Gap> result = new ArrayList<Gap>();

        final Resource resource = queue.getResource();
        final List<LimitingResourceQueueElement> elements = new LinkedList<LimitingResourceQueueElement>(
                queue.getLimitingResourceQueueElements());
        final int size = elements.size();

        int pos = moveUntil(elements, since);

        // Iterate through queue elements
        while (pos <= size) {
            Gap gap = getGapInQueueAtPosition(
                    resource, elements, since, pos++);

            // The queue cannot hold this element (queue.resource
            // doesn't meet element.criteria)
            if (gap != null) {
                List<Gap> subgaps = getFittingSubgaps(
                        element, gap, resource);
                result.addAll(subgaps);
            }
        }

        return result;
    }

    private static int moveUntil(List<LimitingResourceQueueElement> elements, DateAndHour until) {
        int pos = 0;

        if (elements.size() > 0) {
            // Space between until and first element start time
            LimitingResourceQueueElement first = elements.get(0);
            if (until.isBefore(first.getStartTime())) {
                return 0;
            }

            for (pos = 0; pos < elements.size(); pos++) {
                final LimitingResourceQueueElement each = elements.get(pos);
                final DateAndHour startTime = each.getStartTime();
                if (until.isAfter(startTime) || until.isEquals(startTime)) {
                    return pos;
                }
            }
        }
        return pos;
    }

    private static boolean isGeneric(LimitingResourceQueueElement element) {
        return element.getResourceAllocation() instanceof GenericResourceAllocation;
    }

    private static boolean isSpecific(LimitingResourceQueueElement element) {
        return element.getResourceAllocation() instanceof SpecificResourceAllocation;
    }

    public static DateAndHour getFirstElementTime(List<DayAssignment> dayAssignments) {
        final DayAssignment start = dayAssignments.get(0);
        return new DateAndHour(start.getDay(), start.getHours());
    }

    public static DateAndHour getLastElementTime(List<DayAssignment> dayAssignments) {
        final DayAssignment end = dayAssignments.get(dayAssignments.size() - 1);
        return new DateAndHour(end.getDay(), end.getHours());
    }

    private static Gap getGapInQueueAtPosition(
            Resource resource, List<LimitingResourceQueueElement> elements,
            DateAndHour startTimeBecauseOfGantt, int pos) {

        final int size = elements.size();

        // No elements in queue
        if (size == 0) {
            return createLastGap(startTimeBecauseOfGantt, null, resource);
        }

        // Last element
        if (pos == size) {
            return createLastGap(startTimeBecauseOfGantt, elements.get(size - 1), resource);
        }

        LimitingResourceQueueElement next = elements.get(pos);

        // First element
        if (pos == 0
                && startTimeBecauseOfGantt.getDate().isBefore(
                        next.getStartDate())) {
            return Gap.create(resource,
                    startTimeBecauseOfGantt, next.getStartTime());
        }

        // In the middle of two elements
        if (pos > 0) {
            LimitingResourceQueueElement previous = elements.get(pos - 1);
            return Gap.create(resource, DateAndHour
                    .Max(previous.getEndTime(), startTimeBecauseOfGantt), next
                    .getStartTime());
        }

        return null;
    }

    private static DateAndHour getStartTimeBecauseOfGantt(LimitingResourceQueueElement element) {
        return new DateAndHour(new LocalDate(element.getEarlierStartDateBecauseOfGantt()), 0);
    }

    private static Gap createLastGap(
            DateAndHour _startTime, LimitingResourceQueueElement lastElement,
            Resource resource) {

        final DateAndHour queueEndTime = (lastElement != null) ? lastElement
                .getEndTime() : null;
        DateAndHour startTime = DateAndHour.Max(_startTime, queueEndTime);
        return Gap
                .create(resource, startTime, null);
    }

    /**
     * Generates a list of {@link DayAssignment} for {@link Resource} starting
     * from startTime
     *
     * The returned list is not associated to resouceAllocation.
     *
     * resourceAllocation is passed to know if the list of day assignments
     * should be {@link GenericDayAssignment} or {@link SpecificDayAssignment}
     *
     * @param resourceAllocation
     * @param resource
     * @param startTime
     * @return
     */
    public static List<DayAssignment> generateDayAssignments(
            ResourceAllocation<?> resourceAllocation,
            Resource resource,
            DateAndHour startTime, DateAndHour endsAfter) {

        List<DayAssignment> assignments = new LinkedList<DayAssignment>();

        LocalDate date = startTime.getDate();
        final int totalHours = resourceAllocation.getIntendedTotalHours();
        int hoursAssigned = 0;
        // Generate first day assignment
        int hoursCanAllocate = hoursCanWorkOnDay(resource, date, startTime.getHour());
        int hoursToAllocate = Math.min(totalHours, hoursCanAllocate);
        DayAssignment dayAssignment = createDayAssignment(resourceAllocation,
                resource, date, hoursToAllocate);
        hoursAssigned += addDayAssignment(assignments, dayAssignment);

        // Generate rest of day assignments
        for (date = date.plusDays(1); hoursAssigned < totalHours
                || endsAfter.isAfter(date); date = date.plusDays(1)) {
            hoursAssigned += addDayAssignment(assignments,
                    generateDayAssignment(resourceAllocation, resource, date,
                            totalHours));
        }
        if (hoursAssigned > totalHours) {
            stripStartAssignments(assignments, hoursAssigned - totalHours);
        }
        return new ArrayList<DayAssignment>(assignments);
    }

    private static void stripStartAssignments(List<DayAssignment> assignments,
            int hoursSurplus) {
        ListIterator<DayAssignment> listIterator = assignments.listIterator();
        while (listIterator.hasNext() && hoursSurplus > 0) {
            DayAssignment current = listIterator.next();
            int hoursTaken = Math.min(hoursSurplus, current.getHours());
            hoursSurplus -= hoursTaken;
            if (hoursTaken == current.getHours()) {
                listIterator.remove();
            } else {
                listIterator.set(current.withDuration(hours(hoursTaken)));
            }
        }
    }

    private static List<DayAssignment> generateDayAssignmentsStartingFromEnd(ResourceAllocation<?> resourceAllocation,
            Resource resource,
            DateAndHour endTime) {

        List<DayAssignment> assignments = new ArrayList<DayAssignment>();

        LocalDate date = endTime.getDate();
        int totalHours = resourceAllocation.getIntendedTotalHours();

        // Generate last day assignment
        int hoursCanAllocate = hoursCanWorkOnDay(resource, date, endTime.getHour());
        if (hoursCanAllocate > 0) {
            int hoursToAllocate = Math.min(totalHours, hoursCanAllocate);
            DayAssignment dayAssignment = createDayAssignment(resourceAllocation, resource, date, hoursToAllocate);
            totalHours -= addDayAssignment(assignments, dayAssignment);
        }

        // Generate rest of day assignments
        for (date = date.minusDays(1); totalHours > 0; date = date.minusDays(1)) {
            totalHours -= addDayAssignment(assignments, generateDayAssignment(
                    resourceAllocation, resource, date, totalHours));
        }
        return assignments;
    }

    private static DayAssignment createDayAssignment(ResourceAllocation<?> resourceAllocation,
            Resource resource, LocalDate date, int hoursToAllocate) {
        if (resourceAllocation instanceof SpecificResourceAllocation) {
            return SpecificDayAssignment.create(date, hoursToAllocate, resource);
        } else if (resourceAllocation instanceof GenericResourceAllocation) {
            return GenericDayAssignment.create(date, hoursToAllocate, resource);
        }
        return null;
    }

    private static int addDayAssignment(List<DayAssignment> list, DayAssignment dayAssignment) {
        if (dayAssignment != null) {
            list.add(dayAssignment);
            return dayAssignment.getHours();
        }
        return 0;
    }

    private static int hoursCanWorkOnDay(final Resource resource,
            final LocalDate date, int alreadyWorked) {
        final ResourceCalendar calendar = resource.getCalendar();
        int hoursCanAllocate = calendar.toHours(date, ONE_RESOURCE_PER_DAY);
        return hoursCanAllocate - alreadyWorked;
    }

    private static DayAssignment generateDayAssignment(
            final ResourceAllocation<?> resourceAllocation,
            Resource resource,
            final LocalDate date, int intentedHours) {

        final ResourceCalendar calendar = resource.getCalendar();

        int hoursCanAllocate = calendar.toHours(date, ONE_RESOURCE_PER_DAY);
        if (hoursCanAllocate > 0) {
            int hoursToAllocate = Math.min(intentedHours, hoursCanAllocate);
            return createDayAssignment(resourceAllocation, resource, date, hoursToAllocate);
        }
        return null;
    }

    public static DateAndHour startTimeToAllocateStartingFromEnd(
            ResourceAllocation<?> resourceAllocation, Resource resource,
            Gap gap) {

        // Last element, time is end of last element (gap.starttime)
        if (gap.getEndTime() == null) {
            return gap.getStartTime();
        }

        final List<DayAssignment> dayAssignments = LimitingResourceAllocator
                .generateDayAssignmentsStartingFromEnd(resourceAllocation,
                        resource, gap.getEndTime());

        return LimitingResourceAllocator.getLastElementTime(dayAssignments);
    }

}
