import { fixupConfigRules } from '@eslint/compat';
import { FlatCompat } from '@eslint/eslintrc';
import js from '@eslint/js';
import prettier from 'eslint-plugin-prettier';
import { defineConfig } from 'eslint/config';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
});

export default defineConfig([
  {
    extends: fixupConfigRules(compat.extends('@react-native', 'prettier')),
    plugins: { prettier },
    rules: {
      'react/react-in-jsx-scope': 'off',
      'prettier/prettier': 'error',
    },
  },
  {
    // Build output. The lint script globs `**/*.{js,ts,tsx}`, and Gradle drops
    // JS into its HTML test reports — thousands of prettier errors after any
    // local `./gradlew` run. CI never sees it because it lints a fresh checkout.
    // Kept in step with the `clean` script in package.json.
    ignores: [
      'node_modules/',
      'lib/',
      'android/build/',
      'ios/build/',
      'example/android/build/',
      'example/android/app/build/',
      'example/ios/build/',
    ],
  },
]);
