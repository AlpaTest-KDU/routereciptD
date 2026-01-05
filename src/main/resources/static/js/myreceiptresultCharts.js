// =============================
// 0) 도넛/파이 퍼센트 플러그인 (성별 도넛에서만 사용)
// =============================
const DoughnutPercentagePlugin = {
  id: "doughnutPercentage",
  afterDraw(chart, args, options) {
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

    ctx.restore();
  }
};

Chart.register(DoughnutPercentagePlugin);

// =============================
// analysisPage 전용: 4)성별 5)나vs전체 평균 (+ 6 데이터)
// =============================
document.addEventListener("DOMContentLoaded", function () {

  // 컨트롤러(Thymeleaf)에서 세팅한 데이터
  const data = window.receiptData || {};

  const genderData = data.genderData || [];
  const myAvg = Number(data.myAvg || 0);
  const allAvg = Number(data.allAvg || 0);

  // -----------------------------
  // 4) 성별 지출 도넛 차트 (여=빨강, 남=파랑 고정)
  // -----------------------------
  const genderCanvas = document.getElementById("genderChart");
  const genderSummaryBox = document.getElementById("genderSummary");

  const genderSum = { FEMALE: 0, MALE: 0, OTHER: 0 };

  (genderData || []).forEach(row => {
    const g = String(row.gender || "").toUpperCase();
    const v = Number(row.total || 0);

    if (g === "FEMALE") genderSum.FEMALE += v;
    else if (g === "MALE") genderSum.MALE += v;
    else genderSum.OTHER += v;
  });

  const genderLabels = [];
  const genderTotals = [];
  const genderBgColors = [];
  const genderBorderColors = [];

  if (genderSum.FEMALE > 0) {
    genderLabels.push("여");
    genderTotals.push(genderSum.FEMALE);
    genderBgColors.push("rgba(255, 99, 132, 0.9)");
    genderBorderColors.push("rgba(255, 99, 132, 1)");
  }

  if (genderSum.MALE > 0) {
    genderLabels.push("남");
    genderTotals.push(genderSum.MALE);
    genderBgColors.push("rgba(80, 120, 255, 0.9)");
    genderBorderColors.push("rgba(80, 120, 255, 1)");
  }

  if (genderSum.OTHER > 0) {
    genderLabels.push("기타");
    genderTotals.push(genderSum.OTHER);
    genderBgColors.push("rgba(153, 102, 255, 0.9)");
    genderBorderColors.push("rgba(153, 102, 255, 1)");
  }

  if (genderCanvas && genderLabels.length > 0) {
    const totalSum = genderTotals.reduce((sum, v) => sum + Number(v || 0), 0) || 1;

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
          backgroundColor: genderBgColors,
          borderColor: genderBorderColors,
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: "65%",
        plugins: {
          legend: { position: "bottom", labels: { boxWidth: 18, padding: 16 } },
          tooltip: { enabled: false },
          doughnutPercentage: { color: "#ffffff", fontSize: 14, fontFamily: "system-ui" }
        }
      }
    });
  }

  // -----------------------------
  // 5) 나의 평균 vs 전체 평균 (bar)  (6 데이터 사용)
  // -----------------------------
  const avgCanvas = document.getElementById("avgChart");
  if (avgCanvas) {
    new Chart(avgCanvas, {
      type: "bar",
      data: {
        labels: ["나의 평균", "전체 평균"],
        datasets: [{
          label: "일별 지출 평균",
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
