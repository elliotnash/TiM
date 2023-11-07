use serde::{Deserialize, Serialize};
use crate::render::RenderOptions;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum Request {
    Render(RenderRequest),
    Version(),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RenderRequest {
    pub code: String,
    pub options: RenderOptions
}

#[derive(Debug, Serialize, Deserialize)]
pub enum Response {
    #[serde(with="base64")]
    RenderSuccess(Vec<u8>),
    RenderError(String),
    Version(VersionResponse),
}

#[derive(Debug, Serialize, Deserialize)]
pub struct VersionResponse {
    pub version: String,
    pub git_hash: String,
}

mod base64 {
    use serde::{Serialize, Deserialize};
    use serde::{Deserializer, Serializer};
    use base64::{Engine as _, engine::general_purpose};

    pub fn serialize<S: Serializer>(data: &Vec<u8>, s: S) -> Result<S::Ok, S::Error> {
        let encoded = general_purpose::STANDARD_NO_PAD.encode(data);
        String::serialize(&encoded, s)
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(d: D) -> Result<Vec<u8>, D::Error> {
        let base64 = String::deserialize(d)?;
        general_purpose::STANDARD_NO_PAD.decode(base64.as_bytes())
            .map_err(|e| serde::de::Error::custom(e))
    }
}
