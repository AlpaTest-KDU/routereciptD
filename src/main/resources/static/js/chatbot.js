// src/main/resources/static/js/chat/openai.js

document.addEventListener("DOMContentLoaded", () => {
	// 질문 입력창 , 전송버튼, 대화 메세지를 담을 채팅창
    const input = document.getElementById("chat-input-text");
    const sendBtn = document.getElementById("chat-send-btn");
    const messages = document.getElementById("chat-messages");
    const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

	// role :누가 보냈는지 구분하기 위해 
	// 사용자, 챗봇 , system(오류 출력)
	// div 테그를 만들어서 말풍선 디자인 출력
    function addMessage(role, text) {
        const div = document.createElement("div");
        div.classList.add("chat-msg");
        if (role === "user") {
            div.classList.add("chat-msg-user");
        } else if (role === "bot") {
            div.classList.add("chat-msg-bot");
        } else {
            div.classList.add("chat-msg-system");
        }
        // 말풍선 안에 텍스트 넣기
        div.textContent = text;
        // 말풍선을 채팅창 화면(messages)에 추가
        messages.appendChild(div);
        //스크롤을 맨 아래로 자동 이동해서  새 메시지가 보이게 처리
        messages.scrollTop = messages.scrollHeight;
    }
	//async는 비동기 작업을 쉽게 처리할 수 있게 도와주는 키워드 
    async function sendMessage() {
		// 메세지 전송
        const text = input.value.trim();
        if (!text) return;

        // 사용자 메세지 채팅창에 추가
        addMessage("user", text);
        // 입력창 초기화
        input.value = "";
        // 입력창에 다시 커서를 자동으로 맞춤 (다음 메시지 바로 입력 가능)
        input.focus();
        //전송 버튼을 일시적으로 비활성화 (중복 클릭 방지)
        sendBtn.disabled = true;

        try {
            //서버에 메시지 보내기 await 결과가 나올 때까지 기다렸다가 다음 줄 실행하게 해주는 문법
            const res = await fetch("/api/chat", {
				// "POST"로 데이터 보냄 데이터를 보낼 때 쓰는 방식
                method: "POST",
                // 보내는 데이터가 JSON이라는 걸 서버에 알려줍니다
                headers: { "Content-Type": "application/json" },
                // { "message": "영수증은 어디서 등록해요?" } 이런 형태로 보냄
                body: JSON.stringify({ message: text })
            });
			
			// 서버 응답 확인
            if (!res.ok) {
                addMessage("bot", "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
            } else {
                const data = await res.json();
                addMessage("bot", data.reply || "답변을 가져오지 못했어요.");
            }
            // 서버가 아예 죽었거나, 인터넷이 끊긴 경우 
        } catch (e) {
            console.error(e);
            addMessage("bot", "네트워크 오류가 발생했습니다.");
        } finally {
			// 마지막에 실행되는 부분 전송 버튼 활송화
            sendBtn.disabled = false;
        }
    }

	// 전송 버튼 or enter 키 누르면 전송
    sendBtn.addEventListener("click", sendMessage);
    input.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            e.preventDefault();
            sendMessage();
        }
    });
});
