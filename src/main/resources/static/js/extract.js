import {renderMarkdownWithMermaid} from './util.js'

const GREETING_MESSAGE    = "안녕하세요. **EXTRACT BOT** 입니다.\n\n한글 문서를 업로드 하시면, 문서를 기반으로 표 데이터를 마크다운으로 추출해드리겠습니다."
const SERVICE_NAME        = "extract"

const content         = document.getElementById("content");
const sendMarkDownBtn = document.getElementById("sendMarkDownBtn");
const sendHtmlBtn     = document.getElementById("sendHtmlBtn");
const resetBtn        = document.getElementById("resetBtn");
const dropZone        = document.getElementById("dropZone");
const fileInput       = document.getElementById("fileInput");
const uploadFile      = document.getElementById("uploadFile");

let btnEnable    = true;
let currentUploadFile= null;

// 입력 단 비 활성화
const disableInput = () => {
    btnEnable = false;
    sendMarkDownBtn.hidden = true;
    sendHtmlBtn.hidden = true;
    resetBtn.hidden = true
    fileInput.disabled = true;
};

// 입력 단 활성화
const enableInput = () => {
    btnEnable = true;
    sendMarkDownBtn.hidden = false;
    sendHtmlBtn.hidden = false;
    resetBtn.hidden = false
    fileInput.disabled = false;
};

// 파일 처리 함수
function handleFiles(files) {
    const file = files[0];
    if (!file) return;

    currentUploadFile = file;
    // 파일 정보 출력 (예시)
    uploadFile.innerHTML = `<p><strong>${currentUploadFile.name}</strong> (${Math.round(currentUploadFile.size / 1024)} KB)</p>`;
}

// 질의 전송
const sendExtract = (extractType) => {
    if (!currentUploadFile) {
        alert("파일 선택 필요!");
        return;
    } else if (!btnEnable) return;
    else disableInput();

    sendExtractApi(extractType)
};

const sendExtractApi = (extractType) => {
    const fileName = currentUploadFile.name;
    console.log(`📡 추출 요청 : ${fileName}`);

    const msgDiv = document.createElement("div");
    msgDiv.className = "message query";
    msgDiv.innerHTML += `<div>추출 문서: ${fileName}</div>`;
    msgDiv.innerHTML += `<div>추출 방식: ${extractType}</div>`;
    content.appendChild(msgDiv);
    content.scrollTop = content.scrollHeight;

    const processMsg = document.createElement("div");
    processMsg.className = "message answer";
    processMsg.innerHTML += `<div>${fileName} 데이터 추출중</div>`;
    content.appendChild(processMsg);

    // 실제 업로드 로직
    const formData = new FormData();
    formData.append("uploadFile", currentUploadFile);
    formData.append("requestDto", JSON.stringify({
        extractType: extractType,
    }));

    fetch(`http://127.0.0.1:8000/${SERVICE_NAME}`, {
        method: "POST",
        body: formData
    })
        .then(response => {
            content.removeChild(processMsg);
            if (response.status === 200) {
                response.json().then(body => {
                    body.lines.forEach((line, index) => {
                        const extractMsg = document.createElement("div");
                        extractMsg.className = "message answer";
                        extractMsg.innerHTML += `<div class="nodrag noselect" draggable="false"><strong>[${index}] >> ${fileName}</strong></div>`;
                        extractMsg.innerHTML += `<div>${line.content.replace("<table>", "<table border=\"1\">")}</div>`;
                        content.appendChild(extractMsg);
                    })
                });

                uploadFile.innerHTML = "여기에 파일을 드래그하거나 클릭해서 업로드하세요."
                currentUploadFile = null;
                fileInput.value = "";
                enableInput();
            } else {
                alert(`서버 통신 오류`);
                enableInput();
            }
        })
        .catch(reason => {
            console.error(reason);
            processMsg.innerHTML += `<div>서버 통신 오류 발생</div>`;
            content.scrollTop = content.scrollHeight;
            enableInput();
        });
}

// 첫 화면
window.onload = () => {
    // 전송 버튼 클릭 이벤트
    sendMarkDownBtn.addEventListener("click", (_) => sendExtract("markdown"));
    sendHtmlBtn.addEventListener("click", (_) => sendExtract("html"));

    // 초기화 버튼 클릭 이벤트
    resetBtn.addEventListener("click", () => {
        uploadFile.innerHTML = "여기에 파일을 드래그하거나 클릭해서 업로드하세요."
        currentUploadFile = null;
        fileInput.value = "";
    });

    // 드래그 상태 진입
    dropZone.addEventListener("dragover", (e) => {
        e.preventDefault();
        dropZone.classList.add("dragover");
    });

    // 드래그 해제
    dropZone.addEventListener("dragleave", () => {
        dropZone.classList.remove("dragover");
    });

    // 드롭 처리
    dropZone.addEventListener("drop", (e) => {
        e.preventDefault();
        dropZone.classList.remove("dragover");

        if (e.dataTransfer.files.length > 0) {
            handleFiles(e.dataTransfer.files);
        }
    });

    // 클릭 시 파일 선택
    dropZone.addEventListener("click", () => {
        fileInput.click();
    });

    // input 선택 시 처리
    fileInput.addEventListener("change", () => {
        if (fileInput.files.length > 0) {
            handleFiles(fileInput.files);
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
