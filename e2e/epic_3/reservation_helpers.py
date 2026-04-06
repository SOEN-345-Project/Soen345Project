"""Steps for reservation modal and /reservation page."""
import re

from selenium.common.exceptions import StaleElementReferenceException
from selenium.webdriver.common.by import By
from selenium.webdriver.remote.webelement import WebElement

from support.waits import (
    wait_till_element_is_clickable,
    wait_till_element_is_hidden,
    wait_till_element_is_present,
    wait_till_element_is_visible,
)

CARD_TITLE = (By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
RESERVE_IN_MODAL = (By.XPATH, "//button[contains(., 'Reserve') and contains(., 'ticket')]")
HEADER_RESERVATION = (By.XPATH, "//button[normalize-space()='Reservation']")


def find_event_card_h2(driver, title_substring: str):
    for h2 in driver.find_elements(*CARD_TITLE):
        if title_substring in h2.text:
            return h2
    return None


def open_reservation_modal(driver, title_substring: str) -> None:
    h2 = find_event_card_h2(driver, title_substring)
    if h2 is None:
        raise AssertionError(f"No event card with title containing {title_substring!r}")
    card = h2.find_element(By.XPATH, "./ancestor::div[contains(@class,'rounded-2xl')][1]")
    btn = card.find_element(By.CSS_SELECTOR, "[aria-label='Reserve tickets']")
    wait_till_element_is_clickable(driver, btn).click()
    wait_till_element_is_present(driver, RESERVE_IN_MODAL)


def click_reserve_in_modal(driver) -> None:
    wait_till_element_is_clickable(driver, RESERVE_IN_MODAL).click()


def wait_for_reservation_success(driver) -> None:
    wait_till_element_is_present(driver, (By.XPATH, "//p[contains(., 'Reservation confirmed!')]"))


def read_modal_date_and_location_lines(driver) -> tuple[str, str]:
    """Return raw text of the Date and Location lines in the open reserve modal (may be empty location)."""
    date_el = wait_till_element_is_present(driver, (By.XPATH, "//p[.//strong[contains(., 'Date')]]"))
    date_text = date_el.text.strip()
    loc_els = driver.find_elements(By.XPATH, "//p[.//strong[contains(., 'Location')]]")
    loc_text = loc_els[0].text.strip() if loc_els else ""
    return date_text, loc_text


def parse_ticket_quantity_from_success_modal(driver) -> int:
    """Parse 'N ticket(s) successfully reserved' after a successful reserve."""
    p = wait_till_element_is_present(driver, (By.XPATH, "//p[contains(., 'successfully reserved')]"))
    m = re.search(r"(\d+)\s+tickets?\s+successfully reserved", p.text, re.IGNORECASE)
    if not m:
        raise AssertionError(f"Could not parse ticket quantity from success text: {p.text!r}")
    return int(m.group(1))


def location_fragments_from_modal_line(location_line: str) -> list[str]:
    """Split 'Location: A, B' into ['A', 'B'] for substring checks on /reservation."""
    if not location_line:
        return []
    without_label = re.sub(r"^\s*Location:\s*", "", location_line, flags=re.IGNORECASE).strip()
    return [p.strip() for p in without_label.split(",") if len(p.strip()) > 1]


def find_reservation_row_block(driver, title_substring: str) -> WebElement:
    """The bordered card on /reservation that contains the event title."""
    for s in driver.find_elements(By.XPATH, "//strong"):
        if title_substring not in s.text:
            continue
        return s.find_element(By.XPATH, "./ancestor::div[contains(@style,'1px solid')][1]")
    raise AssertionError(f"No reservation row with title containing {title_substring!r}")


def wait_for_sold_out_message(driver) -> None:
    wait_till_element_is_present(driver, (By.XPATH, "//*[contains(., 'Tickets Sold Out')]"))


def click_modal_done(driver) -> None:
    wait_till_element_is_clickable(driver, (By.XPATH, "//button[contains(., 'Done')]")).click()
    wait_till_element_is_hidden(driver, (By.XPATH, "//p[contains(., 'Reservation confirmed!')]"))


def wait_reservations_page_ready(driver) -> None:
    wait_till_element_is_present(driver, (By.XPATH, "//h1[contains(., 'Reservations')]"))
    wait_till_element_is_hidden(driver, (By.XPATH, "//p[contains(., 'Loading...')]"))


def go_to_reservations_list(driver) -> None:
    wait_till_element_is_clickable(driver, HEADER_RESERVATION).click()
    wait_reservations_page_ready(driver)


def cancel_active_reservation_for_title(driver, title_substring: str) -> None:
    for s in driver.find_elements(By.XPATH, "//strong"):
        try:
            if title_substring not in s.text:
                continue
            block = s.find_element(By.XPATH, "./ancestor::div[contains(@style,'1px solid')][1]")
            btns = block.find_elements(By.XPATH, ".//button[contains(., 'Cancel reservation')]")
        except StaleElementReferenceException:
            continue
        if not btns:
            continue
        wait_till_element_is_clickable(driver, btns[0]).click()
        return
    raise AssertionError(f"No active reservation row for title containing {title_substring!r}")


def wait_till_no_reservations_empty_state(driver) -> None:
    """Wait for the /reservation empty list copy (after cancel + reload, only RESERVED rows are shown)."""
    wait_till_element_is_visible(driver, (By.XPATH, "//p[contains(., 'No reservations found.')]"))
