#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_PROPERTIES="$ROOT_DIR/local.properties"

fail() {
  printf 'error: %s\n' "$*" >&2
  exit 1
}

read_property() {
  local key="$1"
  local file="$2"

  [ -f "$file" ] || return 0

  awk -v search_key="$key" '
    /^[[:space:]]*($|#|!)/ { next }
    {
      line = $0
      sub(/^[[:space:]]+/, "", line)
      eq = index(line, "=")
      colon = index(line, ":")
      sep = eq
      if (sep == 0 || (colon > 0 && colon < sep)) {
        sep = colon
      }
      if (sep == 0) {
        next
      }

      prop_key = substr(line, 1, sep - 1)
      prop_value = substr(line, sep + 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", prop_key)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", prop_value)

      if (prop_key == search_key) {
        print prop_value
        exit
      }
    }
  ' "$file"
}

expand_path() {
  local value="$1"

  case "$value" in
    "~")
      printf '%s\n' "$HOME"
      ;;
    "~/"*)
      printf '%s/%s\n' "$HOME" "${value#~/}"
      ;;
    /*)
      printf '%s\n' "$value"
      ;;
    *)
      printf '%s/%s\n' "$ROOT_DIR" "$value"
      ;;
  esac
}

[ -f "$LOCAL_PROPERTIES" ] || fail "missing local.properties"

sdk_dir="$(read_property "sdk.dir" "$LOCAL_PROPERTIES")"
if [ -z "$sdk_dir" ]; then
  sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
fi
[ -n "$sdk_dir" ] || fail "set sdk.dir in local.properties"
sdk_dir="$(expand_path "$sdk_dir")"
[ -d "$sdk_dir" ] || fail "Android SDK directory not found: $sdk_dir"

build_tools_version="$(read_property "release.buildToolsVersion" "$LOCAL_PROPERTIES")"
if [ -z "$build_tools_version" ]; then
  build_tools_version="$(
    find "$sdk_dir/build-tools" -mindepth 1 -maxdepth 1 -type d -print 2>/dev/null \
      | sed 's#.*/##' \
      | sort \
      | tail -n 1
  )"
fi
[ -n "$build_tools_version" ] || fail "no Android build-tools found under $sdk_dir/build-tools"

build_tools_dir="$sdk_dir/build-tools/$build_tools_version"
zipalign="$build_tools_dir/zipalign"
apksigner="$build_tools_dir/apksigner"
aapt="$build_tools_dir/aapt"

[ -x "$zipalign" ] || fail "zipalign not found: $zipalign"
[ -x "$apksigner" ] || fail "apksigner not found: $apksigner"
[ -x "$aapt" ] || fail "aapt not found: $aapt"

keystore_file="$(read_property "release.keystoreFile" "$LOCAL_PROPERTIES")"
signing_config_file="$(read_property "release.signingConfigFile" "$LOCAL_PROPERTIES")"
[ -n "$keystore_file" ] || fail "set release.keystoreFile in local.properties"
[ -n "$signing_config_file" ] || fail "set release.signingConfigFile in local.properties"

keystore_file="$(expand_path "$keystore_file")"
signing_config_file="$(expand_path "$signing_config_file")"
[ -f "$keystore_file" ] || fail "release keystore not found: $keystore_file"
[ -f "$signing_config_file" ] || fail "release signing config not found: $signing_config_file"

store_password="$(read_property "storePassword" "$signing_config_file")"
key_alias="$(read_property "keyAlias" "$signing_config_file")"
key_password="$(read_property "keyPassword" "$signing_config_file")"
[ -n "$store_password" ] || fail "set storePassword in $signing_config_file"
[ -n "$key_alias" ] || fail "set keyAlias in $signing_config_file"
[ -n "$key_password" ] || fail "set keyPassword in $signing_config_file"

cd "$ROOT_DIR"
./gradlew :app:assembleRelease

unsigned_apk="$ROOT_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"
[ -f "$unsigned_apk" ] || fail "unsigned release APK not found: $unsigned_apk"

package_line="$("$aapt" dump badging "$unsigned_apk" | sed -n '1p')"
version_name="$(printf '%s\n' "$package_line" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p")"
[ -n "$version_name" ] || version_name="release"

output_dir="$(read_property "release.outputDir" "$LOCAL_PROPERTIES")"
[ -n "$output_dir" ] || output_dir="build/release"
output_dir="$(expand_path "$output_dir")"

apk_name="$(read_property "release.apkName" "$LOCAL_PROPERTIES")"
[ -n "$apk_name" ] || apk_name="MockLocation-$version_name-release.apk"

mkdir -p "$output_dir"
aligned_apk="$output_dir/${apk_name%.apk}-aligned.apk"
signed_apk="$output_dir/$apk_name"

rm -f "$aligned_apk" "$signed_apk" "$signed_apk.idsig"
"$zipalign" -p -f 4 "$unsigned_apk" "$aligned_apk"

export storePassword="$store_password"
export keyPassword="$key_password"
"$apksigner" sign \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --v4-signing-enabled false \
  --ks "$keystore_file" \
  --ks-key-alias "$key_alias" \
  --ks-pass "env:storePassword" \
  --key-pass "env:keyPassword" \
  --out "$signed_apk" \
  "$aligned_apk"

"$apksigner" verify --verbose --print-certs "$signed_apk"
"$zipalign" -c -p 4 "$signed_apk"

printf '\nRelease APK: %s\n' "$signed_apk"
printf 'SHA256: %s\n' "$(shasum -a 256 "$signed_apk" | awk '{print $1}')"
