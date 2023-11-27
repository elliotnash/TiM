use std::fmt::Display;
use std::path::Path;
use tiny_skia::{Pixmap, PixmapPaint, Transform};
use typst::diag::SourceResult;
use typst::eval::Tracer;
use typst::geom::{Abs, Color};
use std::rc::Rc;
use no_comment::IntoWithoutComments;
use serde::{Deserialize, Serialize};
use crate::sandbox::Sandbox;


pub fn render<S: AsRef<str> + Display, P: AsRef<Path>>(code: S, options: RenderOptions, font_dir: P) -> SourceResult<Pixmap> {
    let mut tracer = Tracer::new();
    let sandbox = Rc::new(Sandbox::new("./cache".into(), font_dir));

    let source = options.preamble(code);
    let world = Sandbox::with_source(&sandbox, source.clone());
    let document = typst::compile(&world, &mut tracer)?;

    let pixmap = typst::export::render_merged(&document.pages, 12f32, Color::from_u32(0), Abs::zero(), Color::from_u32(0));

    let aspect = pixmap.width() as f64 / pixmap.height() as f64;

    if aspect > 2.0 {
        let mut resized = Pixmap::new(pixmap.width(), ((pixmap.height() as f64) * aspect / 2.0) as u32).unwrap();
        let paint = PixmapPaint::default();
        resized.draw_pixmap(0, 0, pixmap.as_ref(), &paint, Transform::default(), None);
        Ok(resized)
    } else {
        Ok(pixmap)
    }
}

#[derive(Deserialize, Serialize, Default, Debug, Clone, Copy)]
pub enum PageSize {
    Preview,
    #[default]
    Auto,
    Default,
}

impl PageSize {
    pub const fn preamble(self) -> &'static str {
        match self {
            Self::Preview => "#set page(width: 300pt, height: auto, margin: 10pt)\n",
            Self::Auto => "#set page(width: auto, height: auto, margin: 10pt)\n",
            Self::Default => "",
        }
    }
}

#[derive(Deserialize, Serialize, Default, Debug, Clone, Copy)]
pub enum Theme {
    Light,
    #[default]
    Dark
}

impl Theme {
    pub fn preamble(self, transparent: bool) -> &'static str {
        if transparent {
            match self {
                Self::Light => "",
                Self::Dark => "#set text(fill: rgb(219, 222, 225))\n",
            }
        } else {
            match self {
                Self::Light => "#set page(fill: white)\n",
                Self::Dark => concat!(
                "#set page(fill: black)\n",
                "#set text(fill: rgb(219, 222, 225))\n",
                ),
            }
        }
    }
}

#[derive(Deserialize, Serialize, Default, Debug, Clone, Copy)]
pub struct RenderOptions {
    pub page_size: PageSize,
    pub theme: Theme,
    pub transparent: bool,
}

impl RenderOptions {
    pub fn preamble<S: AsRef<str> + Display>(self, body: S) -> String {
        // Remove all comments - these aren't rendered and by removing comments, we can check for
        // the presence of page rules.
        let body = body.to_string().chars().without_comments(no_comment::languages::rust()).collect::<String>();

        let page_size = self.page_size.preamble();
        let theme = self.theme.preamble(self.transparent);

        if matches!(self.page_size, PageSize::Auto) && !body.contains("set page(") {
            // In this case we should use fancy formatting
            format!(
                r###"
                // Define body
                #let body = [
                    {body}
                ]
                // Begin preamble
                // Theme:
                {theme}
                // Page size:
                #style(styles => {{
                  let margin = 10pt

                  let size = measure(body, styles)

                  let width = size.width
                  let height = size.height
                  let aspect = width/calc.max(height, 1pt)

                  if aspect > 2 {{
                    if width < 300pt {{
                      width = auto
                      height = height * (aspect/1.75) + margin*2
                    }} else {{
                      width = 300pt + margin*2
                      height = auto
                    }}
                  }} else if aspect < 0.75 {{
                    width = size.height * 0.75 + margin*2
                    height = auto
                  }} else {{
                    width = width + margin*2
                    height = height + margin*2
                  }}

                  set page(width: width, height: height, margin: margin)

                  body
                }})
                "###,
                body = body,
                theme = theme,
            )
        } else {
            if theme.is_empty() && page_size.is_empty() {
                String::new()
            } else {
                format!(
                    concat!(
                    "// Begin preamble\n",
                    "// Page size:\n",
                    "{page_size}",
                    "// Theme:\n",
                    "{theme}",
                    "// End preamble\n",
                    "{body}"
                    ),
                    page_size = page_size,
                    theme = theme,
                    body = body,
                )
            }
        }
    }
}
