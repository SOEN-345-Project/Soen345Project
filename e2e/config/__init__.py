"""Loads ``e2e/.env`` when imported."""
from __future__ import annotations

from pathlib import Path

from dotenv import load_dotenv

E2E_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(E2E_ROOT / ".env")

__all__ = ["E2E_ROOT"]
