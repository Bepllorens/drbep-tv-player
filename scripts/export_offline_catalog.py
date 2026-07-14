#!/usr/bin/env python3
import argparse
import copy
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


SENSITIVE_DRM_FIELDS = {
    "clearkey",
    "clear_keys",
    "clearkey_json",
    "license_key",
    "drm_key",
    "key",
    "kid",
}


def secure_drm_reference_for(item):
    if not isinstance(item, dict):
        return ""
    for candidate in ("id", "channel_id", "external_id", "provider_id", "name"):
        value = str(item.get(candidate, "")).strip()
        if value:
            return value
    return ""


def has_embedded_drm_secret(item):
    if not isinstance(item, dict):
        return False
    for field in SENSITIVE_DRM_FIELDS:
        value = item.get(field)
        if value:
            return True
    license_url = str(item.get("license_url", "") or item.get("drm_license_url", "")).strip().lower()
    return license_url.startswith("data:application/json")


def scrub_drm_secrets(item):
    if not isinstance(item, dict):
        return item
    cleaned = copy.deepcopy(item)
    if not has_embedded_drm_secret(cleaned):
        return cleaned
    drm_ref = secure_drm_reference_for(cleaned)
    for field in SENSITIVE_DRM_FIELDS:
        cleaned.pop(field, None)
    cleaned.pop("license_url", None)
    cleaned.pop("drm_license_url", None)
    cleaned["drm_ref"] = drm_ref
    cleaned["secure_drm"] = True
    if not str(cleaned.get("drm_scheme", "") or cleaned.get("drm_type", "")).strip():
        cleaned["drm_scheme"] = "clearkey"
    return cleaned


def scrub_drm_secrets_in_list(rows):
    if not isinstance(rows, list):
        return []
    return [scrub_drm_secrets(row) for row in rows]


def scrub_catalog_drm_secrets(catalog):
    if not isinstance(catalog, dict):
        return catalog
    cleaned = copy.deepcopy(catalog)
    cleaned["channels"] = scrub_drm_secrets_in_list(cleaned.get("channels", []))
    return cleaned


def parse_epoch_seconds(value):
    if value is None:
        return 0
    if isinstance(value, (int, float)):
        numeric = int(value)
        return numeric // 1000 if numeric > 10_000_000_000 else numeric
    text = str(value).strip()
    if not text:
        return 0
    if text.endswith("Z"):
        text = text[:-1] + "+00:00"
    try:
        from datetime import datetime
        parsed = datetime.fromisoformat(text)
        return int(parsed.timestamp())
    except Exception:
        return 0


def normalize_lookup(value):
    return str(value or "").strip().lower()


def build_now_index(now_rows):
    by_channel_id = {}
    by_tvg_id = {}
    by_name = {}
    for row in now_rows or []:
        if not isinstance(row, dict):
            continue
        channel_id = normalize_lookup(row.get("channel_id"))
        tvg_id = normalize_lookup(row.get("tvg_id"))
        channel_name = normalize_lookup(row.get("channel_name"))
        if channel_id and channel_id not in by_channel_id:
            by_channel_id[channel_id] = row
        if tvg_id and tvg_id not in by_tvg_id:
            by_tvg_id[tvg_id] = row
        if channel_name and channel_name not in by_name:
            by_name[channel_name] = row
    return {
        "by_channel_id": by_channel_id,
        "by_tvg_id": by_tvg_id,
        "by_name": by_name,
    }


def match_now_program(channel, now_index):
    if not isinstance(channel, dict) or not now_index:
        return None
    channel_id = normalize_lookup(channel.get("id"))
    if channel_id and channel_id in now_index["by_channel_id"]:
        return now_index["by_channel_id"][channel_id]
    tvg_id = normalize_lookup(channel.get("tvg_id"))
    if tvg_id and tvg_id in now_index["by_tvg_id"]:
        return now_index["by_tvg_id"][tvg_id]
    channel_name = normalize_lookup(channel.get("name"))
    if channel_name and channel_name in now_index["by_name"]:
        return now_index["by_name"][channel_name]
    return None


def program_title(program):
    if not isinstance(program, dict):
        return ""
    title = str(program.get("title", "") or "").strip()
    if not title:
        return ""
    return title


