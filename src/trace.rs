use ruffle_core::backend::log::LogBackend;
use std::cell::RefCell;
use std::fs::File;
use std::io::{LineWriter, Write};
use std::path::Path;

pub struct FileLogBackend {
    writer: Option<RefCell<LineWriter<File>>>,
}

impl FileLogBackend {
    pub fn new(path: Option<&Path>) -> Self {
        Self {
            writer: path
                .map(|path| File::create(path).unwrap())
                .map(LineWriter::new)
                .map(RefCell::new),
        }
    }
}

impl LogBackend for FileLogBackend {
    fn avm_trace(&self, message: &str) {
        log::info!("avm_trace: {message}");
        if let Some(writer) = &self.writer {
            writer.borrow_mut().write_all(message.as_bytes()).unwrap();
            writer.borrow_mut().write_all("\n".as_bytes()).unwrap();
        }
    }
}
