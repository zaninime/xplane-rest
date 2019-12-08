#!/usr/bin/env nix-shell
#!nix-shell --pure -i bash -p nix-prefetch-git cacert

REPO_URL="https://github.com/NixOS/nixpkgs-channels.git"
REF="refs/heads/nixpkgs-unstable"
DIR=$(dirname "$(readlink -f "$0")")

nix-prefetch-git --no-deepClone "$REPO_URL"  "$REF" > "$DIR/nixpkgs-unstable.json"
