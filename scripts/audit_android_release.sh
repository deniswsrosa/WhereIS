#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd -- "$script_dir/.." && pwd)"
android_dir="$repo_dir/android"
billing_file="$android_dir/app/src/main/java/com/acme/clara/billing/BillingManager.kt"
gradle_file="$android_dir/app/build.gradle.kts"
aab_file="$android_dir/app/build/outputs/bundle/release/app-release.aab"

require_line() {
    local pattern="$1"
    local file="$2"
    local message="$3"
    if ! rg -q -- "$pattern" "$file"; then
        echo "RELEASE BLOCKER: $message" >&2
        exit 1
    fi
}

require_line 'const val SALES_ENABLED = true' "$billing_file" \
    'BillingManager.SALES_ENABLED must be true for the Play-tested artifact.'
require_line 'const val PRODUCT_ID = "world_campaign_unlock"' "$billing_file" \
    'The app product ID does not match the Play Console setup guide.'
require_line 'applicationId = "com.acme.clara"' "$gradle_file" \
    'The application ID changed from the Play package name.'
require_line 'targetSdk = 36' "$gradle_file" \
    'The release target SDK is not the audited API level.'

python3 - "$repo_dir" <<'PY'
import json
import re
import sys
from pathlib import Path

repo = Path(sys.argv[1])
source = json.loads((repo / "translation/source/ui2.json").read_text())
placeholders = re.compile(r"\{(?:\d+|[sSp])\}|%s")
languages = ("de", "es", "fr", "id", "it", "nl", "pl", "pt", "ru", "tr")

for language in languages:
    catalog = json.loads((repo / f"translation/{language}/ui2.json").read_text())
    runtime = json.loads(
        (repo / f"android/app/src/main/assets/i18n/{language}.json").read_text()
    )
    missing = [key for key in source if key not in catalog or key not in runtime]
    empty = [key for key in source if not str(catalog.get(key, "")).strip()]
    bad_placeholders = [
        key
        for key, value in source.items()
        if key in catalog
        and sorted(placeholders.findall(value))
        != sorted(placeholders.findall(str(catalog[key])))
    ]
    stale_runtime = [
        key for key in source if key in catalog and runtime.get(key) != catalog[key]
    ]
    if missing or empty or bad_placeholders or stale_runtime:
        print(
            f"RELEASE BLOCKER: {language} translation is incomplete "
            f"(missing={len(missing)}, empty={len(empty)}, "
            f"placeholders={len(bad_placeholders)}, stale_runtime={len(stale_runtime)}).",
            file=sys.stderr,
        )
        sys.exit(1)

print(f"Translation audit passed: {len(source)} source keys in {len(languages)} languages")
PY

(
    cd "$android_dir"
    ./gradlew testDebugUnitTest testReleaseUnitTest lintRelease bundleRelease
)

if [[ ! -s "$aab_file" ]]; then
    echo "RELEASE BLOCKER: release AAB was not produced at $aab_file" >&2
    exit 1
fi

archive_listing="$(unzip -Z1 "$aab_file")"
for required in \
    'base/manifest/AndroidManifest.xml' \
    'base/assets/sprites/suspects/suspect_marina_valentine.png' \
    'base/assets/sprites/suspects/suspect_pepper_sterling.png' \
    'base/assets/sprites/suspects/suspect_miles_memento.png'; do
    if ! rg -qx -- "$required" <<<"$archive_listing"; then
        echo "RELEASE BLOCKER: AAB is missing $required" >&2
        exit 1
    fi
done

# BuildConfig.DEBUG is a compile-time false constant in release. Its guarded menu labels should
# therefore not survive into the release DEX even though the developer helpers remain testable.
if unzip -p "$aab_file" 'base/dex/*.dex' | strings | rg -q \
    'Jump: hideout doorstep|Jump: result \(win|Paid version'; then
    echo 'RELEASE BLOCKER: debug-menu text is present in the release DEX.' >&2
    exit 1
fi

# AndroidX may contribute native helpers even though the app itself is Kotlin. Every ELF LOAD
# segment must be aligned to at least 16 KB for current Play devices. Keep this in the release
# gate so a future dependency upgrade cannot silently undo the manual accessibility audit.
native_entries="$(rg '\.so$' <<<"$archive_listing" || true)"
if [[ -n "$native_entries" ]]; then
    native_dir="$(mktemp -d)"
    trap 'rm -rf -- "$native_dir"' EXIT
    unzip -qq "$aab_file" 'base/lib/*/*.so' -d "$native_dir"
    while IFS= read -r so_file; do
        while IFS= read -r alignment; do
            alignment="${alignment#0x}"
            if (( 16#$alignment < 16384 )); then
                echo "RELEASE BLOCKER: ${so_file#"$native_dir/"} has a LOAD alignment below 16 KB." >&2
                exit 1
            fi
        done < <(readelf -lW "$so_file" | awk '$1 == "LOAD" { print $NF }')
    done < <(find "$native_dir" -type f -name '*.so' -print)
fi

signing_report="$(jarsigner -verify -verbose -certs "$aab_file" 2>&1 || true)"
if rg -q '^jar verified\.$' <<<"$signing_report" && ! rg -q '^jar is unsigned\.$' <<<"$signing_report"; then
    signing='signed'
else
    signing='unsigned — configure android/keystore.properties before Play upload'
fi

echo "Release audit passed"
echo "AAB: $aab_file"
echo "Signing: $signing"
echo 'Play Console still must have an active world_campaign_unlock one-time product.'
