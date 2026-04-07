"""
Easy-to-read Selenium waits (WebDriverWait + EC + named predicates — avoid lambdas in test files).
"""
from __future__ import annotations

from typing import Callable, Tuple, TypeVar, Union

from selenium.webdriver.common.by import By
from selenium.webdriver.remote.webdriver import WebDriver
from selenium.webdriver.remote.webelement import WebElement
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import Select
from selenium.webdriver.support.ui import WebDriverWait

# Either a (By, selector) pair or an already-found element
ClickableTarget = Union[Tuple[str, str], WebElement]

DEFAULT_TIMEOUT = 25

# Customer /event list page
CUSTOMER_EVENTS_H1 = (By.XPATH, "//h1[contains(., 'Available Events')]")

T = TypeVar("T")


def _wait(driver: WebDriver, timeout: float) -> WebDriverWait:
    return WebDriverWait(driver, timeout)


def wait_till_element_is_visible(
    driver: WebDriver,
    locator: Tuple[str, str],
    *,
    timeout: float = DEFAULT_TIMEOUT,
) -> WebElement:
    """Wait until the element is on the page and visible, then return it."""
    return _wait(driver, timeout).until(EC.visibility_of_element_located(locator))


def wait_till_element_is_present(
    driver: WebDriver,
    locator: Tuple[str, str],
    *,
    timeout: float = DEFAULT_TIMEOUT,
) -> WebElement:
    """Wait until the element exists in the DOM (might still be hidden)."""
    return _wait(driver, timeout).until(EC.presence_of_element_located(locator))


def wait_till_element_is_clickable(
    driver: WebDriver,
    target: ClickableTarget,
    *,
    timeout: float = DEFAULT_TIMEOUT,
) -> WebElement:
    """Wait until the element (locator or WebElement) can be clicked."""
    return _wait(driver, timeout).until(EC.element_to_be_clickable(target))


def wait_till_element_is_hidden(
    driver: WebDriver,
    locator: Tuple[str, str],
    *,
    timeout: float = DEFAULT_TIMEOUT,
) -> None:
    """Wait until the element is not visible or removed from the page."""
    _wait(driver, timeout).until(EC.invisibility_of_element_located(locator))


def wait_till_url_contains(driver: WebDriver, text: str, *, timeout: float = DEFAULT_TIMEOUT) -> None:
    """Wait until the browser URL contains ``text`` (e.g. ``/event``)."""

    def url_ok(d: WebDriver) -> bool:
        return text in d.current_url

    _wait(driver, timeout).until(url_ok)


def _customer_event_grid_ready(driver: WebDriver) -> bool:
    """Customer /event: at least one card, or the explicit empty state."""
    return bool(
        driver.find_elements(By.CSS_SELECTOR, "h2.text-lg.font-semibold.text-stone-900")
        or driver.find_elements(By.XPATH, "//p[contains(., 'No events found')]")
    )


def wait_till_customer_event_grid_ready(driver: WebDriver, *, timeout: float = DEFAULT_TIMEOUT) -> None:
    _wait(driver, timeout).until(_customer_event_grid_ready)


def wait_till_input_value_equals(
    driver: WebDriver,
    locator: Tuple[str, str],
    expected: str,
    *,
    timeout: float = DEFAULT_TIMEOUT,
) -> None:
    """Wait until ``<input>`` value equals ``expected`` (uses get_property('value'))."""

    def matches(d: WebDriver) -> bool:
        return (d.find_element(*locator).get_property("value") or "") == expected

    _wait(driver, timeout).until(matches)


def wait_till_nth_select_first_option_contains(
    driver: WebDriver,
    select_index: int,
    text: str,
    *,
    timeout: float = DEFAULT_TIMEOUT,
) -> None:
    """Wait until ``Select(driver.find_elements('select')[select_index]).first_selected_option`` contains ``text``."""

    def ok(d: WebDriver) -> bool:
        sels = d.find_elements(By.TAG_NAME, "select")
        if len(sels) <= select_index:
            return False
        return text in Select(sels[select_index]).first_selected_option.text

    _wait(driver, timeout).until(ok)


def wait_till_custom_condition(
    driver: WebDriver,
    check: Callable[[WebDriver], T],
    *,
    timeout: float = DEFAULT_TIMEOUT,
) -> T:
    """Wait until ``check(driver)`` returns something truthy."""
    return _wait(driver, timeout).until(check)
