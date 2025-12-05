import {randomUUID, renderMarkdownWithMermaid, replaceEventDataToText} from './util.js'

const GREETING_MESSAGE    = "안녕하세요. **LLM TEST BOT** 입니다.\n\n관련 문서 및 질의, 시스템 프롬프트를 기반으로 답변 드리겠습니다."
const SERVICE_NAME        = "llm"
const SESSION_ID          = randomUUID();
const QUERY_EVENT_NAME    = `/${SERVICE_NAME}/query/${SESSION_ID}`;
const ANSWER_EVENT_NAME   = `/${SERVICE_NAME}/answer/${SESSION_ID}`;
const INFERENCE_EVENT_NAME= `/${SERVICE_NAME}/inference/${SESSION_ID}`;
const STREAM_START_PREFIX = "[STREAM_START]";

const content         = document.getElementById("content");
const sendBtn         = document.getElementById("sendBtn");
const resetBtn        = document.getElementById("resetBtn");
const userInput       = document.getElementById("userInput");
const contextInput    = document.getElementById("contextInput");
const promptInput     = document.getElementById("promptInput");
const maxTokensInput  = document.getElementById("maxTokensInput");
const temperatureInput= document.getElementById("temperatureInput");
const topPInput       = document.getElementById("topPInput");

let btnEnable = true;
let currentLlmMsg = null;
let currentLlmText= null;
let currentInferenceMsg = null;
let currentInferenceText= null;
let currentInferenceRow = null;

// 입력 단 비 활성화
const disableInput = () => {
    btnEnable = false;
    sendBtn.hidden = true;
    resetBtn.hidden = true
    userInput.disabled = true;
    contextInput.disabled = true;
    promptInput.disabled = true;
    maxTokensInput.disabled = true;
    temperatureInput.disabled = true;
    topPInput.disabled = true;
};

// 입력 단 활성화
const enableInput = () => {
    btnEnable = true;
    sendBtn.hidden = false;
    resetBtn.hidden = false
    userInput.disabled = false;
    contextInput.disabled = false;
    promptInput.disabled = false;
    maxTokensInput.disabled = false;
    temperatureInput.disabled = false;
    topPInput.disabled = false;
};

