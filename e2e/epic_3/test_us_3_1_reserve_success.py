"""
US-3.1 / US-3.3: Reserve → Done → /reservation row matches modal date, location, and quantity.
"""
import os
import re

import pytest

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
from support.waits import CUSTOMER_EVENTS_H1, wait_till_customer_event_grid_ready, wait_till_element_is_present


def test_reserve_shows_confirmation(logged_in_customer, base_url):
    title = os.environ.get("E2E_EPIC3_EVENT_TITLE", "").strip()
    if not title:
        pytest.skip("Set E2E_EPIC3_EVENT_TITLE in e2e/.env")

    driver = logged_in_customer
    driver.get(f"{base_url}/event")
    wait_till_element_is_present(driver, CUSTOMER_EVENTS_H1)
    wait_till_customer_event_grid_ready(driver)

    open_reservation_modal(driver, title)
    date_line, location_line = read_modal_date_and_location_lines(driver)

    click_reserve_in_modal(driver)
    wait_for_reservation_success(driver)
    qty = parse_ticket_quantity_from_success_modal(driver)
    qty_phrase = "1 ticket" if qty == 1 else f"{qty} tickets"

    click_modal_done(driver)
    go_to_reservations_list(driver)
    row = find_reservation_row_block(driver, title)

    assert title in row.text
    assert qty_phrase in row.text
    for frag in location_fragments_from_modal_line(location_line):
        assert frag in row.text, f"Expected location fragment {frag!r} in row: {row.text!r}"
    for year in re.findall(r"\b(20\d{2})\b", date_line):
        assert year in row.text, f"Expected year {year} from modal date in row: {row.text!r}"
