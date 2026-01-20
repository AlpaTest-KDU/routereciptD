console.log("📦 receipt.js LOADED");

/* =====================================================
 * 전역 상태
 * ===================================================== */
var selectedIdx = -1;
var pollingTimer = null;
var POLLING_INTERVAL = 2000;

/* =====================================================
 * 카테고리 상수
 * ===================================================== */
const CATEGORY_MAP = {
  FOOD: "음식",
  MEDICAL: "의료",
  CLOTHES: "의류",
  HOME: "주거",
  LIVING: "생활",
  CULTURE: "문화",
  TRAFFIC: "교통",
  ETC: "기타"
};

/* =====================================================
 * 페이지 진입 시 초기화
 * ===================================================== */
function initPage() {
  hideLoadingOverlay();
  renderReceiptList();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", initPage);
} else {
  initPage();
}

/* =====================================================
 * 로딩 오버레이
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
 * 영수증 목록
 * ===================================================== */
function renderReceiptList() {
  const list = document.getElementById("receiptList");
  if (!list) return;

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
    const status =
      r.ocr_status === "DONE" ? "분석 완료" : "분석 중";

    html += `
      <button type="button"
              class="receiptListNo"
              onclick="showReceipt(${idx})">
        ${idx + 1}번 - ${escapeHtml(r.r_place ?? "상호명 없음")}
        <span class="status">(${status})</span>
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

  document.getElementById("placeText").textContent = receipt.r_place || "";
  document.getElementById("dateText").textContent =
    (receipt.r_date || "").toString().substring(0, 10);
  document.getElementById("priceText").textContent =
    Number(receipt.r_price || 0).toLocaleString();

  document.getElementById("rNoInput").value = receipt.r_no;
  document.getElementById("rPlaceInput").value = receipt.r_place || "";
  document.getElementById("rDateInput").value =
    (receipt.r_date || "").toString().substring(0, 10);
  document.getElementById("rPriceInput").value = receipt.r_price || 0;

  renderItemsByCategory(receipt.items || []);
  document.getElementById("detailArea").style.display = "block";
  recalcTotalToHidden();
}

/* =====================================================
 * category select 생성
 * ===================================================== */
function renderCategorySelect(selected) {
  let html = `<select name="item_categories">`;
  for (const [key, label] of Object.entries(CATEGORY_MAP)) {
    const sel = key === selected ? "selected" : "";
    html += `<option value="${key}" ${sel}>${label}</option>`;
  }
  html += `</select>`;
  return html;
}

/* =====================================================
 * 아이템 렌더링 (⭐ 핵심 수정)
 * ===================================================== */
function renderItemsByCategory(items) {
  let html = "";

  if (!items.length) {
    html = "<p>아직 분석된 상품이 없습니다.</p>";
  } else {
    for (const [key, label] of Object.entries(CATEGORY_MAP)) {
      const group = items.filter(it => it.item_category === key);
      if (!group.length) continue;

      html += `<h4>${label}</h4><ul style="list-style:none;padding-left:0;">`;

      group.forEach(it => {
        html += `
          <li class="item-row" style="margin:8px 0; display:flex; gap:10px;">
            ${renderCategorySelect(it.item_category)}
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

  document.getElementById("priceText").textContent =
    total.toLocaleString();
  document.getElementById("rPriceInput").value = total;
}

/* =====================================================
 * 파일 업로드
 * ===================================================== */
async function submitUpload() {
  const form = document.getElementById("uploadForm");
  const fileInput = document.getElementById("receiptFile");

  if (!fileInput || fileInput.files.length === 0) {
    alert("영수증 이미지를 선택해주세요.");
    return;
  }

  showLoadingOverlay();

  try {
    const response = await fetch(form.action, {
      method: "POST",
      body: new FormData(form)
    });

    if (!response.ok) {
      alert("분석에 실패했습니다.");
      hideLoadingOverlay();
      return;
    }

    receipts = [];
    renderReceiptList();
    startPolling();

  } catch (e) {
    console.error(e);
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
    const res = await fetch("/receipt/polling");
    if (!res.ok) return;

    receipts = await res.json();
    renderReceiptList();

    if (receipts.some(r => r.ocr_status === "DONE")) {
      stopPolling();
      showReceipt(receipts.findIndex(r => r.ocr_status === "DONE"));
    }
  }, POLLING_INTERVAL);
}

function stopPolling() {
  clearInterval(pollingTimer);
  pollingTimer = null;
  hideLoadingOverlay();
}

/* =====================================================
 * 유틸
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
  if (!document.getElementById("rNoInput").value) {
    alert("확정할 영수증이 선택되지 않았습니다.");
    return false;
  }
  return true;
}

function myPageBtn() {
  location.href = "/user/userInfoShowPage";
}
