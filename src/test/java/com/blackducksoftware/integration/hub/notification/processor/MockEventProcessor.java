/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.hub.notification.processor;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.api.item.MetaService;
import com.blackducksoftware.integration.hub.api.notification.VulnerabilitySourceQualifiedId;
import com.blackducksoftware.integration.hub.api.policy.PolicyRule;
import com.blackducksoftware.integration.hub.dataservice.notification.item.NotificationContentItem;
import com.blackducksoftware.integration.hub.dataservice.notification.item.PolicyOverrideContentItem;
import com.blackducksoftware.integration.hub.dataservice.notification.item.PolicyViolationClearedContentItem;
import com.blackducksoftware.integration.hub.dataservice.notification.item.PolicyViolationContentItem;
import com.blackducksoftware.integration.hub.dataservice.notification.item.VulnerabilityContentItem;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.notification.processor.event.NotificationEvent;
import com.blackducksoftware.integration.hub.notification.processor.event.NotificationEventConstants;
import com.blackducksoftware.integration.hub.notification.processor.event.PolicyEvent;
import com.blackducksoftware.integration.hub.notification.processor.event.VulnerabilityEvent;

public class MockEventProcessor extends NotificationSubProcessor<NotificationEvent> {
    private final Logger logger = LoggerFactory.getLogger(MockEventProcessor.class);

    public MockEventProcessor(final MapProcessorCache<NotificationEvent> cache, final MetaService metaService) {
        super(cache, metaService);
    }

    @Override
    public void process(final NotificationContentItem notification) {
        // because of inheritance the order of the PolicyViolationContentItem matters it must be last since it is a base
        // class for the other types.
        try {
            if (notification instanceof PolicyOverrideContentItem) {
                final PolicyOverrideContentItem policyOverrideContentItem = (PolicyOverrideContentItem) notification;
                handlePolicyOverride(policyOverrideContentItem);
            } else if (notification instanceof PolicyViolationClearedContentItem) {
                final PolicyViolationClearedContentItem policyViolationCleared = (PolicyViolationClearedContentItem) notification;
                handlePolicyCleared(policyViolationCleared);
            } else if (notification instanceof PolicyViolationContentItem) {
                final PolicyViolationContentItem policyViolationContentItem = (PolicyViolationContentItem) notification;
                handlePolicyViolation(policyViolationContentItem);
            } else if (notification instanceof VulnerabilityContentItem) {
                final VulnerabilityContentItem vulnerabilityContentItem = (VulnerabilityContentItem) notification;
                handleVulnerability(vulnerabilityContentItem);
            }
        } catch (final HubIntegrationException ex) {
            logger.error("error processing event", ex);
        }
    }

    private void handlePolicyViolation(final PolicyViolationContentItem policyViolationContentItem) throws HubIntegrationException {
        for (final PolicyRule rule : policyViolationContentItem.getPolicyRuleList()) {
            final String eventKey = generatePolicyEventKey(policyViolationContentItem, rule);
            final Map<String, Object> dataSet = generatePolicyDataSet(policyViolationContentItem, rule);
            final PolicyEvent event = new PolicyEvent(eventKey, NotificationCategoryEnum.POLICY_VIOLATION, rule, dataSet);
            getCache().addEvent(event);
        }
    }

    private void handlePolicyOverride(final PolicyOverrideContentItem policyOverrideContentItem) throws HubIntegrationException {
        for (final PolicyRule rule : policyOverrideContentItem.getPolicyRuleList()) {
            final String eventKey = generatePolicyEventKey(policyOverrideContentItem, rule);
            final Map<String, Object> dataSet = generatePolicyOverrideDataSet(policyOverrideContentItem, rule);
            final PolicyEvent event = new PolicyEvent(eventKey, NotificationCategoryEnum.POLICY_VIOLATION,
                    rule, dataSet);
            if (getCache().hasEvent(event.getEventKey())) {
                getCache().removeEvent(event);
            } else {
                event.setCategoryType(NotificationCategoryEnum.POLICY_VIOLATION_OVERRIDE);
                getCache().addEvent(event);
            }
        }
    }

    private void handlePolicyCleared(final PolicyViolationClearedContentItem policyViolationCleared) throws HubIntegrationException {
        for (final PolicyRule rule : policyViolationCleared.getPolicyRuleList()) {
            final String eventKey = generatePolicyEventKey(policyViolationCleared, rule);
            final Map<String, Object> dataSet = generatePolicyDataSet(policyViolationCleared, rule);
            final PolicyEvent event = new PolicyEvent(eventKey, NotificationCategoryEnum.POLICY_VIOLATION, rule,
                    dataSet);
            if (getCache().hasEvent(event.getEventKey())) {
                getCache().removeEvent(event);
            } else {
                event.setCategoryType(NotificationCategoryEnum.POLICY_VIOLATION_CLEARED);
                getCache().addEvent(event);
            }
        }
    }

