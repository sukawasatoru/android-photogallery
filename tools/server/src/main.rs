use anyhow::Result as Fallible;
use hyper::Method;
use juniper::{graphql_object, graphql_value, FieldError, FieldResult};
use std::sync::Arc;
use structopt::StructOpt;
use tracing::{info, warn};
use url::Url;

#[derive(StructOpt)]
struct Opt {
    /// Print verbose log
    #[structopt(short, long, parse(from_occurrences))]
    verbose: u8,

    /// Server address
    #[structopt(short, long)]
    address: String,

    /// Base URL
    #[structopt(short, long, parse(try_from_str))]
    base_url: Url,

    /// Port number
    #[structopt(short, long, default_value = "38082")]
    port: u16,
}

struct Context {
    base_url: Url,
    image_count: i32,
}

impl juniper::Context for Context {
    // do nothing.
}

struct Query;

#[graphql_object(Context = Context)]
impl Query {
    async fn images(
        context: &Context,
        first: i32,
        after: Option<String>,
    ) -> FieldResult<Option<ImagesConnection>> {
        let after = match after {
            Some(data) => {
                let segments = data.split('-').collect::<Vec<_>>();
                if segments.len() != 2 {
                    return Err(FieldError::new(
                        anyhow::anyhow!("INVALID_CURSOR_ARGUMENTS"),
                        graphql_value!({ "internal_error": "Connection refused" }),
                    ));
                }
                match segments[1].parse::<i32>() {
                    Ok(data) => {
                        // after: "image-50" -- start from --> "image-51".
                        if context.image_count < data + 1 {
                            return Ok(None);
                        }

                        Some(data)
                    }
                    Err(e) => {
                        return Err(FieldError::new(
                            anyhow::anyhow!("parse error: {:?}", e),
                            graphql_value!({ "internal_error": "Connection refused" }),
                        ))
                    }
                }
            }
            None => None,
        };

        Ok(Some(ImagesConnection { first, after }))
    }
}

struct ImagesConnection {
    first: i32,
    after: Option<i32>,
}

fn range_max(first: i32, after: Option<i32>, max: i32) -> i32 {
    match after {
        Some(data) => {
            let sum = first + data;
            if max < sum {
                max
            } else {
                sum
            }
        }
        None => {
            if max < first {
                max
            } else {
                first
            }
        }
    }
}

#[graphql_object(Context = Context)]
impl ImagesConnection {
    async fn edge(&self, context: &Context) -> Vec<ImagesEdge> {
        let start = match self.after {
            Some(data) => data + 1,
            None => 1,
        };

        (start..=range_max(self.first, self.after, context.image_count))
            .map(|id| ImagesEdge { id })
            .collect()
    }

    async fn nodes(&self, context: &Context) -> Vec<Image> {
        let start = match self.after {
            Some(data) => data + 1,
            None => 1,
        };

        (start..=range_max(self.first, self.after, context.image_count))
            .map(|id| Image { id })
            .collect()
    }

    fn page_info(&self) -> PageInfo {
        PageInfo {
            first: self.first,
            after: self.after,
        }
    }

    async fn total_count(context: &Context) -> i32 {
        context.image_count
    }
}

struct ImagesEdge {
    id: i32,
}

#[graphql_object(Context = Context)]
impl ImagesEdge {
    fn cursor(&self) -> String {
        format!("image-{}", self.id)
    }

    fn node(&self) -> Image {
        Image { id: self.id }
    }
}

struct Image {
    id: i32,
}

#[graphql_object(Context = Context)]
impl Image {
    fn url(&self, context: &Context) -> Url {
        context
            .base_url
            .join(&format!("image/image-{}.png", self.id))
            .unwrap()
    }
}

struct PageInfo {
    first: i32,
    after: Option<i32>,
}

#[graphql_object(Context = Context)]
impl PageInfo {
    fn start_cursor(&self) -> String {
        match self.after {
            Some(data) => format!("image-{}", data + 1),
            None => "image-1".into(),
        }
    }

    fn end_cursor(&self, context: &Context) -> String {
        let max = range_max(self.first, self.after, context.image_count);
        let next = if max < context.image_count {
            max
        } else {
            context.image_count
        };
        format!("image-{}", next)
    }

    async fn has_next_page(&self, context: &Context) -> bool {
        range_max(self.first, self.after, context.image_count) < context.image_count
    }

    async fn has_previous_page() -> bool {
        false
    }
}

