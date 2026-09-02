const pages = [
    {
        id: "system-status",
        title: "System Status",
        icon: "S",
        summary: "Health and runtime readiness for the Spring Boot backend.",
        details: ["Backend health probe", "Application identity", "Future database/vector-store status"],
        metrics: [
            ["Backend", "Checking..."],
            ["Relational DB", "Not wired"],
            ["Vector store", "Not wired"]
        ]
    },
    {
        id: "import",
        title: "Import",
        icon: "I",
        summary: "Dataset import controls and statistics for the arXiv metadata slice.",
        details: ["Upload or reference the dataset file", "Show category/date filters", "Display import progress and counts"]
    },
    {
        id: "papers",
        title: "Papers",
        icon: "P",
        summary: "Browse canonical paper records stored in PostgreSQL.",
        details: ["List imported papers", "Filter by category, author, and submitted date", "Open paper metadata and citations"]
    },
    {
        id: "search",
        title: "Search",
        icon: "Q",
        summary: "Exercise semantic and structured retrieval before chat generation is added.",
        details: ["Run top-K semantic search", "Show vector scores", "Resolve vector hits back to paper records"]
    },
    {
        id: "chat",
        title: "Chat",
        icon: "C",
        summary: "Ask research questions and receive answers grounded with canonical academic citations.",
        details: ["Ask questions in natural language", "Render inline citations automatically", "Explore resolved paper titles and authors in details drawers"]
    },
    {
        id: "vector-stores",
        title: "Vector Stores",
        icon: "V",
        summary: "Inspect and switch between pgvector, Milvus, and Chroma experiments.",
        details: ["Show active backend", "Compare index/config settings", "Display ingestion coverage by store"]
    },
    {
        id: "benchmarks",
        title: "Benchmarks",
        icon: "B",
        summary: "Capture ingestion, query latency, and quality comparison results.",
        details: ["Track p50/p95 retrieval latency", "Compare vector-store operations", "Record answer-quality evaluations"]
    },
    {
        id: "settings",
        title: "Settings",
        icon: "G",
        summary: "Central location for runtime configuration visibility.",
        details: ["Show active Spring profiles", "Expose non-secret feature flags", "Keep secrets and tokens hidden"]
    }
];

const navigation = document.querySelector("#navigation");
const pageTitle = document.querySelector("#page-title");
const pageContent = document.querySelector("#page-content");
const environmentPill = document.querySelector("#environment-pill");
const globalError = document.querySelector("#global-error");
const healthRefreshButton = document.querySelector("#health-refresh-button");
let runtimeConfig = null;
let runtimeConfigError = null;
let backendHealth = {
    status: "checking",
    message: "Checking backend health...",
    checkedAt: null
};

// Conversational RAG state
let chatHistory = [];

