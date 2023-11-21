use std::io::{stderr, Write};
use std::panic;
use log::{Level, LevelFilter, Record};
use serde::{Deserialize, Serialize};
use crate::EOT;

pub struct WorkerLogger;

impl WorkerLogger {
    // This should only be called once
    pub fn init() {
        set_panic_handler();

        log::set_boxed_logger(Box::new(WorkerLogger)).expect("Failed to set logger");
        log::set_max_level(LevelFilter::max());
    }
}

fn set_panic_handler() {
    panic::set_hook(Box::new(|info| {
        let mut builder = Record::builder();
        builder.level(Level::Error).target("panic");

        if let Some(loc) = info.location() {
            builder.file(Some(loc.file()));
            builder.line(Some(loc.line()));
        }

        log::logger().log(&builder.args(format_args!("{}", info)).build());
    }));
}

#[derive(Debug, Serialize, Deserialize)]
pub struct LogMessage<'a> {
    pub message: &'a str,
    pub level: Level,
    pub target: &'a str,
    pub file: Option<&'a str>,
    pub line: Option<u32>,
}

impl log::Log for WorkerLogger {
    fn enabled(&self, _: &log::Metadata) -> bool {
        true
    }

    fn log(&self, record: &Record) {
        let msg = LogMessage {
            message: &record.args().to_string(),
            level: record.level(),
            target: record.target(),
            file: record.file().or(record.file_static()),
            line: record.line(),
        };

        serde_json::to_writer(stderr(), &msg).expect("Failed to write response!");
        stderr().write(&[EOT]).expect("Failed to write terminator!");
        stderr().flush().expect("Failed to flush stderr!");
    }

    // currently no flush implementation
    fn flush(&self) {
        stderr().flush().expect("Failed to flush stderr!");
    }
}
