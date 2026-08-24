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
        summary: "Future grounded RAG chat interface with citations and retrieval transparency.",
        details: ["Ask research questions", "Render cited source papers", "Show retrieval/debug context"]
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
let runtimeConfig = null;
let runtimeConfigError = null;

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

function renderSettingsPage(page) {
    const configMarkup = runtimeConfig
        ? `
            <div class="config-grid">
                <div class="config-item">
                    <span>Application</span>
                    <strong>${runtimeConfig.applicationName}</strong>
                </div>
                <div class="config-item">
                    <span>Configured profile</span>
                    <strong>${runtimeConfig.configuredProfile}</strong>
                </div>
                <div class="config-item">
                    <span>Active Spring profiles</span>
                    <strong>${runtimeConfig.activeProfiles.join(", ")}</strong>
                </div>
            </div>
            <h3>Feature flags</h3>
            <ul class="feature-flags">
                ${runtimeConfig.features.map((feature) => `
                    <li>
                        <span>${feature.name}</span>
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

async function refreshBackendStatus() {
    try {
        const response = await fetch("/actuator/health", {headers: {"Accept": "application/json"}});
        if (!response.ok) {
            throw new Error(`Health endpoint returned ${response.status}`);
        }

        const health = await response.json();
        environmentPill.textContent = `Backend ${health.status ?? "UP"}`;
        environmentPill.classList.toggle("ok", health.status === "UP");
    } catch {
        environmentPill.textContent = "Backend unavailable";
        environmentPill.classList.remove("ok");
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
        runtimeConfigError = "Runtime configuration is unavailable.";
    }

    if ((window.location.hash.replace("#/", "") || "system-status") === "settings") {
        renderPage();
    }
}

window.addEventListener("hashchange", renderPage);
renderPage();
refreshBackendStatus();
refreshRuntimeConfig();
