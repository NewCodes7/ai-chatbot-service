(function () {
    var state = {
        token: null,
        threads: [],
        selectedThreadId: null,
        isSending: false
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

    function setStatus(text, busy) {
        var pill = $("statusPill");
        pill.textContent = text;
        pill.className = busy ? "status-pill busy" : "status-pill";
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

    function selectedThread() {
        return state.threads.find(function (thread) {
            return thread.threadId === state.selectedThreadId;
        });
    }

    function renderThreads() {
        var list = $("threadList");
        if (state.threads.length === 0) {
            list.innerHTML = '<div class="thread-empty">아직 저장된 대화가 없습니다.</div>';
            return;
        }

        list.innerHTML = state.threads.map(function (thread) {
            var lastChat = thread.chats[thread.chats.length - 1];
            var title = lastChat ? lastChat.question : "빈 대화";
            var meta = lastChat ? new Date(lastChat.createdAt).toLocaleString("ko-KR") : "";
            var active = thread.threadId === state.selectedThreadId ? " active" : "";
            return '<button class="thread-item' + active + '" type="button" data-thread-id="' + thread.threadId + '">' +
                '<strong>' + escapeHtml(title) + '</strong>' +
                '<span>' + escapeHtml(meta) + '</span>' +
                '</button>';
        }).join("");
    }

    function renderMessages(chats, pending) {
        var list = $("messageList");
        var allChats = chats || [];

        if (allChats.length === 0 && !pending) {
            list.innerHTML = '<div class="empty-state">' +
                '<strong>질문을 입력하면 문서 검색 후 답변합니다.</strong>' +
                '<p>금융 상품, 투자 기초, 시장 지표 문서에 기반한 답변을 확인할 수 있습니다.</p>' +
                '</div>';
            return;
        }

        var html = allChats.map(function (chat) {
            return messagePairHtml(chat.question, chat.answer, "", chat.id);
        }).join("");

        if (pending) {
            html += messagePairHtml(pending.question, pending.answer || "답변을 생성하고 있습니다.", pending.answer ? "" : " thinking");
        }

        list.innerHTML = html;
        list.scrollTop = list.scrollHeight;
    }

    function messagePairHtml(question, answer, extraClass, chatId) {
        var feedbackAction = chatId
            ? '<a class="message-action" href="/feedback?chatId=' + encodeURIComponent(chatId) + '">Chat ID ' + chatId + ' · 피드백</a>'
            : "";
        return '<article class="message-pair">' +
            '<div class="message user-message"><span>질문</span><p>' + escapeHtml(question) + '</p></div>' +
            '<div class="message assistant-message' + (extraClass || "") + '">' +
            '<div class="message-label-row"><span>답변</span>' + feedbackAction + '</div>' +
            '<p>' + escapeHtml(answer) + '</p></div>' +
            '</article>';
    }

    async function loadThreads() {
        setStatus("기록 불러오는 중", true);
        var response = await fetch("/chats?size=20&sort=desc", {
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
        state.threads = page.content || [];
        if (!state.selectedThreadId && state.threads.length > 0) {
            state.selectedThreadId = state.threads[0].threadId;
        }
        renderThreads();
        renderMessages(selectedThread() ? selectedThread().chats : []);
        setStatus("대기 중", false);
    }

    function parseSseChunk(buffer, onData) {
        var events = buffer.split("\n\n");
        var rest = events.pop() || "";
        events.forEach(function (eventText) {
            eventText.split("\n").forEach(function (line) {
                if (line.indexOf("data:") === 0) {
                    onData(line.slice(5).trimStart());
                }
            });
        });
        return rest;
    }

    async function sendQuestion(question, isStreaming) {
        state.isSending = true;
        $("sendButton").disabled = true;
        setStatus("답변 생성 중", true);

        var baseChats = selectedThread() ? selectedThread().chats : [];
        var pending = { question: question, answer: "" };
        renderMessages(baseChats, pending);

        try {
            var response = await fetch("/chats", {
                method: "POST",
                headers: apiHeaders({ "Content-Type": "application/json" }),
                body: JSON.stringify({ question: question, isStreaming: isStreaming })
            });

            if (!response.ok || !response.body) {
                setStatus(await readError(response), false);
                return;
            }

            var reader = response.body.getReader();
            var decoder = new TextDecoder("utf-8");
            var buffer = "";

            while (true) {
                var result = await reader.read();
                if (result.done) break;
                buffer += decoder.decode(result.value, { stream: true });
                buffer = parseSseChunk(buffer, function (data) {
                    pending.answer += data;
                    renderMessages(baseChats, pending);
                });
            }

            buffer = parseSseChunk(buffer + "\n\n", function (data) {
                pending.answer += data;
                renderMessages(baseChats, pending);
            });

            await loadThreads();
        } catch (error) {
            setStatus("네트워크 오류가 발생했습니다.", false);
        } finally {
            state.isSending = false;
            $("sendButton").disabled = false;
            $("questionInput").focus();
        }
    }

    function bindEvents() {
        $("chatForm").addEventListener("submit", function (event) {
            event.preventDefault();
            if (state.isSending) return;

            var input = $("questionInput");
            var question = input.value.trim();
            if (!question) return;

            input.value = "";
            sendQuestion(question, $("streamingInput").checked);
        });

        $("threadList").addEventListener("click", function (event) {
            var button = event.target.closest("[data-thread-id]");
            if (!button) return;
            state.selectedThreadId = Number(button.dataset.threadId);
            renderThreads();
            renderMessages(selectedThread() ? selectedThread().chats : []);
        });

        $("newChatButton").addEventListener("click", function () {
            state.selectedThreadId = null;
            renderThreads();
            renderMessages([]);
            $("questionInput").focus();
        });

        $("refreshButton").addEventListener("click", loadThreads);

        $("logoutButton").addEventListener("click", function () {
            localStorage.removeItem("chatbotToken");
            window.location.href = "/login";
        });

        document.querySelectorAll("[data-prompt]").forEach(function (button) {
            button.addEventListener("click", function () {
                $("questionInput").value = button.dataset.prompt;
                $("questionInput").focus();
            });
        });
    }

    function init() {
        state.token = localStorage.getItem("chatbotToken");
        if (!state.token) {
            window.location.href = "/login";
            return;
        }

        bindEvents();
        loadThreads();
    }

    window.chatPage = { init: init };
})();
