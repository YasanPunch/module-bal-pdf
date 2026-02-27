// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Converts an HTML report to PDF using the ballerina/pdf module.
//
// Usage:
//   cd examples/html-to-pdf
//   bal run
//
// Output: resources/output.pdf

import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    string html = check io:fileReadString("resources/report.html");
    byte[] pdfBytes = check pdf:parseHtml(html);
    check io:fileWriteBytes("resources/output.pdf", pdfBytes);
    io:println("Wrote output.pdf (" + pdfBytes.length().toString() + " bytes)");
}