#[tokio::main]
async fn main() -> Fallible<()> {
    dotenv::dotenv().ok();

    let opt: Opt = Opt::from_args();

    setup_log(opt.verbose);

    info!("Hello");

    let socket_addr = format!("{}:{}", opt.address, opt.port).parse()?;
    info!(%socket_addr);

    let apollo_content_type = Arc::new(hyper::header::HeaderValue::from_str(
        "application/json; charset=utf-8",
    )?);

    hyper::Server::bind(&socket_addr)
        .serve(hyper::service::make_service_fn(move |_| {
            let base_url = opt.base_url.clone();
            let apollo_content_type = apollo_content_type.clone();
            async {
                Ok::<_, hyper::Error>(hyper::service::service_fn(move |mut req| {
                    let base_url = base_url.clone();
                    let apollo_content_type = apollo_content_type.clone();
                    async {
                        info!(?req, uri = ?req.uri());
                        let apollo_content_type = apollo_content_type;
                        let context = Arc::new(Context {
                            base_url,
                            image_count: 100_000,
                        });

                        let root_node = Arc::new(juniper::RootNode::new(
                            Query,
                            juniper::EmptyMutation::new(),
                            juniper::EmptySubscription::new(),
                        ));

                        let req_uri = req.uri().path().split('/').collect::<Vec<_>>();
                        if req.method() == &Method::GET && req_uri.get(1) == Some(&"image") {
                            let image_name = req_uri.last().unwrap_or(&"(none)");
                            return Ok(image(image_name));
                        }

                        match (req.method(), req.uri().path()) {
                            (&Method::GET, "/graphiql") => {
                                juniper_hyper::graphiql("/graphql", None)
                                    .await
                                    .map(|mut data| {
                                        data.headers_mut().append(
                                            hyper::header::ACCESS_CONTROL_ALLOW_ORIGIN,
                                            hyper::header::HeaderValue::from_str("*").unwrap(),
                                        );
                                        data
                                    })
                            }
                            (&Method::OPTIONS, "/graphql") => {
                                warn!("TODO: Support OPTIONS method for juniper");
                                Ok(hyper::Response::builder()
                                    .status(hyper::StatusCode::NOT_FOUND)
                                    .body(hyper::Body::from("TODO: Support OPTIONS method"))
                                    .unwrap())
                            }
                            (&Method::GET, "/graphql") => {
                                juniper_hyper::graphql(root_node, context, req).await.map(
                                    |mut data| {
                                        data.headers_mut().append(
                                            hyper::header::ACCESS_CONTROL_ALLOW_ORIGIN,
                                            "*".parse().unwrap(),
                                        );
                                        data
                                    },
                                )
                            }
                            (&Method::POST, "/graphql") => {
                                if req.headers().get(hyper::header::CONTENT_TYPE)
                                    == Some(&apollo_content_type)
                                {
                                    req.headers_mut().insert(
                                        hyper::header::CONTENT_TYPE,
                                        "application/json".parse().unwrap(),
                                    );
                                }
                                juniper_hyper::graphql(root_node, context, req).await.map(
                                    |mut data| {
                                        data.headers_mut().append(
                                            hyper::header::ACCESS_CONTROL_ALLOW_ORIGIN,
                                            "*".parse().unwrap(),
                                        );
                                        data
                                    },
                                )
                            }
                            _ => Ok(hyper::Response::builder()
                                .status(hyper::StatusCode::NOT_FOUND)
                                .body(hyper::Body::empty())
                                .unwrap()),
                        }
                    }
                }))
            }
        }))
        .with_graceful_shutdown(async {
            tokio::signal::ctrl_c()
                .await
                .expect("failed to install CTRL+C signal handler")
        })
        .await?;

    info!("Bye");

    Ok(())
}

fn image(name: &str) -> hyper::Response<hyper::Body> {
    let (ps, ts) = silicon::utils::init_syntect();
    let syntax = ps.find_syntax_by_extension("txt").unwrap();
    let theme = &ts.themes["Dracula"];
    let mut h = syntect::easy::HighlightLines::new(syntax, theme);
    let highlight = syntect::util::LinesWithEndings::from(name)
        .map(|line| h.highlight(line, &ps))
        .collect::<Vec<_>>();
    let mut buf = Vec::with_capacity(100 * 1024 * 1024);
    silicon::formatter::ImageFormatterBuilder::new()
        .font(vec![("JetBrains Mono", 48.0), ("Ubuntu Mono", 48.0)])
        .line_number(false)
        .shadow_adder(silicon::utils::ShadowAdder::default())
        .build()
        .unwrap()
        .format(&highlight, theme)
        .write_to(&mut buf, image::ImageOutputFormat::Png)
        .unwrap();
    use hyper::header;
    hyper::Response::builder()
        .header(header::CONTENT_ENCODING, "identity")
        .header(header::CONTENT_TYPE, "image/png")
        .body(hyper::Body::from(buf))
        .unwrap()
}

fn setup_log(level: u8) {
    let builder = tracing_subscriber::fmt();
    match std::env::var(tracing_subscriber::EnvFilter::DEFAULT_ENV) {
        Ok(data) => {
            let builder = builder.with_env_filter(tracing_subscriber::EnvFilter::new(data));
            match level {
                0 => builder.init(),
                1 => builder.with_max_level(tracing::Level::DEBUG).init(),
                _ => builder.with_max_level(tracing::Level::TRACE).init(),
            }
        }
        Err(_) => match level {
            0 => builder.with_max_level(tracing::Level::INFO).init(),
            1 => builder.with_max_level(tracing::Level::DEBUG).init(),
            _ => builder.with_max_level(tracing::Level::TRACE).init(),
        },
    }
}