function escapeHtml(value) {
    if (value == null) return "";
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatCheckedAt(value) {
    return value ? new Date(value).toLocaleTimeString([], {hour: "2-digit", minute: "2-digit", second: "2-digit"}) : "Not checked yet";
}

function showGlobalError(message) {
    globalError.textContent = message;
    globalError.classList.remove("hidden");
}

function clearGlobalError() {
    globalError.textContent = "";
    globalError.classList.add("hidden");
}

function renderNavigation(activePageId) {
    navigation.innerHTML = pages
        .map((page) => `
            <a class="nav-link ${page.id === activePageId ? "active" : ""}" href="#/${page.id}">
                <span>${page.icon}</span>
                <span>${page.title}</span>
            </a>
        `)
        .join("");
}

function renderPage() {
    const route = window.location.hash.replace("#/", "") || "system-status";
    const page = pages.find((candidate) => candidate.id === route) ?? pages[0];

    pageTitle.textContent = page.title;
    renderNavigation(page.id);

    if (page.id === "settings") {
        renderSettingsPage(page);
        return;
    }

    if (page.id === "system-status") {
        renderSystemStatusPage(page);
        return;
    }

    if (page.id === "chat") {
        renderChatPage(page);
        return;
    }

    if (page.id === "benchmarks") {
        renderBenchmarksPage(page);
        return;
    }

    const metrics = page.metrics
        ? `<div class="metric-grid">${page.metrics.map(([label, value]) => `
            <div class="metric">
                <strong>${value}</strong>
                <span>${label}</span>
            </div>
        `).join("")}</div>`
        : "";

    pageContent.innerHTML = `
        <div class="hero-grid">
            <div>
                <h2>${page.title}</h2>
                <p class="placeholder-copy">${page.summary}</p>
                <ul class="feature-list">
                    ${page.details.map((detail) => `<li><strong>${detail}</strong><br><span>Placeholder area reserved for the upcoming feature story.</span></li>`).join("")}
                </ul>
            </div>
            <aside>
                <span class="status-badge">Placeholder</span>
                ${metrics}
            </aside>
        </div>
    `;
}

function renderSystemStatusPage(page) {
    const isUp = backendHealth.status === "UP";
    const isChecking = backendHealth.status === "checking";
    const statusLabel = backendHealth.status === "checking" ? "Checking..." : backendHealth.status;

    pageContent.innerHTML = `
        <div class="hero-grid">
            <div>
                <h2>${page.title}</h2>
                <p class="placeholder-copy">${page.summary}</p>
                <div class="health-panel ${isUp ? "ok" : isChecking ? "" : "error"}">
                    <div>
                        <span class="status-badge ${isUp ? "ok" : isChecking ? "" : "error"}">${escapeHtml(statusLabel)}</span>
                        <h3>Backend API</h3>
                        <p>${escapeHtml(backendHealth.message)}</p>
                        <small>Last checked: ${escapeHtml(formatCheckedAt(backendHealth.checkedAt))}</small>
                    </div>
                    <button class="primary-button" type="button" data-health-refresh>Refresh</button>
                </div>
                <ul class="feature-list">
                    ${page.details.map((detail) => `<li><strong>${detail}</strong><br><span>Placeholder area reserved for the upcoming feature story.</span></li>`).join("")}
                </ul>
            </div>
            <aside>
                <span class="status-badge ${isUp ? "ok" : isChecking ? "" : "error"}">${isUp ? "Ready" : isChecking ? "Checking" : "Needs attention"}</span>
                <div class="metric-grid">
                    <div class="metric">
                        <strong>${escapeHtml(statusLabel)}</strong>
                        <span>Backend</span>
                    </div>
                    <div class="metric">
                        <strong>${isUp ? "Wired" : "Not wired"}</strong>
                        <span>Relational DB</span>
                    </div>
                    <div class="metric">
                        <strong>${isUp ? "Wired" : "Not wired"}</strong>
                        <span>Vector store</span>
                    </div>
                </div>
            </aside>
        </div>
    `;
}

function renderSettingsPage(page) {
    const configMarkup = runtimeConfig
        ? `
            <div class="config-grid">
                <div class="config-item">
                    <span>Application</span>
                    <strong>${escapeHtml(runtimeConfig.applicationName)}</strong>
                </div>
                <div class="config-item">
                    <span>Configured profile</span>
                    <strong>${escapeHtml(runtimeConfig.configuredProfile)}</strong>
                </div>
                <div class="config-item">
                    <span>Active Spring profiles</span>
                    <strong>${escapeHtml(runtimeConfig.activeProfiles.join(", "))}</strong>
                </div>
            </div>
            <h3>Feature flags</h3>
            <ul class="feature-flags">
                ${runtimeConfig.features.map((feature) => `
                    <li>
                        <span>${escapeHtml(feature.name)}</span>
                        <strong class="${feature.enabled ? "enabled" : "disabled"}">
                            ${feature.enabled ? "Enabled" : "Disabled"}
                        </strong>
                    </li>
                `).join("")}
            </ul>
            <p class="safe-config-note">Only whitelisted runtime values are shown. Secrets, API keys, passwords, and tokens are never returned by this endpoint.</p>
        `
        : `<p class="placeholder-copy">${runtimeConfigError ?? "Loading runtime configuration..."}</p>`;

    pageContent.innerHTML = `
        <div>
            <h2>${page.title}</h2>
            <p class="placeholder-copy">${page.summary}</p>
            ${configMarkup}
        </div>
    `;
}

/**
 * Renders the Interactive Conversational RAG & Agentic Chat Page
 */
function renderChatPage(page) {
    pageContent.innerHTML = `
        <div class="chat-layout">
            <div class="chat-messages" id="chat-messages-container">
                <!-- Chat message bubbles injected here -->
            </div>
            <form class="chat-input-area" id="chat-form">
                <input class="chat-input" id="chat-input" type="text" placeholder="Ask a question grounded by your academic database..." autocomplete="off" required>
                <label class="agent-toggle-wrapper" for="agent-toggle">
                    <input type="checkbox" id="agent-toggle">
                    <span>Agent Mode</span>
                </label>
                <button class="primary-button" style="margin:0;" type="submit" id="chat-send-btn">Send</button>
            </form>
        </div>
    `;

    const messagesContainer = document.querySelector("#chat-messages-container");
    const chatForm = document.querySelector("#chat-form");
    const chatInput = document.querySelector("#chat-input");
    const agentToggle = document.querySelector("#agent-toggle");

    // 1. Initial render of existing history
    renderChatHistory(messagesContainer);

    // 2. Form submission handler
    chatForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const queryText = chatInput.value.trim();
        if (!queryText) return;

        const useAgent = agentToggle.checked;

        // Append user query bubble
        chatHistory.push({ sender: "user", text: queryText });
        
        // Append a typing placeholder bubble
        chatHistory.push({ sender: "bot", text: "Thinking...", isTyping: true });
        
        chatInput.value = "";
        renderChatHistory(messagesContainer);

        try {
            // Post to agent or standard RAG endpoint depending on toggle state
            const targetUrl = useAgent ? "/api/chat/agent" : "/api/chat";
            const response = await fetch(targetUrl, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify({ message: queryText, topK: 5 })
            });

            if (!response.ok) {
                throw new Error(`Chat endpoint returned ${response.status}`);
            }

            const data = await response.json();
            
            // Replace the typing placeholder with the real grounded answer
            chatHistory[chatHistory.length - 1] = {
                sender: "bot",
                text: data.responseText,
                citations: data.citations || []
            };
        } catch {
            chatHistory[chatHistory.length - 1] = {
                sender: "bot",
                text: "An error occurred while connecting to the backend. Please verify your PostgreSQL connection is active and try again."
            };
        } finally {
            renderChatHistory(messagesContainer);
        }
    });
}

