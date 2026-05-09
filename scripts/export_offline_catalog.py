#!/usr/bin/env python3
import argparse
import json
import sys
import time
import urllib.request


def fetch_json(base_url, path, timeout):
    url = base_url.rstrip("/") + path
    request = urllib.request.Request(url, headers={"Accept": "application/json", "User-Agent": "drbep-offline-export/1.0"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = response.read().decode("utf-8")
    return json.loads(body)


def optional_fetch(base_url, path, timeout, fallback):
    try:
        return fetch_json(base_url, path, timeout)
    except Exception as exc:
        print(f"warn: {path} no disponible: {exc}", file=sys.stderr)
        return fallback


def main():
    parser = argparse.ArgumentParser(description="Exporta un snapshot JSON para DRBEP TV Offline.")
    parser.add_argument("--base-url", required=True, help="URL base del backend, por ejemplo http://192.168.93.223:8080")
    parser.add_argument("--output", required=True, help="Ruta de salida catalog_snapshot.json")
    parser.add_argument("--timeout", type=int, default=45, help="Timeout HTTP por endpoint en segundos")
    args = parser.parse_args()

    catalog = fetch_json(args.base_url, "/api/channels/catalog?include_disabled=0", args.timeout)
    tivify = optional_fetch(args.base_url, "/api/vod/tivify", args.timeout, {})
    runtime = optional_fetch(args.base_url, "/api/vod/runtime", args.timeout, {})

    snapshot = {
        "schema": "drbep-offline-catalog-v1",
        "generated_at": int(time.time()),
        "source_base_url": args.base_url.rstrip("/"),
        "catalog": catalog,
        "vod": tivify.get("vod", []),
        "adult": tivify.get("adult", []),
        "runtime_movies": runtime.get("movies", []),
    }

    with open(args.output, "w", encoding="utf-8") as handle:
        json.dump(snapshot, handle, ensure_ascii=False, separators=(",", ":"))
    print(f"ok: snapshot escrito en {args.output}")
    print(f"canales={len(catalog.get('channels', []))} vod={len(snapshot['vod'])} adult={len(snapshot['adult'])} runtime={len(snapshot['runtime_movies'])}")


if __name__ == "__main__":
    main()
