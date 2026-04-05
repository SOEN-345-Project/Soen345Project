"""US-4.1: Admin creates an event → it appears on the list."""
import os
import time

import pytest

from epic_4.admin_helpers import (
    close_modal_done,
    fill_admin_event_form,
    find_admin_event_card,
    goto_admin_events,
    open_add_event_modal,
    submit_create_event,
)
from support.waits import wait_till_custom_condition

WAIT_SECONDS = 10


def _need(*keys: str) -> None:
    missing = [k for k in keys if not os.environ.get(k, "").strip()]
    if missing:
        pytest.skip("Set in e2e/.env: " + ", ".join(missing))


def test_add_event_appears_in_admin_list(logged_in_admin, base_url):
    _need(
        "E2E_ADD_EVENT_TITLE",
        "E2E_ADD_EVENT_DATETIME",
        "E2E_ADD_EVENT_CATEGORY",
        "E2E_ADD_EVENT_LOCATION",
        "E2E_ADD_EVENT_TICKETS",
    )
    title = os.environ["E2E_ADD_EVENT_TITLE"].strip()
    description = os.environ.get("E2E_ADD_EVENT_DESCRIPTION", "").strip()
    dt_local = os.environ["E2E_ADD_EVENT_DATETIME"].strip()
    category = os.environ["E2E_ADD_EVENT_CATEGORY"].strip()
    location = os.environ["E2E_ADD_EVENT_LOCATION"].strip()
    tickets = int(os.environ["E2E_ADD_EVENT_TICKETS"].strip())

    driver = logged_in_admin
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

    wait_till_custom_condition(driver, lambda d: find_admin_event_card(d, title) is not None)
    card = find_admin_event_card(driver, title)
    assert card is not None
    assert title in card.text
    assert category in card.text
    assert location in card.text or location.split(",")[0].strip() in card.text
    assert str(tickets) in card.text
    assert "Status: ACTIVE" in card.text
