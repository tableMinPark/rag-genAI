
const sidebar    = document.getElementById('sidebar');
const menuToggle = document.getElementById('menu-toggle');
const homeTab = document.getElementById('home');
const lawTab = document.getElementById('law');
const llmTab = document.getElementById('llm');

// 사이드 바 토글 버튼 클릭 이벤트
menuToggle.addEventListener('click', () => {
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

// LLM 탭 클릭 이벤트
llmTab.addEventListener('click', () => {
    location.href = '/llm.html';
});
