
const sidebar    = document.getElementById('sidebar');
const sidebarToggle = document.getElementById('sidebar-toggle');

const homeTab = document.getElementById('home');
const lawTab = document.getElementById('law');
const myaiTab = document.getElementById('myai');
const llmTab = document.getElementById('llm');

// 사이드 바 토글 버튼 클릭 이벤트
sidebarToggle.addEventListener('click', () => {
    sidebar.classList.toggle('hidden');
});

// HOME 탭 클릭 이벤트
homeTab.addEventListener('click', () => {
    location.href = '/';
});

// LAW 탭 클릭 이벤트
lawTab.addEventListener('click', () => {
    location.href = '/law.html';
});

// MYAI 탭 클릭 이벤트
myaiTab.addEventListener('click', () => {
    alert('개발 진행중!')
});

// LLM 탭 클릭 이벤트
llmTab.addEventListener('click', () => {
    location.href = '/llm.html';
});