"""US-3.1 (sold out): API returns Tickets Sold Out → shown in the modal."""
import os
import time

import pytest
from selenium.webdriver.common.by import By

from epic_3.reservation_helpers import click_reserve_in_modal, open_reservation_modal, wait_for_sold_out_message
from support.waits import wait_till_custom_condition, wait_till_element_is_present

WAIT_SECONDS = 5


def test_reserve_sold_out_shows_message(logged_in_customer, base_url):
    title = os.environ.get("E2E_SOLD_OUT_EVENT_TITLE", "").strip()
    if not title:
        pytest.skip("Set E2E_SOLD_OUT_EVENT_TITLE in e2e/.env for a sold-out event.")

    driver = logged_in_customer
    driver.get(f"{base_url}/event")

    wait_till_element_is_present(driver, (By.XPATH, "//h1[contains(., 'Available Events')]"))
    wait_till_custom_condition(
        driver,
        lambda d: bool(
            d.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
            or d.find_elements(By.XPATH, "//p[contains(., 'No events found')]")
        ),
    )

    open_reservation_modal(driver, title)
    click_reserve_in_modal(driver)
    wait_for_sold_out_message(driver)

    time.sleep(WAIT_SECONDS)
    assert "Tickets Sold Out" in driver.page_source
