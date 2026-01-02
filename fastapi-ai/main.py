print("🔥🔥🔥 FASTAPI APP LOADED 🔥🔥🔥")
# =========================
# 🔥 UTF-8 고정 (반드시 최상단)
# =========================
import os

os.environ["PYTHONUTF8"] = "1"
os.environ["PYTHONIOENCODING"] = "utf-8"

import numpy as np
import tensorflow as tf
from fastapi import FastAPI
from keras.models import load_model
from pydantic import BaseModel

# =========================
# 1. 모델 로드
# =========================
model = load_model("model/receipt_category_model.keras")

CATEGORY_ORDER = [
    "FOOD",
    "CLOTHES",
    "MEDICAL",
    "TRAFFIC",
    "CULTURE",
    "HOME",
    "LIVING",
    "ETC"
]

AI_THRESHOLD = 0.6   # 🔥 현재 데이터 기준

# =========================
# 2. RULE 정의 (❗ 변경 없음)
# =========================
STORE_RULES = {
    "FOOD": [
        "스타벅스", "투썸", "이디야",
        "맥도날드", "버거킹", "롯데리아",
        "KFC", "서브웨이", "김밥천국", "탕화쿵푸", "파이브가이즈", "하이디라오",
        "장호덕손만두", "춘리마라탕", "니뽕내뽕",
        "성심당", "빽다방", "메가커피", "다방", "킹콩부대찌개", "엽기떡볶이", 
        "해찬들", "풀무원", "농심", "삼양", "델몬트", "비비고", "해태", "청정원", 
        "오뚜기", "맥심", "동원", "크라운", "롯데제과", "삼립", "크리스피크림도넛",
        "켈로그", "버거", "포스트", "누데이크", "쿠우쿠우"
    ],
    "CULTURE": [
        "CGV", "메가박스", "롯데시네마",
        "교보문고", "알라딘", "예스24", "인터파크", "공방", "책방",
    ],
    "LIVING": [
        "다이소", "이마트", "홈플러스", "롯데마트",
        "CU", "GS25", "세븐일레븐", "트레이더스", "스타필드",
        "스탠리", "올리브영", "랄라블라", "다비치안경", "피존", "다이슨",
        "크리넥스", "딥씨크", "다우니", "락앤락", "샤넬", "디올", "루이비통",
        "젠틀몬스터", "돌체앤가바나"
    ],
    "HOME": [
        "이케아", "한샘", "일룸",
    ],
    "TRAFFIC": [
        "카카오택시", "우버", "항공", "코레일", "고속"
    ],
    "MEDICAL": [
        "약국", "종근당"
    ]
}

ITEM_RULES = {
    "FOOD": [
        "아메리카노", "라떼", "커피",
        "햄버거", "피자", "치킨",
        "김밥", "국밥", "분식",
        "빵", "베이커리", "디저트", "우유", "훠궈",
        "마라탕", "두바이쫀득쿠키", "마라샹궈", "곱창",
        "곱창전골", "마들렌", "베이글", "소금빵", "바케트", "약과",
        "콜라", "사이다", "탄산", "음료"
    ],
    "CLOTHES": [
        "의류", "셔츠", "바지", "청바지",
        "자켓", "코트", "패딩", "나시", "남방",
        "신발", "운동화", "구두",
        "양말", "모자", "장화"
    ],
    "MEDICAL": [
        "병원", "의원", "진료",
        "처방", "약", "의약품",
        "치과", "한의원"
    ],
    "TRAFFIC": [
        "택시", "버스", "지하철",
        "주차", "주차장",
        "주유", "하이패스"
    ],
    "CULTURE": [
        "영화", "공연", "전시",
        "뮤지컬", "콘서트",
        "도서", "책"
    ],
    "HOME": [
        "침대", "소파", "가구",
        "책상", "의자", "매트리스"
    ],
    "LIVING": [
        "마트", "편의점",
        "생필품", "휴지", "물티슈",
        "세제", "샴푸", "치약", "전기밥솥"
    ]
}

# =========================
# 3. 유틸 함수 (🔥 여기만 수정)
# =========================
def normalize(text: str) -> str:
    return text.replace(" ", "").lower()

def rule_based_classify(text: str) -> str | None:
    text = normalize(text)

    for category, keywords in STORE_RULES.items():
        for k in keywords:
            if k.lower() in text:
                return category

    for category, keywords in ITEM_RULES.items():
        for k in keywords:
            if k.lower() in text:
                return category

    return None

def ai_classify(text: str) -> tuple[str, float]:
    preds = model(tf.constant([text], dtype=tf.string)).numpy()
    idx = int(np.argmax(preds, axis=1)[0])
    conf = float(preds[0][idx])
    return CATEGORY_ORDER[idx], conf

# =========================
# 4. FastAPI
# =========================
app = FastAPI(title="Receipt Category AI")

class PredictRequest(BaseModel):
    text: str

class PredictResponse(BaseModel):
    category: str
    confidence: float
    source: str   # RULE / AI / FALLBACK

# =========================
# 5. 헬스 체크
# =========================
@app.get("/health")
def health():
    return {"status": "ok"}

# =========================
# 6. 예측
# =========================
@app.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest):
    print("🔥 FASTAPI RECEIVED REQUEST")
    print("🔥 FASTAPI RECEIVED TEXT =", req.text)

    rule_category = rule_based_classify(req.text)
    if rule_category:
        return {
            "category": rule_category,
            "confidence": 1.0,
            "source": "RULE"
        }

    ai_category, conf = ai_classify(req.text)
    if conf >= AI_THRESHOLD:
        return {
            "category": ai_category,
            "confidence": round(conf, 4),
            "source": "AI"
        }

    return {
        "category": "ETC",
        "confidence": round(conf, 4),
        "source": "FALLBACK"
    }
