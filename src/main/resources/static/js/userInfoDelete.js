function confirmDelete() {
    const userIdElement = document.getElementById('currentUserId');
    if (!userIdElement) return;

    const currentUserId = userIdElement.value;
    console.log("가져온 아이디 : ", currentUserId);
    const userInput = prompt("삭제하시려면 아이디('" + currentUserId + "')를 입력하세요.");
    
    if (userInput === null) {
        return;
    }

    if (userInput === currentUserId) {
        if (confirm("정말로 탈퇴하시겠습니까? 모든 정보가 삭제됩니다.")) {
            document.getElementById('deleteForm').submit();
        }
    } else {
        alert("아이디가 일치하지 않습니다.");
    }
}