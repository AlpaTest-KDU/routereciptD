import time
import redis

r = redis.Redis(host="redis", port=6379, decode_responses=True)

LAST_TRAINED_KEY = "ai:last_trained_at"
COUNT_KEY = "ai:training_count"
LOCK_KEY = "ai:training_lock"


def increase_count():
    r.incr(COUNT_KEY)


def get_count() -> int:
    return int(r.get(COUNT_KEY) or 0)


def should_retrain() -> bool:
    last = r.get(LAST_TRAINED_KEY)
    now = int(time.time())

    if last is None:
        return True

    if now - int(last) >= 3 * 60 * 60:
        return True

    if get_count() >= 100:
        return True

    return False


def acquire_lock() -> bool:
    return r.set(LOCK_KEY, 1, nx=True, ex=3600)


def release_lock():
    r.delete(LOCK_KEY)


def mark_trained():
    r.set(LAST_TRAINED_KEY, int(time.time()))
    r.set(COUNT_KEY, 0)
