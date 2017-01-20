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
package com.blackducksoftware.integration.hub.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.blackducksoftware.integration.hub.exception.MissingUUIDException;

public class HubUrlParserTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testGetUUIDFromURLNullIdentifier() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No identifier was provided.");
        HubUrlParser.getUUIDFromURL(null, null);
    }

    @Test
    public void testGetUUIDFromURLBlankIdentifier() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No identifier was provided.");
        HubUrlParser.getUUIDFromURL("", null);
    }

    @Test
    public void testGetUUIDFromURLNullURL() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No URL was provided to parse.");
        final String identifier = "none";
        HubUrlParser.getUUIDFromURL(identifier, null);
    }

    @Test
    public void testGetUUIDFromURLNoUUID() throws Exception {
        final String identifier = "none";
        final URL url = new URL("http://google");
        exception.expect(MissingUUIDException.class);
        exception.expectMessage("The String provided : " + url
                + ", does not contain any UUID's for the specified identifer : " + identifier);
        HubUrlParser.getUUIDFromURL(identifier, url);
    }

    @Test
    public void testGetUUIDFromURLNoUUIDFromIdentifier() throws Exception {
        final UUID uuid = UUID.randomUUID();
        final String identifier = "none";
        final URL url = new URL("http://google/" + uuid + "/");
        exception.expect(MissingUUIDException.class);
        exception.expectMessage("The String provided : " + url
                + ", does not contain any UUID's for the specified identifer : " + identifier);
        HubUrlParser.getUUIDFromURL(identifier, url);
    }

    @Test
    public void testGetUUIDFromURL() throws Exception {
        final UUID uuid = UUID.randomUUID();
        final String identifier = "google";
        final URL url = new URL("http://google/" + uuid + "/");
        final UUID uuidFound = HubUrlParser.getUUIDFromURL(identifier, url);
        assertNotNull(uuidFound);
        assertEquals(uuid, uuidFound);
    }

    @Test
    public void testGetUUIDFromURLMultipleUUIDs() throws Exception {
        final UUID uuid = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();
        final UUID uuid3 = UUID.randomUUID();
        final UUID uuid4 = UUID.randomUUID();
        final String identifier = "google";
        final URL url = new URL(
                "http://" + uuid + "/google/" + uuid2 + "/version/" + uuid3 + "/component/" + uuid4 + "/");
        final UUID uuidFound = HubUrlParser.getUUIDFromURL(identifier, url);
        assertNotNull(uuidFound);
        assertEquals(uuid2, uuidFound);
    }

    @Test
    public void testGetUUIDFromURLStringNullIdentifier() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No identifier was provided.");
        HubUrlParser.getUUIDFromURLString(null, null);
    }

    @Test
    public void testGetUUIDFromURLStringBlankIdentifier() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No identifier was provided.");
        HubUrlParser.getUUIDFromURLString("", null);
    }

    @Test
    public void testGetUUIDFromURLStringNullURLString() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("No url String was provided to parse.");
        final String identifier = "none";
        HubUrlParser.getUUIDFromURLString(identifier, null);
    }

    @Test
    public void testGetUUIDFromURLStringNoUUID() throws Exception {
        final String identifier = "none";
        final String url = "http://google";
        exception.expect(MissingUUIDException.class);
        exception.expectMessage("The String provided : " + url
                + ", does not contain any UUID's for the specified identifer : " + identifier);
        HubUrlParser.getUUIDFromURLString(identifier, url);
    }

    @Test
    public void testGetUUIDFromURLStringNoUUIDFromIdentifier() throws Exception {
        final UUID uuid = UUID.randomUUID();
        final String identifier = "none";
        final String url = "http://google/" + uuid + "/";
        exception.expect(MissingUUIDException.class);
        exception.expectMessage("The String provided : " + url
                + ", does not contain any UUID's for the specified identifer : " + identifier);
        HubUrlParser.getUUIDFromURLString(identifier, url);
    }

    @Test
    public void testGetUUIDFromURLString() throws Exception {
        final UUID uuid = UUID.randomUUID();
        final String identifier = "google";
        final String url = "http://google/" + uuid + "/";
        final UUID uuidFound = HubUrlParser.getUUIDFromURLString(identifier, url);
        assertNotNull(uuidFound);
        assertEquals(uuid, uuidFound);
    }

    @Test
    public void testGetUUIDFromURLStringMultipleUUIDs() throws Exception {
        final UUID uuid = UUID.randomUUID();
        final UUID uuid2 = UUID.randomUUID();
        final UUID uuid3 = UUID.randomUUID();
        final UUID uuid4 = UUID.randomUUID();
        final String identifier = "google";
        final String url = "http://" + uuid + "/google/" + uuid2 + "/version/" + uuid3 + "/component/" + uuid4 + "/";
        final UUID uuidFound = HubUrlParser.getUUIDFromURLString(identifier, url);
        assertNotNull(uuidFound);
        assertEquals(uuid2, uuidFound);
    }

    @Test
    public void testGetBaseUrl() throws URISyntaxException {
        assertEquals("http://server.bds.com:8080/", HubUrlParser.getBaseUrl("http://server.bds.com:8080/api/projects"));
        assertEquals("https://server.bds.com:8080/",
                HubUrlParser.getBaseUrl("https://server.bds.com:8080/api/projects"));
        assertEquals("http://server.bds.com/", HubUrlParser.getBaseUrl("http://server.bds.com/api/projects"));
    }

    @Test
    public void testGetRelativeUrl() throws URISyntaxException {
        assertEquals("api/projects", HubUrlParser.getRelativeUrl("http://server.bds.com:8080/api/projects"));
        assertEquals("api/projects", HubUrlParser.getRelativeUrl("https://server.bds.com:8080/api/projects"));
        assertEquals("api/projects", HubUrlParser.getRelativeUrl("http://server.bds.com/api/projects"));

        assertEquals(null, HubUrlParser.getRelativeUrl(null));
    }
}