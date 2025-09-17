
const GREETING_MESSAGE    = "안녕하세요. AI BOT 입니다.\n질의를 작성해주시면 법령에 대한 문서를 기반으로 답변 드리겠습니다."
const SERVICE_NAME        = "aibot"
const TAB_ID = crypto.randomUUID();
const QUERY_EVENT_NAME    = `/${SERVICE_NAME}/query/${TAB_ID}`;
const ANSWER_EVENT_NAME   = `/${SERVICE_NAME}/answer/${TAB_ID}`;
const ANSWER_START_PREFIX = "[ANSWER_START]";
const ANSWER_END_PREFIX   = "[ANSWER_END]";

const content      = document.getElementById("content");
const sendBtn      = document.getElementById("sendBtn");
const userInput    = document.getElementById("userInput");

let sendBtnEnable = true;
let currentLlmMsg = null;
let eventSource = null;

// 세션 기반 SSE 연결


// 입력 단 비 활성화
const disableInput = () => {
    sendBtnEnable = false;
    sendBtn.hidden = true;
    userInput.disabled = true;
};

// 입력 단 활성화
const enableInput = () => {
    sendBtnEnable = true;
    sendBtn.hidden = false;
    userInput.disabled = false;
};

// 질의 전송 요청
const sendQuery = async () => {
    if (userInput.value.trim() === "") return;
    else if (!sendBtnEnable) return;
    else disableInput();

    fetch(`/${SERVICE_NAME}/chat`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            tabId: TAB_ID,
            query: userInput.value,
        })
    })
        .then(response=> {
            if (response.ok) {
                userInput.value = "";
            } else {
                alert(`[${response.status}] 서버 통신 오류`);
                enableInput();
            }
        })
        .catch(reason => {
            alert(reason);
            enableInput();
        });
};

// 첫 화면
window.onload = () => {
    eventSource = new EventSource(`/${SERVICE_NAME}/stream/${TAB_ID}`);

    // 질의 SSE 수신 이벤트
    eventSource.addEventListener(QUERY_EVENT_NAME, (event) => {
        const msgDiv = document.createElement("div");
        msgDiv.className = "message query";
        msgDiv.textContent = event.data;
        content.appendChild(msgDiv);
        content.scrollTop = content.scrollHeight;
    });

    // 답변 SSE 수신 이벤트
    eventSource.addEventListener(ANSWER_EVENT_NAME, (event) => {
        if (event.data === ANSWER_START_PREFIX) {
            currentLlmMsg = document.createElement("div");
            currentLlmMsg.className = "message answer";
            content.appendChild(currentLlmMsg);
            disableInput();
            return;
        }
        if (event.data === ANSWER_END_PREFIX) {
            currentLlmMsg = null;
            enableInput();
            return;
        }
        if (currentLlmMsg) {
            currentLlmMsg.innerHTML += event.data;
            currentLlmMsg.innerHTML = currentLlmMsg.innerHTML.replaceAll("\\n", "<br>")
            content.scrollTop = content.scrollHeight;
        }
    });

    // 전송 버튼 클릭 이벤트
    sendBtn.addEventListener("click", (_) => sendQuery());

    // 질의문 입력 키 다운 이벤트
    userInput.addEventListener("keydown", (event) => {
        if(event.key === 'Enter' && !event.isComposing) {
            sendQuery();
        }
    });

    if (GREETING_MESSAGE.length > 0) {
        disableInput();

        currentLlmMsg = document.createElement("div");
        currentLlmMsg.className = "message answer";
        content.appendChild(currentLlmMsg);

        let index = 0;

        const interval = setInterval(() => {
            currentLlmMsg.innerHTML += GREETING_MESSAGE[index];
            currentLlmMsg.innerHTML = currentLlmMsg.innerHTML.replaceAll("\\n", "<br>")
            content.scrollTop = content.scrollHeight;
            index++;
            if (index >= GREETING_MESSAGE.length) {
                clearInterval(interval);
                enableInput();
            }
        }, 50);
    }
}
