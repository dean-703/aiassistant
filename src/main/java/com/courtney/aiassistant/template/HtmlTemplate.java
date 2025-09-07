package com.courtney.aiassistant.template;

import com.courtney.aiassistant.model.AppSettings;

public class HtmlTemplate {

    public static String baseHtml(AppSettings s) {
        String css = """
                :root {
                    --bg: #2b2b2b;
                    --panel: #3a3a3a;
                    --assistant: #4a4a4a;
                    --user: #5a5a5a;
                    --text: #e1e1e1;
                    --muted: #b6b6b6;
                    --accent: #4db8ff;
                }
                html, body {
                    margin: 0; padding: 0; background: var(--bg); color: var(--text);
                    font-family: -apple-system, Segoe UI, Roboto, Inter, "Noto Sans", Helvetica, Arial, sans-serif;
                }
                .container { padding: 18px; max-width: 1000px; margin: 0 auto; padding-right: 20px;  padding-left: 20px; }
                .msg { border-radius: 14px; padding: 14px 16px; margin: 12px 20px; line-height: 1.55;
                    box-shadow: 0 0 0 1px rgba(255,255,255,0.06) inset; }
                .msg.user { background: linear-gradient(180deg, var(--user), rgba(30,41,59,0.6)); }
                .msg.assistant { background: linear-gradient(180deg, var(--assistant), rgba(11,19,43,0.7)); }
                .msg .role { font-size: 12px; color: var(--muted); margin-bottom: 8px; }
                .msg .content { font-size: 16.5px; }
                .msg .content p { margin: 8px 0; }
                .msg .content h1, .msg .content h2, .msg .content h3, .msg .content h4, .msg .content h5, .msg .content h6 {
                    margin: 14px 0 8px; line-height: 1.25;
                }
                .msg .content ul, .msg .content ol { margin: 8px 0 8px 22px; }
                .msg .content blockquote { margin: 10px 0; padding-left: 12px; border-left: 4px solid rgba(255,255,255,0.15); color: var(--muted); }
                .msg .content code { background: rgba(255,255,255,0.06); padding: 2px 4px; border-radius: 6px; }
                .msg .content pre { background: rgba(255,255,255,0.06); padding: 10px; overflow: auto; border-radius: 10px; }
                .msg .content pre code { background: transparent; padding: 0; }
                .msg .content table { border-collapse: collapse; margin: 10px 0; }
                .msg .content table th, .msg .content table td { border: 1px solid rgba(255,255,255,0.15); padding: 6px 10px; }
                .msg .content a { color: var(--accent); text-decoration: none; }
                .msg .content a:hover { text-decoration: underline; }
                .footer { color: var(--muted); text-align: center; font-size: 12px; padding: 25px 10px; }
                """;

        String headCdn = """
                <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/highlight.js@11.9.0/styles/github-dark-dimmed.min.css">
                <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/dompurify@3.1.6/dist/purify.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/highlight.js@11.9.0/lib/common.min.js"></script>
                """;

        String js = """
                let container;

                // Streaming state
                let assistantCurrentEl = null;
                let assistantCurrentBuffer = "";
                let renderScheduled = false;
                let lastRenderTime = 0;
                const RENDER_INTERVAL_MS = 75; // throttle to avoid flicker

                function escHtml(s) {
                  if (s == null) return "";
                  return s.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
                }

                // Convert Markdown to sanitized HTML with optional highlighting
                function toHtml(s) {
                  try {
                    if (!s) return "";
                    if (typeof marked !== "undefined") {
                      if (!marked._chatConfigured) {
                        marked.setOptions({
                          gfm: true,
                          breaks: true,
                          smartLists: true,
                          headerIds: false,
                          mangle: false,
                          highlight: function(code, lang) {
                            try {
                              if (typeof hljs !== "undefined") {
                                if (lang && hljs.getLanguage(lang)) {
                                  return hljs.highlight(code, {language: lang}).value;
                                } else {
                                  return hljs.highlightAuto(code).value;
                                }
                              }
                            } catch (e) { /* ignore highlight errors */ }
                            return code;
                          }
                        });
                        marked._chatConfigured = true;
                      }
                      const rawHtml = marked.parse(s);
                      if (typeof DOMPurify !== "undefined") {
                        return DOMPurify.sanitize(rawHtml, {
                          ALLOWED_ATTR: ["href", "title", "alt", "src", "class", "target", "rel"]
                        });
                      } else {
                        return rawHtml;
                      }
                    } else {
                      return escHtml(s).replace(/\\n/g,"<br/>");
                    }
                  } catch (e) {
                    return escHtml(s).replace(/\\n/g,"<br/>");
                  }
                }

                function applyHighlight(rootEl) {
                  try {
                    if (typeof hljs === "undefined" || !rootEl) return;
                    const blocks = rootEl.querySelectorAll('pre code');
                    blocks.forEach(b => {
                      try { hljs.highlightElement(b); } catch (e) { /* ignore */ }
                    });
                  } catch (e) { /* ignore */ }
                }

                function ensureSafeLinks(rootEl) {
                  try {
                    if (!rootEl) return;
                    rootEl.querySelectorAll('a[href]').forEach(a => {
                      a.setAttribute('target', '_blank');
                      a.setAttribute('rel', 'noopener noreferrer');
                    });
                  } catch (e) { /* ignore */ }
                }

                // Throttled render of current assistant buffer as Markdown
                function scheduleRender() {
                  if (!assistantCurrentEl) return;
                  const now = (typeof performance !== "undefined" && performance.now) ? performance.now() : Date.now();

                  const doRender = () => {
                    try {
                      const html = toHtml(assistantCurrentBuffer);
                      assistantCurrentEl.innerHTML = html;
                      ensureSafeLinks(assistantCurrentEl);
                      applyHighlight(assistantCurrentEl);
                    } catch (e) {
                      assistantCurrentEl.innerHTML = escHtml(assistantCurrentBuffer).replace(/\\n/g,"<br/>");
                    } finally {
                      lastRenderTime = (typeof performance !== "undefined" && performance.now) ? performance.now() : Date.now();
                      renderScheduled = false;
                      // Auto-scroll to latest
                      //window.scrollTo(0, document.body.scrollHeight);
                    }
                  };

                  const elapsed = now - lastRenderTime;
                  if (elapsed >= RENDER_INTERVAL_MS) {
                    renderScheduled = true;
                    if (typeof requestAnimationFrame !== "undefined") {
                      requestAnimationFrame(doRender);
                    } else {
                      setTimeout(doRender, 0);
                    }
                  } else if (!renderScheduled) {
                    renderScheduled = true;
                    const delay = Math.max(0, RENDER_INTERVAL_MS - elapsed);
                    setTimeout(() => {
                      if (typeof requestAnimationFrame !== "undefined") {
                        requestAnimationFrame(doRender);
                      } else {
                        doRender();
                      }
                    }, delay);
                  }
                }

                function addMessage(role, text) {
                  const wrap = document.createElement('div');
                  wrap.className = 'msg ' + role;

                  const r = document.createElement('div');
                  r.className = 'role';
                  r.textContent = role === 'user' ? 'You' : 'Assistant';

                  const c = document.createElement('div');
                  c.className = 'content';

                  try {
                    const html = toHtml(text);
                    c.innerHTML = html;
                    ensureSafeLinks(c);
                    applyHighlight(c);
                  } catch (e) {
                    c.innerHTML = escHtml(text).replace(/\\n/g,"<br/>");
                  }

                  wrap.appendChild(r); wrap.appendChild(c);
                  container.appendChild(wrap);
                  //window.scrollTo(0, document.body.scrollHeight);
                }

                function addUserMessage(text){ addMessage('user', text); window.scrollTo(10, document.body.scrollHeight);}
                function addAssistantMessage(text){ addMessage('assistant', text); }
                function clearMessages(){ container.innerHTML = ''; }

                // Streaming helpers (formatted during stream)
                function beginAssistantMessage() {
                   const wrap = document.createElement('div');
                   wrap.className = 'msg assistant';

                   const r = document.createElement('div');
                   r.className = 'role';
                   r.textContent = 'Assistant';

                   const c = document.createElement('div');
                   c.className = 'content';

                   wrap.appendChild(r); wrap.appendChild(c);
                   container.appendChild(wrap);

                   assistantCurrentEl = c;
                   assistantCurrentBuffer = "";
                   renderScheduled = false;
                   lastRenderTime = 0;

                   // Initial empty render to ensure container exists
                   scheduleRender();
                }

                function appendAssistant(text) {
                   if (!assistantCurrentEl) return;
                   assistantCurrentBuffer += (text || "");
                   scheduleRender();
                }

                function endAssistantMessage() {
                   if (!assistantCurrentEl) return;
                   // Final immediate render to ensure the last tokens are shown
                   try {
                     const html = toHtml(assistantCurrentBuffer);
                     assistantCurrentEl.innerHTML = html;
                     ensureSafeLinks(assistantCurrentEl);
                     applyHighlight(assistantCurrentEl);
                   } catch (e) {
                     assistantCurrentEl.innerHTML = escHtml(assistantCurrentBuffer).replace(/\\n/g,"<br/>");
                   } finally {
                     assistantCurrentEl = null;
                     assistantCurrentBuffer = "";
                     renderScheduled = false;
                     //window.scrollTo(0, document.body.scrollHeight);
                   }
                }

                window.addEventListener('DOMContentLoaded', () => { container = document.querySelector('.container'); });
                """;

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Chat</title>
                  <style>""" + css + "</style>\n" +
                headCdn +
                "<script>" + js + "</script>\n" +
                "</head><body>" +
                "<div class='container'></div>" +
                "<div class='footer'>AI Assistant OpenAI Desktop Client" + "</div>" +
                "</body></html>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
