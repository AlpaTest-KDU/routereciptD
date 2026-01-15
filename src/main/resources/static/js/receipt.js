/* ======================================================
 * 공통 유틸
 * ====================================================== */
console.log("receipt.js LOADED");
function escapeHtml(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function escapeHtmlAttr(str) {
  return escapeHtml(str);
}

/* ======================================================
 * OCR / 영수증 선택
 * ====================================================== */
let selectedIdx = -1;

function showReceipt(idx) {
  selectedIdx = idx;
  const receipt = receipts[idx];
  if (!receipt) return;

  const place = receipt.r_place ?? "";
  const date  = (receipt.r_date ?? "").toString().substring(0, 10);
  const price = Number(receipt.r_price ?? 0);

  document.getElementById("placeText").textContent = place;
  document.getElementById("dateText").textContent  = date;
  document.getElementById("priceText").textContent = price.toLocaleString();

  document.getElementById("rNoInput").value    = receipt.r_no ?? "";
  document.getElementById("rPlaceInput").value = place;
  document.getElementById("rDateInput").value  = date;
  document.getElementById("rPriceInput").value = price;

  renderItemsByCategory(receipt.items ?? []);
  document.getElementById("detailArea").style.display = "block";
  recalcTotalToHidden();
}

function renderItemsByCategory(items) {
  const categoryMap = {
    FOOD: "음식", MEDICAL: "의료", CLOTHES: "의류",
    HOME: "주거", LIVING: "생활", CULTURE: "문화",
    TRAFFIC: "교통", ETC: "기타"
  };

  let html = "";

  if (!items.length) {
    html = "<p>카테고리 정보가 없습니다.</p>";
  } else {
    for (const [key, label] of Object.entries(categoryMap)) {
      const group = items.filter(it => it.item_category === key);
      if (!group.length) continue;

      html += `<h4>${label}</h4><ul style="list-style:none;padding-left:0;">`;

      group.forEach(it => {
        html += `
          <li style="margin:8px 0; display:flex; gap:10px; flex-wrap:wrap;">
            <input type="hidden" name="item_categories" value="${escapeHtmlAttr(it.item_category)}">
            <input type="text" name="item_names" value="${escapeHtmlAttr(it.item_name ?? "")}">
            <input type="number" name="item_prices" value="${Number(it.item_price ?? 0)}"
                   min="0" oninput="recalcTotalToHidden()">
            <button type="button" onclick="removeItemRow(this)">삭제</button>
          </li>`;
      });

      html += "</ul>";
    }
  }

  document.getElementById("categoryArea").innerHTML = html;
}

/* ======================================================
 * 가격 / 항목 조작
 * ====================================================== */
function removeItemRow(btn) {
  btn.closest("li")?.remove();
  recalcTotalToHidden();
}

function recalcTotalToHidden() {
  let total = 0;
  document.querySelectorAll('#confirmForm input[name="item_prices"]').forEach(i => {
    total += Number(i.value || 0);
  });
  document.getElementById("priceText").textContent = total.toLocaleString();
  document.getElementById("rPriceInput").value = total;
}

function beforeSubmitConfirm() {
  const names = document.querySelectorAll('#confirmForm input[name="item_names"]');
  if (!names.length) {
    alert("상품이 1개 이상 있어야 합니다.");
    return false;
  }
  recalcTotalToHidden();
  return true;
}

/* ======================================================
 * 수기 입력
 * ====================================================== */
let manualItems = [];

function openManualModal() {
  manualItems = [];
  document.getElementById("manualItemTable").innerHTML = "";
  document.getElementById("manualPlaceInput").value = "";
  document.getElementById("manualDateInput").value =
    new Date().toISOString().substring(0, 10);

  document.getElementById("manualModal").style.display = "flex";
}

function closeManualModal() {
  document.getElementById("manualModal").style.display = "none";
}

function addManualItem() {
  const category = manualCategorySelect.value;
  const name = manualItemName.value.trim();
  const price = parseInt(manualItemPrice.value, 10);

  if (!name || isNaN(price)) {
    alert("상품명과 가격을 입력하세요.");
    return;
  }

  manualItems.push({ category, name, price });
  manualItemName.value = "";
  manualItemPrice.value = "";

  renderManualTable();
}

function renderManualTable() {
  const tbody = document.getElementById("manualItemTable");
  tbody.innerHTML = "";

  manualItems.forEach((item, idx) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${item.category}</td>
      <td>${item.name}</td>
      <td>${item.price.toLocaleString()}원</td>
      <td>
        <button type="button" onclick="removeManualItem(${idx})">삭제</button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function removeManualItem(idx) {
  manualItems.splice(idx, 1);
  renderManualTable();
}

function submitManualForm() {
  const place = manualPlaceInput.value.trim();
  const date  = manualDateInput.value;

  if (!place) {
    alert("상호명을 입력하세요.");
    return false;
  }

  document.getElementById("placeText").textContent = place;
  document.getElementById("dateText").textContent = date;

  document.getElementById("rNoInput").value = "0";
  document.getElementById("rPlaceInput").value = place;
  document.getElementById("rDateInput").value = date;

  // 🔥 핵심: 수기 입력 항목 → 기존 categoryArea로 변환
  renderItemsByCategory(manualItems);

  closeManualModal();
  document.getElementById("detailArea").style.display = "block";
  return false;
}
