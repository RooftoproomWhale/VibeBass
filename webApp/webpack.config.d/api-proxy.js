// Browser requests stay on the web origin; only the development server knows the backend address.
config.devServer = config.devServer || {};
config.devServer.proxy = [{
    context: ['/api'],
    target: process.env.VIBEBASS_DEV_API_TARGET || 'http://127.0.0.1:8082'
}];
