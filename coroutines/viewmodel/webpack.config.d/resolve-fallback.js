/* eslint-disable no-undef -- config is injected by Kotlin/JS webpack.config.d */
config.resolve.fallback = {
    ...config.resolve.fallback,
    path: false,
    os: false,
};
