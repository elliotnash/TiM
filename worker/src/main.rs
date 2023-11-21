mod sandbox;
mod render;
mod model;
mod logger;

use std::io::{stdin, stdout, Write};
use std::panic;
use std::path::{Path, PathBuf};
use serde::Deserialize;
use crate::model::{RenderRequest, Request, Response, VersionResponse};
use crate::render::render;

const EOT: u8 = 0x04;

fn main() {
    logger::WorkerLogger::init();

    let font_dir: PathBuf = std::env::args().nth(1).expect("No font directory passed!").into();
    if !font_dir.is_dir() {
        panic!("Font directory does not exist!")
    }

    log::debug!("Worker started");

    loop {
        let mut de = serde_json::Deserializer::from_reader(stdin());
        let req = Request::deserialize(&mut de).unwrap();

        let res = match req {
            Request::Version() => handle_version(),
            Request::Render(req) => handle_render(req, font_dir.clone()),
        };

        serde_json::to_writer(stdout(), &res).expect("Failed to write response!");
        stdout().write(&[EOT]).expect("Failed to write terminator!");
        stdout().flush().expect("Failed to flush stderr!");
    }
}

fn handle_version() -> Response {
    Response::Version(VersionResponse{
        version: env!("CARGO_PKG_VERSION").to_string(),
        git_hash: env!("GIT_HASH").to_string(),
    })
}

fn handle_render<P: AsRef<Path>>(request: RenderRequest, font_dir: P) -> Response {
    let res = render(&request.code, request.options, font_dir);
    let res = match res {
        Ok(pixmap) => {
            pixmap.encode_png().map_err(|e| e.to_string())
        },
        Err(err) => {
            let err_msg = if let Some(err) = err.get(0) {
                err.message.to_string()
            } else {
                "unknown error".to_string()
            };
            Err(err_msg)
        },
    };

    match res {
        Ok(data) => Response::RenderSuccess(data),
        Err(err) => Response::RenderError(err),
    }
}
