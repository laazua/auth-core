module.exports = {
  extends: [
    'stylelint-config-standard',
    'stylelint-config-recess-order',
    'stylelint-prettier/recommended',
  ],
  plugins: ['stylelint-prettier'],
  rules: {
    'prettier/prettier': true,
    'color-hex-case': 'lower',
    'color-hex-length': 'short',
    'selector-class-pattern':
      '^[a-z][a-z0-9]*(-[a-z0-9]+)*(__[a-z0-9]+(-[a-z0-9]+)*)?(--[a-z0-9]+(-[a-z0-9]+)*)?$',
    'custom-property-pattern': '^[a-z][a-z0-9]*(-[a-z0-9]+)*$',
    'selector-pseudo-class-no-unknown': [
      true,
      { ignorePseudoClasses: ['global', 'local', 'deep'] },
    ],
  },
  ignoreFiles: ['dist/**', 'node_modules/**', '*.min.css'],
};
