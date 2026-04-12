"""
US-2.2: Search by keyword → every visible card title contains that keyword.

Algorithm:
  1. Require E2E_SEARCH_KEYWORD (must match seeded data).
  2. Open /event; wait for heading and search box; type keyword.
  3. wait_till_input_value_equals — input value bound (React state).
  4. Click Search; wait_till_customer_event_grid_ready — results settled.
  5. For each card title: assert keyword appears in title text (case-insensitive).

Asserts: every h2 title includes the keyword; skip if no cards after search.
"""
import os
import time
import pytest
from selenium.webdriver.common.by import By

from support.waits import (
    CUSTOMER_EVENTS_H1,
    wait_till_customer_event_grid_ready,
    wait_till_element_is_clickable,
    wait_till_element_is_present,
    wait_till_input_value_equals,
)

SEARCH_BOX = (By.CSS_SELECTOR, "input[placeholder='Search by keyword...']")
SEARCH_BTN = (By.XPATH, "//button[normalize-space()='Search']")


def test_search_shows_only_matching_events(logged_in_customer, base_url):
    keyword = os.environ.get("E2E_SEARCH_KEYWORD", "").strip()
    if not keyword:
        pytest.skip("Set E2E_SEARCH_KEYWORD in e2e/.env")

    driver = logged_in_customer

    driver.get(f"{base_url}/event")
    wait_till_element_is_present(driver, CUSTOMER_EVENTS_H1)
    search_input = wait_till_element_is_present(driver, SEARCH_BOX)
    search_input.clear()
    search_input.send_keys(keyword)

    wait_till_input_value_equals(driver, SEARCH_BOX, keyword)

    wait_till_element_is_clickable(driver, SEARCH_BTN).click()

    wait_till_customer_event_grid_ready(driver)

    titles = driver.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
    if not titles:
        pytest.skip("No events after search; change E2E_SEARCH_KEYWORD or seed data.")

    kw = keyword.lower()
    for h2 in titles:
        assert kw in h2.text.lower(), f"Title should match keyword: {h2.text!r}"

    time.sleep(2)
