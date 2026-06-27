(function () {
    var state = {
        token: null,
        activity: null,
        isLoading: false
    };

    function $(id) {
        return document.getElementById(id);
    }

    function setStatus(text, busy) {
        var pill = $("analyticsStatus");
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

    function formatNumber(value) {
        return Number(value || 0).toLocaleString("ko-KR");
    }

    function setMetric(id, value) {
        $(id).textContent = formatNumber(value);
    }

    function renderActivity(activity) {
        setMetric("signupCount", activity.signupCount);
        setMetric("loginCount", activity.loginCount);
        setMetric("chatCount", activity.chatCount);

        var total = Number(activity.signupCount || 0) + Number(activity.loginCount || 0) + Number(activity.chatCount || 0);
        $("reportPreview").innerHTML =
            "<strong>최근 24시간 총 활동 " + formatNumber(total) + "건</strong>" +
            "<span>리포트에는 같은 기간의 대화 데이터가 포함됩니다.</span>";
    }

    function handleUnauthorized() {
        localStorage.removeItem("chatbotToken");
        window.location.href = "/login";
    }

    function handleForbidden() {
        setStatus("권한 없음", false);
        $("downloadReportButton").disabled = true;
        $("reportPreview").innerHTML =
            "<strong>관리자 권한이 필요합니다.</strong>" +
            "<span>분석 API는 ADMIN 계정으로 로그인한 경우에만 사용할 수 있습니다.</span>";
    }

    async function loadActivity() {
        if (state.isLoading) return;
        state.isLoading = true;
        $("refreshAnalyticsButton").disabled = true;
        setStatus("불러오는 중", true);

        try {
            var response = await fetch("/analytics/activity", {
                headers: apiHeaders()
            });

            if (response.status === 401) {
                handleUnauthorized();
                return;
            }

            if (response.status === 403) {
                handleForbidden();
                return;
            }

            if (!response.ok) {
                setStatus(await readError(response), false);
                return;
            }

            state.activity = await response.json();
            renderActivity(state.activity);
            $("downloadReportButton").disabled = false;
            setStatus("최신", false);
        } catch (error) {
            setStatus("네트워크 오류", false);
        } finally {
            state.isLoading = false;
            $("refreshAnalyticsButton").disabled = false;
        }
    }

    async function downloadReport() {
        $("downloadReportButton").disabled = true;
        setStatus("리포트 생성 중", true);

        try {
            var response = await fetch("/analytics/report", {
                headers: apiHeaders()
            });

            if (response.status === 401) {
                handleUnauthorized();
                return;
            }

            if (response.status === 403) {
                handleForbidden();
                return;
            }

            if (!response.ok) {
                setStatus(await readError(response), false);
                return;
            }

            var blob = await response.blob();
            var url = URL.createObjectURL(blob);
            var link = document.createElement("a");
            link.href = url;
            link.download = "report.csv";
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
            setStatus("다운로드 완료", false);
        } catch (error) {
            setStatus("네트워크 오류", false);
        } finally {
            if (state.activity) {
                $("downloadReportButton").disabled = false;
            }
        }
    }

    function bindEvents() {
        $("refreshAnalyticsButton").addEventListener("click", loadActivity);
        $("downloadReportButton").addEventListener("click", downloadReport);
        $("logoutButton").addEventListener("click", function () {
            localStorage.removeItem("chatbotToken");
            window.location.href = "/login";
        });
    }

    function init() {
        state.token = localStorage.getItem("chatbotToken");
        if (!state.token) {
            window.location.href = "/login";
            return;
        }

        bindEvents();
        loadActivity();
    }

    window.analyticsPage = { init: init };
})();
