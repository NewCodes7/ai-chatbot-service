(function () {
    var state = {
        token: null,
        page: 0,
        totalPages: 1,
        isSaving: false
    };

    function $(id) {
        return document.getElementById(id);
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function apiHeaders(extra) {
        return Object.assign({
            "Authorization": "Bearer " + state.token
        }, extra || {});
    }

    async function readError(response) {
        try {
            var body = await response.json();
            return body.message || body.error || "요청을 처리하지 못했습니다.";
        } catch (error) {
            return "요청을 처리하지 못했습니다.";
        }
    }

    function setStatus(text, busy) {
        var pill = $("statusPill");
        pill.textContent = text;
        pill.className = busy ? "status-pill busy" : "status-pill";
    }

    function setMessage(message, type) {
        var messageEl = $("formMessage");
        messageEl.textContent = message;
        messageEl.className = "form-message " + (type || "");
    }

    function setSaving(saving) {
        state.isSaving = saving;
        $("submitButton").disabled = saving;
        $("submitButton").textContent = saving ? "등록 중..." : "피드백 등록";
    }

    function selectedRating() {
        var checked = document.querySelector("input[name='isPositive']:checked");
        return checked ? checked.value === "true" : true;
    }

    function formatDate(value) {
        return new Date(value).toLocaleString("ko-KR");
    }

    function statusLabel(status) {
        var labels = {
            PENDING: "검토 대기",
            APPROVED: "승인",
            REJECTED: "반려"
        };
        return labels[status] || status;
    }

    function renderList(items) {
        var list = $("feedbackList");
        if (!items || items.length === 0) {
            list.innerHTML = '<div class="empty-state feedback-empty">' +
                '<strong>아직 등록된 피드백이 없습니다.</strong>' +
                '<p>대화 화면에서 채팅 ID를 확인한 뒤 답변 품질을 남겨보세요.</p>' +
                '</div>';
            return;
        }

        list.innerHTML = items.map(function (item) {
            var toneClass = item.isPositive ? "positive" : "negative";
            var toneText = item.isPositive ? "도움됨" : "개선 필요";
            var nextValue = item.isPositive ? "false" : "true";
            var nextText = item.isPositive ? "개선 필요로 변경" : "도움됨으로 변경";
            return '<article class="feedback-item" data-feedback-id="' + item.id + '">' +
                '<div class="feedback-item-main">' +
                '<div>' +
                '<span class="feedback-id">#' + item.id + ' · Chat ' + item.chatId + '</span>' +
                '<strong class="feedback-tone ' + toneClass + '">' + toneText + '</strong>' +
                '</div>' +
                '<p>' + escapeHtml(formatDate(item.createdAt)) + '</p>' +
                '</div>' +
                '<div class="feedback-meta">' +
                '<span>' + escapeHtml(statusLabel(item.status)) + '</span>' +
                '<div class="feedback-actions">' +
                '<button class="secondary-button compact" type="button" data-action="toggle" data-value="' + nextValue + '">' + nextText + '</button>' +
                '<button class="link-button compact danger-link" type="button" data-action="delete">삭제</button>' +
                '</div>' +
                '</div>' +
                '</article>';
        }).join("");
    }

    function updatePager() {
        var total = Math.max(state.totalPages, 1);
        $("pageInfo").textContent = (state.page + 1) + " / " + total;
        $("prevButton").disabled = state.page <= 0;
        $("nextButton").disabled = state.page + 1 >= total;
    }

    async function loadFeedbacks() {
        setStatus("불러오는 중", true);

        var sort = $("sortSelect").value;
        var response = await fetch("/feedbacks?page=" + state.page + "&size=8&sort=" + encodeURIComponent(sort), {
            headers: apiHeaders()
        });

        if (response.status === 401 || response.status === 403) {
            localStorage.removeItem("chatbotToken");
            window.location.href = "/login";
            return;
        }

        if (!response.ok) {
            setStatus(await readError(response), false);
            return;
        }

        var page = await response.json();
        state.totalPages = page.totalPages || 1;
        if (state.page >= state.totalPages && state.page > 0) {
            state.page = state.totalPages - 1;
            await loadFeedbacks();
            return;
        }

        renderList(page.content || []);
        updatePager();
        setStatus("대기 중", false);
    }

    async function createFeedback(event) {
        event.preventDefault();
        if (state.isSaving) return;

        var chatId = Number($("chatIdInput").value);
        if (!chatId || chatId < 1) {
            setMessage("채팅 ID를 입력해 주세요.", "error");
            $("chatIdInput").focus();
            return;
        }

        setSaving(true);
        setMessage("", "");

        try {
            var response = await fetch("/feedbacks", {
                method: "POST",
                headers: apiHeaders({ "Content-Type": "application/json" }),
                body: JSON.stringify({ chatId: chatId, isPositive: selectedRating() })
            });

            if (!response.ok) {
                setMessage(await readError(response), "error");
                return;
            }

            $("feedbackForm").reset();
            document.querySelector("input[name='isPositive'][value='true']").checked = true;
            state.page = 0;
            setMessage("피드백이 등록되었습니다.", "success");
            await loadFeedbacks();
        } catch (error) {
            setMessage("네트워크 오류가 발생했습니다.", "error");
        } finally {
            setSaving(false);
        }
    }

    async function updateFeedback(id, isPositive) {
        setStatus("수정 중", true);
        var response = await fetch("/feedbacks/" + id, {
            method: "PATCH",
            headers: apiHeaders({ "Content-Type": "application/json" }),
            body: JSON.stringify({ isPositive: isPositive })
        });

        if (!response.ok) {
            setStatus(await readError(response), false);
            return;
        }

        await loadFeedbacks();
    }

    async function deleteFeedback(id) {
        setStatus("삭제 중", true);
        var response = await fetch("/feedbacks/" + id, {
            method: "DELETE",
            headers: apiHeaders()
        });

        if (!response.ok) {
            setStatus(await readError(response), false);
            return;
        }

        await loadFeedbacks();
    }

    function bindEvents() {
        $("feedbackForm").addEventListener("submit", createFeedback);
        $("refreshButton").addEventListener("click", loadFeedbacks);

        $("logoutButton").addEventListener("click", function () {
            localStorage.removeItem("chatbotToken");
            window.location.href = "/login";
        });

        $("sortSelect").addEventListener("change", function () {
            state.page = 0;
            loadFeedbacks();
        });

        $("prevButton").addEventListener("click", function () {
            if (state.page <= 0) return;
            state.page -= 1;
            loadFeedbacks();
        });

        $("nextButton").addEventListener("click", function () {
            if (state.page + 1 >= state.totalPages) return;
            state.page += 1;
            loadFeedbacks();
        });

        $("feedbackList").addEventListener("click", function (event) {
            var button = event.target.closest("[data-action]");
            if (!button) return;

            var item = button.closest("[data-feedback-id]");
            var id = item.dataset.feedbackId;
            if (button.dataset.action === "toggle") {
                updateFeedback(id, button.dataset.value === "true");
                return;
            }

            deleteFeedback(id);
        });
    }

    function init() {
        state.token = localStorage.getItem("chatbotToken");
        if (!state.token) {
            window.location.href = "/login";
            return;
        }

        bindEvents();
        var chatId = new URLSearchParams(window.location.search).get("chatId");
        if (chatId) {
            $("chatIdInput").value = chatId;
        }
        loadFeedbacks();
    }

    window.feedbackPage = { init: init };
})();
