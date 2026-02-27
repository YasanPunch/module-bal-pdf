## Overview

The `ballerina/pdf` module provides functionality to convert HTML content to PDF documents. It processes HTML strings — including full documents, fragments, and messy real-world markup — and produces PDF byte arrays suitable for writing to files or sending over the network.

All processing is done locally with no external service dependencies. The module handles HTML cleanup, CSS injection, and PDF rendering in a single pipeline.

## Quickstart

### Convert HTML to PDF

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check pdf:parseHtml("<h1>Hello World</h1><p>Generated with Ballerina.</p>");
    check io:fileWriteBytes("output.pdf", pdfBytes);
}
```

### Convert with custom options

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    string html = check io:fileReadString("report.html");

    byte[] pdfBytes = check pdf:parseHtml(html,
        fallbackFontSize = 10.0,
        pageSize = pdf:LETTER,
        margins = {top: 72, right: 54, bottom: 72, left: 54},
        additionalCss = "body { font-family: sans-serif; } .container { width: 100% !important; }"
    );

    check io:fileWriteBytes("report.pdf", pdfBytes);
}
```

### Extract text from a PDF

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check io:fileReadBytes("document.pdf");
    string[] pages = check pdf:extractText(pdfBytes);
    foreach int i in 0 ..< pages.length() {
        io:println("Page ", i + 1, ": ", pages[i]);
    }
}
```

You can also extract text directly from a file path or URL:

```ballerina
string[] pages = check pdf:fileExtractText("document.pdf");
string[] pages = check pdf:urlExtractText("https://example.com/document.pdf");
```

### Convert PDF pages to images

```ballerina
import ballerina/io;
import ballerina/pdf;

public function main() returns error? {
    byte[] pdfBytes = check io:fileReadBytes("document.pdf");
    string[] base64Images = check pdf:toImages(pdfBytes);
    // Each element is a Base64-encoded PNG string (one per page)
}
```

File and URL variants are also available: `fileToImages()` and `urlToImages()`.

## Examples

The `pdf` module provides practical examples illustrating usage in various scenarios. Explore these [examples](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/).

1. [CIBIL credit report conversion](https://github.com/ballerina-platform/module-ballerina-pdf/tree/main/examples/cibil-report/) — Converts a complex, table-heavy CIBIL credit report HTML file to PDF with consumer-specific CSS overrides and custom page options.

## Known limitations

The HTML/CSS renderer supports CSS 2.1 core layout (block, inline, float, table, absolute/relative positioning) but has gaps compared to browser rendering. Key limitations:

- **Layout:** No flexbox, CSS Grid, or multi-column layout. No `position: fixed` or `position: sticky`.
- **Tables:** No `rowspan`, no `<caption>`, no `table-layout: fixed` algorithm.
- **Text:** No `text-align: justify`, no hyphenation, no `text-indent`, no `text-overflow: ellipsis`.
- **CSS features:** No `::before`/`::after` pseudo-elements, no CSS counters, no `calc()`, no custom properties (`var()`), no `@import`, no `@media` queries (print/all media types are supported).
- **Visual:** No CSS gradients, no `text-shadow`, no CSS transforms, no SVG rendering. Only `solid` border style is supported.
- **Fonts:** No `@font-face` (use the `customFonts` option instead). No OpenType features. Bundled fonts: Liberation Sans and Liberation Serif (metrically compatible with Arial and Times New Roman).
- **Page control:** No `page-break-inside: avoid`, no orphans/widows control, no `@page` margin boxes.
