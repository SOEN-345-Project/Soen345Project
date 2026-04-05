"""US-3.1 / US-3.3: reserve → in-modal confirmation (incl. quantity) → /reservation row matches booking."""
import os
import re

import pytest
from selenium.webdriver.common.by import By

from epic_3.reservation_helpers import (
    click_modal_done,
    click_reserve_in_modal,
    find_reservation_row_block,
    go_to_reservations_list,
    location_fragments_from_modal_line,
    open_reservation_modal,
    parse_ticket_quantity_from_success_modal,
    read_modal_date_and_location_lines,
    wait_for_reservation_success,
)
from support.waits import wait_till_custom_condition, wait_till_element_is_present


def test_reserve_shows_confirmation(logged_in_customer, base_url):
    title = os.environ.get("E2E_EPIC3_EVENT_TITLE", "").strip()
    if not title:
        pytest.skip("Set E2E_EPIC3_EVENT_TITLE in e2e/.env")

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
    date_line, location_line = read_modal_date_and_location_lines(driver)

    click_reserve_in_modal(driver)
    wait_for_reservation_success(driver)

    confirmed = driver.find_element(By.XPATH, "//p[contains(., 'Reservation confirmed!')]")
    assert confirmed.is_displayed()

    qty = parse_ticket_quantity_from_success_modal(driver)
    success_qty_line = driver.find_element(By.XPATH, "//p[contains(., 'successfully reserved')]")
    assert success_qty_line.is_displayed()
    assert str(qty) in success_qty_line.text
    qty_phrase = "1 ticket" if qty == 1 else f"{qty} tickets"
    assert qty_phrase in success_qty_line.text

    click_modal_done(driver)
    go_to_reservations_list(driver)

    row = find_reservation_row_block(driver, title)
    assert title in row.text
    assert qty_phrase in row.text

    for frag in location_fragments_from_modal_line(location_line):
        assert frag in row.text, f"Expected location fragment {frag!r} in row: {row.text!r}"

    for year in re.findall(r"\b(20\d{2})\b", date_line):
        assert year in row.text, f"Expected year {year} from modal date in row: {row.text!r}"
