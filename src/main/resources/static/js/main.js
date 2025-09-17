
const sidebar    = document.getElementById('sidebar');
const menuToggle = document.getElementById('menu-toggle');
const homeTab = document.getElementById('home');
const aiBotTab = document.getElementById('aiBot');

// 사이드 바 토글 버튼 클릭 이벤트
menuToggle.addEventListener('click', () => {
    sidebar.classList.toggle('hidden');
});

// HOME 탭 클릭 이벤트
homeTab.addEventListener('click', () => {
    location.href = '/';
});

// AIBOT 탭 클릭 이벤트
aiBotTab.addEventListener('click', () => {
    location.href = '/aibot.html';
});