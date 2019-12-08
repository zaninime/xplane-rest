let
  repo = builtins.fetchTarball {
    url =
      "https://github.com/zaninime/sbt-derivation/archive/1ef212261cf7ad878c253192a1c8171de4d75b6d.tar.gz";
    sha256 = "1mz2s4hajc9cnrfs26d99ap4gswcidxcq441hg3aplnrmzrxbqbp";
  };
in import repo
