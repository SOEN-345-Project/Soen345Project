"""
US-3.2: Cancel an existing reservation; empty list state if that was the only booking.

Algorithm:
  1. Require E2E_EPIC3_EVENT_TITLE matching an active reservation.
  2. Open /event; go to reservations list.
  3. Scan rows: find title with an active “Cancel reservation” button — assert found (else fail: book first).
  4. cancel_active_reservation_for_title; wait_till_no_reservations_empty_state.

Asserts: active reservation existed before cancel; after cancel the UI reaches the empty-state message used by the helper.
"""
import os

import pytest
from selenium.common.exceptions import StaleElementReferenceException
from selenium.webdriver.common.by import By

from epic_3.reservation_helpers import (
    cancel_active_reservation_for_title,
    go_to_reservations_list,
    wait_till_no_reservations_empty_state,
)
from support.waits import CUSTOMER_EVENTS_H1, wait_till_element_is_present


def test_cancel_existing_reservation(logged_in_customer, base_url):
    title = os.environ.get("E2E_EPIC3_EVENT_TITLE", "").strip()
    if not title:
        pytest.skip("Set E2E_EPIC3_EVENT_TITLE in e2e/.env (existing active reservation).")

    driver = logged_in_customer

    driver.get(f"{base_url}/event")
    wait_till_element_is_present(driver, CUSTOMER_EVENTS_H1)
    go_to_reservations_list(driver)

    found_active = False
    for s in driver.find_elements(By.XPATH, "//strong"):
        try:
            if title not in s.text:
                continue
            block = s.find_element(By.XPATH, "./ancestor::div[contains(@style,'1px solid')][1]")
            if block.find_elements(By.XPATH, ".//button[contains(., 'Cancel reservation')]"):
                found_active = True
                break
        except StaleElementReferenceException:
            continue
    assert found_active, f"No active reservation for {title!r}; book one first."

    cancel_active_reservation_for_title(driver, title)
    wait_till_no_reservations_empty_state(driver)
