"""US-4.2: Edit title → verify → change everything back with Save."""
import os
import time

import pytest
from selenium.webdriver.common.by import By

from epic_4.admin_helpers import (
    click_modify_on_card,
    close_modal_done,
    fill_admin_event_form,
    find_admin_event_card,
    goto_admin_events,
    submit_save_changes,
)
from support.waits import wait_till_custom_condition, wait_till_element_is_present

WAIT_SECONDS = 10


def _need(*keys: str) -> None:
    missing = [k for k in keys if not os.environ.get(k, "").strip()]
    if missing:
        pytest.skip("Set in e2e/.env: " + ", ".join(missing))


def test_edit_event_then_revert(logged_in_admin, base_url):
    _need(
        "E2E_EDIT_EVENT_TITLE",
        "E2E_EDIT_TEMP_TITLE",
        "E2E_EDIT_EVENT_DATETIME",
        "E2E_EDIT_EVENT_CATEGORY",
        "E2E_EDIT_EVENT_LOCATION",
        "E2E_EDIT_EVENT_TICKETS",
    )
    target_sub = os.environ["E2E_EDIT_EVENT_TITLE"].strip()
    temp_title = os.environ["E2E_EDIT_TEMP_TITLE"].strip()
    description = os.environ.get("E2E_EDIT_EVENT_DESCRIPTION", "").strip()
    dt_local = os.environ["E2E_EDIT_EVENT_DATETIME"].strip()
    category = os.environ["E2E_EDIT_EVENT_CATEGORY"].strip()
    location = os.environ["E2E_EDIT_EVENT_LOCATION"].strip()
    tickets = int(os.environ["E2E_EDIT_EVENT_TICKETS"].strip())

    driver = logged_in_admin
    goto_admin_events(driver, base_url)
    card = find_admin_event_card(driver, target_sub)
    assert card is not None, f"No card matching {target_sub!r}"

    original_title = None
    for h2 in driver.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900"):
        if target_sub in h2.text:
            original_title = h2.text.strip()
            break
    assert original_title

    click_modify_on_card(driver, card)
    title_el = wait_till_element_is_present(driver, (By.CSS_SELECTOR, "input[placeholder='Event title']"))
    title_el.clear()
    title_el.send_keys(temp_title)
    submit_save_changes(driver)
    time.sleep(WAIT_SECONDS)
    close_modal_done(driver)

    wait_till_custom_condition(driver, lambda d: find_admin_event_card(d, temp_title) is not None)

    card2 = find_admin_event_card(driver, temp_title)
    click_modify_on_card(driver, card2)
    fill_admin_event_form(
        driver,
        title=original_title,
        description=description,
        datetime_local=dt_local,
        category=category,
        location=location,
        tickets=tickets,
    )
    submit_save_changes(driver)
    time.sleep(WAIT_SECONDS)
    close_modal_done(driver)

    wait_till_custom_condition(driver, lambda d: find_admin_event_card(d, original_title) is not None)
    assert find_admin_event_card(driver, temp_title) is None
    assert find_admin_event_card(driver, original_title) is not None
