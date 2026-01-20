console.log("📦 receipt.js LOADED");

/* =====================================================
 * 전역 상태
 * ===================================================== */
var selectedIdx = -1;
var pollingTimer = null;
var POLLING_INTERVAL = 2000; // 2초

/* =====================================================
 * 페이지 진입 시 초기화
 * ===================================================== */
function initPage() {
  console.log("🟢 initPage() CALLED");
  hideLoadingOverlay();
  renderReceiptList();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initPage);
} else {
  initPage();
}

/* =====================================================
 * 로딩 오버레이 제어
 * ===================================================== */
function showLoadingOverlay() {
  const overlay = document.getElementById("loadingOverlay");
  const btn = document.querySelector(".ocrBtn");

  if (overlay) {
    overlay.classList.add("isOpen");
    overlay.setAttribute("aria-hidden", "false");
    document.body.classList.add("isLoading");
  }

  if (btn) {
    btn.disabled = true;
    btn.textContent = "분석 중...";
  }
}

function hideLoadingOverlay() {
  const overlay = document.getElementById("loadingOverlay");
  const btn = document.querySelector(".ocrBtn");

  if (overlay) {
    overlay.classList.remove("isOpen");
    overlay.setAttribute("aria-hidden", "true");
    document.body.classList.remove("isLoading");
  }

  if (btn) {
    btn.disabled = false;
    btn.textContent = "영수증 분석";
  }
}

/* =====================================================
 * 영수증 목록 렌더링
 * ===================================================== */
function renderReceiptList() {
  const list = document.getElementById("receiptList");
  if (!list) return;

  // 분석 중
  if (!receipts || receipts.length === 0) {
    list.innerHTML = `
      <p class="pendingTitle">영수증을 분석 중입니다.</p>
      <p class="pendingDesc">
        업로드는 정상적으로 완료되었습니다.<br>
        분석에는 약간의 시간이 소요될 수 있습니다.
      </p>
    `;
    return;
  }

  let html = `
    <p class="pendingTitle">영수증 목록</p>
    <h3>영수증 목록</h3>
  `;

  receipts.forEach((r, idx) => {
    const statusText =
      r.ocr_status === "DONE" ? "분석 완료" : "분석 중";

    html += `
      <button type="button"
              class="receiptListNo"
              onclick="showReceipt(${idx})">
        ${idx + 1}번 - ${escapeHtml(r.r_place ?? "상호명 없음")}
        <span class="status">(${statusText})</span>
      </button>
    `;
  });

  list.innerHTML = html;
}

/* =====================================================
 * 영수증 선택
 * ===================================================== */
function showReceipt(idx) {
  selectedIdx = idx;
  const receipt = receipts[idx];
  if (!receipt) return;

  if (receipt.ocr_status !== "DONE") {
    alert("아직 분석이 완료되지 않았습니다.");
    return;
  }

  const place = receipt.r_place || "";
  const date  = (receipt.r_date || "").toString().substring(0, 10);
  const price = Number(receipt.r_price || 0);

  document.getElementById("placeText").textContent = place;
  document.getElementById("dateText").textContent  = date;
  document.getElementById("priceText").textContent = price.toLocaleString();

  document.getElementById("rNoInput").value    = receipt.r_no || "";
  document.getElementById("rPlaceInput").value = place;
  document.getElementById("rDateInput").value  = date;
  document.getElementById("rPriceInput").value = price;

  renderItemsByCategory(receipt.items || []);
  document.getElementById("detailArea").style.display = "block";
  recalcTotalToHidden();
}

/* =====================================================
 * 아이템 렌더링
 * ===================================================== */
function renderItemsByCategory(items) {
  const categoryMap = {
    FOOD: "음식",
    MEDICAL: "의료",
    CLOTHES: "의류",
    HOME: "주거",
    LIVING: "생활",
    CULTURE: "문화",
    TRAFFIC: "교통",
    ETC: "기타"
  };

  let html = "";

  if (!items.length) {
    html = "<p>아직 분석된 상품이 없습니다.</p>";
  } else {
    for (const [key, label] of Object.entries(categoryMap)) {
      const group = items.filter(it => it.item_category === key);
      if (!group.length) continue;

      html += `<h4>${label}</h4><ul style="list-style:none;padding-left:0;">`;

      group.forEach(it => {
        html += `
          <li class="item-row" style="margin:8px 0; display:flex; gap:10px;">
            <input type="hidden" name="item_categories" value="${key}">
            <input type="text" name="item_names"
                   value="${escapeHtml(it.item_name ?? "")}">
            <input type="number" name="item_prices"
                   value="${Number(it.item_price ?? 0)}"
                   min="0"
                   oninput="recalcTotalToHidden()">
            <button type="button" onclick="removeItemRow(this)">삭제</button>
          </li>
        `;
      });

      html += "</ul>";
    }
  }

  document.getElementById("categoryArea").innerHTML = html;
}

/* =====================================================
 * 가격 계산
 * ===================================================== */
function removeItemRow(btn) {
  btn.closest("li")?.remove();
  recalcTotalToHidden();
}

function recalcTotalToHidden() {
  let total = 0;

  document
    .querySelectorAll('#confirmForm input[name="item_prices"]')
    .forEach(p => total += Number(p.value || 0));

  document.getElementById("priceText").textContent = total.toLocaleString();
  document.getElementById("rPriceInput").value = total;
}

/* =====================================================
 * 파일 업로드 (트리거)
 * ===================================================== */
async function submitUpload() {
  const form = document.getElementById("uploadForm");
  const fileInput = document.getElementById("receiptFile");

  if (!fileInput || fileInput.files.length === 0) {
    alert("영수증 이미지를 선택해주세요.");
    return;
  }

  // ✅ 로딩 ON
  showLoadingOverlay();

  try {
    const formData = new FormData(form);
    const response = await fetch(form.action, {
      method: "POST",
      body: formData
    });

    if (!response.ok) {
      alert("분석에 실패했습니다.");
      hideLoadingOverlay();
      return;
    }

    // 업로드 성공 → polling으로 결과 대기
    receipts = [];
    renderReceiptList();
    startPolling();

  } catch (e) {
    console.error("Upload error:", e);
    alert("오류가 발생했습니다.");
    hideLoadingOverlay();
  }
}

/* =====================================================
 * Polling
 * ===================================================== */
function startPolling() {
  if (pollingTimer) return;

  pollingTimer = setInterval(async () => {
    try {
      const res = await fetch("/receipt/polling");
      if (!res.ok) return;

      const data = await res.json();
      receipts = data || [];
      renderReceiptList();

      if (receipts.some(r => r.ocr_status === "DONE")) {
        stopPolling();
        const idx = receipts.findIndex(r => r.ocr_status === "DONE");
        if (idx !== -1) showReceipt(idx);
      }
    } catch (e) {
      console.error("polling error:", e);
    }
  }, POLLING_INTERVAL);
}

function stopPolling() {
  if (!pollingTimer) return;

  clearInterval(pollingTimer);
  pollingTimer = null;

  // ✅ OCR 완료 시에만 로딩 OFF
  hideLoadingOverlay();
}

/* =====================================================
 * 기타 유틸
 * ===================================================== */
function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function beforeSubmitConfirm() {
  recalcTotalToHidden();
  const rNo = document.getElementById("rNoInput").value;
  if (!rNo) {
    alert("확정할 영수증이 선택되지 않았습니다.");
    return false;
  }
  return true;
}

function myPageBtn() {
  location.href = "/user/userInfoShowPage";
}
