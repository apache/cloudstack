# pre-commit

We run [pre-commit](https://pre-commit.com/) with
[GitHub Actions](https://github.com/apache/cloudstack/blob/main/.github/workflows/linter.yml) so installation on your
local machine is currently optional.

The `pre-commit` [configuration file](https://github.com/apache/cloudstack/blob/main/.pre-commit-config.yaml)
is in the repository root. Before you can run the hooks, you need to have `pre-commit` installed. `pre-commit` is a
[Python package](https://pypi.org/project/pre-commit/).

From the repository root run: `pip install -r requirements-dev.txt` to install `pre-commit` and after you install
`pre-commit` you will then need to install the pre-commit hooks by running `pre-commit install`.

The hooks run when running `git commit` and also from the command line with `pre-commit`. Some of the hooks will auto
fix the code after the hooks fail whilst most will print error messages from the linters. If a hook fails the overall
commit will fail, and you will need to fix the issues or problems and `git add` and `git commit` again. On `git commit`
the hooks will run mostly only against modified files so if you want to test all hooks against all files and when you
are adding a new hook you should always run:

`pre-commit run --all-files`

Sometimes you might need to skip a hook to commit because the hook is stopping you from committing or your computer
might not have all the installation requirements for all the hooks. The `SKIP` variable is comma separated for two or
more hooks:

`SKIP=codespell git commit -m "foo"`

The same applies when running pre-commit:

`SKIP=codespell pre-commit run --all-files`

Occasionally you can have more serious problems when using `pre-commit` with `git commit`. You can use `--no-verify` to
commit and stop `pre-commit` from checking the hooks. For example:

`git commit --no-verify -m "foo"`

If you are having major problems using `pre-commit` you can always uninstall it.

To run a single hook use `pre-commit run --all-files <hook_id>`

For example just run the `codespell` hook:

`pre-commit run --all-files codespell`
