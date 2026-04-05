"""US-2.1: Event list shows name, date, location, category; skip cancelled in status line."""
import time

import pytest
from selenium.webdriver.common.by import By

from support.waits import wait_till_custom_condition, wait_till_element_is_present

WAIT_SECONDS = 10


def test_event_cards_show_name_date_location_category(logged_in_customer, base_url):
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
    time.sleep(WAIT_SECONDS)

    titles = driver.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
    if not titles:
        pytest.skip("No event cards; seed events or adjust filters.")

    for h2 in titles:
        card = h2.find_element(By.XPATH, "./ancestor::div[contains(@class,'rounded-2xl')][1]")
        assert h2.text.strip(), "Event name should be visible"

        date_bits = card.find_elements(By.CSS_SELECTOR, "span.text-xl.font-bold.text-stone-900")
        assert date_bits, "Date badge should be visible"

        loc_ps = card.find_elements(By.CSS_SELECTOR, "p.text-stone-400")
        assert any("tickets" in p.text.lower() or "," in p.text for p in loc_ps), "Location line expected"

        badges = card.find_elements(By.XPATH, ".//span[contains(@class,'rounded-full')]")
        assert any(len(s.text.strip()) > 1 for s in badges), "Category pill should be visible"

        assert "CANCELLED" not in card.text.upper(), (
            "Cancelled events should not appear in the customer event list"
        )
