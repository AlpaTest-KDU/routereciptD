import logging
import subprocess

from training_state import (acquire_lock, mark_trained, release_lock,
                            should_retrain)

logger = logging.getLogger("retrain")
logging.basicConfig(level=logging.INFO)


def trigger_retrain():
    """
    재학습 트리거 엔트리포인트
    - 조건 미충족: skip
    - 이미 학습 중: locked
    - 학습 성공: trained
    """

    # 1️⃣ 재학습 조건 확인
    if not should_retrain():
        logger.info("[RETRAIN] skip (condition not met)")
        return {
            "status": "skip",
            "reason": "condition_not_met"
        }

    # 2️⃣ 동시 실행 방지
    if not acquire_lock():
        logger.warning("[RETRAIN] locked (already running)")
        return {
            "status": "locked",
            "reason": "already_running"
        }

    try:
        logger.info("[RETRAIN] start training")

        # 3️⃣ 실제 학습 실행
        subprocess.run(
            ["python", "retrain_model.py"],
            check=True
        )

        # 4️⃣ 학습 성공 처리
        mark_trained()
        logger.info("[RETRAIN] training completed")

        return {
            "status": "trained"
        }

    except subprocess.CalledProcessError as e:
        logger.error("[RETRAIN] training failed", exc_info=e)
        return {
            "status": "failed",
            "error": str(e)
        }

    finally:
        # 5️⃣ 반드시 락 해제
        release_lock()
