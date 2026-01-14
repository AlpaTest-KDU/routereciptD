console.log("receipt.js LOADED (FINAL - CONFIRMED)");

/* =====================================================
 * 엔드포인트
 * ===================================================== */
const UPLOAD_ENDPOINT  = "/receipt/uploadReceipt";
const POLLING_ENDPOINT = "/receipt/polling";
const CONFIRM_ENDPOINT = "/receipt/confirm";
const POLLING_INTERVAL = 2000;

/* =====================================================
 * 카테고리 그룹 (백엔드 enum 기준)
 * ===================================================== */
const CATEGORY_GROUPS = {
  FOOD:    ["FOOD"],
  LIFE:    ["LIFE", "HOME", "LIVING"],
  TRAFFIC: ["TRAFFIC"],
  MEDICAL: ["MEDICAL"],
  CULTURE: ["CULTURE"],
  ETC:     ["ETC"]
};

/* =====================================================
 * 전역 상태
 * (HTML에서 receipts가 이미 선언돼 있을 수 있음)
 * ===================================================== */
if (typeof receipts === "undefined") {
  receipts = [];
}

let pollingTimer = null;

/* =====================================================
 * 페이지 초기화
 * ===================================================== */
window.addEventListener("load", () => {
  hideLoadingOverlay();
  renderReceiptList();
});

/* =====================================================
 * 🔴 영수증 업로드 + 분석 시작 (절대 삭제 금지)
 * ===================================================== */
function submitUpload() {
  const input = document.querySelector('input[type="file"]');

  if (!input || !input.files || input.files.length === 0) {
    alert("영수증 파일을 선택하세요.");
    return;
  }

  console.log("[UPLOAD] receipt files:", input.files);

  const formData = new FormData();

  // 🔒 파라미터명 고정: receipt
  Array.from(input.files).forEach(file => {
    formData.append("receipt", file);
  });

  showLoadingOverlay();

  fetch(UPLOAD_ENDPOINT, {
    method: "POST",
    body: formData
  })
    .then(async res => {
      if (!res.ok) {
        const text = await res.text();
        console.error("[UPLOAD ERROR]", res.status, text);
        throw new Error("upload failed");
      }
      return res.json();
    })
    .then(() => {
      // 업로드 성공 → OCR 비동기 처리 시작
      receipts = [];
      renderReceiptList();   // 분석 중 UI
      startPolling();        // 결과 대기
    })
    .catch(err => {
      console.error(err);
      alert("영수증 업로드 중 오류가 발생했습니다.");
      hideLoadingOverlay();
    });
}

/* =====================================================
 * OCR 결과 폴링 (분석 완료 대기)
 * ===================================================== */
function startPolling() {
  if (pollingTimer) return;

  pollingTimer = setInterval(() => {
    fetch(POLLING_ENDPOINT)
      .then(res => res.json())
      .then(data => {
        if (data && data.length > 0) {
          receipts = data;
          clearInterval(pollingTimer);
          pollingTimer = null;
          hideLoadingOverlay();
          renderReceiptList();
        }
      })
      .catch(err => console.error("[POLLING ERROR]", err));
  }, POLLING_INTERVAL);
}

/* =====================================================
 * 영수증 목록 렌더링
 * ===================================================== */
function renderReceiptList() {
  const list = document.getElementById("receiptList");
  if (!list) return;

  list.innerHTML = "";

  if (!receipts || receipts.length === 0) {
    list.innerHTML = `<li class="empty">영수증 분석 중입니다...</li>`;
    return;
  }

  receipts.forEach((receipt, rIdx) => {
    const li = document.createElement("li");
    li.className = "receiptBox";

    li.innerHTML = `
      <div class="receiptHeader">
        <span class="store">${escapeHtml(receipt.store_name || "상호명")}</span>
        <span class="date">${escapeHtml(receipt.purchase_date || "")}</span>
      </div>

      <ul class="itemList">
        ${receipt.items.map((item, iIdx) =>
          renderItem(item, rIdx, iIdx)
        ).join("")}
      </ul>

      <div class="receiptFooter">
        <button type="button" class="confirmBtn"
          onclick="confirmReceipt(${rIdx})">
          영수증 확정
        </button>
      </div>
    `;

    list.appendChild(li);
  });
}

/* =====================================================
 * 개별 상품 렌더링 (카테고리 분석 결과 포함)
 * ===================================================== */
function renderItem(item, rIdx, iIdx) {
  return `
    <li class="itemRow">
      <span class="itemName">${escapeHtml(item.item_name)}</span>

      <select class="categorySelect"
        data-ridx="${rIdx}"
        data-iidx="${iIdx}"
        onchange="onCategoryChange(this)">
        ${buildCategoryOptions(item.category)}
      </select>

      <span class="price">${formatPrice(item.price)}</span>
    </li>
  `;
}

/* =====================================================
 * 카테고리 옵션 생성
 * ===================================================== */
function buildCategoryOptions(current) {
  const allowed = CATEGORY_GROUPS[current] || ["ETC"];

  return allowed.map(cat =>
    `<option value="${cat}" ${cat === current ? "selected" : ""}>
      ${cat}
    </option>`
  ).join("");
}

/* =====================================================
 * 카테고리 변경 (프론트 상태만 반영)
 * ===================================================== */
function onCategoryChange(selectEl) {
  const rIdx = selectEl.dataset.ridx;
  const iIdx = selectEl.dataset.iidx;
  receipts[rIdx].items[iIdx].category = selectEl.value;
}

/* =====================================================
 * 영수증 확정 → 서버 반영
 * ===================================================== */
function confirmReceipt(rIdx) {
  const receipt = receipts[rIdx];

  fetch(CONFIRM_ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      r_no: receipt.r_no,
      items: receipt.items.map(it => ({
        item_no: it.item_no,
        category: it.category
      }))
    })
  })
    .then(res => {
      if (!res.ok) throw new Error("confirm failed");
      return res.json();
    })
    .then(() => {
      alert("영수증이 확정되었습니다.");
    })
    .catch(err => {
      console.error(err);
      alert("영수증 확정 중 오류가 발생했습니다.");
    });
}

/* =====================================================
 * 로딩 오버레이
 * ===================================================== */
function showLoadingOverlay() {
  const overlay = document.getElementById("loadingOverlay");
  if (!overlay) return;
  overlay.classList.add("isOpen");
  document.body.classList.add("isLoading");
}

function hideLoadingOverlay() {
  const overlay = document.getElementById("loadingOverlay");
  if (!overlay) return;
  overlay.classList.remove("isOpen");
  document.body.classList.remove("isLoading");
}

/* =====================================================
 * 유틸
 * ===================================================== */
function escapeHtml(str) {
  return String(str || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function formatPrice(p) {
  if (!p) return "0원";
  return Number(p).toLocaleString() + "원";
}