def build_epg_snapshot(base_url, channels, timeout, max_items_per_channel):
    now_rows = optional_fetch(base_url, "/api/epg/now", timeout, [])
    now_index = build_now_index(now_rows if isinstance(now_rows, list) else [])
    programs = {}
    program_count = 0
    until = 0
    for channel in channels:
        channel_id = str(channel.get("id", "")).strip()
        if not channel_id:
            continue
        rows = optional_fetch(base_url, f"/api/epg/channel/{channel_id}", timeout, [])
        if not isinstance(rows, list) or not rows:
            matched_now = match_now_program(channel, now_index)
            rows = [matched_now] if matched_now else []
        if not rows:
            continue
        trimmed = rows[:max(1, max_items_per_channel)]
        current_title = program_title(trimmed[0] if trimmed else None)
        next_title = program_title(trimmed[1] if len(trimmed) > 1 else None)
        if current_title:
            channel["now_program"] = current_title
        if next_title:
            channel["next_program"] = next_title
        programs[channel_id] = trimmed
        program_count += len(trimmed)
        for item in trimmed:
            end_seconds = parse_epoch_seconds(item.get("end_time"))
            if end_seconds > until:
                until = end_seconds
    return {
        "channel_count": len(programs),
        "program_count": program_count,
        "until": until,
        "programs": programs,
    }


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
    parser.add_argument("--include-epg", action=argparse.BooleanOptionalAction, default=True, help="Incluir EPG offline por canal")
    parser.add_argument("--epg-max-items-per-channel", type=int, default=24, help="Maximo de programas EPG guardados por canal")
    parser.add_argument("--parental-groups", default="", help="Grupos protegidos por PIN, separados por coma")
    parser.add_argument("--parental-channel-ids", default="", help="IDs de canales protegidos por PIN, separados por coma")
    parser.add_argument("--parental-filter-keys", default="", help="Claves de filtro protegidas por PIN, separadas por coma")
    parser.add_argument("--parental-protect-adult", action=argparse.BooleanOptionalAction, default=False, help="Protege VOD adulto con PIN")
    parser.add_argument("--secure-drm-references", action=argparse.BooleanOptionalAction, default=True, help="Sustituir claves DRM embebidas por referencias resueltas bajo demanda")
    args = parser.parse_args()

    catalog = filter_catalog(fetch_json(args.base_url, "/api/channels/catalog?include_disabled=0", args.timeout), args)
    tivify = optional_fetch(args.base_url, "/api/vod/tivify", args.timeout, {})
    runtime = optional_fetch(args.base_url, "/api/vod/runtime", args.timeout, {})
    if args.secure_drm_references:
        catalog = scrub_catalog_drm_secrets(catalog)
        tivify = dict(tivify) if isinstance(tivify, dict) else {}
        tivify["vod"] = scrub_drm_secrets_in_list(tivify.get("vod", []))
        tivify["adult"] = scrub_drm_secrets_in_list(tivify.get("adult", []))
        runtime = dict(runtime) if isinstance(runtime, dict) else {}
        runtime["movies"] = scrub_drm_secrets_in_list(runtime.get("movies", []))
    now = int(time.time())
    permissions = {
        "allow_platforms": sorted(csv_set(args.allow_platforms)),
        "deny_platforms": sorted(csv_set(args.deny_platforms)),
        "allow_groups": sorted(csv_set(args.allow_groups)),
        "deny_groups": sorted(csv_set(args.deny_groups)),
        "vod": args.include_vod,
        "tivify_adult": args.include_adult,
        "runtime": args.include_runtime,
        "parental_vod_adult": args.parental_protect_adult,
        "parental_group_names": sorted(csv_set(args.parental_groups)),
        "parental_channel_ids": sorted(csv_set(args.parental_channel_ids)),
        "parental_filter_keys": sorted(csv_set(args.parental_filter_keys)),
    }
    epg = build_epg_snapshot(args.base_url, catalog.get("channels", []), args.timeout, args.epg_max_items_per_channel) if args.include_epg else {
        "channel_count": 0,
        "program_count": 0,
        "until": 0,
        "programs": {},
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
        "epg": epg,
        "vod": tivify.get("vod", []) if args.include_vod else [],
        "adult": tivify.get("adult", []) if args.include_adult else [],
        "runtime_movies": runtime.get("movies", []) if args.include_runtime else [],
    }

    with open(args.output, "w", encoding="utf-8") as handle:
        json.dump(snapshot, handle, ensure_ascii=False, separators=(",", ":"))
    print(f"ok: snapshot escrito en {args.output}")
    print(f"canales={len(catalog.get('channels', []))} epg_canales={epg['channel_count']} epg_programas={epg['program_count']} vod={len(snapshot['vod'])} adult={len(snapshot['adult'])} runtime={len(snapshot['runtime_movies'])}")


if __name__ == "__main__":
    main()
