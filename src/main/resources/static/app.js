const TOKEN_KEY = "urlshort.token";
const USER_KEY = "urlshort.user";

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

function getToken() { return localStorage.getItem(TOKEN_KEY); }
function getUser() { return localStorage.getItem(USER_KEY); }

function setSession(token, username) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, username);
    refreshAuthUi();
}

function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    refreshAuthUi();
}

async function api(path, { method = "GET", body, auth = false } = {}) {
    const headers = {};
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (auth) {
        const token = getToken();
        if (!token) throw new Error("Not authenticated");
        headers["Authorization"] = `Bearer ${token}`;
    }
    const res = await fetch(path, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
    if (res.status === 204) return null;
    const text = await res.text();
    const data = text ? JSON.parse(text) : null;
    if (!res.ok) {
        const err = new Error(data?.message || data?.error || `HTTP ${res.status}`);
        err.status = res.status;
        err.body = data;
        throw err;
    }
    return data;
}

function showError(el, err) {
    const fieldErrs = err.body?.fieldErrors;
    let msg = err.message;
    if (fieldErrs && typeof fieldErrs === "object") {
        msg = Object.entries(fieldErrs).map(([k, v]) => `${k}: ${v}`).join("; ");
    }
    el.textContent = msg;
    el.hidden = false;
}

function hide(el) { el.hidden = true; }

function switchTab(name) {
    $$(".tab-btn").forEach(b => b.classList.toggle("active", b.dataset.tab === name));
    $$(".tab").forEach(t => t.classList.toggle("active", t.id === name));
    if (name === "my-urls") loadMyUrls();
}

function refreshAuthUi() {
    const authed = !!getToken();
    $$(".auth-only").forEach(el => el.hidden = !authed);
    $$(".anon-only").forEach(el => el.hidden = authed);
    $(".who").textContent = authed ? `Signed in as ${getUser()}` : "";
    if (!authed && $("#my-urls").classList.contains("active")) {
        switchTab("shorten");
    }
}

document.addEventListener("click", e => {
    if (e.target.matches(".tab-btn")) switchTab(e.target.dataset.tab);
    if (e.target.matches(".auth-tab-btn")) {
        const which = e.target.dataset.auth;
        $$(".auth-tab-btn").forEach(b => b.classList.toggle("active", b.dataset.auth === which));
        $$(".auth-form").forEach(f => f.classList.toggle("active", f.id === `${which}-form`));
    }
    if (e.target.id === "logout-btn") {
        clearSession();
        switchTab("shorten");
    }
});

$("#shorten-form").addEventListener("submit", async e => {
    e.preventDefault();
    const result = $("#shorten-result");
    const error = $("#shorten-error");
    hide(result); hide(error);

    const fd = new FormData(e.target);
    const body = { url: fd.get("url") };
    const alias = fd.get("customAlias")?.trim();
    if (alias) body.customAlias = alias;
    const expires = fd.get("expiresAt");
    if (expires) body.expiresAt = new Date(expires).toISOString();

    try {
        const data = await api("/api/shorten", { method: "POST", body, auth: !!getToken() });
        result.innerHTML = `Short URL: <a href="${data.shortUrl}" target="_blank">${data.shortUrl}</a>
            <button type="button" data-copy="${data.shortUrl}">Copy</button>`;
        result.hidden = false;
        e.target.reset();
        if (getToken() && $("#my-urls").classList.contains("active")) loadMyUrls();
    } catch (err) {
        showError(error, err);
    }
});

document.addEventListener("click", async e => {
    if (e.target.matches("[data-copy]")) {
        await navigator.clipboard.writeText(e.target.dataset.copy);
        e.target.textContent = "Copied";
        setTimeout(() => e.target.textContent = "Copy", 1200);
    }
});

$("#login-form").addEventListener("submit", async e => {
    e.preventDefault();
    const err = $(".error", e.target); hide(err);
    const fd = new FormData(e.target);
    try {
        const data = await api("/api/auth/login", {
            method: "POST",
            body: { username: fd.get("username"), password: fd.get("password") },
        });
        setSession(data.token, data.username);
        switchTab("my-urls");
        e.target.reset();
    } catch (ex) {
        showError(err, ex);
    }
});

$("#register-form").addEventListener("submit", async e => {
    e.preventDefault();
    const err = $(".error", e.target); hide(err);
    const fd = new FormData(e.target);
    try {
        const data = await api("/api/auth/register", {
            method: "POST",
            body: {
                username: fd.get("username"),
                email: fd.get("email"),
                password: fd.get("password"),
            },
        });
        setSession(data.token, data.username);
        switchTab("my-urls");
        e.target.reset();
    } catch (ex) {
        showError(err, ex);
    }
});

$("#refresh-urls").addEventListener("click", loadMyUrls);

async function loadMyUrls() {
    const tbody = $("#urls-table tbody");
    tbody.innerHTML = "";
    try {
        const urls = await api("/api/me/urls", { auth: true });
        $("#urls-empty").hidden = urls.length > 0;
        urls.forEach(u => tbody.appendChild(rowFor(u)));
    } catch (err) {
        if (err.status === 401) { clearSession(); switchTab("auth"); return; }
        $("#urls-empty").hidden = true;
        const tr = document.createElement("tr");
        tr.innerHTML = `<td colspan="5" class="error">${err.message}</td>`;
        tbody.appendChild(tr);
    }
}

function rowFor(u) {
    const tr = document.createElement("tr");
    const expired = u.expiresAt && new Date(u.expiresAt) < new Date();
    tr.innerHTML = `
        <td><a href="${u.shortUrl}" target="_blank">${u.shortCode}</a></td>
        <td title="${escapeAttr(u.originalUrl)}">${truncate(u.originalUrl, 60)}</td>
        <td>${formatDate(u.createdAt)}</td>
        <td class="${expired ? "expired" : ""}">${u.expiresAt ? formatDate(u.expiresAt) : "—"}</td>
        <td class="actions">
            <button data-stats="${u.id}">Stats</button>
            <button data-delete="${u.id}">Delete</button>
        </td>`;
    return tr;
}

document.addEventListener("click", async e => {
    if (e.target.matches("[data-delete]")) {
        const id = e.target.dataset.delete;
        if (!confirm("Delete this short URL?")) return;
        try {
            await api(`/api/url/${id}`, { method: "DELETE", auth: true });
            loadMyUrls();
        } catch (err) { alert(err.message); }
    }
    if (e.target.matches("[data-stats]")) {
        openStats(e.target.dataset.stats);
    }
});

async function openStats(id) {
    const modal = $("#stats-modal");
    $("#stats-for").textContent = `(id ${id})`;
    $("#stats-summary").innerHTML = "<dt>Loading…</dt><dd></dd>";
    $("#stats-uas").innerHTML = "";
    modal.showModal();
    try {
        const s = await api(`/api/url/${id}/stats`);
        $("#stats-summary").innerHTML = `
            <dt>Total clicks</dt><dd>${s.totalClicks}</dd>
            <dt>Last clicked</dt><dd>${s.lastClickedAt ? formatDate(s.lastClickedAt) : "—"}</dd>`;
        $("#stats-uas").innerHTML = (s.topUserAgents || []).length
            ? s.topUserAgents.map(x => `<li>${escapeHtml(x.label)} — ${x.count}</li>`).join("")
            : "<li class='hint'>None yet</li>";
    } catch (err) {
        $("#stats-summary").innerHTML = `<dt>Error</dt><dd>${err.message}</dd>`;
    }
}

function formatDate(iso) {
    const d = new Date(iso);
    return d.toLocaleString();
}

function truncate(s, n) { return s.length > n ? s.slice(0, n - 1) + "…" : s; }
function escapeHtml(s) { return s.replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
function escapeAttr(s) { return escapeHtml(s); }

refreshAuthUi();
