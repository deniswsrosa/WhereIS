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
    'base/assets/sprites/suspects/suspect_marina_valentine.png'; do
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
