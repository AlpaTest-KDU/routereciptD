console.log("📦 receipt.js LOADED");

/* =====================================================
 * ✅ 페이지 진입 시 초기화
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
function hideLoadingOverlay() {
  console.log("🔵 hideLoadingOverlay()");

  const overlay = document.getElementById("loadingOverlay");
  const btn = document.querySelector(".ocrBtn");

  console.log("overlay:", overlay);
  console.log("ocrBtn:", btn);

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
 * 전역 receipts (HTML에서 주입됨)
 * ===================================================== */
var selectedIdx = -1;

/* =====================================================
 * 영수증 목록 렌더링
 * ===================================================== */
function renderReceiptList() {
  console.log("🟣 renderReceiptList() called");

  const list = document.getElementById("receiptList");
  console.log("receiptList element:", list);

  if (!list) {
    console.warn("❗ receiptList element not found");
    return;
  }

  console.log("📥 receipts:", typeof receipts, receipts);

  // receipts 없음 → 분석 중 상태
  if (!receipts || receipts.length === 0) {
    console.log("⏳ receipts is empty → pending UI");

    list.innerHTML = `
      <p class="pendingTitle">영수증을 분석 중입니다.</p>
      <p class="pendingDesc">
        업로드는 정상적으로 완료되었습니다.<br>
        분석에는 약간의 시간이 소요될 수 있습니다.
      </p>
    `;
    return;
  }

  console.log(`✅ receipts count = ${receipts.length}`);

  let html = `
    <p class="pendingTitle">분석된 영수증 목록</p>
    <h3>영수증 목록</h3>
  `;

  receipts.forEach((r, idx) => {
    console.log(`- receipt[${idx}]`, r);

    html += `
      <button type="button"
              class="receiptListNo"
              onclick="showReceipt(${idx})">
        ${idx + 1}번 - ${escapeHtml(r.r_place ?? "상호명 없음")}
      </button>
    `;
  });

  list.innerHTML = html;
}

/* =====================================================
 * 영수증 선택
 * ===================================================== */
function showReceipt(idx) {
  console.log("🟠 showReceipt()", idx);

  selectedIdx = idx;
  const receipt = receipts[idx];

  if (!receipt) {
    console.warn("❗ receipt not found for idx:", idx);
    return;
  }

  console.log("📄 selected receipt:", receipt);

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

  console.log("📦 receipt.items:", receipt.items);

  renderItemsByCategory(receipt.items || []);
  document.getElementById("detailArea").style.display = "block";
  recalcTotalToHidden();
}

/* =====================================================
 * 아이템 렌더링
 * ===================================================== */
function renderItemsByCategory(items) {
  console.log("🟡 renderItemsByCategory()", items);

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
    console.log("ℹ️ no items yet");
    html = "<p>아직 분석된 상품이 없습니다.</p>";
  } else {
    for (const [key, label] of Object.entries(categoryMap)) {
      const group = items.filter(it => it.item_category === key);

      console.log(`category ${key} count =`, group.length);

      if (!group.length) continue;

      html += `<h4>${label}</h4><ul style="list-style:none;padding-left:0;">`;

      group.forEach(it => {
        html += `
          <li class="item-row" style="margin:8px 0; display:flex; gap:10px;">
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
  console.log("❌ removeItemRow()");
  btn.closest("li")?.remove();
  recalcTotalToHidden();
}

function recalcTotalToHidden() {
  let total = 0;

  document
    .querySelectorAll('#confirmForm input[name="item_prices"]')
    .forEach(p => total += Number(p.value || 0));

  console.log("💰 recalculated total =", total);

  document.getElementById("priceText").textContent = total.toLocaleString();
  document.getElementById("rPriceInput").value = total;
}

/* =====================================================
 * 파일 선택
 * ===================================================== */
document.addEventListener("DOMContentLoaded", () => {
  console.log("📁 DOMContentLoaded (file input)");

  const fileInput = document.getElementById("receiptFile");
  const fileBox   = document.getElementById("selectedFileNames");

  if (!fileInput || !fileBox) {
    console.warn("❗ file input elements missing");
    return;
  }

  fileInput.addEventListener("change", () => {
    const files = Array.from(fileInput.files || []);
    console.log("📎 selected files:", files);

    if (!files.length) {
      fileBox.innerHTML = `<span class="fileNamePlaceholder">선택된 파일 없음</span>`;
      return;
    }

    if (files.length <= 3) {
      fileBox.innerHTML = files.map(f =>
        `<span class="fileChip">${escapeHtml(f.name)}</span>`
      ).join("");
    } else {
      fileBox.innerHTML = `
        <span class="fileChip">${escapeHtml(files[0].name)}</span>
        <span class="fileMore">외 ${files.length - 1}개</span>
      `;
    }
  });
});

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

/* =====================================================
 * 폼 제출 전 검증 (beforeSubmitConfirm)
 * ===================================================== */
function beforeSubmitConfirm() {
  console.log("📝 beforeSubmitConfirm() called");

  // 1. 제출 전 총액 다시 계산 (hidden input 동기화)
  recalcTotalToHidden();

  // 2. 필수 값 체크 (예: 영수증 번호가 없는 경우)
  const rNo = document.getElementById("rNoInput").value;
  if (!rNo) {
    alert("확정할 영수증이 선택되지 않았습니다.");
    return false; // 제출 중단
  }

  return true; // 제출 진행
}
