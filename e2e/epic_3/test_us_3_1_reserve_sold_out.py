"""
US-3.1 (sold out): Reserve on a full event → “Tickets Sold Out” appears.

Algorithm:
  1. Require E2E_SOLD_OUT_EVENT_TITLE.
  2. Open /event; wait for grid.
  3. Open modal; Reserve; wait_for_sold_out_message.
  4. Assert page source contains “Tickets Sold Out”.

Asserts: user-visible sold-out copy is present after backend rejects the booking.
"""
import os

import pytest

from epic_3.reservation_helpers import click_reserve_in_modal, open_reservation_modal, wait_for_sold_out_message
from support.waits import CUSTOMER_EVENTS_H1, wait_till_customer_event_grid_ready, wait_till_element_is_present


def test_reserve_sold_out_shows_message(logged_in_customer, base_url):
    title = os.environ.get("E2E_SOLD_OUT_EVENT_TITLE", "").strip()
    if not title:
        pytest.skip("Set E2E_SOLD_OUT_EVENT_TITLE in e2e/.env for a sold-out event.")

    driver = logged_in_customer

    driver.get(f"{base_url}/event")
    wait_till_element_is_present(driver, CUSTOMER_EVENTS_H1)
    wait_till_customer_event_grid_ready(driver)

    open_reservation_modal(driver, title)
    click_reserve_in_modal(driver)
    wait_for_sold_out_message(driver)

    assert "Tickets Sold Out" in driver.page_source
