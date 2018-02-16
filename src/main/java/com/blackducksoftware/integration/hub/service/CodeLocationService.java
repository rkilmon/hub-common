/**
 * hub-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.core.LinkSingleResponse;
import com.blackducksoftware.integration.hub.api.generated.discovery.ApiDiscovery;
import com.blackducksoftware.integration.hub.api.generated.enumeration.CodeLocationType;
import com.blackducksoftware.integration.hub.api.generated.view.CodeLocationView;
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.api.view.ScanSummaryView;
import com.blackducksoftware.integration.hub.exception.DoesNotExistException;
import com.blackducksoftware.integration.hub.request.BodyContent;
import com.blackducksoftware.integration.hub.request.Request;
import com.blackducksoftware.integration.hub.request.Response;
import com.blackducksoftware.integration.hub.rest.HttpMethod;
import com.blackducksoftware.integration.hub.service.model.RequestFactory;

public class CodeLocationService extends DataService {
    public CodeLocationService(final HubService hubService) {
        super(hubService);
    }

    public void importBomFile(final File file) throws IntegrationException {
        importBomFile(file, "application/ld+json");
    }

    public void importBomFile(final File file, final String mimeType) throws IntegrationException {
        String jsonPayload;
        try {
            jsonPayload = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new IntegrationException("Failed to import Bom file: " + file.getAbsolutePath() + " to the Hub because : " + e.getMessage(), e);
        }

        final String uri = hubService.getUriFromPath(HubService.BOMIMPORT_LINK);
        final Request request = RequestFactory.createCommonPostRequestBuilder(jsonPayload).uri(uri).mimeType(mimeType).build();
        try (Response response = hubService.executeRequest(request)) {
        } catch (final IOException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    public List<CodeLocationView> getAllCodeLocationsForCodeLocationType(final CodeLocationType codeLocationType) throws IntegrationException {
        final Request.Builder requestBuilder = RequestFactory.createCommonGetRequestBuilder().addQueryParameter("codeLocationType", codeLocationType.toString());
        final List<CodeLocationView> allCodeLocations = hubService.getAllResponsesFromPath(ApiDiscovery.CODELOCATIONS_LINK_RESPONSE, requestBuilder);
        return allCodeLocations;
    }

    public void unmapCodeLocations(final List<CodeLocationView> codeLocationItems) throws IntegrationException {
        for (final CodeLocationView codeLocationItem : codeLocationItems) {
            unmapCodeLocation(codeLocationItem);
        }
    }

    public void unmapCodeLocation(final CodeLocationView codeLocationItem) throws IntegrationException {
        final String codeLocationItemUrl = hubService.getHref(codeLocationItem);
        final CodeLocationView requestCodeLocationView = createRequestCodeLocationView(codeLocationItem, "");
        updateCodeLocation(codeLocationItemUrl, hubService.getGson().toJson(requestCodeLocationView));
    }

    public void mapCodeLocation(final CodeLocationView codeLocationItem, final ProjectVersionView version) throws IntegrationException {
        mapCodeLocation(codeLocationItem, hubService.getHref(version));
    }

    public void mapCodeLocation(final CodeLocationView codeLocationItem, final String versionUrl) throws IntegrationException {
        final String codeLocationItemUrl = hubService.getHref(codeLocationItem);
        final CodeLocationView requestCodeLocationView = createRequestCodeLocationView(codeLocationItem, versionUrl);
        updateCodeLocation(codeLocationItemUrl, hubService.getGson().toJson(requestCodeLocationView));
    }

    public void updateCodeLocation(final CodeLocationView codeLocationItem) throws IntegrationException {
        final String codeLocationItemUrl = hubService.getHref(codeLocationItem);
        updateCodeLocation(codeLocationItemUrl, hubService.getGson().toJson(codeLocationItem));
    }

    public void updateCodeLocation(final String codeLocationItemUrl, final String codeLocationItemJson) throws IntegrationException {
        final Request request = new Request.Builder(codeLocationItemUrl).method(HttpMethod.PUT).bodyContent(new BodyContent(codeLocationItemJson)).build();
        try (Response response = hubService.executeRequest(request)) {
        } catch (final IOException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    public void deleteCodeLocations(final List<CodeLocationView> codeLocationItems) throws IntegrationException {
        for (final CodeLocationView codeLocationItem : codeLocationItems) {
            deleteCodeLocation(codeLocationItem);
        }
    }

    public void deleteCodeLocation(final CodeLocationView codeLocationItem) throws IntegrationException {
        final String codeLocationItemUrl = hubService.getHref(codeLocationItem);
        deleteCodeLocation(codeLocationItemUrl);
    }

    public void deleteCodeLocation(final String codeLocationItemUrl) throws IntegrationException {
        final Request deleteRequest = new Request.Builder(codeLocationItemUrl).method(HttpMethod.DELETE).build();
        try (Response response = hubService.executeRequest(deleteRequest)) {
        } catch (final IOException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
    }

    public CodeLocationView getCodeLocationByName(final String codeLocationName) throws IntegrationException {
        if (StringUtils.isNotBlank(codeLocationName)) {
            final Request.Builder requestBuilder = RequestFactory.createCommonGetRequestBuilder().addQueryParameter("q", "name:" + codeLocationName);
            final List<CodeLocationView> codeLocations = hubService.getAllResponsesFromPath(ApiDiscovery.CODELOCATIONS_LINK_RESPONSE, requestBuilder);
            for (final CodeLocationView codeLocation : codeLocations) {
                if (codeLocationName.equals(codeLocation.name)) {
                    return codeLocation;
                }
            }
        }

        throw new DoesNotExistException("This Code Location does not exist. Code Location: " + codeLocationName);
    }

    public CodeLocationView getCodeLocationById(final String codeLocationId) throws IntegrationException {
        final String link = ApiDiscovery.CODELOCATIONS_LINK + "/" + codeLocationId;
        final LinkSingleResponse<CodeLocationView> codeLocationResponse = new LinkSingleResponse<>(link, CodeLocationView.class);
        return hubService.getResponseFromPath(codeLocationResponse);
    }

    private CodeLocationView createRequestCodeLocationView(final CodeLocationView codeLocationItem, final String versionUrl) {
        final CodeLocationView requestCodeLocationView = new CodeLocationView();
        requestCodeLocationView.createdAt = codeLocationItem.createdAt;
        requestCodeLocationView.mappedProjectVersion = versionUrl;
        requestCodeLocationView.name = codeLocationItem.name;
        requestCodeLocationView.type = codeLocationItem.type;
        requestCodeLocationView.updatedAt = codeLocationItem.updatedAt;
        requestCodeLocationView.url = codeLocationItem.url;
        return requestCodeLocationView;
    }

    public ScanSummaryView getScanSummaryViewById(final String scanSummaryId) throws IntegrationException {
        final String uri = HubService.SCANSUMMARIES_LINK + "/" + scanSummaryId;
        return hubService.getResponse(uri, ScanSummaryView.class);
    }

}
