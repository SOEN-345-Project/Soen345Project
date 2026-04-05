"""US-1.2: Sign in with valid credentials → land on /event or /adminEvent."""
import os

import pytest

from support.login_helpers import perform_sign_in


def test_login_redirects_to_dashboard(driver, base_url):
    email = os.environ.get("E2E_EMAIL_OR_PHONE", "").strip()
    password = os.environ.get("E2E_PASSWORD", "").strip()
    if not email or not password:
        pytest.skip("Set E2E_EMAIL_OR_PHONE and E2E_PASSWORD in e2e/.env")

    perform_sign_in(driver, base_url, email, password)

    url = driver.current_url
    assert "/event" in url or "/adminEvent" in url
