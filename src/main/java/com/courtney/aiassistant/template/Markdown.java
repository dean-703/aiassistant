package com.courtney.aiassistant.template;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.html.MutableAttributes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Markdown {

    private static final Set<String> SAFE_PROTOCOLS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList("http", "https", "mailto")));

    private static final MutableDataSet OPTIONS = new MutableDataSet()
            .set(Parser.EXTENSIONS, Arrays.asList(
                    TablesExtension.create(),
                    AutolinkExtension.create(),
                    StrikethroughExtension.create(),
                    TaskListExtension.create(),
                    FootnoteExtension.create(),
                    EmojiExtension.create(),
                    TocExtension.create()
            ))
            .set(HtmlRenderer.ESCAPE_HTML, true)
            .set(HtmlRenderer.SUPPRESS_HTML, true)
            .set(HtmlRenderer.SOFT_BREAK, "<br/>")
            .set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
            .set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "language-");

    private static final Parser PARSER = Parser.builder(OPTIONS).build();

    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS)
            .attributeProviderFactory(new IndependentAttributeProviderFactory() {
                @Override
                public AttributeProvider apply(LinkResolverContext context) {
                    return new SafeLinkAttributeProvider();
                }
            })
            .build();

    private Markdown() {}

    public static String render(String markdown) {
        try {
            if (markdown == null || markdown.isEmpty()) return "";
            Node document = PARSER.parse(markdown);
            return RENDERER.render(document);
        } catch (Exception e) {
            return escapeHtml(markdown).replace("\n", "<br/>");
        }
    }

    private static class SafeLinkAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(Node node, AttributablePart part, MutableAttributes attributes) {
            if (part == AttributablePart.LINK) {
                attributes.replaceValue("target", "_blank");
                attributes.replaceValue("rel", "noopener noreferrer");

                String url = attributes.getValue("href");
                if (url != null) {
                    String lower = url.toLowerCase();
                    int idx = lower.indexOf(':');
                    if (idx > 0) {
                        String scheme = lower.substring(0, idx);
                        if (!SAFE_PROTOCOLS.contains(scheme)) {
                            attributes.replaceValue("href", "#");
                        }
                    }
                }
            }
        }
    }

    private static String escapeHtml(String s) {
        return s == null ? "" : s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}