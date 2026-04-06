"""
US-2.1: Event cards show name, date, location, category; cancelled events must not appear.

Algorithm:
  1. Open /event.
  2. Wait for “Available Events” heading and for the grid (cards or empty state).
  3. For each card (skip if none): walk the card DOM.
  4. Assert: non-empty title; date badge present; a location-style line; category pill; card text must not contain CANCELLED.

Asserts: structure and content of each card; last assert guards that cancelled events are filtered out of the customer list.
"""
import pytest
from selenium.webdriver.common.by import By

from support.waits import (
    CUSTOMER_EVENTS_H1,
    wait_till_customer_event_grid_ready,
    wait_till_element_is_present,
)


def test_event_cards_show_name_date_location_category(logged_in_customer, base_url):
    driver = logged_in_customer

    driver.get(f"{base_url}/event")

    wait_till_element_is_present(driver, CUSTOMER_EVENTS_H1)

    wait_till_customer_event_grid_ready(driver)

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
