console.log("receipt.js LOADED (FINAL - CONFIRMED)");

/* =====================================================
 * 엔드포인트
 * ===================================================== */
const UPLOAD_ENDPOINT  = "/receipt/uploadReceipt";
const POLLING_ENDPOINT = "/receipt/polling";
const CONFIRM_ENDPOINT = "/receipt/confirm";
const POLLING_INTERVAL = 2000;
const MAX_POLL_COUNT   = 30; // 최대 60초 대기 (2000ms * 30)

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
let pollCount = 0;

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

  pollCount = 0; // 카운트 초기화

  pollingTimer = setInterval(() => {
    pollCount++;

    // 타임아웃 체크 (무한 로딩 방지)
    if (pollCount > MAX_POLL_COUNT) {
      stopPolling();
      alert("분석 시간이 초과되었습니다. 다시 시도해주세요.");
      return;
    }

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

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer);
    pollingTimer = null;
  }
  hideLoadingOverlay();
}

/* =====================================================
 * 영수증 목록 렌더링
 * ===================================================== */
function renderReceiptList() {
  const list = document.getElementById("receiptList");
  if (!list) return;

  // 기존 내용을 초기화하되, 제목(h3)은 유지하거나 다시 그려줌
  list.innerHTML = "<h3>영수증 목록</h3>";
  
  const ul = document.createElement("ul");
  ul.className = "receiptUl"; // 스타일링을 위한 클래스 추가

  if (!receipts || receipts.length === 0) {
    ul.innerHTML = `<li class="empty">분석된 영수증이 없습니다.</li>`;
    list.appendChild(ul);
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
          onclick="confirmReceipt('${receipt.r_no}')">
          영수증 확정
        </button>
      </div>
    `;

    ul.appendChild(li);
  });
  
  list.appendChild(ul);
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
function confirmReceipt(rNo) {
  // r_no를 기준으로 영수증 객체 찾기 (문자열/숫자 비교를 위해 == 사용)
  const receipt = receipts.find(r => r.r_no == rNo);

  if (!receipt) {
    alert("해당 영수증 정보를 찾을 수 없습니다.");
    return;
  }

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

// 마이페이지 이동
function myPageBtn() {
  location.href = '/user/userInfoShowPage';
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

function recalcTotalToHidden() {
  let total = 0;

  document
      .querySelectorAll('#confirmForm input[name="item_prices"]')
      .forEach(p => total += Number(p.value || 0));

  console.log("💰 recalculated total =", total);

  document.getElementById("priceText").textContent = total.toLocaleString();
  document.getElementById("rPriceInput").value = total;
}

function removeItemRow(btn) {
  console.log("❌ removeItemRow()");
  btn.closest("li")?.remove();
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