"""Skip tests when required env vars are missing (single place for pytest.skip messages)."""
from __future__ import annotations

import os

import pytest


def require_env(*keys: str) -> None:
    missing = [k for k in keys if not os.environ.get(k, "").strip()]
    if missing:
        pytest.skip("Set in e2e/.env: " + ", ".join(missing))
