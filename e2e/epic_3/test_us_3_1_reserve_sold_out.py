"""
US-3.1 (sold out): Reserve on a full event → 0 tickets shown, error message appears.

Algorithm:
  1. Require E2E_SOLD_OUT_EVENT_TITLE.
  2. Open /event; wait for grid.
  3. Open modal; assert "0 tickets" is displayed.
  4. Click reserve; assert "Something went wrong. Please try again." error appears.
  5. Sleep 2 seconds.

Asserts: modal shows 0 available tickets; error message is displayed after attempting to reserve.
"""
import os
import time

import pytest
from selenium.webdriver.common.by import By

from epic_3.reservation_helpers import click_reserve_in_modal, open_reservation_modal
from support.waits import (
    CUSTOMER_EVENTS_H1,
    wait_till_customer_event_grid_ready,
    wait_till_element_is_present,
    wait_till_element_is_visible,
)


def test_reserve_sold_out_shows_message(logged_in_customer, base_url):
    title = os.environ.get("E2E_SOLD_OUT_EVENT_TITLE", "").strip()
    if not title:
        pytest.skip("Set E2E_SOLD_OUT_EVENT_TITLE in e2e/.env for a sold-out event.")

    driver = logged_in_customer

    driver.get(f"{base_url}/event")
    wait_till_element_is_present(driver, CUSTOMER_EVENTS_H1)
    wait_till_customer_event_grid_ready(driver)

    open_reservation_modal(driver, title)

    available_el = wait_till_element_is_visible(driver, (By.XPATH, "//*[contains(., '0 tickets')]"))
    assert available_el is not None, "Expected modal to show '0 tickets' for a sold-out event"

    click_reserve_in_modal(driver)

    error_el = wait_till_element_is_visible(
        driver, (By.XPATH, "//*[contains(., 'Something went wrong. Please try again.')]")
    )
    assert error_el is not None, "Expected 'Something went wrong. Please try again.' to appear after reserving a sold-out event"

    time.sleep(2)
