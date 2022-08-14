// This entire file is only needed for the Oboe host of the Cpal audio backend,
// see: https://github.com/katyo/oboe-rs/issues/28#issuecomment-1001103335

fn main() {
    println!("cargo:rustc-link-lib=c++_shared");
}
