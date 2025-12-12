import {randomUUID, renderMarkdownWithMermaid, replaceEventDataToText} from './util.js'

const GREETING_MESSAGE    = "안녕하세요. **AI MATE** 입니다.\n\n질의를 작성해주시면 문서를 기반으로 답변 드리겠습니다."
const SESSION_ID          = randomUUID();
const RECOMMEND_QUERY    = [
    "승선 근무 예비역의 경우 복무 기간이 상근 예비역과 동일해?",
    "국가 유공자의 후손인 경우, 일반 현역으로 입대하는 사람들과 복무 기간의 차이가 있을까?",
    "의약품에 대한 거짓 광고를 하는 경우 처벌이 어떻게 돼?",
    "동물에 대한 의약품 관리 법령이 있어?",
    "장기 요양 기관에서의 개인이 CCTV 열람이 가능해?",
    "승선 근무 예비역의 입대 절차에 대해 상세하게 알려줘",
]

const content      = document.getElementById("content");
const userInput    = document.getElementById("userInput");
const sendBtn      = document.getElementById("sendBtn");
const cancelBtn    = document.getElementById("cancelBtn");

const referenceDocuments = [];
let btnEnable = true;

/**
 * 입력 단 비 활성화 이벤트
 */
const disableInput = () => {
    btnEnable = false;
    sendBtn.hidden = true;
    cancelBtn.hidden = false;
    userInput.disabled = true;
};

/**
 * 입력 단 활성화 이벤트
 */
const enableInput = () => {
    btnEnable = true;
    sendBtn.hidden = false;
    cancelBtn.hidden = true;
    userInput.disabled = false;
};

/**
 * 참고 문서 토글 이벤트
 *
 * @param refHeader
 */
const toggleReferenceCard = (refHeader) => {
    const body = refHeader.nextElementSibling;
    body.classList.toggle('open');
};

/**
 * 질의 전송 요청
 *
 * @param query 질의문
 */
const sendQuery = (query) => {
    if (query.trim() === "") {
        alert("유저 프롬프트 입력 필요!");
        return;
    } else if (!btnEnable) return;
    else disableInput();

    let answerDiv= document.createElement("div");
    let answer = "";
    let inferenceDiv= document.createElement("div");
    let inference = "";
    let inferenceTitleDiv = document.createElement("div");

    // 세션 기반 SSE 연결
    const eventSource = new EventSource(`/stream/${SESSION_ID}`);

    eventSource.addEventListener("connect", (_) => {
        console.log(`📡 스트림 연결`);
        sendQueryApi(query);

        // 질의 등록
        const queryDiv = document.createElement("div");
        queryDiv.className = "message query";
        queryDiv.textContent = query;
        content.appendChild(queryDiv);
        content.scrollTop = content.scrollHeight;
    });

    eventSource.addEventListener("inference-start", (_) => {
        console.log("📋 추론 과정 표출 시작");

        // 추론 텍스트 박스
        const inferenceBox = document.createElement("div");
        inferenceBox.className = "inference-box";
        // 토글 버튼
        const toggleBtn = document.createElement("button");
        toggleBtn.innerHTML = "▼ 추론 과정 보기";
        toggleBtn.className = "toggle";
        toggleBtn.addEventListener("click", () => {
            toggleBtn.classList.toggle('active');
            if (toggleBtn.classList.contains('active')) {
                toggleBtn.textContent = '▲ 추론 과정 숨기기';
            } else {
                toggleBtn.textContent = '▼ 추론 과정 보기';
            }
        });

        // 추론 텍스트 박스 타이틀 문자열
        const inferenceTitleText= document.createElement("div");
        inferenceTitleText.innerText = "답변을 위해 생각하는중";
        inferenceTitleText.className = "title";
        // 스피너
        const spinner = document.createElement("div");
        spinner.className = "spinner";

        // 추론 텍스트 박스 타이틀
        inferenceTitleDiv.className =  "status-row";
        inferenceTitleDiv.appendChild(inferenceTitleText);
        inferenceTitleDiv.appendChild(spinner);

        inferenceDiv = document.createElement("div");
        inferenceDiv.className = "stream-box";

        inferenceBox.appendChild(inferenceTitleDiv);
        inferenceBox.appendChild(toggleBtn);
        inferenceBox.appendChild(inferenceDiv);
        content.appendChild(inferenceBox);
    });

    eventSource.addEventListener("inference", (event) => {
        inference += replaceEventDataToText(event.data);
        renderMarkdownWithMermaid(inference, inferenceDiv);
        content.scrollTop = content.scrollHeight;
    });

    eventSource.addEventListener("inference-done", (_) => {
        console.log("📋 추론 과정 표출 종료");

        if (inferenceTitleDiv) {
            inferenceTitleDiv.remove();
        }
    });

    eventSource.addEventListener("answer-start", (_) => {
        console.log("📋 답변 시작");

        answerDiv.className = "message answer";
        content.appendChild(answerDiv);
    });

    eventSource.addEventListener("answer", (event) => {
        answer += replaceEventDataToText(event.data);
        renderMarkdownWithMermaid(answer, answerDiv);
        content.scrollTop = content.scrollHeight;
    });

    eventSource.addEventListener("answer-done", (_) => {
        console.log(`📋 답변 종료`);

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

        answerDiv.appendChild(references);
    });

    eventSource.addEventListener("disconnect", (_) => {
        eventSource.close();
        console.log(`❌ 스트림 닫힘`);
        enableInput();
    });

    eventSource.addEventListener("exception", (_) => {
        eventSource.close();
        console.log(`❌ 예외 발생`);
        enableInput();
    });
};

/**
 * 질의 요청 API 호출
 *
 * @param query 질의문
 */
const sendQueryApi = (query) => {
    console.log(`📡 질의 요청 : ${query}`);

    fetch(`/mate/chat`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({
            sessionId: SESSION_ID,
            query: query,
            chatId: 1,
        })
    })
        .then(response => {
            if (response.status === 200) {
                response.json().then(body => {
                    console.log(`📡 ${body.message}`);
                    referenceDocuments.splice(0, referenceDocuments.length);
                    body.data.documents.forEach(referenceDocument => {
                        referenceDocuments.push(referenceDocument);
                    });
                    userInput.value = "";
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

/**
 * 답변 중지 API 호출
 */
const cancelAnswerApi = () => {
    console.log(`📡 답변 스트림 중지 요청`);

    fetch(`/stream/${SESSION_ID}`, {
        method: "DELETE",
        headers: {"Content-Type": "application/json"}
    }).then(response => {
            if (response.status === 200) {
                response.json().then(body => {
                    console.log(`📡 ${body.message}`);
                });
            } else {
                enableInput();
            }
        })
        .catch(reason => {
            console.error(reason);
            enableInput();
        });

}

// 첫 화면
window.onload = () => {
    // 전송 버튼 클릭 이벤트
    sendBtn.addEventListener("click", (_) => sendQuery(userInput.value));

    // 중지 버튼 클릭 이벤트
    cancelBtn.addEventListener("click", (_) => cancelAnswerApi())

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