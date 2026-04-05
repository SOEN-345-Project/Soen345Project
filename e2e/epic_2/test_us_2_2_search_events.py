"""US-2.2: Type keyword, click Search → every card title contains the keyword."""
import os
import time

import pytest
from selenium.webdriver.common.by import By

from support.waits import (
    wait_till_custom_condition,
    wait_till_element_is_clickable,
    wait_till_element_is_present,
)

WAIT_SECONDS = 10
SEARCH_BOX = (By.CSS_SELECTOR, "input[placeholder='Search by keyword...']")
SEARCH_BTN = (By.XPATH, "//button[normalize-space()='Search']")


def test_search_shows_only_matching_events(logged_in_customer, base_url):
    keyword = os.environ.get("E2E_SEARCH_KEYWORD", "").strip()
    if not keyword:
        pytest.skip("Set E2E_SEARCH_KEYWORD in e2e/.env")

    driver = logged_in_customer
    driver.get(f"{base_url}/event")

    wait_till_element_is_present(driver, (By.XPATH, "//h1[contains(., 'Available Events')]"))

    search_input = wait_till_element_is_present(driver, SEARCH_BOX)
    search_input.clear()
    search_input.send_keys(keyword)

    wait_till_custom_condition(
        driver,
        lambda d: keyword in (d.find_element(*SEARCH_BOX).get_property("value") or ""),
    )

    wait_till_element_is_clickable(driver, SEARCH_BTN).click()

    wait_till_custom_condition(
        driver,
        lambda d: bool(
            d.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
            or d.find_elements(By.XPATH, "//p[contains(., 'No events found')]")
        ),
    )
    time.sleep(WAIT_SECONDS)

    titles = driver.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
    if not titles:
        pytest.skip("No events after search; change E2E_SEARCH_KEYWORD or seed data.")

    kw = keyword.lower()
    for h2 in titles:
        assert kw in h2.text.lower(), f"Title should match keyword: {h2.text!r}"