function renderChatHistory(container) {
    if (chatHistory.length === 0) {
        container.innerHTML = `
            <div style="display:flex; flex-direction:column; justify-content:center; align-items:center; height:100%; color:var(--muted); text-align:center; padding: 2rem;">
                <span style="font-size:2.8rem; margin-bottom:0.75rem;">🤖</span>
                <h3>Conversational arXiv Agent</h3>
                <p style="max-width: 25rem; font-size:0.92rem; line-height:1.55;">Ask questions about your imported papers. Toggle <strong>Agent Mode</strong> to let the LLM autonomously decide which search tools to call and see its real-time thought trace!</p>
            </div>
        `;
        return;
    }

    container.innerHTML = chatHistory
        .map((msg) => {
            const isUser = msg.sender === "user";
            
            // Build citation cards accordion if citations are present
            let citationsAccordion = "";
            if (msg.citations && msg.citations.length > 0) {
                citationsAccordion = `
                    <details class="citations-accordion">
                        <summary>Grounded Context (${msg.citations.length} Cited Sources)</summary>
                        <div class="citations-list">
                            ${msg.citations.map((cit, idx) => `
                                <div class="citation-card">
                                    <a href="${escapeHtml(cit.url)}" target="_blank" rel="noopener noreferrer">[${idx + 1}] ${escapeHtml(cit.title)}</a>
                                    <span class="citation-meta">Authors: ${escapeHtml(cit.authors.join(", "))} | ID: ${escapeHtml(cit.arxivId)}</span>
                                </div>
                            `).join("")}
                        </div>
                    </details>
                `;
            }

            // Split and isolate Agent Thought Trace if present
            let finalHtml = "";
            const text = msg.text || "";
            const traceMarker = "🔧 **[Agent Thought Trace]**";
            
            if (text.startsWith(traceMarker)) {
                // Find where the trace finishes and the answer begins
                const parts = text.split("According to the");
                if (parts.length > 1) {
                    const traceContent = parts[0].replace(traceMarker, "").trim();
                    const mainAnswer = "According to the" + parts.slice(1).join("According to the");
                    
                    finalHtml = `
                        <div class="agent-trace">${escapeHtml(traceContent)}</div>
                        <div class="chat-bubble">${escapeHtml(mainAnswer)}</div>
                    `;
                } else {
                    finalHtml = `<div class="chat-bubble">${escapeHtml(text)}</div>`;
                }
            } else {
                finalHtml = `<div class="chat-bubble">${escapeHtml(text)}</div>`;
            }

            const bubbleClass = msg.isTyping ? "chat-bubble typing" : "";

            return `
                <div class="chat-bubble-wrapper ${msg.sender}">
                    ${msg.isTyping ? `<div class="${bubbleClass}">${escapeHtml(msg.text)}</div>` : finalHtml}
                    ${citationsAccordion}
                </div>
            `;
        })
        .join("");

    container.scrollTop = container.scrollHeight;
}

