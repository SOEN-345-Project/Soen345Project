"""Pytest fixtures: Chrome, base URL, customer / admin sessions."""
from __future__ import annotations

import os

import config  # noqa: F401 — loads e2e/.env
import pytest

from support.browser import create_chrome_driver
from support.login_helpers import perform_sign_in


@pytest.fixture(scope="function")
def driver():
    drv = create_chrome_driver()
    drv.implicitly_wait(5)
    try:
        yield drv
    finally:
        drv.quit()


@pytest.fixture
def base_url() -> str:
    return os.environ.get("E2E_BASE_URL", "http://localhost:3000").rstrip("/")


@pytest.fixture
def logged_in_customer(driver, base_url):
    email = os.environ.get("E2E_EMAIL_OR_PHONE", "").strip()
    password = os.environ.get("E2E_PASSWORD", "").strip()
    if not email or not password:
        pytest.skip("Set E2E_EMAIL_OR_PHONE and E2E_PASSWORD in e2e/.env")
    perform_sign_in(driver, base_url, email, password)
    yield driver


@pytest.fixture
def logged_in_admin(driver, base_url):
    email = os.environ.get("E2E_ADMIN_EMAIL_OR_PHONE", "").strip()
    password = os.environ.get("E2E_ADMIN_PASSWORD", "").strip()
    if not email or not password:
        pytest.skip("Set E2E_ADMIN_EMAIL_OR_PHONE and E2E_ADMIN_PASSWORD in e2e/.env")
    perform_sign_in(driver, base_url, email, password)
    if "/adminEvent" not in driver.current_url:
        pytest.skip("Use administrator credentials; sign-in did not open /adminEvent.")
    yield driver