    private void handleVulnerability(final VulnerabilityContentItem vulnerabilityContentItem) {
        final List<VulnerabilitySourceQualifiedId> addedVulnList = vulnerabilityContentItem.getAddedVulnList();
        final List<VulnerabilitySourceQualifiedId> updatedVulnList = vulnerabilityContentItem.getUpdatedVulnList();
        final List<VulnerabilitySourceQualifiedId> deletedVulnList = vulnerabilityContentItem.getDeletedVulnList();

        if (addedVulnList != null && !addedVulnList.isEmpty()) {
            getCache().addEvent(createEvent(vulnerabilityContentItem, getVulnerabilityIds(addedVulnList)));
        }

        if (updatedVulnList != null && !updatedVulnList.isEmpty()) {
            getCache().addEvent(createEvent(vulnerabilityContentItem, getVulnerabilityIds(updatedVulnList)));
        }

        if (deletedVulnList != null && !deletedVulnList.isEmpty()) {
            getCache().removeEvent(createEvent(vulnerabilityContentItem, getVulnerabilityIds(deletedVulnList)));
        }
    }

    private VulnerabilityEvent createEvent(VulnerabilityContentItem vulnerabilityContent,
            final Set<String> vulnerabilityIdList) {
        final String eventKey = generateVulnerabilityEventKey(vulnerabilityContent);
        final Map<String, Object> dataSet = generateVulnerabilityDataSet(vulnerabilityContent);
        return new VulnerabilityEvent(eventKey, NotificationCategoryEnum.VULNERABILITY, vulnerabilityIdList, dataSet);
    }

    private Set<String> getVulnerabilityIds(final List<VulnerabilitySourceQualifiedId> itemList) {
        final Set<String> set = new HashSet<>();
        for (final VulnerabilitySourceQualifiedId item : itemList) {
            set.add(item.getVulnerabilityId());
        }

        return set;
    }

    private String generatePolicyEventKey(final PolicyViolationContentItem content, PolicyRule rule) throws HubIntegrationException {
        final StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_ISSUE_TYPE_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_ISSUE_TYPE_VALUE_POLICY);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(NotificationEventConstants.EVENT_KEY_HUB_PROJECT_VERSION_REL_URL_HASHED_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(content.getProjectVersion().getUrl()));
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(NotificationEventConstants.EVENT_KEY_HUB_COMPONENT_REL_URL_HASHED_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(content.getComponentUrl()));
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(NotificationEventConstants.EVENT_KEY_HUB_COMPONENT_VERSION_REL_URL_HASHED_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(content.getComponentVersionUrl()));
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(NotificationEventConstants.EVENT_KEY_HUB_POLICY_RULE_REL_URL_HASHED_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(getMetaService().getHref(rule)));
        final String key = keyBuilder.toString();
        return key;
    }

    private String generateVulnerabilityEventKey(final VulnerabilityContentItem content) {
        final StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_ISSUE_TYPE_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_ISSUE_TYPE_VALUE_VULNERABILITY);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(NotificationEventConstants.EVENT_KEY_HUB_PROJECT_VERSION_REL_URL_HASHED_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(content.getProjectVersion().getUrl()));
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(NotificationEventConstants.EVENT_KEY_HUB_COMPONENT_REL_URL_HASHED_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(""); // There is never a component URL
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_PAIR_SEPARATOR);

        keyBuilder.append(NotificationEventConstants.EVENT_KEY_HUB_COMPONENT_VERSION_REL_URL_HASHED_NAME);
        keyBuilder.append(NotificationEventConstants.EVENT_KEY_NAME_VALUE_SEPARATOR);
        keyBuilder.append(hashString(content.getComponentVersionUrl()));

        final String key = keyBuilder.toString();
        return key;
    }

    private Map<String, Object> generatePolicyDataSet(PolicyViolationContentItem content, PolicyRule rule) {
        final Map<String, Object> dataSet = new LinkedHashMap<>(4);
        dataSet.put(ItemTypeEnum.RULE.name(), rule.getName());
        dataSet.put(ItemTypeEnum.COMPONENT.name(), content.getComponentName());
        dataSet.put(ItemTypeEnum.VERSION.name(), content.getComponentVersion());
        return dataSet;
    }

    private Map<String, Object> generatePolicyOverrideDataSet(PolicyOverrideContentItem content, PolicyRule rule) {
        final Map<String, Object> dataSet = generatePolicyDataSet(content, rule);
        final String person = StringUtils.join(" ", content.getFirstName(), content.getLastName());
        dataSet.put(ItemTypeEnum.PERSON.name(), person);

        return dataSet;
    }

    private Map<String, Object> generateVulnerabilityDataSet(VulnerabilityContentItem vulnerabilityContent) {
        final Map<String, Object> dataSet = new LinkedHashMap<>();
        dataSet.put(ItemTypeEnum.COMPONENT.name(), vulnerabilityContent.getComponentName());
        dataSet.put(ItemTypeEnum.VERSION.name(), vulnerabilityContent.getComponentVersion());
        return dataSet;
    }

    @Override
    public String generateEventKey(Map<String, Object> dataMap) throws HubIntegrationException {
        return ""; // ignore since we create multiple types of events from this processor
    }

    @Override
    public Map<String, Object> generateDataSet(Map<String, Object> inputData) {
        return Collections.emptyMap();
    }
}
