let
  bootPkgs = import <nixpkgs> { };
  inherit (bootPkgs) lib fetchFromGitHub;
  inherit (lib) hasSuffix filterAttrs attrNames;

  versionInfo = lib.importJSON ./nixpkgs-unstable.json;
  src = fetchFromGitHub {
    owner = "NixOS";
    repo = "nixpkgs";
    inherit (versionInfo) rev sha256;
  };

  overlays = let
    dir = ./overlays;
    isNixFile = n: v: hasSuffix ".nix" n && v == "regular";
    filesToImport = filterAttrs isNixFile (builtins.readDir dir);
  in map (file: import ("${dir}/${file}")) (attrNames filesToImport);
in import src { inherit overlays; }