/**
 * Renders the Dynamic, In-Browser Benchmarks Telemetry Dashboard Page
 */
async function renderBenchmarksPage(page) {
    pageContent.innerHTML = `
        <div style="display:flex; flex-direction:column; justify-content:center; align-items:center; height:22rem; text-align:center; padding: 2rem;" id="benchmark-loader">
            <div class="spinner"></div>
            <h3>Running live performance benchmarks...</h3>
            <p style="color:var(--muted); max-width:25rem; font-size:0.9rem; line-height:1.6;">Triggering 50 concurrent semantic similarity queries on your active vector store to capture latency averages, medians (p50), and tail distributions (p95) dynamically.</p>
        </div>
        <div class="hidden" id="benchmark-results-container">
            <!-- Metric cards and tables injected here -->
        </div>
    `;

    const loader = document.querySelector("#benchmark-loader");
    const resultsContainer = document.querySelector("#benchmark-results-container");

    try {
        const response = await fetch("/api/benchmark", { headers: { "Accept": "application/json" } });
        if (!response.ok) {
            throw new Error(`Benchmark API returned status ${response.status}`);
        }
        const data = await response.json();
        
        loader.classList.add("hidden");
        resultsContainer.classList.remove("hidden");

        resultsContainer.innerHTML = `
            <div>
                <h2>Performance Bake-Off Dashboard</h2>
                <p class="placeholder-copy">Programmatic telemetry captured from 50 consecutive search queries against the active vector database.</p>
                
                <div class="benchmark-card">
                    <div style="display:flex; justify-content:space-between; align-items:center; border-bottom: 1px solid var(--border); padding-bottom: 0.75rem;">
                        <h3 style="margin:0;">Active Database: <span style="color:var(--accent); font-weight:800;">[${escapeHtml(data.activeStoreProfile)}]</span></h3>
                        <button class="primary-button" style="margin:0;" type="button" id="benchmark-rerun-btn">Re-Run Performance Test</button>
                    </div>
                    
                    <div class="benchmark-gauge-container">
                        <div class="benchmark-gauge">
                            <strong>${escapeHtml(data.qps.toFixed(2))}</strong>
                            <span>Queries Per Sec (QPS)</span>
                        </div>
                        <div class="benchmark-gauge">
                            <strong>${escapeHtml(data.avgLatencyMs.toFixed(2))} ms</strong>
                            <span>Average Latency</span>
                        </div>
                        <div class="benchmark-gauge">
                            <strong>${escapeHtml(data.p50LatencyMs.toFixed(2))} ms</strong>
                            <span>p50 (Median)</span>
                        </div>
                        <div class="benchmark-gauge">
                            <strong>${escapeHtml(data.p95LatencyMs.toFixed(2))} ms</strong>
                            <span>p95 (Tail Latency)</span>
                        </div>
                    </div>

                    <table class="benchmark-table">
                        <thead>
                            <tr>
                                <th>Metric Parameter</th>
                                <th>Measured Value</th>
                                <th>Description / SLA Interpretation</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td><strong>Minimum Latency</strong></td>
                                <td>${escapeHtml(data.minLatencyMs.toFixed(2))} ms</td>
                                <td>Fastest query execution recorded in this loop.</td>
                            </tr>
                            <tr>
                                <td><strong>p50 Latency (Median)</strong></td>
                                <td>${escapeHtml(data.p50LatencyMs.toFixed(2))} ms</td>
                                <td>Median performance boundary: 50% of your queries are faster than this threshold.</td>
                            </tr>
                            <tr>
                                <td><strong>Average Latency (Mean)</strong></td>
                                <td>${escapeHtml(data.avgLatencyMs.toFixed(2))} ms</td>
                                <td>Calculated average (arithmetic mean) of all 50 search runs.</td>
                            </tr>
                            <tr>
                                <td><strong>p95 Latency (SLA)</strong></td>
                                <td>${escapeHtml(data.p95LatencyMs.toFixed(2))} ms</td>
                                <td>SLA boundary: 95% of queries execute faster than this ceiling under peak load.</td>
                            </tr>
                            <tr>
                                <td><strong>Maximum Latency</strong></td>
                                <td>${escapeHtml(data.maxLatencyMs.toFixed(2))} ms</td>
                                <td>Slowest query execution recorded (typically representing initial database thread-locks).</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `;

        document.querySelector("#benchmark-rerun-btn").addEventListener("click", () => {
            renderBenchmarksPage(page);
        });

    } catch (e) {
        loader.innerHTML = `
            <span style="font-size:3rem; margin-bottom:1rem;">⚠️</span>
            <h3>Telemetry Benchmark Failed</h3>
            <p style="color:var(--danger); max-width:25rem; font-size:0.9rem; line-height:1.6;">${escapeHtml(e.message)}</p>
            <p style="color:var(--muted); max-width:25rem; font-size:0.86rem; margin-top:0.5rem;">Please ensure the target database container is running and healthy, and that the spring active profile is selected correctly.</p>
            <button class="primary-button" style="margin-top:1.25rem;" type="button" id="benchmark-retry-btn">Retry Benchmark</button>
        `;
        document.querySelector("#benchmark-retry-btn").addEventListener("click", () => {
            renderBenchmarksPage(page);
        });
    }
}