// 질의 전송 요청
const sendQuery = (query) => {
    if (userInput.value.trim() === "") {
        alert("유저 프롬프트 입력 필요!");
        return;
    }
    // else if (contextInput.value.trim() === "") {
    //     alert("컨텍스트 입력 필요!");
    //     return;
    // }
    // else if (promptInput.value.trim() === "") {
    //     alert("시스템 프롬프트 입력 필요!");
    //     return;
    // }
    else if (maxTokensInput.value.trim() === "") {
        alert("MAX TOKENS 입력 필요!");
        return;
    } else if (temperatureInput.value.trim() === "") {
        alert("TEMPERATURE 입력 필요!");
        return;
    } else if (topPInput.value.trim() === "") {
        alert("TOP P 입력 필요!");
        return;
    } else if (!btnEnable) return;
    else disableInput();

    // 세션 기반 SSE 연결
    const eventSource = new EventSource(`/${SERVICE_NAME}/stream/${SESSION_ID}`);

    eventSource.addEventListener("error", (event) => {
        // 답변 로그
        console.log(currentLlmText);
        console.log(`❌ 에러 또는 연결 끊김 발생`);

        currentLlmMsg = null;
        currentLlmText = null;
        currentInferenceMsg = null;
        currentInferenceText = null;
        currentInferenceRow = null;
        eventSource.close();
        enableInput();
    });

    eventSource.addEventListener("open", () => {
        console.log("📡 SSE 연결 열림");
        sendQueryApi(
            userInput.value,
            contextInput.value,
            promptInput.value,
            maxTokensInput.value,
            temperatureInput.value,
            topPInput.value,
        );
    });

    // 질의 SSE 수신 이벤트
    eventSource.addEventListener(QUERY_EVENT_NAME, (event) => {
        const msgDiv = document.createElement("div");
        msgDiv.className = "message query";
        msgDiv.textContent = event.data;
        content.appendChild(msgDiv);
        content.scrollTop = content.scrollHeight;
    });

    // 추론 과정 SSE 수신 이벤트
    eventSource.addEventListener(INFERENCE_EVENT_NAME, (event) => {
        if (currentInferenceMsg) {
            currentInferenceText += replaceEventDataToText(event.data);
            renderMarkdownWithMermaid(currentInferenceText, currentInferenceMsg);
            content.scrollTop = content.scrollHeight;
        } else {
            console.log("📋 추론 과정 표출 시작");
            currentInferenceText = "";

            const inferenceBox = document.createElement("div");
            inferenceBox.className = "inference-box";

            const toggle = document.createElement("button");
            toggle.innerHTML = "▼ 추론 과정 보기";
            toggle.className = "toggle";

            toggle.addEventListener("click", () => {
                toggle.classList.toggle('active');
                if (toggle.classList.contains('active')) {
                    toggle.textContent = '▲ 추론 과정 숨기기';
                } else {
                    toggle.textContent = '▼ 추론 과정 보기';
                }
            });

            const title = document.createElement("div");
            title.innerText = "답변을 위해 생각하는중";
            title.className = "title";

            const spinner = document.createElement("div");
            spinner.className = "spinner";

            currentInferenceRow = document.createElement("div");
            currentInferenceRow.className =  "status-row";
            currentInferenceRow.appendChild(title);
            currentInferenceRow.appendChild(spinner);

            currentInferenceMsg = document.createElement("div");
            currentInferenceMsg.className = "stream-box";

            inferenceBox.appendChild(currentInferenceRow);
            inferenceBox.appendChild(toggle);
            inferenceBox.appendChild(currentInferenceMsg);
            content.appendChild(inferenceBox);
        }
    });

    // 답변 SSE 수신 이벤트
    eventSource.addEventListener(ANSWER_EVENT_NAME, (event) => {
        if (currentLlmMsg) {
            currentLlmText += replaceEventDataToText(event.data);
            renderMarkdownWithMermaid(currentLlmText, currentLlmMsg);
            content.scrollTop = content.scrollHeight;
        } else {
            if (!currentLlmMsg && event.data.trim().length > 0) {
                console.log(event.data.trim());
                console.log("📋 답변 시작");
                currentLlmText = "";

                if (currentInferenceRow) {
                    currentInferenceRow.remove();
                }

                currentLlmMsg = document.createElement("div");
                currentLlmMsg.className = "message answer";
                content.appendChild(currentLlmMsg);
            }
        }
    });
};

const sendQueryApi = (query, context, prompt, maxTokens, temperature, topP) => {
    console.log(`📡 질의 요청 : ${userInput.value}`);

    fetch(`/${SERVICE_NAME}/chat`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            sessionId: SESSION_ID,
            query: query,
            context: context,
            prompt: prompt,
            maxTokens: maxTokens,
            temperature: temperature,
            topP: topP,
        })
    })
        .then(response => {
            if (response.status === 200) {
                response.json().then(body => {
                    console.log(`📡 ${body.message}`);
                });
            } else if (response.status === 202) {
                response.json().then(body => console.error(`❌ ${body.message}`));
                alert(`새로 고침 필요`);
                enableInput();
            }  else {
                alert(`서버 통신 오류`);
                enableInput();
            }
        })
        .catch(reason => {
            console.error(reason);
            enableInput();
        });
};

// 첫 화면
window.onload = () => {
    // 전송 버튼 클릭 이벤트
    sendBtn.addEventListener("click", (_) => sendQuery(userInput.value));

    // 초기화 버튼 클릭 이벤트
    resetBtn.addEventListener("click", () => {
        userInput.value = "";
        contextInput.value = "";
        promptInput.value = "";
    });

    // 질의문 입력 키 다운 이벤트
    userInput.addEventListener("keydown", (event) => {
        if (event.key === 'Enter' && !event.isComposing) {
            sendQuery(userInput.value);
        }
    });

    // 그리팅
    if (GREETING_MESSAGE.length > 0) {
        const greetingMsg = document.createElement("div");
        greetingMsg.className = "message answer";
        content.appendChild(greetingMsg);

        let index = 0;
        const interval = setInterval(() => {
            renderMarkdownWithMermaid(GREETING_MESSAGE.substring(0, index), greetingMsg);
            content.scrollTop = content.scrollHeight;
            index++;
            if (index >= GREETING_MESSAGE.length) {
                clearInterval(interval);
            }
        }, 10);
    }
}