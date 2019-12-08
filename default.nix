{ pkgs ? import ./nix/pkgs.nix }:

with pkgs.lib;

let
  inherit (pkgs) sbt makeWrapper jdk12_headless;
  mainClass = "me.zanini.xplanerest.Boot";
in sbt.mkDerivation rec {
  pname = "xplane-rest";
  version = "0.1.0";

  depsSha256 = "1kac3x2zxfsisq4i5b0wq4lnm5yljwyk68h63bwcvy76pvipjk1m";

  nativeBuildInputs = [ makeWrapper ];

  src = sourceByRegex ./. [
    "^project$"
    "^project/.*$"
    "^src$"
    "^src/.*$"
    "^build.sbt$"
  ];

  buildPhase = ''
    sbt stage
  '';

  installPhase = ''
    mkdir -p $out/{bin,lib}
    cp -ar target/universal/stage/lib $out/lib/${pname}

    makeWrapper ${jdk12_headless}/bin/java $out/bin/${pname} \
      --add-flags "-cp '$out/lib/${pname}/*' ${escapeShellArg mainClass}"
  '';
}
