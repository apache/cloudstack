# The scss files will automatically be compiled in the build for the client ui module.
## How to compile SASS with NPM and GULP for development purposes without building the whole module?

  1. install Node (v11.10.0 will work) on your machine or "N" or "nvm"
  2. run $(npm install)
  3. run $(npm start)
  4. gulp automaticaly watches for .scss changes and compiles children scss-files of scss-folder to css now
  5. Find a plugin for your code-editor to get use of .jsbeautifyrc as second instance for more rules that sass-lint-auto-fix doesn't support. (for vscode: "Beautify")

### Keep package versions up to date if possible. check with $(npm outdated) inside package.json folder
