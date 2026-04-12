"""
US-4.3: Admin cancels an ACTIVE event; title disappears from the admin grid (ACTIVE-only list).

Algorithm:
  1. require_env E2E_ADD_EVENT_TITLE; fixture already logged in as admin.
  2. goto_admin_events — grid ready.
  3. scroll_admin_page_to_bottom_once — ensure lower cards are in DOM.
  4. read_admin_event_cards: assert some card title contains the string and one match is ACTIVE; find_admin_active_event_card.
  5. click_cancel_event_on_card — triggers reload.
  6. wait_admin_event_grid_ready (long timeout).
  7. scroll again (no second grid wait); wait_till_no_admin_card_contains_title — poll until no h2 contains the title.
  8. read_admin_event_cards again; assert no card title still contains the string.

Asserts: preconditions for cancel; post-cancel no card shows that title (matches ACTIVE-only behaviour after cancel).
"""
import os
import time

from epic_4.admin_helpers import (
    ADMIN_GRID_AFTER_CANCEL_TIMEOUT,
    click_cancel_event_on_card,
    find_admin_active_event_card,
    goto_admin_events,
    read_admin_event_cards,
    scroll_admin_page_to_bottom_once,
    wait_admin_event_grid_ready,
    wait_till_no_admin_card_contains_title,
)
from support.env_checks import require_env


def test_cancel_event_removes_card_from_admin_grid(logged_in_admin, base_url):
    require_env("E2E_ADD_EVENT_TITLE")
    title = os.environ["E2E_ADD_EVENT_TITLE"].strip()
    driver = logged_in_admin

    goto_admin_events(driver, base_url)

    scroll_admin_page_to_bottom_once(driver)

    cards = read_admin_event_cards(driver)
    matching = [c for c in cards if title in c["title"]]
    assert matching, (
        f"Expected an event card whose title contains {title!r}. "
        f"Found titles: {[c['title'] for c in cards]!r}"
    )
    assert any("ACTIVE" in m["body"].upper() for m in matching), (
        f"Target card for {title!r} must be ACTIVE before cancel. Bodies: {[m['body'] for m in matching]!r}"
    )
    desired = find_admin_active_event_card(driver, title)
    assert desired is not None

    click_cancel_event_on_card(driver, desired)

    wait_admin_event_grid_ready(driver, timeout=ADMIN_GRID_AFTER_CANCEL_TIMEOUT)

    scroll_admin_page_to_bottom_once(driver, wait_for_grid=False)
    wait_till_no_admin_card_contains_title(driver, title, timeout=ADMIN_GRID_AFTER_CANCEL_TIMEOUT)

    after_cancel = read_admin_event_cards(driver)
    assert all(title not in c["title"] for c in after_cancel), (
        f"Expected {title!r} on no admin card after cancel; still see "
        f"{[c['title'] for c in after_cancel if title in c['title']]!r}"
    )
    time.sleep(2)
