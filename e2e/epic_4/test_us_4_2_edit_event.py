"""
US-4.2: Rename to a temp title, verify, then revert all fields to the original title.

Algorithm:
  1. require_env for target substring, temp title, and full revert field set.
  2. Find card by target substring; read full h2 text as original_title.
  3. Modify: change title to temp; save; close; wait_till_admin_card_with_title_visible(temp).
  4. Re-open; fill_admin_event_form with original_title and env metadata; save; close.
  5. wait_till_admin_card_with_title_visible(original); assert no card for temp; assert card for original.

Asserts: intermediate rename persisted; revert restores list to original title only.
"""
import os
import time

from selenium.webdriver.common.by import By

from epic_4.admin_helpers import (
    click_modify_on_card,
    close_modal_done,
    fill_admin_event_form,
    find_admin_event_card,
    goto_admin_events,
    scroll_admin_page_to_bottom_once,
    submit_save_changes,
    wait_admin_event_grid_ready,
    wait_till_admin_card_with_title_visible,
)
from support.env_checks import require_env
from support.waits import wait_till_element_is_present


def test_edit_event_then_revert(logged_in_admin, base_url):
    require_env(
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

    # scroll down so all cards are visible before starting edits
    scroll_admin_page_to_bottom_once(driver, wait_for_grid=False)

    card = find_admin_event_card(driver, target_sub)
    assert card is not None, f"No card matching {target_sub!r}"
    original_title = None
    for h2 in driver.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900"):
        if target_sub in h2.text:
            original_title = h2.text.strip()
            break
    assert original_title

    # first edit: rename to temp title
    click_modify_on_card(driver, card)
    title_el = wait_till_element_is_present(driver, (By.CSS_SELECTOR, "input[placeholder='Event title']"))
    title_el.clear()
    title_el.send_keys(temp_title)
    submit_save_changes(driver)
    close_modal_done(driver)

    # wait for grid then scroll down before second edit
    wait_till_admin_card_with_title_visible(driver, temp_title)
    wait_admin_event_grid_ready(driver)
    scroll_admin_page_to_bottom_once(driver, wait_for_grid=False)

    card2 = find_admin_event_card(driver, temp_title)

    # second edit: revert to original title
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
    close_modal_done(driver)

    # wait for grid then scroll down before final assertions
    wait_till_admin_card_with_title_visible(driver, original_title)
    wait_admin_event_grid_ready(driver)
    scroll_admin_page_to_bottom_once(driver, wait_for_grid=False)
    time.sleep(2)
    assert find_admin_event_card(driver, temp_title) is None
    assert find_admin_event_card(driver, original_title) is not None
