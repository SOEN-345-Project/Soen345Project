"""Fetch pending email verification code from the gated backend E2E endpoint (stdlib only)."""
from __future__ import annotations

import json
import os
import urllib.error
import urllib.parse
import urllib.request


def fetch_email_verification_code(*, api_base_url: str, email: str, secret: str) -> str:
    """
    Call GET /auth/e2e/verification-code?email=... with X-E2E-Secret.

    Requires backend with e2e.expose-verification-code=true and matching e2e.verification-secret.
    """
    base = api_base_url.rstrip("/")
    q = urllib.parse.urlencode({"email": email})
    url = f"{base}/auth/e2e/verification-code?{q}"
    req = urllib.request.Request(url, headers={"X-E2E-Secret": secret})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            body = resp.read().decode()
    except urllib.error.HTTPError as e:
        detail = e.read().decode(errors="replace")
        raise RuntimeError(f"E2E verification-code HTTP {e.code}: {detail}") from e

    data = json.loads(body)
    code = data.get("verificationCode")
    if not code:
        raise RuntimeError(f"E2E verification-code response missing verificationCode: {body!r}")
    return str(code)


def env_api_base_url() -> str:
    return os.environ.get("E2E_API_BASE_URL", "http://localhost:8000").strip()


def env_verification_secret() -> str:
    return os.environ.get("E2E_VERIFICATION_SECRET", "").strip()
