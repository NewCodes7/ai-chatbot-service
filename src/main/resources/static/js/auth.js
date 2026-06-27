(function () {
    function formToJson(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    async function readMessage(response) {
        try {
            const body = await response.json();
            return body.message || body.error || "요청을 처리하지 못했습니다.";
        } catch (error) {
            return "요청을 처리하지 못했습니다.";
        }
    }

    function setMessage(message, type) {
        const messageEl = document.getElementById("formMessage");
        messageEl.textContent = message;
        messageEl.className = "form-message " + type;
    }

    function setBusy(form, busy) {
        const button = form.querySelector("button[type='submit']");
        button.disabled = busy;
        button.textContent = busy ? "처리 중..." : button.dataset.label;
    }

    function initLogin() {
        const form = document.getElementById("loginForm");
        const button = form.querySelector("button[type='submit']");
        button.dataset.label = button.textContent;

        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            setBusy(form, true);
            setMessage("", "");

            const response = await fetch("/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formToJson(form))
            });

            if (!response.ok) {
                setMessage(await readMessage(response), "error");
                setBusy(form, false);
                return;
            }

            const body = await response.json();
            localStorage.setItem("chatbotToken", body.token);
            setMessage("로그인되었습니다. 대화 화면으로 이동합니다.", "success");
            setBusy(form, false);
            window.setTimeout(function () {
                window.location.href = "/app";
            }, 500);
        });
    }

    function initSignup() {
        const form = document.getElementById("signupForm");
        const button = form.querySelector("button[type='submit']");
        button.dataset.label = button.textContent;

        form.addEventListener("submit", async function (event) {
            event.preventDefault();
            setBusy(form, true);
            setMessage("", "");

            const response = await fetch("/signup", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formToJson(form))
            });

            if (!response.ok) {
                setMessage(await readMessage(response), "error");
                setBusy(form, false);
                return;
            }

            setMessage("회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.", "success");
            window.setTimeout(function () {
                window.location.href = "/login";
            }, 700);
        });
    }

    window.authPage = {
        initLogin: initLogin,
        initSignup: initSignup
    };
})();
