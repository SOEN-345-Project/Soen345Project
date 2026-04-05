"""
Easy-to-read Selenium waits (instead of raw WebDriverWait + EC).

Typical calls::

    wait_till_element_is_visible(driver, (By.ID, "x"))
    wait_till_element_is_clickable(driver, (By.CSS_SELECTOR, "button.submit"))
    wait_till_element_is_clickable(driver, some_web_element)
    wait_till_element_is_hidden(driver, (By.XPATH, "//p[.='Loading...']"))
    wait_till_url_contains(driver, "/event")
    wait_till_custom_condition(driver, lambda d: d.find_elements(...))
"""
from __future__ import annotations

from typing import Callable, Tuple, TypeVar, Union

from selenium.webdriver.remote.webdriver import WebDriver
from selenium.webdriver.remote.webelement import WebElement

# Either a (By, selector) pair or an already-found element
ClickableTarget = Union[Tuple[str, str], WebElement]
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

DEFAULT_TIMEOUT = 25

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
    _wait(driver, timeout).until(lambda d: text in d.current_url)


def wait_till_custom_condition(
    driver: WebDriver,
    check: Callable[[WebDriver], T],
    *,
    timeout: float = DEFAULT_TIMEOUT,
) -> T:
    """Wait until ``check(driver)`` returns something truthy."""
    return _wait(driver, timeout).until(check)
