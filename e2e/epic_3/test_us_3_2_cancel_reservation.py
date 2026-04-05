"""US-3.2: Cancel an existing reservation (must already exist for E2E_EPIC3_EVENT_TITLE)."""
import os

import pytest
from selenium.webdriver.common.by import By

from epic_3.reservation_helpers import (
    cancel_active_reservation_for_title,
    go_to_reservations_list,
    wait_reservation_row_cancelled,
)
from support.waits import wait_till_element_is_present


def test_cancel_existing_reservation(logged_in_customer, base_url):
    title = os.environ.get("E2E_EPIC3_EVENT_TITLE", "").strip()
    if not title:
        pytest.skip("Set E2E_EPIC3_EVENT_TITLE in e2e/.env (existing active reservation).")

    driver = logged_in_customer
    driver.get(f"{base_url}/event")
    wait_till_element_is_present(driver, (By.XPATH, "//h1[contains(., 'Available Events')]"))

    go_to_reservations_list(driver)

    active_block = None
    for s in driver.find_elements(By.XPATH, "//strong"):
        if title not in s.text:
            continue
        block = s.find_element(By.XPATH, "./ancestor::div[contains(@style,'1px solid')][1]")
        if block.find_elements(By.XPATH, ".//button[contains(., 'Cancel reservation')]"):
            active_block = block
            break
    assert active_block is not None, f"No active reservation for {title!r}; book one first."
    assert title in active_block.text

    cancel_active_reservation_for_title(driver, title)
    wait_reservation_row_cancelled(driver, title)
