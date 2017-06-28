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

import com.blackducksoftware.integration.hub.model.enumeration.CodeLocationEnum

class PendingScansRequest {
    String projectName
    String projectVersionName
    CodeLocationEnum codeLocationType

    public static PendingScansRequest createCodeLocationRequest(String projectName, String projectVersionName, CodeLocationEnum codeLocationType) {
        return new PendingScansRequest(projectName, projectVersionName, codeLocationType)
    }

    public static PendingScansRequest createProjectVersionRequest(String projectName, String projectVersionName) {
        return new PendingScansRequest(projectName, projectVersionName, null)
    }

    private PendingScansRequest(String projectName, String projectVersionName, CodeLocationEnum codeLocationType) {
        this.projectName = projectName
        this.projectVersionName = projectVersionName
        this.codeLocationType = codeLocationType
    }

    boolean isCodeLocationRequest() {
        null != codeLocationType
    }
}