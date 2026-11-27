/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // Type checking is its own verify step (`npm run typecheck`, run before `next build` in
  // scripts/verify_p07.ps1). Skipping it inside `next build` keeps the build independent of
  // whether Next can drive TypeScript 7 (QUESTIONS.md P07 L1).
  typescript: {
    ignoreBuildErrors: true,
  },
};

export default nextConfig;
