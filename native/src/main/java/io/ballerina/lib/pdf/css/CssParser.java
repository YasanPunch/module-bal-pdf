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

package io.ballerina.lib.pdf.css;

import io.ballerina.lib.pdf.util.DomUtils;
import org.htmlunit.cssparser.dom.AbstractCSSRuleImpl;
import org.htmlunit.cssparser.dom.CSSMediaRuleImpl;
import org.htmlunit.cssparser.dom.CSSPageRuleImpl;
import org.htmlunit.cssparser.dom.CSSRuleListImpl;
import org.htmlunit.cssparser.dom.CSSStyleDeclarationImpl;
import org.htmlunit.cssparser.dom.CSSStyleRuleImpl;
import org.htmlunit.cssparser.dom.CSSStyleSheetImpl;
import org.htmlunit.cssparser.dom.Property;
import org.htmlunit.cssparser.parser.CSSErrorHandler;
import org.htmlunit.cssparser.parser.CSSException;
import org.htmlunit.cssparser.parser.CSSOMParser;
import org.htmlunit.cssparser.parser.CSSParseException;
import org.htmlunit.cssparser.parser.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CSS from <style> blocks and inline style attributes.
 * Uses htmlunit-cssparser (JavaCC-generated CSS3 parser) for robust handling of
 * at-rules, nested braces, and other CSS constructs.
 */
public class CssParser {

    private static final CSSErrorHandler SILENT_ERROR_HANDLER = new CSSErrorHandler() {
        @Override
        public void warning(CSSParseException exception) throws CSSException { }
        @Override
        public void error(CSSParseException exception) throws CSSException { }
        @Override
        public void fatalError(CSSParseException exception) throws CSSException { }
    };

    /**
     * Parses all <style> blocks in the document into a stylesheet.
     */
    public CssStylesheet parse(Document document) {
        return parse(document, null);
    }

    /**
     * Parses all <style> blocks in the document, then appends rules from
     * additionalCss. The additional rules get strictly higher source order
     * indices, so they win cascade ties at equal specificity.
     */
    public CssStylesheet parse(Document document, String additionalCss) {
        CssStylesheet stylesheet = new CssStylesheet();
        // sourceOrder is used to track the order of the rules in the stylesheet.
        // The first rule has a sourceOrder of 0, the second rule 1, etc.
        // This is used to resolve ties in the cascade.
        int[] sourceOrder = {0};

        // 1. Find all <style> elements in the document.
        List<Element> styleElements = DomUtils.findAll(document, "style");
        for (Element styleEl : styleElements) {
            String cssText = styleEl.getTextContent();
            if (cssText == null || cssText.isBlank()) {
                continue;
            }

            // 2. Parse the CSS text using htmlunit-cssparser.
            CSSStyleSheetImpl sheet = parseCssText(cssText);
            if (sheet == null) {
                continue;
            }

            // 3. Extract the rules from the parsed sheet and add them to the stylesheet.
            CSSRuleListImpl rules = (CSSRuleListImpl) sheet.getCssRules();
            extractRules(rules, stylesheet, sourceOrder);
        }

        // Parse additionalCss — appended AFTER document rules.
        // sourceOrder counter continues from where <style> parsing left off,
        // so these rules have strictly higher source order and win ties at equal specificity.
        if (additionalCss != null && !additionalCss.isBlank()) {
            CSSStyleSheetImpl sheet = parseCssText(additionalCss);
            if (sheet != null) {
                // 4. Extract the additional parsed CSS rules and add them to the stylesheet.
                CSSRuleListImpl rules = (CSSRuleListImpl) sheet.getCssRules();
                extractRules(rules, stylesheet, sourceOrder);
            }
        }

        return stylesheet;
    }

