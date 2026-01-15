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

// 마이페이지 이동
function myPageBtn() {
  location.href = '/user/userInfoShowPage';
}

/* =====================================================
 * 파일 업로드 제출 (submitUpload)
 * ===================================================== */
async function submitUpload() {
  console.log("📤 submitUpload() called");

  const form = document.getElementById("uploadForm");
  const fileInput = document.getElementById("receiptFile");
  if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
    alert("영수증 이미지를 선택해주세요.");
    return;
  }

  // 로딩 UI 활성화
  const overlay = document.getElementById("loadingOverlay");
  if (overlay) {
    overlay.classList.add("isOpen");
    overlay.setAttribute("aria-hidden", "false");
  }
  document.body.classList.add("isLoading");

  // 버튼 비활성화 (중복 제출 방지)
  const btn = document.querySelector(".ocrBtn");
  if (btn) {
    btn.disabled = true;
    btn.textContent = "분석 중...";
  }

  try {
    const formData = new FormData(form);

    // 비동기 요청 (AJAX)
    const response = await fetch(form.action, {
      method: 'POST',
      body: formData
    });

    if (response.ok) {
      const data = await response.json();
      console.log("✅ Analysis success:", data);

      // 1. 전역 데이터 갱신
      receipts = data;

      // 2. 목록 UI 갱신
      renderReceiptList();

      // 3. 결과가 있다면 첫 번째 항목 상세 표시
      if (receipts && receipts.length > 0) {
        showReceipt(0);
      } else {
        alert("분석된 내용이 없습니다.");
      }
    } else {
      console.error("Upload failed status:", response.status);
      alert("분석에 실패했습니다. (서버 오류)");
    }
  } catch (error) {
    console.error("Upload error:", error);
    alert("오류가 발생했습니다.");
  } finally {
    // 로딩 해제
    hideLoadingOverlay();
  }
}

/* =====================================================
 * 직접 입력 모달 (openManualModal)
 * ===================================================== */
function openManualModal() {
  console.log("👐 openManualModal() called");

  // 1. 선택 초기화
  selectedIdx = -1;

  // 2. 상호명 입력 (HTML 구조상 prompt로 대체)
  const placeName = prompt("상호명을 입력해주세요.", "직접 입력");
  if (placeName === null) return; // 취소 시 중단

  const today = new Date().toISOString().substring(0, 10);

  // 3. 화면 텍스트 초기화
  document.getElementById("placeText").textContent = placeName;
  document.getElementById("dateText").textContent  = today;
  document.getElementById("priceText").textContent = "0";

  // 4. Hidden Input 초기화 (r_no="0" -> 신규 등록 처리용)
  document.getElementById("rNoInput").value    = "0";
  document.getElementById("rPlaceInput").value = placeName;
  document.getElementById("rDateInput").value  = today;
  document.getElementById("rPriceInput").value = "0";

  // 5. 아이템 리스트 초기화
  renderItemsByCategory([]);

  // 6. 상세 영역 표시 및 스크롤 이동
  const detailArea = document.getElementById("detailArea");
  detailArea.style.display = "block";
  detailArea.scrollIntoView({ behavior: "smooth" });

  recalcTotalToHidden();
}
