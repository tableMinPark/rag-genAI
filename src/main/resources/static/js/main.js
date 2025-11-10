
const sidebar    = document.getElementById('sidebar');
const sidebarToggle = document.getElementById('sidebar-toggle');

const homeTab = document.getElementById('home');
const mateTab = document.getElementById('mate');
const myaiTab = document.getElementById('myai');
const llmTab = document.getElementById('llm');
const extractTab = document.getElementById('extract');

// 사이드 바 토글 버튼 클릭 이벤트
sidebarToggle.addEventListener('click', () => {
    sidebar.classList.toggle('hidden');
});

// HOME 탭 클릭 이벤트
homeTab.addEventListener('click', () => {
    location.href = '/';
});

// MATE 탭 클릭 이벤트
mateTab.addEventListener('click', () => {
    location.href = '/mate.html';
});

// MYAI 탭 클릭 이벤트
myaiTab.addEventListener('click', () => {
    alert('개발 진행중!')
});

// LLM 탭 클릭 이벤트
llmTab.addEventListener('click', () => {
    location.href = '/llm.html';
});

// EXTRACT 탭 클릭 이벤트
extractTab.addEventListener('click', () => {
    location.href = '/extract.html';
});

window.onload = () => {
    window.mermaid.initialize({ startOnLoad: false, theme: "default" });
};