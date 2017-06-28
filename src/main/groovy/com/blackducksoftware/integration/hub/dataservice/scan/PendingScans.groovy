/**
 * Hub Common
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
 */
package com.blackducksoftware.integration.hub.dataservice.scan

import com.blackducksoftware.integration.exception.IntegrationException
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRequestService
import com.blackducksoftware.integration.hub.api.item.MetaService
import com.blackducksoftware.integration.hub.api.project.ProjectRequestService
import com.blackducksoftware.integration.hub.api.project.version.ProjectVersionRequestService
import com.blackducksoftware.integration.hub.api.scan.ScanSummaryRequestService
import com.blackducksoftware.integration.hub.exception.HubIntegrationException
import com.blackducksoftware.integration.hub.model.enumeration.ScanSummaryStatusEnum
import com.blackducksoftware.integration.hub.model.view.CodeLocationView
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView
import com.blackducksoftware.integration.hub.model.view.ProjectView
import com.blackducksoftware.integration.hub.model.view.ScanSummaryView

class PendingScans {
    ProjectRequestService projectRequestService
    ProjectVersionRequestService projectVersionRequestService
    CodeLocationRequestService codeLocationRequestService
    ScanSummaryRequestService scanSummaryRequestService
    MetaService metaService

    PendingScans(ProjectRequestService projectRequestService, ProjectVersionRequestService projectVersionRequestService, CodeLocationRequestService codeLocationRequestService, ScanSummaryRequestService scanSummaryRequestService, MetaService metaService) {
        this.projectRequestService = projectRequestService
        this.projectVersionRequestService = projectVersionRequestService
        this.codeLocationRequestService = codeLocationRequestService
        this.scanSummaryRequestService = scanSummaryRequestService
        this.metaService = metaService
    }

    List<ScanSummaryView> getPendingScans(PendingScansRequest pendingScansRequest) {
        List<ScanSummaryView> pendingScans = []

        try {
            final ProjectView projectItem = projectRequestService.getProjectByName(pendingScansRequest.projectName)
            final ProjectVersionView projectVersionView = projectVersionRequestService.getProjectVersion(projectItem, pendingScansRequest.projectVersionName)
            final String projectVersionUrl = metaService.getHref(projectVersionView)

            List<CodeLocationView> codeLocations = []
            if (pendingScansRequest.isCodeLocationRequest()) {
                codeLocations = codeLocationRequestService.getAllCodeLocationsForCodeLocationType(pendingScansRequest.codeLocationType)
            } else {
                codeLocations = codeLocationRequestService.getAllCodeLocationsForProjectVersion(projectVersionView)
            }

            populatePendingScans(projectVersionUrl, pendingScans, codeLocations)
        } catch (final Exception e) {
            pendingScans = new ArrayList<>();
            // ignore, since we might not have found a project or version, etc
            // so just keep waiting until the timeout
        }

        pendingScans
    }

    List<ScanSummaryView> getPendingScans(final List<ScanSummaryView> scanSummaries) throws IntegrationException {
        final List<ScanSummaryView> pendingScans = new ArrayList<>();
        for (final ScanSummaryView scanSummaryItem : scanSummaries) {
            final String scanSummaryLink = metaService.getHref(scanSummaryItem);
            final ScanSummaryView currentScanSummaryItem = scanSummaryRequestService.getItem(scanSummaryLink, ScanSummaryView.class);
            if (isPending(currentScanSummaryItem.status)) {
                pendingScans.add(currentScanSummaryItem);
            } else if (isError(currentScanSummaryItem.status)) {
                throw new HubIntegrationException("There was a problem in the Hub processing the scan(s). Error Status : "
                + currentScanSummaryItem.status.toString() + ", " + currentScanSummaryItem.statusMessage);
            }
        }

        return pendingScans;
    }

    private void populatePendingScans(final String projectVersionUrl, List<ScanSummaryView> pendingScans, final List<CodeLocationView> codeLocationViews)
    throws IntegrationException {
        final List<String> allScanSummariesLinks = new ArrayList<>();
        for (final CodeLocationView codeLocationItem : codeLocationViews) {
            final String mappedProjectVersionUrl = codeLocationItem.mappedProjectVersion;
            if (projectVersionUrl.equals(mappedProjectVersionUrl)) {
                final String scanSummariesLink = metaService.getFirstLink(codeLocationItem, MetaService.SCANS_LINK);
                allScanSummariesLinks.add(scanSummariesLink);
            }
        }

        final List<ScanSummaryView> allScanSummaries = new ArrayList<>();
        for (final String scanSummaryLink : allScanSummariesLinks) {
            allScanSummaries.addAll(scanSummaryRequestService.getAllScanSummaryItems(scanSummaryLink));
        }

        pendingScans = new ArrayList<>();
        for (final ScanSummaryView scanSummaryItem : allScanSummaries) {
            if (isPending(scanSummaryItem.status)) {
                pendingScans.add(scanSummaryItem);
            }
        }
    }

    private static final Set<ScanSummaryStatusEnum> PENDING_STATES = EnumSet.of(ScanSummaryStatusEnum.UNSTARTED, ScanSummaryStatusEnum.SCANNING, ScanSummaryStatusEnum.SAVING_SCAN_DATA, ScanSummaryStatusEnum.SCAN_DATA_SAVE_COMPLETE, ScanSummaryStatusEnum.REQUESTED_MATCH_JOB, ScanSummaryStatusEnum.MATCHING, ScanSummaryStatusEnum.BOM_VERSION_CHECK, ScanSummaryStatusEnum.BUILDING_BOM);
    private static final Set<ScanSummaryStatusEnum> DONE_STATES = EnumSet.of(ScanSummaryStatusEnum.COMPLETE, ScanSummaryStatusEnum.CANCELLED, ScanSummaryStatusEnum.CLONED, ScanSummaryStatusEnum.ERROR_SCANNING, ScanSummaryStatusEnum.ERROR_SAVING_SCAN_DATA, ScanSummaryStatusEnum.ERROR_MATCHING, ScanSummaryStatusEnum.ERROR_BUILDING_BOM, ScanSummaryStatusEnum.ERROR);
    private static final Set<ScanSummaryStatusEnum> ERROR_STATES = EnumSet.of(ScanSummaryStatusEnum.CANCELLED, ScanSummaryStatusEnum.ERROR_SCANNING, ScanSummaryStatusEnum.ERROR_SAVING_SCAN_DATA, ScanSummaryStatusEnum.ERROR_MATCHING, ScanSummaryStatusEnum.ERROR_BUILDING_BOM, ScanSummaryStatusEnum.ERROR);

    public boolean isPending(final ScanSummaryStatusEnum statusEnum) {
        return PENDING_STATES.contains(statusEnum);
    }

    public boolean isDone(final ScanSummaryStatusEnum statusEnum) {
        return DONE_STATES.contains(statusEnum);
    }

    public boolean isError(final ScanSummaryStatusEnum statusEnum) {
        return ERROR_STATES.contains(statusEnum);
    }
}
