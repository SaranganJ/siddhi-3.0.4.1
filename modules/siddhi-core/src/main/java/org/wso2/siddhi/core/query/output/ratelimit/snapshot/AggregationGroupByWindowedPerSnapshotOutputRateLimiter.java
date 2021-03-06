/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.core.query.output.ratelimit.snapshot;

import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.GroupedComplexEvent;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

public class AggregationGroupByWindowedPerSnapshotOutputRateLimiter extends AggregationWindowedPerSnapshotOutputRateLimiter {
    private Map<String, Map<Integer, Object>> groupByAggregateAttributeValueMap;
    protected LinkedList<GroupedComplexEvent> eventList;

    protected AggregationGroupByWindowedPerSnapshotOutputRateLimiter(String id, Long value, ScheduledExecutorService scheduledExecutorService, List<Integer> aggregateAttributePositionList, WrappedSnapshotOutputRateLimiter wrappedSnapshotOutputRateLimiter) {
        super(id, value, scheduledExecutorService, aggregateAttributePositionList, wrappedSnapshotOutputRateLimiter);
        groupByAggregateAttributeValueMap = new HashMap<String, Map<Integer, Object>>();
        eventList = new LinkedList<GroupedComplexEvent>();
    }

    @Override
    public void process(ComplexEventChunk complexEventChunk) {
        try {
            lock.lock();
            complexEventChunk.reset();
            String currentGroupByKey = null;
            Map<Integer, Object> currentAggregateAttributeValueMap = null;
            while (complexEventChunk.hasNext()) {
                ComplexEvent event = complexEventChunk.next();
                if (event.getType() == ComplexEvent.Type.TIMER) {
                    if (event.getTimestamp() >= scheduledTime) {
                        sendEvents();
                        scheduledTime = scheduledTime + value;
                        scheduler.notifyAt(scheduledTime);
                    }
                } else {
                    complexEventChunk.remove();
                    GroupedComplexEvent groupedComplexEvent = ((GroupedComplexEvent) event);
                    if (currentGroupByKey == null || !currentGroupByKey.equals(groupedComplexEvent.getGroupKey())) {
                        currentGroupByKey = groupedComplexEvent.getGroupKey();
                        currentAggregateAttributeValueMap = groupByAggregateAttributeValueMap.get(currentGroupByKey);
                        if (currentAggregateAttributeValueMap == null) {
                            currentAggregateAttributeValueMap = new HashMap<Integer, Object>(aggregateAttributePositionList.size());
                            groupByAggregateAttributeValueMap.put(currentGroupByKey, currentAggregateAttributeValueMap);
                        }

                    }
                    if (groupedComplexEvent.getType() == ComplexEvent.Type.CURRENT) {
                        eventList.add(groupedComplexEvent);
                        for (Integer position : aggregateAttributePositionList) {
                            currentAggregateAttributeValueMap.put(position, event.getOutputData()[position]);
                        }
                    } else if (groupedComplexEvent.getType() == ComplexEvent.Type.EXPIRED) {
                        for (Iterator<GroupedComplexEvent> iterator = eventList.iterator(); iterator.hasNext(); ) {
                            GroupedComplexEvent currentEvent = iterator.next();
                            if (comparator.compare(currentEvent.getComplexEvent(), groupedComplexEvent.getComplexEvent()) == 0) {
                                iterator.remove();
                                for (Integer position : aggregateAttributePositionList) {
                                    currentAggregateAttributeValueMap.put(position, groupedComplexEvent.getOutputData()[position]);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private synchronized void sendEvents() {
        ComplexEventChunk<ComplexEvent> complexEventChunk = new ComplexEventChunk<ComplexEvent>();

        for (GroupedComplexEvent originalComplexEvent : eventList) {
            String currentGroupByKey = null;
            Map<Integer, Object> currentAggregateAttributeValueMap = null;
            if (currentGroupByKey == null || !currentGroupByKey.equals(originalComplexEvent.getGroupKey())) {
                currentGroupByKey = originalComplexEvent.getGroupKey();
                currentAggregateAttributeValueMap = groupByAggregateAttributeValueMap.get(currentGroupByKey);
            }
            ComplexEvent eventCopy = cloneComplexEvent(originalComplexEvent.getComplexEvent());
            for (Integer position : aggregateAttributePositionList) {
                eventCopy.getOutputData()[position] = currentAggregateAttributeValueMap.get(position);
            }
            complexEventChunk.add(eventCopy);
        }
        sendToCallBacks(complexEventChunk);
    }

    @Override
    public Object[] currentState() {
        return new Object[]{eventList, groupByAggregateAttributeValueMap};
    }

    @Override
    public void restoreState(Object[] state) {
        eventList = (LinkedList<GroupedComplexEvent>) state[0];
        groupByAggregateAttributeValueMap = (Map<String, Map<Integer, Object>>) state[1];
    }

    @Override
    public SnapshotOutputRateLimiter clone(String key, WrappedSnapshotOutputRateLimiter wrappedSnapshotOutputRateLimiter) {
        return new AggregationGroupByWindowedPerSnapshotOutputRateLimiter(id + key, value, scheduledExecutorService, aggregateAttributePositionList, wrappedSnapshotOutputRateLimiter);
    }
}
