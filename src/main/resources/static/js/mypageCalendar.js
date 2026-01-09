/**
 * 마이페이지 소비 달력 스크립트
 */

window.CalendarManager = {
    currentYear: new Date().getFullYear(),
    currentMonth: new Date().getMonth() + 1, // 1-12
    receiptData: [], // 서버에서 받아온 영수증 데이터 저장

    init: function() {
        this.loadCalendarData(this.currentYear, this.currentMonth);
        
        // 카테고리 변경 시 이벤트 리스너
        const categorySelect = document.getElementById('categoryFilter');
        if(categorySelect) {
            categorySelect.addEventListener('change', () => {
                this.renderCalendar();
            });
        }
        
        // 모달 외부 클릭 시 닫기 이벤트
        const modal = document.getElementById('dateDetailModal');
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.closeModal();
                }
            });
        }
    },

    // 서버에서 달력 데이터(영수증) 가져오기
    loadCalendarData: function(year, month) {
        fetch(`/user/userInfoShowPage/calendar?year=${year}&month=${month}`)
            .then(response => {
                if (!response.ok) throw new Error('Network response was not ok');
                return response.json();
            })
            .then(data => {
                // data 구조: { month: "YYYY-MM", receipt: [...] }
                this.receiptData = data.receipt || [];
                this.renderCalendar();
            })
            .catch(error => {
                console.error('달력 데이터를 불러오는 중 오류 발생:', error);
            });
    },

    // 이전 달로 이동
    prevMonth: function() {
        this.currentMonth--;
        if (this.currentMonth < 1) {
            this.currentMonth = 12;
            this.currentYear--;
        }
        this.loadCalendarData(this.currentYear, this.currentMonth);
    },

    // 다음 달로 이동
    nextMonth: function() {
        this.currentMonth++;
        if (this.currentMonth > 12) {
            this.currentMonth = 1;
            this.currentYear++;
        }
        this.loadCalendarData(this.currentYear, this.currentMonth);
    },

    // 달력 그리기
    renderCalendar: function() {
        const calendarBox = document.querySelector('.calendarBox');
        if (!calendarBox) return;

        const selectedCategory = document.getElementById('categoryFilter').value;

        // 1. 달력 헤더 생성 (년월, 이동 버튼)
        let html = `
            <div class="calendar-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                <button onclick="CalendarManager.prevMonth()" style="cursor:pointer; border:none; background:none; font-size:1.2em;">&lt;</button>
                <span style="font-weight:bold; font-size:1.2em;">${this.currentYear}.${String(this.currentMonth).padStart(2, '0')}</span>
                <button onclick="CalendarManager.nextMonth()" style="cursor:pointer; border:none; background:none; font-size:1.2em;">&gt;</button>
            </div>
            <table class="calendar-table" style="width: 100%; border-collapse: collapse; text-align: center; table-layout: fixed;">
                <thead>
                    <tr style="background-color: #f4f4f4;">
                        <th style="color:red;">일</th>
                        <th>월</th>
                        <th>화</th>
                        <th>수</th>
                        <th>목</th>
                        <th>금</th>
                        <th style="color:blue;">토</th>
                    </tr>
                </thead>
                <tbody>
        `;

        // 2. 날짜 계산
        const firstDay = new Date(this.currentYear, this.currentMonth - 1, 1).getDay(); // 이번 달 1일의 요일 (0:일 ~ 6:토)
        const lastDate = new Date(this.currentYear, this.currentMonth, 0).getDate(); // 이번 달 마지막 날짜

        let date = 1;
        // 6주(최대) 루프
        for (let i = 0; i < 6; i++) {
            html += '<tr>';
            for (let j = 0; j < 7; j++) {
                if (i === 0 && j < firstDay) {
                    // 첫 주 빈칸
                    html += '<td></td>';
                } else if (date > lastDate) {
                    // 마지막 날짜 이후 빈칸
                    html += '<td></td>';
                } else {
                    // 날짜 셀 생성
                    const dateStr = `${this.currentYear}-${String(this.currentMonth).padStart(2, '0')}-${String(date).padStart(2, '0')}`;
                    const dailyTotal = this.calculateDailyTotal(dateStr, selectedCategory);
                    
                    // 금액 표시 (0원이면 표시 안 함)
                    const priceHtml = dailyTotal > 0
                        ? `<div style="font-size:0.8em; color:#333; margin-top:2px; word-break: break-all;">${dailyTotal.toLocaleString()}</div>` 
                        : '';

                    html += `
                        <td onclick="CalendarManager.openModal('${dateStr}')" style="padding: 8px 2px; vertical-align: top; height: 60px; border: 1px solid #eee; cursor: pointer;">
                            <div style="font-weight:bold;">${date}</div>
                            ${priceHtml}
                        </td>
                    `;
                    date++;
                }
            }
            html += '</tr>';
            if (date > lastDate) break;
        }

        html += `
                </tbody>
            </table>
        `;

        calendarBox.innerHTML = html;
    },

    // 특정 날짜, 특정 카테고리의 총 지출액 계산
    calculateDailyTotal: function(dateStr, category) {
        let total = 0;

        // 해당 날짜의 영수증 필터링
        const dailyReceipts = this.receiptData.filter(r => r.r_date === dateStr);

        dailyReceipts.forEach(receipt => {
            if (category === 'ALL') {
                // 전체 카테고리인 경우 영수증 총액 합산
                total += receipt.r_price;
            } else {
                // 특정 카테고리인 경우, 영수증 내 아이템(items)을 순회하며 합산
                if (receipt.items && receipt.items.length > 0) {
                    receipt.items.forEach(item => {
                        // item_category가 선택된 카테고리와 일치하는지 확인
                        // DB에 저장된 카테고리 값과 select value가 일치해야 함 (예: 'FOOD')
                        if (item.item_category === category) {
                            total += item.item_price;
                        }
                    });
                }
            }
        });

        return total;
    },

    // 모달 열기
    openModal: function(dateStr) {
        const modal = document.getElementById('dateDetailModal');
        const modalTitle = document.getElementById('modalDateTitle');
        const modalList = document.getElementById('modalList');
        const category = document.getElementById('categoryFilter').value;
        const categoryText = document.getElementById('categoryFilter').options[document.getElementById('categoryFilter').selectedIndex].text;

        if (!modal || !modalTitle || !modalList) return;

        // 제목 설정
        modalTitle.innerText = `${dateStr} (${categoryText})`;
        modalList.innerHTML = '';

        // 해당 날짜 데이터 필터링
        const dailyReceipts = this.receiptData.filter(r => r.r_date === dateStr);
        let hasData = false;

        if (dailyReceipts.length > 0) {
            dailyReceipts.forEach(receipt => {
                if (category === 'ALL') {
                    // 전체보기: 영수증 단위로 표시 (가게명 + 총액)
                    hasData = true;
                    const li = document.createElement('li');
                    li.className = 'modal-item';
                    li.innerHTML = `
                        <span class="modal-item-name">${receipt.r_place || '가게명 없음'}</span>
                        <span class="modal-item-price">${receipt.r_price.toLocaleString()}원</span>
                    `;
                    modalList.appendChild(li);
                } else {
                    // 카테고리별 보기: 해당 카테고리의 아이템 단위로 표시
                    if (receipt.items && receipt.items.length > 0) {
                        receipt.items.forEach(item => {
                            if (item.item_category === category) {
                                hasData = true;
                                const li = document.createElement('li');
                                li.className = 'modal-item';
                                li.innerHTML = `
                                    <div style="display:flex; flex-direction:column;">
                                        <span class="modal-item-name">${item.item_name}</span>
                                        <span style="font-size:0.8em; color:#888;">${receipt.r_place}</span>
                                    </div>
                                    <span class="modal-item-price">${item.item_price.toLocaleString()}원</span>
                                `;
                                modalList.appendChild(li);
                            }
                        });
                    }
                }
            });
        }

        if (!hasData) {
            modalList.innerHTML = '<li class="modal-empty">해당 내역이 없습니다.</li>';
        }

        // 모달 표시
        modal.style.display = 'flex';
    },

    // 모달 닫기
    closeModal: function() {
        const modal = document.getElementById('dateDetailModal');
        if (modal) {
            modal.style.display = 'none';
        }
    }
};

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', () => {
    CalendarManager.init();
});