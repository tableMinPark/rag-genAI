import {randomUUID, replaceEventDataToText, replaceToHtmlTag} from './util.js'

const GREETING_MESSAGE    = "안녕하세요. LAW AI BOT 입니다.\n\n질의를 작성해주시면 법령에 대한 문서를 기반으로 답변 드리겠습니다.\n\n(시스템 프롬프트 튜닝 전이라, 답변 형식이 비정상적일 수 있습니다.)"
const SERVICE_NAME        = "law"
const SESSION_ID          = randomUUID();
const QUERY_EVENT_NAME    = `/${SERVICE_NAME}/query/${SESSION_ID}`;
const ANSWER_EVENT_NAME   = `/${SERVICE_NAME}/answer/${SESSION_ID}`;
const ANSWER_START_PREFIX = "[ANSWER_START]";
const ANSWER_END_PREFIX   = "[ANSWER_END]";
const RECOMMEND_QUERY    = [
    "승선 근무 예비역의 경우 복무 기간이 상근 예비역과 동일해?",
    "국가 유공자의 후손인 경우, 일반 현역으로 입대하는 사람들과 복무 기간의 차이가 있을까?",
    "의약품에 대한 거짓 광고를 하는 경우 처벌이 어떻게 돼?",
    "동물에 대한 의약품 관리 법령이 있어?",
    "장기 요양 기관에서의 개인이 CCTV 열람이 가능해?",
    "승선 근무 예비역의 입대 절차에 대해 상세하게 알려줘",
]

const content      = document.getElementById("content");
const sendBtn      = document.getElementById("sendBtn");
const userInput    = document.getElementById("userInput");

let referenceDocuments= [];
let btnEnable = true;
let currentLlmMsg = null;
let currentLlmText= null;

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

// 참고 문서 토글
const toggleReferenceCard = (refHeader) => {
    const body = refHeader.nextElementSibling;
    body.classList.toggle('open');
};

// 질의 전송 요청
const sendQuery = (query) => {
    if (query.trim() === "") {
        alert("유저 프롬프트 입력 필요!");
        return;
    } else if (!btnEnable) return;
    else disableInput();

    // 세션 기반 SSE 연결
    const eventSource = new EventSource(`/${SERVICE_NAME}/stream/${SESSION_ID}`);

    eventSource.addEventListener("error", (event) => {
        console.log(`❌ 에러 또는 연결 끊김 발생: ${event.type}`);

        if (currentLlmMsg) {
            const references = document.createElement("div");
            references.className = "references";

            referenceDocuments.forEach((referenceDocument, index) => {
                const refCard = document.createElement("div");
                refCard.className = "ref-card"

                const refHeader = document.createElement("div");
                refHeader.className = "ref-header"
                refHeader.onclick = () => toggleReferenceCard(refHeader);
                refHeader.innerHTML += `<span class="ref-header-num">참고문서 #${index + 1}</span>`;
                refHeader.innerHTML += `<span class="ref-header-title">${referenceDocument.title} ${referenceDocument.subTitle} ${referenceDocument.thirdTitle}</span>`;

                const refBody = document.createElement("div");
                refBody.className = "ref-body";
                refBody.innerHTML += `<p>${referenceDocument.title}</p>`;
                refBody.innerHTML += `<p>${referenceDocument.subTitle}</p>`;
                refBody.innerHTML += `<p>${referenceDocument.thirdTitle}</p>`;
                refBody.innerHTML += `<p>${referenceDocument.content}</p>`;

                refCard.appendChild(refHeader);
                refCard.appendChild(refBody);
                references.appendChild(refCard);
            });

            currentLlmMsg.appendChild(references);
        }

        currentLlmMsg = null;
        currentLlmText = null;
        eventSource.close();
        enableInput();
    });

    eventSource.addEventListener("open", () => {
        console.log("📡 SSE 연결 열림");
        sendQueryApi(query);
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
            currentLlmText = "";
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
            currentLlmText += replaceEventDataToText(event.data);
            currentLlmMsg.innerHTML = replaceToHtmlTag(currentLlmText);
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
    sendBtn.addEventListener("click", (_) => sendQuery(userInput.value));

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
            greetingMsg.innerHTML = replaceToHtmlTag(GREETING_MESSAGE.substring(0, index));
            content.scrollTop = content.scrollHeight;
            index++;
            if (index >= GREETING_MESSAGE.length) {
                const recommendQueryMsg = document.createElement("div");
                recommendQueryMsg.className = "message answer";

                const recommendQuery = document.createElement("div");
                recommendQuery.className = "recommendQuery";

                RECOMMEND_QUERY.forEach(query => {
                    const recommendQueryCard = document.createElement("div");
                    recommendQueryCard.className = "recommendQueryCard"
                    recommendQueryCard.onclick = () => sendQuery(query);
                    recommendQueryCard.innerHTML += `<p><strong>Q.</strong>${query}</p>`;
                    recommendQuery.append(recommendQueryCard);
                });

                recommendQueryMsg.appendChild(recommendQuery);
                content.appendChild(recommendQueryMsg);
                clearInterval(interval);
            }
        }, 10);
    }
}