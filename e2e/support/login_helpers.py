"""Sign in on /signin (customer → /event, admin → /adminEvent)."""
from selenium.webdriver.common.by import By

from support.waits import wait_till_custom_condition, wait_till_element_is_visible


def perform_sign_in(driver, base_url: str, email_or_phone: str, password: str, timeout: float = 15) -> None:
    driver.get(f"{base_url}/signin")

    email_box = wait_till_element_is_visible(
        driver,
        (By.CSS_SELECTOR, "input[placeholder='Enter your email or phone']"),
        timeout=timeout,
    )
    password_box = driver.find_element(By.CSS_SELECTOR, "input[placeholder='Enter your password']")
    submit_btn = driver.find_element(By.CSS_SELECTOR, "button[type='submit']")

    email_box.clear()
    email_box.send_keys(email_or_phone)
    password_box.clear()
    password_box.send_keys(password)
    submit_btn.click()

    wait_till_custom_condition(
        driver,
        lambda d: "/event" in d.current_url or "/adminEvent" in d.current_url,
        timeout=timeout,
    )
