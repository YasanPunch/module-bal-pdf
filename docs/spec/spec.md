_Owners_: @yashodhanmohandevan \
_Reviewers_: @yashodhanmohandevan \
_Created_: 2025/01/01 \
_Updated_: 2026/02/28 \
_Edition_: Swan Lake

# Specification: Ballerina PDF Module

## Introduction

This is the specification for the `pdf` module of the [Ballerina language](https://ballerina.io/), which provides functionality for HTML-to-PDF conversion and PDF reading operations.

The `pdf` module specification has evolved over time. This specification is written to describe the functionality available from version 0.9.0 onwards.

If you have any feedback or suggestions about the module, start a discussion via a [GitHub issue](https://github.com/ballerina-platform/ballerina-library/issues) or in the [Discord server](https://discord.gg/ballerinalang). Based on the outcome of the discussion, the specification and implementation can be updated. Community contributions are also encouraged. If you notice an implementation that deviates from the specification, please raise an issue.

## Contents

1. [Overview](#1-overview)
2. [Configurations](#2-configurations)
   - 2.1. [ConversionOptions](#21-conversionoptions)
   - 2.2. [PageSize](#22-pagesize)
   - 2.3. [PageMargins](#23-pagemargins)
   - 2.4. [Font](#24-font)
3. [Functions](#3-functions)
   - 3.1. [HTML-to-PDF Conversion](#31-html-to-pdf-conversion)
   - 3.2. [Text Extraction](#32-text-extraction)
   - 3.3. [Image Conversion](#33-image-conversion)
4. [Error Types](#4-error-types)
5. [Samples](#5-samples)
   - 5.1. [Basic HTML-to-PDF Conversion](#51-basic-html-to-pdf-conversion)
   - 5.2. [Conversion with Custom Options](#52-conversion-with-custom-options)
   - 5.3. [Text Extraction](#53-text-extraction)
   - 5.4. [Image Conversion](#54-image-conversion)

## 1. Overview

The `ballerina/pdf` module provides:

- **HTML-to-PDF conversion**: Convert HTML strings (full documents, fragments, or messy real-world markup) to PDF byte arrays.
- **Text extraction**: Extract text content from existing PDF documents (from bytes, file paths, or URLs).
- **Image conversion**: Convert PDF pages to Base64-encoded PNG images (from bytes, file paths, or URLs).

All processing is performed locally with no external service dependencies. The module uses a custom HTML/CSS renderer with Apache PDFBox for PDF generation.

## 2. Configurations

### 2.1. ConversionOptions

Controls HTML-to-PDF conversion behavior. All fields have defaults and are optional.

| Field | Type | Default | Description |
|---|---|---|---|
| `fallbackFontSize` | `float` | `12.0` | Fallback font size in points (CSS spec "medium"). CSS `font-size` declarations take precedence. |
| `pageSize` | `PageSize` | `A4` | Page size preset or custom dimensions. |
| `margins` | `PageMargins` | `{}` | Page margins in points (all zeros by default). |
| `additionalCss` | `string?` | `nil` | Additional CSS to inject before conversion. |
| `customFonts` | `Font[]?` | `nil` | Custom TTF fonts to register. Referenced via `font-family` in CSS. |
| `maxPages` | `int?` | `nil` | Maximum pages in output. Content is scaled to fit. Must be > 0 when provided. |

`ConversionOptions` uses the spread parameter pattern (`*ConversionOptions`) so fields can be passed as named arguments directly to `parseHtml()`.

### 2.2. PageSize

A union type: `StandardPageSize|CustomPageSize`.

**StandardPageSize** (enum):

| Value | Dimensions (points) |
|---|---|
| `A4` | 595 x 842 |
| `LETTER` | 612 x 792 |
| `LEGAL` | 612 x 1008 |

**CustomPageSize** (record):

| Field | Type | Description |
|---|---|---|
| `width` | `float` | Page width in points (1 point = 1/72 inch) |
| `height` | `float` | Page height in points |

### 2.3. PageMargins

| Field | Type | Default | Description |
|---|---|---|---|
| `top` | `float` | `0` | Top margin in points |
| `right` | `float` | `0` | Right margin in points |
| `bottom` | `float` | `0` | Bottom margin in points |
| `left` | `float` | `0` | Left margin in points |

### 2.4. Font

| Field | Type | Default | Description |
|---|---|---|---|
| `family` | `string` | — | CSS `font-family` name used to reference this font |
| `content` | `byte[]` | — | TTF font file content |
| `bold` | `boolean` | `false` | Whether this is a bold variant |
| `italic` | `boolean` | `false` | Whether this is an italic variant |

The module bundles Liberation Sans and Liberation Serif fonts (metrically compatible with Arial and Times New Roman). Custom fonts supplement these bundled fonts.

## 3. Functions

All functions are `isolated` and return the data type or `Error`.

### 3.1. HTML-to-PDF Conversion

```ballerina
isolated function parseHtml(string html, *ConversionOptions options) returns byte[]|Error
```

Converts an HTML string to PDF bytes. Accepts full HTML documents, fragments, or messy real-world markup. The pipeline: HTML preprocessing (cleanup, CSS injection) → DOM parsing → layout → PDF rendering.

Returns `HtmlParseError` if the HTML cannot be parsed, or `RenderError` if the rendering pipeline fails.

### 3.2. Text Extraction

```ballerina
isolated function extractText(byte[] pdf) returns string[]|Error
isolated function fileExtractText(string filePath) returns string[]|Error
isolated function urlExtractText(string url) returns string[]|Error
```

Extracts text content from each page of a PDF document. Returns a `string[]` where each element contains the text of one page. The three variants accept PDF bytes, a local file path, or a URL respectively.

Returns `ReadError` on failure.

### 3.3. Image Conversion

```ballerina
isolated function toImages(byte[] pdf) returns string[]|Error
isolated function fileToImages(string filePath) returns string[]|Error
isolated function urlToImages(string url) returns string[]|Error
```

Converts each page of a PDF document to a Base64-encoded PNG image. Returns a `string[]` where each element is a Base64-encoded PNG string. The three variants accept PDF bytes, a local file path, or a URL respectively.

Returns `ReadError` on failure.

## 4. Error Types

The module defines a distinct error hierarchy:

```
Error (base)
├── HtmlParseError  — HTML parsing or preprocessing failure
├── RenderError     — PDF rendering pipeline failure (layout, painting, generation)
└── ReadError       — PDF reading failure (text extraction, image conversion)
```

All error types are `distinct` subtypes of the base `Error` type.

## 5. Samples

### 5.1. Basic HTML-to-PDF Conversion

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check pdf:parseHtml("<h1>Hello World</h1><p>Generated with Ballerina.</p>");
    check io:fileWriteBytes("output.pdf", pdfBytes);
}
```

### 5.2. Conversion with Custom Options

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    string html = check io:fileReadString("report.html");

    byte[] pdfBytes = check pdf:parseHtml(html,
        fallbackFontSize = 10.0,
        pageSize = pdf:LETTER,
        margins = {top: 72, right: 54, bottom: 72, left: 54},
        additionalCss = "body { font-family: sans-serif; }"
    );

    check io:fileWriteBytes("report.pdf", pdfBytes);
}
```

### 5.3. Text Extraction

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    string[] pages = check pdf:fileExtractText("document.pdf");
    foreach int i in 0 ..< pages.length() {
        io:println("Page ", i + 1, ": ", pages[i]);
    }
}
```

### 5.4. Image Conversion

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check io:fileReadBytes("document.pdf");
    string[] base64Images = check pdf:toImages(pdfBytes);
    // Each element is a Base64-encoded PNG string (one per page)
}
```
