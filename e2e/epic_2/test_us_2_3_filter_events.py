"""US-2.3: Set category / location / dates, click Filter after each step; cards match filters."""
import os
import time

import pytest
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select

from support.waits import wait_till_custom_condition, wait_till_element_is_clickable, wait_till_element_is_present

WAIT_SECONDS = 10
FILTER_BTN = (By.XPATH, "//button[normalize-space()='Filter']")
START_DATE = (By.CSS_SELECTOR, "input[type='date'][title='Start date']")
END_DATE = (By.CSS_SELECTOR, "input[type='date'][title='End date']")


def _filter_env():
    return {
        "category": os.environ.get("E2E_FILTER_CATEGORY_NAME", "").strip(),
        "location": os.environ.get("E2E_FILTER_LOCATION_NAME", "").strip(),
        "start": os.environ.get("E2E_FILTER_START_DATE", "").strip(),
        "end": os.environ.get("E2E_FILTER_END_DATE", "").strip(),
    }


def _set_date_value(driver, element, iso_date: str) -> None:
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
        iso_date,
    )


def _click_filter_and_wait_for_grid(driver) -> None:
    wait_till_element_is_clickable(driver, FILTER_BTN).click()
    wait_till_custom_condition(
        driver,
        lambda d: bool(
            d.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
            or d.find_elements(By.XPATH, "//p[contains(., 'No events found')]")
        ),
    )


def _scroll_down_then_up(driver) -> None:
    driver.execute_script(
        "const h = Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"
        "window.scrollTo(0, h);"
    )
    time.sleep(0.15)
    driver.execute_script("window.scrollTo(0, 0);")
    time.sleep(0.15)


def test_filter_shows_matching_events(logged_in_customer, base_url):
    f = _filter_env()
    if not any(f.values()):
        pytest.skip(
            "Set at least one of E2E_FILTER_CATEGORY_NAME, E2E_FILTER_LOCATION_NAME, "
            "E2E_FILTER_START_DATE, E2E_FILTER_END_DATE in e2e/.env"
        )

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

    assert len(driver.find_elements(By.TAG_NAME, "select")) >= 2

    if f["category"]:
        Select(driver.find_elements(By.TAG_NAME, "select")[0]).select_by_visible_text(f["category"])
        wait_till_custom_condition(
            driver,
            lambda d: f["category"] in Select(d.find_elements(By.TAG_NAME, "select")[0]).first_selected_option.text,
        )
        _click_filter_and_wait_for_grid(driver)
        _scroll_down_then_up(driver)

    if f["location"]:
        Select(driver.find_elements(By.TAG_NAME, "select")[1]).select_by_visible_text(f["location"])
        wait_till_custom_condition(
            driver,
            lambda d: f["location"] in Select(d.find_elements(By.TAG_NAME, "select")[1]).first_selected_option.text,
        )
        _click_filter_and_wait_for_grid(driver)
        _scroll_down_then_up(driver)

    if f["start"]:
        el = wait_till_element_is_present(driver, START_DATE)
        _set_date_value(driver, el, f["start"])
        wait_till_custom_condition(
            driver,
            lambda d: (d.find_element(*START_DATE).get_property("value") or "") == f["start"],
        )
        _click_filter_and_wait_for_grid(driver)
        _scroll_down_then_up(driver)

    if f["end"]:
        el = wait_till_element_is_present(driver, END_DATE)
        _set_date_value(driver, el, f["end"])
        wait_till_custom_condition(
            driver,
            lambda d: (d.find_element(*END_DATE).get_property("value") or "") == f["end"],
        )
        _click_filter_and_wait_for_grid(driver)
        _scroll_down_then_up(driver)

    time.sleep(WAIT_SECONDS)

    titles = driver.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
    if not titles:
        pytest.skip("No events after filter; adjust E2E_FILTER_* in e2e/.env")

    cat = f["category"].lower()
    loc = f["location"].lower()
    for h2 in titles:
        card = h2.find_element(By.XPATH, "./ancestor::div[contains(@class,'rounded-2xl')][1]")
        text = card.text.lower()
        if cat:
            assert cat in text, f"Category filter: expected {f['category']!r} in card"
        if loc:
            assert loc in text, f"Location filter: expected {f['location']!r} in card"
