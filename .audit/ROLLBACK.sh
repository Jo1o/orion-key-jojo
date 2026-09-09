#!/usr/bin/env bash
set -eu

target=${1:?target path is required}
backup=${2:?backup path is required}
cp -- "$backup" "$target"
printf 'RESTORED %s FROM %s\n' "$target" "$backup"
