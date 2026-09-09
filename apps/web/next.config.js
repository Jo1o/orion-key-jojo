/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  reactStrictMode: true,

  // API 代理配置
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: process.env.NEXT_PUBLIC_API_URL
          ? `${process.env.NEXT_PUBLIC_API_URL}/:path*`
          : 'http://localhost:8083/api/:path*',
      },
    ];
  },

  // 图片优化配置
  images: {
    domains: ['localhost', 'jojobuy.top'],
    unoptimized: process.env.NODE_ENV === 'development',
  },

  // 环境变量
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8083/api',
  },
};

module.exports = nextConfig;
