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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfProcessorTest {

    // --- loadFromUrl: scheme validation ---

    @Test
    void loadFromUrlRejectsFtpScheme() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromUrl("ftp://example.com/file.pdf"));
        assertTrue(ex.getMessage().contains("Unsupported URL scheme"));
    }

    @Test
    void loadFromUrlRejectsFileScheme() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromUrl("file:///etc/passwd"));
        assertTrue(ex.getMessage().contains("Unsupported URL scheme"));
    }

    @Test
    void loadFromUrlRejectsInvalidUri() {
        IOException ex = assertThrows(IOException.class,
                () -> PdfProcessor.loadFromUrl("not a valid uri at all"));
        assertTrue(ex.getMessage().contains("Invalid URL"));
    }
}
