#!/usr/bin/env python3
"""Read-only CI probe. Reports availability, never substitutes sample data in the app."""
import concurrent.futures
import datetime as dt
import json
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET

OUTPUT = Path("build/verification/public-sources.json")
FEEDS = [
    "https://agenciabrasil.ebc.com.br/rss/ultimasnoticias/feed.xml",
    "https://agenciabrasil.ebc.com.br/rss/economia/feed.xml",
    "https://agenciabrasil.ebc.com.br/radioagencia-nacional/rss/pesquisa-e-inovacao/feed.xml",
]

def read(url):
    request = urllib.request.Request(url, headers={"User-Agent": "JARVIS-Mobile/3.0 personal-reader", "Accept": "application/json, application/rss+xml, application/xml"})
    with urllib.request.urlopen(request, timeout=12) as response:
        body = response.read(2 * 1024 * 1024 + 1)
        if len(body) > 2 * 1024 * 1024:
            raise ValueError("Response exceeds limit")
        return body, response.status

def rss(url):
    try:
        body, status = read(url)
        if b"<!DOCTYPE" in body.upper():
            raise ValueError("DOCTYPE is unsupported")
        root = ET.fromstring(body)
        if root.tag.split("}")[-1] not in ("rss", "feed"):
            raise ValueError("Not RSS/Atom")
        count = sum(node.tag.split("}")[-1] in ("item", "entry") for node in root.iter())
        return {"url": url, "ok": count > 0, "http": status, "entries": count, "bytes": len(body)}
    except Exception as error:
        return {"url": url, "ok": False, "error_type": type(error).__name__}

def weather():
    # Public CI location, unrelated to any user. App never enables weather until opt-in.
    url = "https://api.open-meteo.com/v1/forecast?latitude=-23.55&longitude=-46.63&daily=temperature_2m_min,temperature_2m_max,precipitation_probability_max&timezone=auto&forecast_days=1"
    try:
        body, status = read(url)
        daily = json.loads(body)["daily"]
        return {"provider": "Open-Meteo", "ok": True, "http": status, "date": daily["time"][0], "location": "Sao Paulo - public CI location", "low": daily["temperature_2m_min"][0], "high": daily["temperature_2m_max"][0]}
    except Exception as error:
        return {"provider": "Open-Meteo", "ok": False, "error_type": type(error).__name__}

def main():
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        feeds = list(pool.map(rss, FEEDS))
        forecast = pool.submit(weather).result()
    result = {"checked_at": dt.datetime.now(dt.timezone.utc).isoformat(), "feeds": feeds, "weather": forecast, "gmail": "Not tested: no OAuth configuration or user authorization in CI"}
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(f"Public sources: {sum(feed['ok'] for feed in feeds)}/{len(feeds)} RSS available; weather available: {forecast['ok']}. See {OUTPUT}")

if __name__ == "__main__":
    main()
