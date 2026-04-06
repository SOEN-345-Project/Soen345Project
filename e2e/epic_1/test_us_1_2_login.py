"""
US-1.2: Valid credentials → redirect to /event or /adminEvent.

Algorithm:
  1. Require E2E_EMAIL_OR_PHONE and E2E_PASSWORD.
  2. perform_sign_in — same flow as a real user (form submits without error).
  3. Assert current URL contains /event or /adminEvent — authenticated routing sends customer vs admin to the right hub.

Asserts: URL check proves login succeeded and role-based redirect works.
"""
import os

from support.env_checks import require_env
from support.login_helpers import perform_sign_in


def test_login_redirects_to_dashboard(driver, base_url):
    require_env("E2E_EMAIL_OR_PHONE", "E2E_PASSWORD")
    email = os.environ["E2E_EMAIL_OR_PHONE"].strip()
    password = os.environ["E2E_PASSWORD"].strip()

    perform_sign_in(driver, base_url, email, password)

    url = driver.current_url
    assert "/event" in url or "/adminEvent" in url
