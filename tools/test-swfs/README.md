# Test movies

Generators of the SWFs in `app/src/androidTest/res/raw` that were made for this fork:

- `game_scenes.py` writes `game_scenes.swf`, for `GameSceneMenu`: a movie that keeps its
  scenes the way the games that Ruffle's game scene menu supports do, with a `scenelist`,
  the current scene number in `pos`, and a `setScreen(name)` function that traces
  "showing <scene>". Run `python3 game_scenes.py > ../../app/src/androidTest/res/raw/game_scenes.swf`.
- `folder_test` writes `folder_test.swf`, for `FolderContent`: a movie that loads
  `parts/hello.txt` and then `PARTS/HELLO.TXT` with `LoadVars`, and traces both. Run
  `cargo run --manifest-path folder_test/Cargo.toml -- ../../app/src/androidTest/res/raw/folder_test.swf`.

`scenes.swf` comes from Ruffle's own tests.
