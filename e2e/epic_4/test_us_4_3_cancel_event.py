"""US-4.3: Cancel event → not on customer /event → re-add same event."""
import os
import time

import pytest

from epic_4.admin_helpers import (
    click_cancel_event_on_card,
    click_logout,
    close_modal_done,
    customer_event_page_shows_title,
    fill_admin_event_form,
    find_admin_active_event_card,
    goto_admin_events,
    open_add_event_modal,
    submit_create_event,
    wait_card_contains_status,
)
from support.login_helpers import perform_sign_in
from support.waits import wait_till_custom_condition

WAIT_SECONDS = 10


def _need(*keys: str) -> None:
    missing = [k for k in keys if not os.environ.get(k, "").strip()]
    if missing:
        pytest.skip("Set in e2e/.env: " + ", ".join(missing))


def test_cancel_event_not_on_customer_list_then_recreate(logged_in_admin, base_url):
    _need(
        "E2E_CANCEL_EVENT_TITLE",
        "E2E_CANCEL_EVENT_DATETIME",
        "E2E_CANCEL_EVENT_CATEGORY",
        "E2E_CANCEL_EVENT_LOCATION",
        "E2E_CANCEL_EVENT_TICKETS",
    )
    title = os.environ["E2E_CANCEL_EVENT_TITLE"].strip()
    description = os.environ.get("E2E_CANCEL_EVENT_DESCRIPTION", "").strip()
    dt_local = os.environ["E2E_CANCEL_EVENT_DATETIME"].strip()
    category = os.environ["E2E_CANCEL_EVENT_CATEGORY"].strip()
    location = os.environ["E2E_CANCEL_EVENT_LOCATION"].strip()
    tickets = int(os.environ["E2E_CANCEL_EVENT_TICKETS"].strip())

    admin_email = os.environ["E2E_ADMIN_EMAIL_OR_PHONE"].strip()
    admin_pwd = os.environ["E2E_ADMIN_PASSWORD"].strip()
    cust_email = os.environ.get("E2E_EMAIL_OR_PHONE", "").strip()
    cust_pwd = os.environ.get("E2E_PASSWORD", "").strip()

    driver = logged_in_admin
    goto_admin_events(driver, base_url)
    card = find_admin_active_event_card(driver, title)
    assert card is not None, f"No ACTIVE event matching {title!r}"

    click_cancel_event_on_card(driver, card)
    time.sleep(WAIT_SECONDS)
    wait_card_contains_status(driver, title, "CANCELLED")

    if cust_email and cust_pwd:
        click_logout(driver)
        perform_sign_in(driver, base_url, cust_email, cust_pwd)
        if "/event" not in driver.current_url:
            driver.get(f"{base_url}/event")
        assert not customer_event_page_shows_title(driver, title)

        click_logout(driver)
        perform_sign_in(driver, base_url, admin_email, admin_pwd)
        assert "/adminEvent" in driver.current_url

    goto_admin_events(driver, base_url)
    open_add_event_modal(driver)
    fill_admin_event_form(
        driver,
        title=title,
        description=description,
        datetime_local=dt_local,
        category=category,
        location=location,
        tickets=tickets,
    )
    submit_create_event(driver)
    time.sleep(WAIT_SECONDS)
    close_modal_done(driver)

    wait_till_custom_condition(driver, lambda d: find_admin_active_event_card(d, title) is not None)
    assert find_admin_active_event_card(driver, title) is not None

    if cust_email and cust_pwd:
        click_logout(driver)
        perform_sign_in(driver, base_url, cust_email, cust_pwd)
        driver.get(f"{base_url}/event")
        assert customer_event_page_shows_title(driver, title)
