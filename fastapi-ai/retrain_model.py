# =========================
# 🔥 UTF-8 고정
# =========================
import os

os.environ["PYTHONUTF8"] = "1"
os.environ["PYTHONIOENCODING"] = "utf-8"

import logging

import joblib
import numpy as np
import pymysql
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from tensorflow import keras
from tensorflow.keras import layers

# =========================
# 1. 로그 설정
# =========================
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("retrain-model")

# =========================
# 2. DB 설정
# =========================
DB_CONFIG = {
    "host": os.getenv("DB_HOST", "mariadb"),
    "user": os.getenv("DB_USER", "reciptback"),
    "password": os.getenv("DB_PASSWORD"),
    "database": "routereciptdb",
    "port": 3306,
    "charset": "utf8mb4"
}

MODEL_PATH = "model/receipt_category_model.keras"
ENCODER_PATH = "model/label_encoder.joblib"

MIN_TRAIN_SIZE = 20
EPOCHS = 10
BATCH_SIZE = 16

# =========================
# 3. 데이터 로드
# =========================
def load_training_data():
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    sql = """
        SELECT item_name, final_category
        FROM ai_training_item
        WHERE final_category IS NOT NULL
    """
    cursor.execute(sql)
    rows = cursor.fetchall()

    cursor.close()
    conn.close()

    texts = [r[0] for r in rows]
    labels = [r[1] for r in rows]

    return texts, labels

# =========================
# 4. 모델 생성
# =========================
def build_model(num_classes, vectorizer):
    model = keras.Sequential([
        vectorizer,
        layers.Embedding(input_dim=20000, output_dim=128),
        layers.GlobalAveragePooling1D(),
        layers.Dense(128, activation="relu"),
        layers.Dropout(0.3),
        layers.Dense(num_classes, activation="softmax")
    ])

    model.compile(
        optimizer="adam",
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"]
    )
    return model

# =========================
# 5. 재학습 메인
# =========================
def main():
    logger.info("🚀 Retrain started")

    texts, labels = load_training_data()

    if len(texts) < MIN_TRAIN_SIZE:
        raise RuntimeError(
            f"훈련 데이터 부족 ({len(texts)} < {MIN_TRAIN_SIZE})"
        )

    # Label Encoding
    label_encoder = LabelEncoder()
    y = label_encoder.fit_transform(labels)

    joblib.dump(label_encoder, ENCODER_PATH)
    logger.info("Label encoder saved")

    # Text Vectorization
    vectorizer = layers.TextVectorization(
        max_tokens=20000,
        output_mode="int",
        output_sequence_length=10
    )
    vectorizer.adapt(texts)

    X_train, X_val, y_train, y_val = train_test_split(
        texts,
        y,
        test_size=0.2,
        random_state=42
    )

    model = build_model(
        num_classes=len(label_encoder.classes_),
        vectorizer=vectorizer
    )

    model.fit(
        np.array(X_train),
        np.array(y_train),
        validation_data=(np.array(X_val), np.array(y_val)),
        epochs=EPOCHS,
        batch_size=BATCH_SIZE
    )

    # =========================
    # 6. 모델 저장 (Keras 3)
    # =========================
    model.save(MODEL_PATH)
    logger.info("✅ Model saved successfully")

# =========================
# 7. 엔트리포인트
# =========================
if __name__ == "__main__":
    main()
