"""Start Chrome for E2E (visible window by default; set E2E_HEADLESS=1 for headless)."""
from __future__ import annotations

import os
from typing import Optional

from selenium import webdriver
from selenium.webdriver.chrome.options import Options


def create_chrome_driver(*, headless: Optional[bool] = None) -> webdriver.Chrome:
    if headless is None:
        headless = os.environ.get("E2E_HEADLESS", "").lower() in ("1", "true", "yes")

    options = Options()
    if headless:
        options.add_argument("--headless=new")
        options.add_argument("--disable-gpu")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    return webdriver.Chrome(options=options)
