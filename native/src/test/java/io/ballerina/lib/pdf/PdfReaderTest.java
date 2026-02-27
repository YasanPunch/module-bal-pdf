/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.pdf;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfReaderTest {

    // --- validateNotInternalAddress: IPv4 private ranges ---

    @Test
    void rejectsLoopbackAddress() throws URISyntaxException {
        URI uri = new URI("http://127.0.0.1/file.pdf");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("private or internal"));
    }

    @Test
    void rejectsLocalhost() throws URISyntaxException {
        URI uri = new URI("http://localhost/file.pdf");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("private or internal"));
    }

    @Test
    void rejectsPrivateClassA() throws URISyntaxException {
        // 10.0.0.0/8
        URI uri = new URI("http://10.0.0.1/file.pdf");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("private or internal"));
    }

    @Test
    void rejectsPrivateClassB() throws URISyntaxException {
        // 172.16.0.0/12
        URI uri = new URI("http://172.16.0.1/file.pdf");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("private or internal"));
    }

    @Test
    void rejectsPrivateClassC() throws URISyntaxException {
        // 192.168.0.0/16
        URI uri = new URI("http://192.168.1.1/file.pdf");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("private or internal"));
    }

    @Test
    void rejectsLinkLocal() throws URISyntaxException {
        // 169.254.0.0/16 — cloud metadata endpoint
        URI uri = new URI("http://169.254.169.254/metadata");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("private or internal"));
    }

    @Test
    void rejectsAnyLocalAddress() throws URISyntaxException {
        // 0.0.0.0
        URI uri = new URI("http://0.0.0.0/file.pdf");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("private or internal"));
    }

    // --- validateNotInternalAddress: IPv6 ---

    @Test
    void rejectsIpv6Loopback() throws URISyntaxException {
        URI uri = new URI("http://[::1]/file.pdf");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("private or internal"));
    }

    // --- validateNotInternalAddress: error cases ---

    @Test
    void rejectsNullHost() throws URISyntaxException {
        URI uri = new URI("http", null, "/path", null);
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("no host"));
    }

    @Test
    void rejectsUnresolvableHost() throws URISyntaxException {
        URI uri = new URI("http://this-host-definitely-does-not-exist.invalid/file.pdf");
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.validateNotInternalAddress(uri));
        assertTrue(ex.getMessage().contains("Cannot resolve host"));
    }

    // --- validateNotInternalAddress: allowed addresses ---

    @Test
    void acceptsPublicAddress() throws URISyntaxException {
        URI uri = new URI("https://example.com/file.pdf");
        assertDoesNotThrow(() -> PdfReader.validateNotInternalAddress(uri));
    }

    // --- loadFromUrl: scheme validation ---

    @Test
    void loadFromUrlRejectsFtpScheme() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.loadFromUrl("ftp://example.com/file.pdf"));
        assertTrue(ex.getMessage().contains("Unsupported URL scheme"));
    }

    @Test
    void loadFromUrlRejectsFileScheme() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.loadFromUrl("file:///etc/passwd"));
        assertTrue(ex.getMessage().contains("Unsupported URL scheme"));
    }

    @Test
    void loadFromUrlRejectsInvalidUri() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfReader.loadFromUrl("not a valid uri at all"));
        assertTrue(ex.getMessage().contains("Invalid URL"));
    }
}
