module.exports = {
  root: true,
  // web/011c：全量 lint 卡死根因治理——构建产物与覆盖率目录不入 lint 面（RED-1/RED-2 对照实证）
  ignorePatterns: ['dist', 'coverage'],
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  parser: 'vue-eslint-parser',
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
    parser: '@typescript-eslint/parser',
    extraFileExtensions: ['.vue'],
  },
  plugins: ['vue', '@typescript-eslint', 'prettier'],
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:prettier/recommended',
  ],
  rules: {
    'prettier/prettier': 'error',
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'off',
    '@typescript-eslint/no-explicit-any': 'warn',
    // ignoreRestSiblings：省略式解构（const { code, ...rest } 排除不可改字段）是契约惯用法，
    // 8 处 no-unused-vars 全属此类——删变量会把 code 带回 update 载荷，破坏「code 不可改」语义
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', ignoreRestSiblings: true }],
    'vue/require-default-prop': 'off',
    'vue/require-prop-types': 'off',
  },
  overrides: [
    {
      files: ['*.vue'],
      rules: {
        'vue/no-mutating-props': 'off',
      },
    },
  ],
}