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
package com.blackducksoftware.integration.hub.api.service;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.item.MetaService;
import com.blackducksoftware.integration.hub.api.vulnerability.VulnerabilityRequestService;
import com.blackducksoftware.integration.hub.api.vulnerablebomcomponent.VulnerableBomComponentRequestService;
import com.blackducksoftware.integration.hub.dataservice.project.ProjectDataService;
import com.blackducksoftware.integration.hub.dataservice.project.ProjectVersionWrapper;
import com.blackducksoftware.integration.hub.model.view.VulnerabilityView;
import com.blackducksoftware.integration.hub.model.view.VulnerableComponentView;
import com.blackducksoftware.integration.hub.rest.RestConnectionTestHelper;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.test.TestLogger;

public class VulnerableBomComponentRequestServiceTestIT {

    private final static RestConnectionTestHelper restConnectionTestHelper = new RestConnectionTestHelper();

    private static final IntLogger logger = new TestLogger();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws IllegalArgumentException, IntegrationException {
        final HubServicesFactory hubServicesFactory = restConnectionTestHelper.createHubServicesFactory();
        final VulnerableBomComponentRequestService vulnerableBomComponentRequestService = hubServicesFactory.createVulnerableBomComponentRequestService();
        final MetaService metaService = hubServicesFactory.createMetaService(logger);
        
        final ProjectDataService projectDataService = hubServicesFactory.createProjectDataService(logger);
        ProjectVersionWrapper projectVersionWrapper = projectDataService.getProjectVersion(restConnectionTestHelper.getProperty("TEST_PROJECT"), restConnectionTestHelper.getProperty("TEST_VERSION_VULNERABLE"));
        
        String vulnerableComponentsURL = metaService.getFirstLinkSafely(projectVersionWrapper.getProjectVersionView(), MetaService.VULNERABLE_COMPONENTS_LINK);

        List<VulnerableComponentView> vulnerableComponents = vulnerableBomComponentRequestService.getVulnerableComponentsMatchingComponentName(vulnerableComponentsURL);
        assertNotNull(vulnerableComponents);
        assertFalse(vulnerableComponents.isEmpty());
        
        for(VulnerableComponentView vc: vulnerableComponents){
        	System.out.println(vc);
        	assertNotNull(vc);
        }
        
        List<VulnerableComponentView> vulnerableComponentViews = vulnerableBomComponentRequestService.getVulnerableComponentsMatchingComponentName(vulnerableComponentsURL, "Apache Commons FileUpload");
        assertNotNull(vulnerableComponentViews);
        assertFalse(vulnerableComponentViews.isEmpty());
    }

}
