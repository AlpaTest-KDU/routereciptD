// src/main/resources/static/js/myreceiptresultMonthlyChart.js
document.addEventListener("DOMContentLoaded", function () {

  const data = window.receiptData || {};

  // 서버에서 내려온 데이터
  const dailyData   = data.dailyData   || [];   // (dt,total) 형태면 OK (기간은 길어도 상관없음)
  const weeklyData  = data.weeklyData  || [];   // ✅ "최근 28일 일별(dt,total)" 데이터여야 함
  const monthlyData = data.monthlyData || [];   // (ym,total)

  // =============================
  // 공통 유틸
  // =============================
  const pad2 = (n) => String(n).padStart(2, "0");

  function formatDateYMD(d) {
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  }

  function formatYM(d) {
    return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`;
  }

  function addDays(baseDate, delta) {
    const d = new Date(baseDate);
    d.setDate(d.getDate() + delta);
    return d;
  }

  // ✅ 날짜 흔들림 방지: 00:00 고정
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // 빠른 조회용 Map
  const dailyMap   = new Map(dailyData.map(r => [String(r.dt), Number(r.total || 0)]));
  const weeklyMap  = new Map(weeklyData.map(r => [String(r.dt), Number(r.total || 0)]));
  const monthlyMap = new Map(monthlyData.map(r => [String(r.ym), Number(r.total || 0)]));

  // =============================
  // 1) 일별 (오늘 포함 7일)
  // =============================
  const last7Labels = [];
  for (let i = 6; i >= 0; i--) {
    last7Labels.push(formatDateYMD(addDays(today, -i)));
  }
  const last7Totals = last7Labels.map(dt => dailyMap.get(dt) || 0);

  const dailyCanvas = document.getElementById("dailyChart");
  if (dailyCanvas) {
    new Chart(dailyCanvas, {
      type: "line",
      data: {
        labels: last7Labels,
        datasets: [{
          label: "일별 지출(7일)",
          data: last7Totals,
          borderColor: "rgba(54, 162, 235, 1)",
          backgroundColor: "rgba(54, 162, 235, 0.15)",
          tension: 0.3,
          pointRadius: 3,
          pointHitRadius: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: { ticks: { maxRotation: 45, minRotation: 0 } },
          y: { beginAtZero: true }
        },
        plugins: { legend: { display: false } }
      }
    });
  }

// -----------------------------
// 2) 주별 지출 차트 (4주 막대 + 요일(월~일) 누적 스택)
// - weeklyData는 최근 28일 "일별" 데이터(dt,total)여야 함
// - 막대 4개(1~4주차), 각 막대는 월~일이 아래부터 쌓임
// - 툴팁: "3주차 일(YYYY-MM-DD): 15,000원"
// -----------------------------
const weeklyCanvas = document.getElementById("weeklyChart");

if (weeklyCanvas) {
  const pad2 = (n) => String(n).padStart(2, "0");
  const toYMD = (d) => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  const addDays = (d, days) => { const x = new Date(d); x.setDate(x.getDate() + days); return x; };

  // 오늘 00:00 고정
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // 최근 28일(오래된 -> 오늘)
  const start = addDays(today, -27);
  const allDates = [];
  for (let i = 0; i < 28; i++) allDates.push(toYMD(addDays(start, i)));

  // weeklyData -> Map(dt -> total)
  const map = new Map();
  (weeklyData || []).forEach(row => {
    const dt = String(row.dt || "");
    const total = Number(row.total || 0);
    if (dt) map.set(dt, total);
  });

  // 요일 인덱스: 월=0 ... 일=6
  const weekdayIdx = (ymd) => {
    const d = new Date(ymd);
    // JS getDay(): 일=0 ... 토=6  -> 월=0..일=6으로 변환
    return (d.getDay() + 6) % 7;
  };

  // weekDates[week][weekday] = 'YYYY-MM-DD'
  // weeks[week][weekday] = total
  const weekDates = Array.from({ length: 4 }, () => Array(7).fill(""));
  const weeks     = Array.from({ length: 4 }, () => Array(7).fill(0));

  allDates.forEach((dt, i) => {
    const w = Math.floor(i / 7);          // 0~3 (1~4주차)
    const wd = weekdayIdx(dt);            // 0~6 (월~일)
    weekDates[w][wd] = dt;
    weeks[w][wd] = map.get(dt) ?? 0;
  });

  const weekLabels = ["1주차", "2주차", "3주차", "4주차"];

  // 요일 라벨(월~일) + 요청 색상(빨/주/노/초/파/남/보)
  const wdLabels = ["월", "화", "수", "목", "금", "토", "일"];
  const wdColors = [
    "rgba(255, 99, 132, 0.85)",  // 월 빨
    "rgba(255, 159, 64, 0.85)",  // 화 주
    "rgba(255, 205, 86, 0.85)",  // 수 노
    "rgba(75, 192, 192, 0.85)",  // 목 초
    "rgba(54, 162, 235, 0.85)",  // 금 파
    "rgba(25, 25, 112, 0.85)",   // 토 남
    "rgba(153, 102, 255, 0.85)"  // 일 보
  ];
  const wdBorders = wdColors.map(c => c.replace("0.85", "1"));

  // datasets 7개(월~일) 생성 -> stacked
  const datasets = wdLabels.map((label, wd) => ({
    label,
    data: [weeks[0][wd], weeks[1][wd], weeks[2][wd], weeks[3][wd]],
    backgroundColor: wdColors[wd],
    borderColor: wdBorders[wd],
    borderWidth: 1,
    stack: "weekStack"
  }));

  new Chart(weeklyCanvas, {
    type: "bar",
    data: { labels: weekLabels, datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        x: { stacked: true },
        y: { stacked: true, beginAtZero: true }
      },
      plugins: {
        legend: { position: "bottom" },
        tooltip: {
          callbacks: {
            // 제목: "3주차"
            title: (items) => items?.[0]?.label ?? "",
            // 내용: "일(2025-12-31): 15,000원"
            label: (ctx) => {
              const w = ctx.dataIndex;        // 0~3
              const wd = ctx.datasetIndex;    // 0~6 (월~일)
              const dateStr = weekDates[w][wd] || "";
              const v = Number(ctx.raw || 0);
              return `${weekLabels[w]} ${ctx.dataset.label}(${dateStr}): ${v.toLocaleString()}원`;
            }
          }
        }
      }
    }
  });
}


  // =============================
  // 3) 월별 (이번달 포함 3개월)
  // =============================
  const monthLabels = [];
  for (let i = 2; i >= 0; i--) {
    const d = new Date(today.getFullYear(), today.getMonth() - i, 1);
    monthLabels.push(formatYM(d));
  }
  const monthTotals = monthLabels.map(ym => monthlyMap.get(ym) || 0);

  const monthlyCanvas = document.getElementById("monthlyChart");
  if (monthlyCanvas) {
    new Chart(monthlyCanvas, {
      type: "bar",
      data: {
        labels: monthLabels,
        datasets: [{
          label: "월별 지출(3개월)",
          data: monthTotals,
          backgroundColor: "rgba(75, 192, 192, 0.7)",
          borderColor: "rgba(75, 192, 192, 1)",
          borderWidth: 1,
          borderRadius: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: { y: { beginAtZero: true } },
        plugins: { legend: { display: false } }
      }
    });
  }

});
