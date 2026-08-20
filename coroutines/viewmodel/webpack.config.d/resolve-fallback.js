/* eslint-disable no-undef -- config: see .eslintrc.json webpack.config.d override */
config.resolve.fallback = {
    ...config.resolve.fallback,
    path: false,
    os: false,
};
