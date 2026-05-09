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


def csv_set(value):
    if not value:
        return set()
    return {item.strip().lower() for item in value.split(",") if item.strip()}


def channel_allowed(channel, allow_platforms, deny_platforms, allow_groups, deny_groups):
    platform_id = str(channel.get("platform_id", "")).strip().lower()
    platform_name = str(channel.get("platform_name", "")).strip().lower()
    group = str(channel.get("group", "")).strip().lower()
    groups = {group}
    for custom_group in channel.get("custom_groups") or []:
        groups.add(str(custom_group).strip().lower())
    platform_tokens = {platform_id, platform_name}
    if allow_platforms and not platform_tokens.intersection(allow_platforms):
        return False
    if deny_platforms and platform_tokens.intersection(deny_platforms):
        return False
    if allow_groups and not groups.intersection(allow_groups):
        return False
    if deny_groups and groups.intersection(deny_groups):
        return False
    return True


def filter_catalog(catalog, args):
    allow_platforms = csv_set(args.allow_platforms)
    deny_platforms = csv_set(args.deny_platforms)
    allow_groups = csv_set(args.allow_groups)
    deny_groups = csv_set(args.deny_groups)
    filtered = dict(catalog)
    channels = catalog.get("channels", [])
    filtered["channels"] = [
        channel for channel in channels
        if channel_allowed(channel, allow_platforms, deny_platforms, allow_groups, deny_groups)
    ]
    return filtered


def main():
    parser = argparse.ArgumentParser(description="Exporta un snapshot JSON para DRBEP TV Offline.")
    parser.add_argument("--base-url", required=True, help="URL base del backend, por ejemplo http://192.168.93.223:8080")
    parser.add_argument("--output", required=True, help="Ruta de salida catalog_snapshot.json")
    parser.add_argument("--timeout", type=int, default=45, help="Timeout HTTP por endpoint en segundos")
    parser.add_argument("--subject", default="", help="Nombre del usuario/dispositivo del snapshot")
    parser.add_argument("--device-id", default="", help="ID de dispositivo esperado")
    parser.add_argument("--ttl-days", type=int, default=7, help="Dias de validez del snapshot")
    parser.add_argument("--allow-platforms", default="", help="IDs o nombres de plataformas permitidas, separados por coma")
    parser.add_argument("--deny-platforms", default="", help="IDs o nombres de plataformas bloqueadas, separados por coma")
    parser.add_argument("--allow-groups", default="", help="Grupos/listas permitidas, separados por coma")
    parser.add_argument("--deny-groups", default="", help="Grupos/listas bloqueadas, separados por coma")
    parser.add_argument("--include-vod", action=argparse.BooleanOptionalAction, default=True, help="Incluir VOD general")
    parser.add_argument("--include-adult", action=argparse.BooleanOptionalAction, default=False, help="Incluir VOD adulto")
    parser.add_argument("--include-runtime", action=argparse.BooleanOptionalAction, default=True, help="Incluir Runtime")
    args = parser.parse_args()

    catalog = filter_catalog(fetch_json(args.base_url, "/api/channels/catalog?include_disabled=0", args.timeout), args)
    tivify = optional_fetch(args.base_url, "/api/vod/tivify", args.timeout, {})
    runtime = optional_fetch(args.base_url, "/api/vod/runtime", args.timeout, {})
    now = int(time.time())
    permissions = {
        "allow_platforms": sorted(csv_set(args.allow_platforms)),
        "deny_platforms": sorted(csv_set(args.deny_platforms)),
        "allow_groups": sorted(csv_set(args.allow_groups)),
        "deny_groups": sorted(csv_set(args.deny_groups)),
        "vod": args.include_vod,
        "adult": args.include_adult,
        "runtime": args.include_runtime,
    }

    snapshot = {
        "schema": "drbep-offline-catalog-v2",
        "generated_at": now,
        "expires_at": now + max(1, args.ttl_days) * 86400,
        "subject": args.subject,
        "device_id": args.device_id,
        "permissions": permissions,
        "source_base_url": args.base_url.rstrip("/"),
        "catalog": catalog,
        "vod": tivify.get("vod", []) if args.include_vod else [],
        "adult": tivify.get("adult", []) if args.include_adult else [],
        "runtime_movies": runtime.get("movies", []) if args.include_runtime else [],
    }

    with open(args.output, "w", encoding="utf-8") as handle:
        json.dump(snapshot, handle, ensure_ascii=False, separators=(",", ":"))
    print(f"ok: snapshot escrito en {args.output}")
    print(f"canales={len(catalog.get('channels', []))} vod={len(snapshot['vod'])} adult={len(snapshot['adult'])} runtime={len(snapshot['runtime_movies'])}")


if __name__ == "__main__":
    main()
