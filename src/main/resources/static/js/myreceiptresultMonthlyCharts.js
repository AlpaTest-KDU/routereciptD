// src/main/resources/static/js/myreceiptresultMonthlyChart.js
document.addEventListener("DOMContentLoaded", function () {

  const data = window.receiptData || {};

  const dailyData   = data.dailyData   || [];
  const weeklyData  = data.weeklyData  || [];
  const monthlyData = data.monthlyData || [];

  // 1) 일별 지출 (line)
  const dailyLabels = dailyData.map(row => row.dt);
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

  // 2) 주별 지출 (bar)
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

  // 3) 월별 지출 (bar)
  const monthlyLabels = monthlyData.map(row => row.ym);
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

});
