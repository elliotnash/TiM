mod sandbox;
mod render;
mod model;

use std::error::Error;
use std::fmt::Display;
use std::io::{Read, stdin, stdout, Write};
use no_comment::IntoWithoutComments;

use typst::World;
use serde::{Deserialize, Serialize};
use crate::model::{RenderRequest, Request, Response, VersionResponse};
use crate::render::{render, RenderOptions};


fn main() {
    // let test = Request::Version();
    // let val = serde_json::to_string(&test).unwrap();
    // println!("{}", val);
    loop {
        let mut de = serde_json::Deserializer::from_reader(stdin());
        let req = Request::deserialize(&mut de).unwrap();

        let res = match req {
            Request::Version() => handle_version(),
            Request::Render(req) => handle_render(req),
        };

        serde_json::to_writer(stdout(), &res).expect("TODO: panic message");
        stdout().flush().unwrap();
    }
}

fn handle_version() -> Response {
    Response::Version(VersionResponse{
        version: env!("CARGO_PKG_VERSION").to_string(),
        git_hash: env!("GIT_HASH").to_string(),
    })
}

fn handle_render(request: RenderRequest) -> Response {
    let res = render(&request.code, request.options);
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
