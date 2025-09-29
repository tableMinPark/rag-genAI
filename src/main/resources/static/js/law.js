import {randomUUID, replaceToHtmlTag} from './util.js'

const GREETING_MESSAGE    = "안녕하세요. LAW AI BOT 입니다.\n질의를 작성해주시면 법령에 대한 문서를 기반으로 답변 드리겠습니다.\n(시스템 프롬프트 튜닝 전이라, 답변 형식이 비정상적일 수 있습니다.)"
const SERVICE_NAME        = "law"
const SESSION_ID          = randomUUID();
const QUERY_EVENT_NAME    = `/${SERVICE_NAME}/query/${SESSION_ID}`;
const ANSWER_EVENT_NAME   = `/${SERVICE_NAME}/answer/${SESSION_ID}`;
const ANSWER_START_PREFIX = "[ANSWER_START]";
const ANSWER_END_PREFIX   = "[ANSWER_END]";

const content      = document.getElementById("content");
const sendBtn      = document.getElementById("sendBtn");
const userInput    = document.getElementById("userInput");

let referenceDocuments= [];
let btnEnable = true;
let currentLlmMsg = null;

// 입력 단 비 활성화
const disableInput = () => {
    btnEnable = false;
    sendBtn.hidden = true;
    userInput.disabled = true;
};

// 입력 단 활성화
const enableInput = () => {
    btnEnable = true;
    sendBtn.hidden = false;
    userInput.disabled = false;
};

// 질의 전송 요청
const sendQuery = () => {
    if (userInput.value.trim() === "") {
        alert("유저 프롬프트 입력 필요!");
        return;
    } else if (!btnEnable) return;
    else disableInput();

    // 세션 기반 SSE 연결
    const eventSource = new EventSource(`/${SERVICE_NAME}/stream/${SESSION_ID}`);

    eventSource.addEventListener("error", (event) => {
        console.log(`❌ 에러 또는 연결 끊김 발생: ${event.type}`);

        const references = document.createElement("div");
        references.className = "references";

        referenceDocuments.forEach((referenceDocument, index) => {
            const refCard = document.createElement("div");
            refCard.className = "ref-card"
            refCard.innerHTML += `<h3>참고문서 # ${index + 1}</h3>`;
            refCard.innerHTML += `<p>${referenceDocument.title}</p>`;
            refCard.innerHTML += `<p>${referenceDocument.subTitle}</p>`;
            refCard.innerHTML += `<p>${referenceDocument.thirdTitle}</p>`;
            refCard.innerHTML += `<p>${referenceDocument.content}</p>`;
            references.append(refCard);
        });

        currentLlmMsg.appendChild(references);
        currentLlmMsg = null;
        eventSource.close();
        enableInput();
    });

    eventSource.addEventListener("open", () => {
        console.log("📡 SSE 연결 열림");
        sendQueryApi(userInput.value);
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
            return;
        }
        if (currentLlmMsg) {
            currentLlmMsg.innerHTML += event.data;
            currentLlmMsg.innerHTML = replaceToHtmlTag(currentLlmMsg.innerHTML);
            content.scrollTop = content.scrollHeight;
        }
    });
};

const sendQueryApi = (query) => {
    console.log(`📡 질의 요청 : ${userInput.value}`);

    fetch(`/${SERVICE_NAME}/chat`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            sessionId: SESSION_ID,
            query: query,
        })
    })
        .then(response => {
            if (response.status === 200) {
                response.json().then(body => {
                    console.log(`📡 ${body.message}`);
                    referenceDocuments = body.data.documents;
                });
                userInput.value = "";
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
    sendBtn.addEventListener("click", (_) => sendQuery());

    // 질의문 입력 키 다운 이벤트
    userInput.addEventListener("keydown", (event) => {
        if (event.key === 'Enter' && !event.isComposing) {
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
