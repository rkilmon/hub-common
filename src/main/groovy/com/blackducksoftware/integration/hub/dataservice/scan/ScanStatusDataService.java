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
package com.blackducksoftware.integration.hub.dataservice.scan;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.codelocation.CodeLocationRequestService;
import com.blackducksoftware.integration.hub.api.item.MetaService;
import com.blackducksoftware.integration.hub.api.project.ProjectRequestService;
import com.blackducksoftware.integration.hub.api.project.version.ProjectVersionRequestService;
import com.blackducksoftware.integration.hub.api.scan.ScanSummaryRequestService;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.exception.HubTimeoutExceededException;
import com.blackducksoftware.integration.hub.model.enumeration.CodeLocationEnum;
import com.blackducksoftware.integration.hub.model.enumeration.ScanSummaryStatusEnum;
import com.blackducksoftware.integration.hub.model.view.ScanSummaryView;
import com.blackducksoftware.integration.log.IntLogger;

public class ScanStatusDataService {
    private static final long FIVE_SECONDS = 5 * 1000;

    private static final long DEFAULT_TIMEOUT = 300000l;

    private final PendingScans pendingScans;

    private final long timeoutInMilliseconds;

    public ScanStatusDataService(final IntLogger logger,
            final ProjectRequestService projectRequestService, final ProjectVersionRequestService projectVersionRequestService,
            final CodeLocationRequestService codeLocationRequestService,
            final ScanSummaryRequestService scanSummaryRequestService, final MetaService metaService,
            final long timeoutInMilliseconds) {

        long timeout = timeoutInMilliseconds;
        if (timeoutInMilliseconds <= 0l) {
            timeout = DEFAULT_TIMEOUT;
            logger.alwaysLog(timeoutInMilliseconds + "ms is not a valid BOM wait time, using : " + timeout + "ms instead");
        }
        this.timeoutInMilliseconds = timeout;

        this.pendingScans = new PendingScans(projectRequestService, projectVersionRequestService, codeLocationRequestService, scanSummaryRequestService,
                metaService);
    }

    /**
     * For the provided projectName and projectVersion, wait at most
     * timeoutInMilliseconds for the project/version to exist and/or
     * at least one pending bom import scan to begin. Then, wait at most
     * timeoutInMilliseconds for all discovered pending scans to
     * complete.
     *
     * If the timeouts are exceeded, a HubTimeoutExceededException will be
     * thrown.
     *
     */
    public void assertBomImportScanStartedThenFinished(final String projectName, final String projectVersion)
            throws HubTimeoutExceededException, IntegrationException {
        final PendingScansRequest pendingScansRequest = PendingScansRequest.createCodeLocationRequest(projectName, projectVersion, CodeLocationEnum.BOM_IMPORT);
        final List<ScanSummaryView> pendingScans = waitForPendingScansToStart(pendingScansRequest, timeoutInMilliseconds);
        waitForScansToComplete(pendingScans, timeoutInMilliseconds);
    }

    /**
     * For the provided projectName and projectVersion, wait at most
     * timeoutInMilliseconds for the project/version to exist and/or
     * at least one pending scan (of any type) to begin. Then, wait at most
     * timeoutInMilliseconds for all discovered pending scans to
     * complete.
     *
     * If the timeouts are exceeded, a HubTimeoutExceededException will be
     * thrown.
     *
     */
    public void assertScanStartedThenFinished(final String projectName, final String projectVersion)
            throws HubTimeoutExceededException, IntegrationException {
        final PendingScansRequest pendingScansRequest = PendingScansRequest.createProjectVersionRequest(projectName, projectVersion);
        final List<ScanSummaryView> pendingScans = waitForPendingScansToStart(pendingScansRequest, timeoutInMilliseconds);
        waitForScansToComplete(pendingScans, timeoutInMilliseconds);
    }

    /**
     * For the given pendingScans, wait at most
     * timeoutInMilliseconds for the scans to complete.
     *
     * If the timeout is exceeded, a HubTimeoutExceededException will be thrown.
     *
     * @deprecated use assertScansFinished instead - code location type doesn't matter if you already have
     *             ScanSummaryViews
     */
    @Deprecated
    public void assertBomImportScansFinished(final List<ScanSummaryView> pendingScans) throws HubTimeoutExceededException, IntegrationException {
        waitForScansToComplete(pendingScans, timeoutInMilliseconds);
    }

    public void assertScansFinished(final List<ScanSummaryView> pendingScans) throws HubTimeoutExceededException, IntegrationException {
        waitForScansToComplete(pendingScans, timeoutInMilliseconds);
    }

    private List<ScanSummaryView> waitForPendingScansToStart(final PendingScansRequest pendingScansRequest, final long scanStartedTimeoutInMilliseconds)
            throws HubIntegrationException {
        List<ScanSummaryView> scanSummaryViews = pendingScans.getPendingScans(pendingScansRequest);
        final long startedTime = System.currentTimeMillis();
        boolean pendingScansOk = scanSummaryViews.size() > 0;
        while (!done(pendingScansOk, scanStartedTimeoutInMilliseconds, startedTime,
                "No scan has started within the specified wait time: %d minutes")) {
            try {
                Thread.sleep(FIVE_SECONDS);
            } catch (final InterruptedException e) {
                throw new HubIntegrationException("The thread waiting for the scan to start was interrupted: " + e.getMessage(), e);
            }
            scanSummaryViews = pendingScans.getPendingScans(pendingScansRequest);
            pendingScansOk = scanSummaryViews.size() > 0;
        }

        return scanSummaryViews;
    }

    private void waitForScansToComplete(List<ScanSummaryView> scanSummaryViews, final long scanStartedTimeoutInMilliseconds)
            throws HubTimeoutExceededException, IntegrationException {
        scanSummaryViews = pendingScans.getPendingScans(scanSummaryViews);
        final long startedTime = System.currentTimeMillis();
        boolean pendingScansOk = scanSummaryViews.isEmpty();
        while (!done(pendingScansOk, scanStartedTimeoutInMilliseconds, startedTime,
                "The pending scans have not completed within the specified wait time: %d minutes")) {
            try {
                Thread.sleep(FIVE_SECONDS);
            } catch (final InterruptedException e) {
                throw new HubIntegrationException("The thread waiting for the scan to complete was interrupted: " + e.getMessage(), e);
            }
            scanSummaryViews = pendingScans.getPendingScans(scanSummaryViews);
            pendingScansOk = scanSummaryViews.isEmpty();
        }
    }

    private boolean done(final boolean pendingScansOk, final long timeoutInMilliseconds, final long startedTime,
            final String timeoutMessage) throws HubTimeoutExceededException {
        if (pendingScansOk) {
            return true;
        }

        if (takenTooLong(timeoutInMilliseconds, startedTime)) {
            throw new HubTimeoutExceededException(
                    String.format(timeoutMessage, TimeUnit.MILLISECONDS.toMinutes(timeoutInMilliseconds)));
        }

        return false;
    }

    private boolean takenTooLong(final long timeoutInMilliseconds, final long startedTime) {
        final long elapsed = System.currentTimeMillis() - startedTime;
        return elapsed > timeoutInMilliseconds;
    }

    public boolean isPending(final ScanSummaryStatusEnum statusEnum) {
        return pendingScans.isPending(statusEnum);
    }

    public boolean isDone(final ScanSummaryStatusEnum statusEnum) {
        return pendingScans.isDone(statusEnum);
    }

    public boolean isError(final ScanSummaryStatusEnum statusEnum) {
        return pendingScans.isError(statusEnum);
    }

}
