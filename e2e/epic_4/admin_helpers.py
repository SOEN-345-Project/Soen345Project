"""Admin /adminEvent: list, add/modify modal, cancel event, customer check."""
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

from support.waits import wait_till_custom_condition, wait_till_element_is_clickable, wait_till_element_is_hidden, wait_till_element_is_present

CARD_H2 = (By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")


def goto_admin_events(driver, base_url: str) -> None:
    driver.get(f"{base_url}/adminEvent")
    wait_till_element_is_present(driver, (By.XPATH, "//h1[contains(., 'All Events')]"))
    wait_till_element_is_hidden(driver, (By.XPATH, "//p[contains(., 'Loading events...')]"))


def click_logout(driver) -> None:
    wait_till_element_is_clickable(driver, (By.XPATH, "//button[contains(., 'Logout')]")).click()
    wait_till_custom_condition(driver, lambda d: "/signin" in d.current_url)


def find_admin_event_card(driver, title_substring: str):
    for h2 in driver.find_elements(*CARD_H2):
        if title_substring in h2.text:
            return h2.find_element(By.XPATH, "./ancestor::div[contains(@class,'rounded-2xl')][1]")
    return None


def find_admin_active_event_card(driver, title_substring: str):
    for h2 in driver.find_elements(*CARD_H2):
        if title_substring not in h2.text:
            continue
        card = h2.find_element(By.XPATH, "./ancestor::div[contains(@class,'rounded-2xl')][1]")
        if "Status: ACTIVE" in card.text:
            return card
    return None


def set_datetime_local(driver, element, value: str) -> None:
    driver.execute_script(
        """
        const el = arguments[0];
        const val = arguments[1];
        const d = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
        d.set.call(el, val);
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.dispatchEvent(new Event('change', { bubbles: true }));
        """,
        element,
        value,
    )


def open_add_event_modal(driver) -> None:
    wait_till_element_is_clickable(driver, (By.XPATH, "//button[contains(., 'Add Event')]")).click()
    wait_till_element_is_present(driver, (By.XPATH, "//h2[contains(., 'Add New Event')]"))


def fill_admin_event_form(
    driver,
    *,
    title: str,
    description: str,
    datetime_local: str,
    category: str,
    location: str,
    tickets: int,
) -> None:
    wait_till_element_is_present(driver, (By.CSS_SELECTOR, "input[placeholder='Event title']"))
    title_el = driver.find_element(By.CSS_SELECTOR, "input[placeholder='Event title']")
    title_el.clear()
    title_el.send_keys(title)

    desc_el = driver.find_element(By.CSS_SELECTOR, "textarea[placeholder='Optional description']")
    desc_el.clear()
    desc_el.send_keys(description)

    date_el = driver.find_element(By.CSS_SELECTOR, "#event-date")
    set_datetime_local(driver, date_el, datetime_local)

    selects = driver.find_elements(By.CSS_SELECTOR, "select")
    assert len(selects) >= 2
    Select(selects[0]).select_by_visible_text(category)
    Select(selects[1]).select_by_visible_text(location)

    num_el = driver.find_element(By.CSS_SELECTOR, "input[type='number']")
    num_el.clear()
    num_el.send_keys(str(tickets))


def submit_create_event(driver) -> None:
    wait_till_element_is_clickable(driver, (By.XPATH, "//button[contains(., 'Create Event')]")).click()
    wait_till_element_is_present(driver, (By.XPATH, "//p[contains(., 'Event created!')]"))


def submit_save_changes(driver) -> None:
    wait_till_element_is_clickable(driver, (By.XPATH, "//button[contains(., 'Save Changes')]")).click()
    wait_till_element_is_present(driver, (By.XPATH, "//p[contains(., 'Event updated!')]"))


def close_modal_done(driver) -> None:
    wait_till_element_is_clickable(driver, (By.XPATH, "//button[contains(., 'Done')]")).click()
    wait_till_element_is_hidden(driver, (By.CSS_SELECTOR, "#event-date"))


def click_modify_on_card(driver, card) -> None:
    btn = card.find_element(By.XPATH, ".//button[contains(., 'Modify')]")
    wait_till_element_is_clickable(driver, btn).click()
    wait_till_element_is_present(driver, (By.XPATH, "//h2[contains(., 'Modify Event')]"))


def click_cancel_event_on_card(driver, card) -> None:
    btn = card.find_element(By.XPATH, ".//button[contains(., 'Cancel')]")
    wait_till_element_is_clickable(driver, btn).click()


def wait_card_contains_status(driver, title_substring: str, status_fragment: str) -> None:
    def card_has_status(d):
        c = find_admin_event_card(d, title_substring)
        return c is not None and status_fragment in c.text

    wait_till_custom_condition(driver, card_has_status)


def customer_event_page_shows_title(driver, title_substring: str) -> bool:
    wait_till_element_is_present(driver, (By.XPATH, "//h1[contains(., 'Available Events')]"))
    wait_till_custom_condition(
        driver,
        lambda d: bool(
            d.find_elements(*CARD_H2)
            or d.find_elements(By.XPATH, "//p[contains(., 'No events found')]")
        ),
    )
    return any(title_substring in h.text for h in driver.find_elements(*CARD_H2))
