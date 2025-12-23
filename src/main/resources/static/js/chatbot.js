document.addEventListener("DOMContentLoaded", () => {
  const input = document.getElementById("chat-input-text");
  const sendBtn = document.getElementById("chat-send-btn");
  const messages = document.getElementById("chat-messages");

  const csrfTokenEl = document.querySelector('meta[name="_csrf"]');
  const csrfHeaderEl = document.querySelector('meta[name="_csrf_header"]');
  const csrfToken = csrfTokenEl ? csrfTokenEl.getAttribute("content") : null;
  const csrfHeader = csrfHeaderEl ? csrfHeaderEl.getAttribute("content") : null;

function addMessage(role, text) {
  if (role === "system") {
    const sys = document.createElement("div");
    sys.className = "chat-msg-system";
    sys.textContent = text;
    messages.appendChild(sys);
    messages.scrollTop = messages.scrollHeight;
    return;
  }

  const row = document.createElement("div");
  row.className = (role === "user") ? "chat-msg-user" : "chat-msg-bot";

  const bubble = document.createElement("div");
  bubble.className = "bubble";
  bubble.textContent = text;

  row.appendChild(bubble);
  messages.appendChild(row);
  messages.scrollTop = messages.scrollHeight;
}

  async function sendMessage() {
    const text = input.value.trim();
    if (!text) return;

    addMessage("user", text);
    input.value = "";
    input.focus();
    sendBtn.disabled = true;

    try {
      const headers = { "Content-Type": "application/json" };
      if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

      const res = await fetch("/api/chat", {
        method: "POST",
        headers,
        body: JSON.stringify({ message: text })
      });

      if (!res.ok) {
        addMessage("bot", `서버 오류: HTTP ${res.status}`);
        return;
      }

      const data = await res.json();
      const reply = (data.reply ?? "").trim();
      addMessage("bot", data.reply || "답변을 가져오지 못했어요.");
    } catch (e) {
      console.error(e);
      addMessage("bot", "네트워크 오류가 발생했습니다.");
    } finally {
      sendBtn.disabled = false;
    }
  }

  sendBtn.addEventListener("click", sendMessage);
  input.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
      e.preventDefault();
      sendMessage();
      
    }
  });
});