    /**
     * Parses a CSS string into a CSSStyleSheetImpl using htmlunit-cssparser.
     * We return a CSSStyleSheetImpl because it is a parsed CSS document that contains the rules.
     */
    private CSSStyleSheetImpl parseCssText(String cssText) {
        try {
            // Parse the CSS text using htmlunit-cssparser.
            CSSOMParser cssParser = new CSSOMParser();
            // Set the error handler to silently ignore errors.
            // Real world CSS is messy and we don't want to fail the parsing. We want to render what we can. 
            cssParser.setErrorHandler(SILENT_ERROR_HANDLER);
            InputSource source = new InputSource(new StringReader(cssText));
            return cssParser.parseStyleSheet(source, null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Extracts style rules, page rules, and media print rules from a parsed rule list.
     */
    private void extractRules(CSSRuleListImpl rules, CssStylesheet stylesheet, int[] sourceOrder) {
        for (AbstractCSSRuleImpl rule : rules.getRules()) {

            //use a switch statement instead of if-else if-else
            if (rule instanceof CSSStyleRuleImpl styleRule) {
                addStyleRule(styleRule, stylesheet, sourceOrder); //regular style rules
            } else if (rule instanceof CSSPageRuleImpl pageRule) {
                addPageRule(pageRule, stylesheet); //@page rules
            } else if (rule instanceof CSSMediaRuleImpl mediaRule) {
                if (matchesPrintMedia(mediaRule)) { //@media print rules. discard other media types.
                    CSSRuleListImpl innerRules = (CSSRuleListImpl) mediaRule.getCssRules();
                    // Recurse into the inner rules to extract the rules.
                    extractRules(innerRules, stylesheet, sourceOrder);
                }
            }
            // @font-face, @keyframes, @supports, @import, unknown at-rules are not supported yet.
        }
    }

    /**
     * Converts a CSSStyleRuleImpl into one or more CssRule objects (one per comma-separated selector)
     * and adds them to the stylesheet.
     * A Style rule is a selector/s (p, div, etc.) and the corresponding list of declarations
     * (color: red, margin: 0, etc.) that are applied to the selector.
     */
    private void addStyleRule(CSSStyleRuleImpl styleRule, CssStylesheet stylesheet, int[] sourceOrder) {
        String selectorText = styleRule.getSelectorText();
        CSSStyleDeclarationImpl decl = (CSSStyleDeclarationImpl) styleRule.getStyle();
        List<CssDeclaration> declarations = convertDeclarations(decl);
        if (declarations.isEmpty()) {
            return;
        }

        // Handle comma-separated selectors
        String[] selectors = selectorText.split(",");
        for (String sel : selectors) {
            sel = sel.trim();
            // Strip redundant universal selector prefix (cssparser canonicalization: .foo → *.foo)
            if (sel.startsWith("*.") || sel.startsWith("*#")) {
                sel = sel.substring(1);
            }
            if (sel.isEmpty()) {
                continue;
            }
            CssSelector selector = new CssSelector(sel);
            stylesheet.addRule(new CssRule(selector, declarations, sourceOrder[0]++));
        }
    }

    /**
     * Extracts declarations from a @page rule and adds them to the stylesheet.
     * A Page rule applies to the entire page and is used to set the page size and margins.
     */
    private void addPageRule(CSSPageRuleImpl pageRule, CssStylesheet stylesheet) {
        CSSStyleDeclarationImpl decl = (CSSStyleDeclarationImpl) pageRule.getStyle();
        List<CssDeclaration> declarations = convertDeclarations(decl);
        for (CssDeclaration d : declarations) {
            stylesheet.addPageDeclaration(d);
        }
    }

    /**
     * Checks whether a @media rule's media list includes "print" or "all", i.e., applicable for PDF rendering.
     * Media means the output medium - what device/format displays content. 
     * print - the CSS will be applied only when the output is printed or rendered.
     * all - the CSS will be applied to all media.
     * other media (like screen) are not applicable for PDF rendering.
     */
    private boolean matchesPrintMedia(CSSMediaRuleImpl mediaRule) {
        String mediaText = mediaRule.getMediaList().getMediaText();
        if (mediaText == null || mediaText.isBlank()) {
            return false;
        }
        String lower = mediaText.toLowerCase();
        // Accept "print", "all", or lists containing either (e.g., "screen, print")
        for (String medium : lower.split(",")) {
            String trimmed = medium.trim();
            if (trimmed.equals("print") || trimmed.equals("all")) { // avoid magic strings
                return true;
            }
        }
        return false;
    }

    /**
     * Converts a CSSStyleDeclarationImpl to a list of CssDeclaration records.
     */
    private List<CssDeclaration> convertDeclarations(CSSStyleDeclarationImpl decl) {
        List<CssDeclaration> result = new ArrayList<>();
        for (Property prop : decl.getProperties()) {
            String propName = prop.getName();
            String propValue = prop.getValue().getCssText();
            boolean important = prop.isImportant();
            if (propName != null && !propName.isEmpty() && propValue != null && !propValue.isEmpty()) {
                result.add(new CssDeclaration(propName.toLowerCase(), propValue, important));
            }
        }
        return result;
    }

    /**
     * Parses an inline style attribute into a list of declarations.
     */
    public List<CssDeclaration> parseInlineStyle(String style) {
        if (style == null || style.isBlank()) {
            return List.of();
        }
        return parseDeclarations(style);
    }

    /**
     * Parses a CSS declaration block (semicolon-separated property:value pairs).
     * Used for inline styles where a full stylesheet parse is unnecessary.
     */
    public static List<CssDeclaration> parseDeclarations(String block) {
        List<CssDeclaration> declarations = new ArrayList<>();
        if (block == null || block.isBlank()) {
            return declarations;
        }

        // Split by semicolons but not inside parentheses (for data URLs, rgb(), etc.)
        // Splits "color: red; font-size: 14px"
        // into ["color: red", "font-size: 14px"]
        String[] parts = splitDeclarations(block);

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }

            int colonIdx = part.indexOf(':');
            if (colonIdx < 0) {
                continue;
            }

            String property = part.substring(0, colonIdx).trim().toLowerCase();
            String value = part.substring(colonIdx + 1).trim();

            // Check for !important
            boolean important = false;
            if (value.toLowerCase().contains("!important")) {
                important = true;
                value = value.replaceAll("!\\s*important", "").trim();
            }

            if (!property.isEmpty() && !value.isEmpty()) {
                declarations.add(new CssDeclaration(property, value, important));
            }
        }

        return declarations;
    }

    /**
     * Splits a declaration block by semicolons, respecting parentheses and quotes.
     */
    private static String[] splitDeclarations(String block) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < block.length(); i++) {
            char ch = block.charAt(i);

            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (ch == '(') {
                    parenDepth++;
                } else if (ch == ')') {
                    parenDepth = Math.max(0, parenDepth - 1);
                } else if (ch == ';' && parenDepth == 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            current.append(ch);
        }
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }

        return parts.toArray(new String[0]);
    }
}
