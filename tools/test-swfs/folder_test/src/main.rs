//! Generates `folder_test.swf`, an AVM1 movie that loads text files from a
//! `parts` subfolder next to it and traces their contents:
//!
//! ```actionscript
//! var lv = new LoadVars();
//! lv.onData = function (src) {
//!     trace(src);
//!     var lv2 = new LoadVars();
//!     lv2.onData = function (src2) { trace(src2); };
//!     lv2.load("PARTS/HELLO.TXT");
//! };
//! lv.load("parts/hello.txt");
//! stop();
//! ```
use swf::avm1::types::{Action, DefineFunction, Push, Value};
use swf::avm1::write::Writer;
use swf::{Header, SwfStr, Tag};

fn s(text: &str) -> Value<'_> {
    Value::Str(SwfStr::from_utf8_str(text))
}

fn push<'a>(values: Vec<Value<'a>>) -> Action<'a> {
    Action::Push(Push { values })
}

fn encode(version: u8, actions: &[Action]) -> Vec<u8> {
    let mut out = vec![];
    let mut writer = Writer::new(&mut out, version);
    for action in actions {
        writer.write_action(action).unwrap();
    }
    out
}

fn new_load_vars<'a>() -> [Action<'a>; 2] {
    [push(vec![Value::Int(0), s("LoadVars")]), Action::NewObject]
}

fn main() {
    let version = 8;

    let inner_body = encode(version, &[push(vec![s("src2")]), Action::GetVariable, Action::Trace]);

    let [new_lv2_a, new_lv2_b] = new_load_vars();
    let outer_body = encode(
        version,
        &[
            push(vec![s("src")]),
            Action::GetVariable,
            Action::Trace,
            push(vec![s("lv2")]),
            new_lv2_a,
            new_lv2_b,
            Action::DefineLocal,
            push(vec![s("lv2")]),
            Action::GetVariable,
            push(vec![s("onData")]),
            Action::DefineFunction(DefineFunction {
                name: SwfStr::from_utf8_str(""),
                params: vec![SwfStr::from_utf8_str("src2")],
                actions: &inner_body,
            }),
            Action::SetMember,
            push(vec![s("PARTS/HELLO.TXT"), Value::Int(1), s("lv2")]),
            Action::GetVariable,
            push(vec![s("load")]),
            Action::CallMethod,
            Action::Pop,
        ],
    );

    let [new_lv_a, new_lv_b] = new_load_vars();
    let main_actions = encode(
        version,
        &[
            push(vec![s("lv")]),
            new_lv_a,
            new_lv_b,
            Action::SetVariable,
            push(vec![s("lv")]),
            Action::GetVariable,
            push(vec![s("onData")]),
            Action::DefineFunction(DefineFunction {
                name: SwfStr::from_utf8_str(""),
                params: vec![SwfStr::from_utf8_str("src")],
                actions: &outer_body,
            }),
            Action::SetMember,
            push(vec![s("parts/hello.txt"), Value::Int(1), s("lv")]),
            Action::GetVariable,
            push(vec![s("load")]),
            Action::CallMethod,
            Action::Pop,
            Action::Stop,
            Action::End,
        ],
    );

    let mut header = Header::default_with_swf_version(version);
    header.num_frames = 1;
    let tags = [Tag::DoAction(&main_actions), Tag::ShowFrame];
    let out = std::env::args().nth(1).expect("output path");
    let file = std::fs::File::create(&out).unwrap();
    swf::write_swf(&header, &tags, file).unwrap();
    println!("wrote {out}");
}
