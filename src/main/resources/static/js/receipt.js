console.log("receipt.js LOADED");

/* =====================================================
 * ✅ 페이지 진입 시 로딩 오버레이 강제 해제 (최종)
 * ===================================================== */
window.addEventListener("load", () => {
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
});

/* =========================
 * 공통 유틸
 * ========================= */
function escapeHtml(str) {
  str = (str === undefined || str === null) ? "" : String(str);
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function escapeHtmlAttr(str) {
  return escapeHtml(str);
}

/* =========================
 * OCR / 영수증 선택
 * ========================= */
var selectedIdx = -1;

function showReceipt(idx) {
  selectedIdx = idx;
  var receipt = receipts[idx];
  if (!receipt) return;

  var place = receipt.r_place || "";
  var date  = (receipt.r_date || "").toString().substring(0, 10);
  var price = Number(receipt.r_price || 0);

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

/* =========================
 * 아이템 렌더링
 * ========================= */
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

  if (!items || !items.length) {
    html = "<p>카테고리 정보가 없습니다.</p>";
  } else {
    for (const [key, label] of Object.entries(categoryMap)) {
      const group = items.filter(it => it.item_category === key);
      if (!group.length) continue;

      html += `<h4>${label}</h4><ul style="list-style:none;padding-left:0;">`;

      group.forEach(it => {
        let selectHtml = `<select name="item_categories">`;
        for (const [catKey, catLabel] of Object.entries(categoryMap)) {
          const selected = (catKey === it.item_category) ? "selected" : "";
          selectHtml += `<option value="${catKey}" ${selected}>${catLabel}</option>`;
        }
        selectHtml += `</select>`;

        html += `
          <li class="item-row" style="margin:8px 0; display:flex; gap:10px; flex-wrap:wrap;">
            ${selectHtml}
            <input type="text" name="item_names"
                   value="${escapeHtmlAttr(it.item_name ?? "")}">
            <input type="number" name="item_prices"
                   value="${Number(it.item_price ?? 0)}"
                   min="0"
                   oninput="recalcTotalToHidden()">
            <button type="button" onclick="removeItemRow(this)">삭제</button>
          </li>`;
      });

      html += "</ul>";
    }
  }

  document.getElementById("categoryArea").innerHTML = html;
}

/* =========================
 * 가격 계산
 * ========================= */
function removeItemRow(btn) {
  const li = btn.closest("li");
  if (li) li.remove();
  recalcTotalToHidden();
}

function recalcTotalToHidden() {
  let total = 0;
  const prices = document.querySelectorAll(
    '#confirmForm input[name="item_prices"]'
  );

  prices.forEach(p => {
    total += Number(p.value || 0);
  });

  document.getElementById("priceText").textContent = total.toLocaleString();
  document.getElementById("rPriceInput").value = total;
}

/* =========================
 * 확정 전 검증
 * ========================= */
function beforeSubmitConfirm() {
  const rows = document.querySelectorAll('#confirmForm .item-row');

  if (!rows.length) {
    alert("상품이 1개 이상 있어야 합니다.");
    return false;
  }

  for (const row of rows) {
    const name  = row.querySelector('[name="item_names"]');
    const price = row.querySelector('[name="item_prices"]');
    const cat   = row.querySelector('[name="item_categories"]');

    if (!name || !price || !cat) {
      alert("아이템 데이터가 손상되었습니다. 새로고침 후 다시 시도하세요.");
      return false;
    }

    price.value = Number(price.value || 0);
  }

  recalcTotalToHidden();
  return true;
}

/* =========================
 * 파일 선택 시 파일명 표시
 * ========================= */
document.addEventListener("DOMContentLoaded", () => {
  const fileInput = document.getElementById("receiptFile");
  const fileBox   = document.getElementById("selectedFileNames");

  if (!fileInput || !fileBox) return;

  fileInput.addEventListener("change", () => {
    const files = Array.from(fileInput.files || []);

    // 파일 선택 안 한 경우
    if (files.length === 0) {
      fileBox.innerHTML =
        `<span class="fileNamePlaceholder">선택된 파일 없음</span>`;
      return;
    }

    // 파일 1~3개는 이름 전부 표시
    if (files.length <= 3) {
      fileBox.innerHTML = `
        <div class="fileNameList">
          ${files.map(f => `
            <span class="fileChip" title="${f.name}">
              ${f.name}
            </span>
          `).join("")}
        </div>
      `;
      return;
    }

    // 파일 4개 이상이면 요약
    const first = files[0].name;
    fileBox.innerHTML = `
      <div class="fileNameList">
        <span class="fileChip" title="${first}">
          ${first}
        </span>
        <span class="fileMore">외 ${files.length - 1}개</span>
      </div>
    `;
  });
});
