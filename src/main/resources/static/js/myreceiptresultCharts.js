// =============================
// 0) 도넛/파이 퍼센트 플러그인
// =============================
const DoughnutPercentagePlugin = {
  id: "doughnutPercentage",
  afterDraw(chart, args, options) {
    // ✅ 도넛 / 파이 차트에만 적용
    if (chart.config.type !== "doughnut" && chart.config.type !== "pie") return;

    const { ctx } = chart;
    const dataset = chart.data.datasets[0];
    if (!dataset) return;

    const data = dataset.data || [];
    const total = data.reduce((sum, v) => sum + Number(v || 0), 0) || 1;
    const meta = chart.getDatasetMeta(0);

    ctx.save();

    meta.data.forEach((arc, index) => {
      const rawValue = Number(data[index]) || 0;
      if (!rawValue) return;

      const percentage = (rawValue / total) * 100;
      const label = `${percentage.toFixed(1)}%`;

      const { x, y } = arc.getCenterPoint();

      ctx.fillStyle = (options && options.color) || "#ffffff";
      const fontSize = (options && options.fontSize) || 14;
      const fontFamily = (options && options.fontFamily) || "sans-serif";
      ctx.font = fontSize + "px " + fontFamily;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(label, x, y);
    });

    // 상태 원복
    ctx.restore();
  }
};

// 전역 플러그인 등록
Chart.register(DoughnutPercentagePlugin);

// =============================
// DOMContentLoaded 이후 차트 생성
// =============================
document.addEventListener("DOMContentLoaded", function () {

  // 컨트롤러(Thymeleaf)에서 세팅한 데이터
  const data = window.receiptData || {};

  const dailyData   = data.dailyData   || [];
  const weeklyData  = data.weeklyData  || [];
  const monthlyData = data.monthlyData || [];
  const genderData  = data.genderData  || [];
  const myAvg       = data.myAvg || 0;
  const allAvg      = data.allAvg || 0;

  // -----------------------------
  // 1) 일별 지출 차트 (line)
  // -----------------------------
  const dailyLabels = dailyData.map(row => row.dt);        // "2025-12-01"
  const dailyTotals = dailyData.map(row => row.total || 0);

  const dailyCanvas = document.getElementById("dailyChart");
  if (dailyCanvas && dailyLabels.length > 0) {
    new Chart(dailyCanvas, {
      type: "line",
      data: {
        labels: dailyLabels,
        datasets: [{
          label: "일별 지출",
          data: dailyTotals,
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
  // 2) 주별 지출 차트 (bar)
  // -----------------------------
  const weeklyLabels = weeklyData.map(row => `${row.weekStart} ~ ${row.weekEnd}`);
  const weeklyTotals = weeklyData.map(row => row.total || 0);

  const weeklyCanvas = document.getElementById("weeklyChart");
  if (weeklyCanvas && weeklyLabels.length > 0) {
    new Chart(weeklyCanvas, {
      type: "bar",
      data: {
        labels: weeklyLabels,
        datasets: [{
          label: "주별 지출",
          data: weeklyTotals,
          backgroundColor: "rgba(255, 159, 64, 0.7)",
          borderColor: "rgba(255, 159, 64, 1)",
          borderWidth: 1,
          borderRadius: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: { ticks: { maxRotation: 30, minRotation: 0 } },
          y: { beginAtZero: true }
        },
        plugins: { legend: { display: false } }
      }
    });
  }

  // -----------------------------
  // 3) 월별 지출 차트 (bar)
  // -----------------------------
  const monthlyLabels = monthlyData.map(row => row.ym);   // "2025-12"
  const monthlyTotals = monthlyData.map(row => row.total || 0);

  const monthlyCanvas = document.getElementById("monthlyChart");
  if (monthlyCanvas && monthlyLabels.length > 0) {
    new Chart(monthlyCanvas, {
      type: "bar",
      data: {
        labels: monthlyLabels,
        datasets: [{
          label: "월별 지출",
          data: monthlyTotals,
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

  // -----------------------------
  // 4) 성별 지출 도넛 차트 (MALE/FEMALE로 변경)
  // -----------------------------
  const genderCanvas = document.getElementById("genderChart");
  const genderSummaryBox = document.getElementById("genderSummary");

  // 성별 합계 정리: FEMALE/MALE/OTHER
  const genderSum = { FEMALE: 0, MALE: 0, OTHER: 0 };

  (genderData || []).forEach(row => {
    const g = String(row.gender || "").toUpperCase();
    const v = Number(row.total || 0);

    if (g === "FEMALE") genderSum.FEMALE += v;
    else if (g === "MALE") genderSum.MALE += v;
    else genderSum.OTHER += v;
  });

  // 여 -> 남 -> 기타
  const genderLabels = [];
  const genderTotals = [];

  if (genderSum.FEMALE > 0) { genderLabels.push("여"); genderTotals.push(genderSum.FEMALE); }
  if (genderSum.MALE > 0)   { genderLabels.push("남"); genderTotals.push(genderSum.MALE); }
  if (genderSum.OTHER > 0)  { genderLabels.push("기타"); genderTotals.push(genderSum.OTHER); }

  if (genderCanvas && genderLabels.length > 0) {
    const totalSum = genderTotals.reduce((sum, v) => sum + Number(v || 0), 0) || 1;

    // 도넛 아래 요약
    if (genderSummaryBox) {
      const pieces = genderLabels.map((label, idx) => {
        const value = Number(genderTotals[idx]) || 0;
        const percent = (value / totalSum) * 100;
        return `<span>${label}: ${percent.toFixed(1)}% (${value.toLocaleString()}원)</span>`;
      });
      genderSummaryBox.innerHTML = pieces.join("<br>");
    }

    new Chart(genderCanvas, {
      type: "doughnut",
      data: {
        labels: genderLabels,
        datasets: [{
          data: genderTotals,
          backgroundColor: [
            "rgba(255, 99, 132, 0.9)", // 여
            "rgba(80, 120, 255, 0.9)", // 남
            "rgba(153, 102, 255, 0.9)" // 기타
          ],
          borderColor: [
            "rgba(255, 99, 132, 1)",
            "rgba(80, 120, 255, 1)",
            "rgba(153, 102, 255, 1)"
          ],
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "65%",
        plugins: {
          legend: {
            position: "bottom",
            labels: { boxWidth: 18, padding: 16 }
          },
          tooltip: { enabled: false },
          doughnutPercentage: {
            color: "#ffffff",
            fontSize: 14,
            fontFamily: "system-ui"
          }
        }
      }
    });
  }

  // -----------------------------
  // 5) 나의 평균 vs 전체 평균 (bar)
  // -----------------------------
  const avgCanvas = document.getElementById("avgChart");
  if (avgCanvas) {
    new Chart(avgCanvas, {
      type: "bar",
      data: {
        labels: ["나의 평균", "전체 평균"],
        datasets: [{
          label: "1회 결제 평균 금액",
          data: [myAvg, allAvg],
          backgroundColor: [
            "rgba(54, 162, 235, 0.7)",
            "rgba(201, 203, 207, 0.7)"
          ],
          borderColor: [
            "rgba(54, 162, 235, 1)",
            "rgba(201, 203, 207, 1)"
          ],
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
