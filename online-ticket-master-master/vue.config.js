const path = require('path')
function resolve(dir){
  return path.join(__dirname,dir)
}
module.exports = {
  chainWebpack: (config) => {
    config.resolve.alias
    .set('@',resolve('/src'))
    .set('assets',resolve('/src/assets'))
    .set('components',resolve('/src/components'))
    .set('network',resolve('/src/network'))
    .set('views',resolve('/src/views'))
    .set('store',resolve('/src/store'))
    .set('common',resolve('/src/common'))
  },
  devServer: {
    proxy: {
      '/apis': {
        target: 'http://127.0.0.1:8087/',
        ws: true,
        changeOrigin: true,
        pathRewrite: {
          '^/apis': ''
        }
      }
    }
  }
}