import {randomUUID, replaceToHtmlTag} from './util.js'

const GREETING_MESSAGE    = "안녕하세요. LLM TEST BOT 입니다.\n관련 문서 및 질의, 시스템 프롬프트를 기반으로 답변 드리겠습니다."
const SERVICE_NAME        = "llm"
const TAB_ID              = randomUUID();
const QUERY_EVENT_NAME    = `/${SERVICE_NAME}/query/${TAB_ID}`;
const ANSWER_EVENT_NAME   = `/${SERVICE_NAME}/answer/${TAB_ID}`;
const ANSWER_START_PREFIX = "[ANSWER_START]";
const ANSWER_END_PREFIX   = "[ANSWER_END]";

const content      = document.getElementById("content");
const sendBtn      = document.getElementById("sendBtn");
const userInput    = document.getElementById("userInput");
const contextInput    = document.getElementById("contextInput");
const promptInput    = document.getElementById("promptInput");

let sendBtnEnable = true;
let currentLlmMsg = null;
let eventSource = null;

// 입력 단 비 활성화
const disableInput = () => {
    sendBtnEnable = false;
    sendBtn.hidden = true;
    userInput.disabled = true;
    contextInput.disabled = true;
    promptInput.disabled = true;
};

// 입력 단 활성화
const enableInput = () => {
    sendBtnEnable = true;
    sendBtn.hidden = false;
    userInput.disabled = false;
    contextInput.disabled = false;
    promptInput.disabled = false;
};

// 질의 전송 요청
const sendQuery = () => {
    if (userInput.value.trim() === "") return;
    if (contextInput.value.trim() === "") return;
    if (promptInput.value.trim() === "") return;
    else if (!sendBtnEnable) return;
    else disableInput();

    console.log(`📡 질의 요청 : ${userInput.value}`);

    fetch(`/${SERVICE_NAME}/chat`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            tabId: TAB_ID,
            query: userInput.value,
            context: contextInput.value,
            prompt: promptInput.value,
        })
    })
        .then(response=> {
            if (response.ok) {
                userInput.value = "";
                contextInput.value = "";
                promptInput.value = "";
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
    // 세션 기반 SSE 연결
    eventSource = new EventSource(`/${SERVICE_NAME}/stream/${TAB_ID}`);

    eventSource.addEventListener("open", () => {
        console.log("📡 SSE 연결 열림");
        disableInput();
    });

    eventSource.addEventListener("error", (event) => {
        console.log(`❌ 에러 또는 연결 끊김 발생: ${event.type}`);
        enableInput();
    });

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
            console.log("📋 답변 시작");
            currentLlmMsg = document.createElement("div");
            currentLlmMsg.className = "message answer";
            content.appendChild(currentLlmMsg);
            return;
        }
        if (event.data === ANSWER_END_PREFIX) {
            console.log("❌ 답변 끝");
            currentLlmMsg = null;
            return;
        }
        if (currentLlmMsg) {
            currentLlmMsg.innerHTML += event.data;
            currentLlmMsg.innerHTML = replaceToHtmlTag(currentLlmMsg.innerHTML);
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

    // 그리팅
    if (GREETING_MESSAGE.length > 0) {
        const greetingMsg = document.createElement("div");
        greetingMsg.className = "message answer";
        content.appendChild(greetingMsg);

        let index = 0;

        const interval = setInterval(() => {
            greetingMsg.innerHTML += GREETING_MESSAGE[index];
            greetingMsg.innerHTML = replaceToHtmlTag(greetingMsg.innerHTML);
            content.scrollTop = content.scrollHeight;
            index++;
            if (index >= GREETING_MESSAGE.length) {
                clearInterval(interval);
            }
        }, 10);
    }
}