async function refreshBackendStatus() {
    backendHealth = {
        status: "checking",
        message: "Checking backend health...",
        checkedAt: null
    };
    updateBackendHealthUi();

    try {
        const response = await fetch("/api/health", {headers: {"Accept": "application/json"}});
        if (!response.ok) {
            throw new Error(`Health endpoint returned ${response.status}`);
        }

        const health = await response.json();
        backendHealth = {
            status: health.status ?? "UNKNOWN",
            message: health.message ?? "Backend responded, but did not include a health message.",
            checkedAt: health.checkedAt ?? new Date().toISOString()
        };
        clearGlobalError();
    } catch {
        backendHealth = {
            status: "UNAVAILABLE",
            message: "The backend API is not reachable. Confirm the Spring Boot service is running, then refresh the health check.",
            checkedAt: new Date().toISOString()
        };
        showGlobalError("Backend services are unavailable. Demo data may not load until the backend is running again.");
    } finally {
        updateBackendHealthUi();
    }
}

function updateBackendHealthUi() {
    const isUp = backendHealth.status === "UP";
    environmentPill.textContent = backendHealth.status === "checking" ? "Backend checking" : `Backend ${backendHealth.status}`;
    environmentPill.classList.toggle("ok", isUp);
    environmentPill.classList.toggle("error", backendHealth.status !== "checking" && !isUp);

    if ((window.location.hash.replace("#/", "") || "system-status") === "system-status") {
        renderPage();
    }
}

async function refreshRuntimeConfig() {
    try {
        const response = await fetch("/api/runtime-config", {headers: {"Accept": "application/json"}});
        if (!response.ok) {
            throw new Error(`Runtime config endpoint returned ${response.status}`);
        }

        runtimeConfig = await response.json();
        runtimeConfigError = null;
    } catch {
        runtimeConfig = null;
        runtimeConfigError = "Runtime configuration is unavailable. Check backend health, then use Refresh to try again.";
    }

    if ((window.location.hash.replace("#/", "") || "system-status") === "settings") {
        renderPage();
    }
}

window.addEventListener("hashchange", renderPage);
window.addEventListener("error", () => {
    showGlobalError("Something went wrong in the demo UI. Refresh the page or retry the last action.");
});
window.addEventListener("unhandledrejection", () => {
    showGlobalError("A backend request failed unexpectedly. Check service health and retry.");
});
healthRefreshButton.addEventListener("click", () => {
    refreshBackendStatus();
    refreshRuntimeConfig();
});
pageContent.addEventListener("click", (event) => {
    if (event.target instanceof Element && event.target.matches("[data-health-refresh]")) {
        refreshBackendStatus();
        refreshRuntimeConfig();
    }
});
renderPage();
refreshBackendStatus();
refreshRuntimeConfig();
