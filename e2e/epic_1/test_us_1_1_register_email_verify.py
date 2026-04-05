"""US-1.1: Register with email → enter verification code from E2E backend → land on /signin."""
from __future__ import annotations

import os
import uuid

import pytest
from selenium.webdriver.common.by import By

from support.verification_code import env_api_base_url, env_verification_secret, fetch_email_verification_code
from support.waits import wait_till_element_is_visible, wait_till_url_contains


def test_register_with_email_and_verification_code(driver, base_url):
    secret = env_verification_secret()
    if not secret:
        pytest.skip("Set E2E_VERIFICATION_SECRET in e2e/.env (must match backend e2e.verification-secret).")

    api_base = env_api_base_url()
    suffix = uuid.uuid4().hex[:10]
    configured_email = os.environ.get("E2E_SIGNUP_EMAIL", "").strip()
    if configured_email and "@" in configured_email:
        local, _, domain = configured_email.partition("@")
        email = f"{local}+e2e{suffix}@{domain}"
    elif configured_email:
        email = configured_email
    else:
        email = f"e2e_signup_{suffix}@example.com"
    password = os.environ.get("E2E_SIGNUP_PASSWORD", "E2E_Signup_Pass_1!").strip()
    first = os.environ.get("E2E_SIGNUP_FIRST_NAME", "E2E").strip() or "E2E"
    last = os.environ.get("E2E_SIGNUP_LAST_NAME", "Signup").strip() or "Signup"

    driver.get(f"{base_url}/signup")

    wait_till_element_is_visible(driver, (By.CSS_SELECTOR, 'input[placeholder="Enter your first name"]')).send_keys(first)
    driver.find_element(By.CSS_SELECTOR, 'input[placeholder="Enter your last name"]').send_keys(last)
    driver.find_element(By.CSS_SELECTOR, 'input[placeholder="Enter your email"]').send_keys(email)
    driver.find_element(By.CSS_SELECTOR, 'input[placeholder="Enter your password"]').send_keys(password)
    driver.find_element(By.CSS_SELECTOR, 'button[type="submit"]').click()

    wait_till_element_is_visible(driver, (By.CSS_SELECTOR, 'input[placeholder="Enter confirmation code"]'))

    code = fetch_email_verification_code(api_base_url=api_base, email=email, secret=secret)
    driver.find_element(By.CSS_SELECTOR, 'input[placeholder="Enter confirmation code"]').clear()
    driver.find_element(By.CSS_SELECTOR, 'input[placeholder="Enter confirmation code"]').send_keys(code)
    driver.find_element(By.XPATH, "//button[contains(., 'Confirm') and not(contains(., 'Confirming'))]").click()

    wait_till_url_contains(driver, "/signin")
