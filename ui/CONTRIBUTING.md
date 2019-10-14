# Contributing to Primate

## Summary

This document covers how to contribute to the Primate project. Primate uses Github PRs to manage code contributions.
These instructions assume you have a GitHub.com account, so if you don't have one you will have to create one.
Your proposed code changes will be published to your own fork of the Primate project and you will submit a Pull Request for your changes to be added.

Please refer to project [docs](docs) for reference on standard way of component
configuration, development, usage, extension and testing.

*Lets get started!!!*

### Bug fixes

It's very important that we can easily track bug fix commits, so their hashes should remain the same in all branches.
Therefore, a pull request (PR) that fixes a bug, should be sent against a release branch.
This can be either the "current release" or the "previous release", depending on which ones are maintained.
Since the goal is a stable master, bug fixes should be "merged forward" to the next branch in order: "previous release" -> "current release" -> master (in other words: old to new)

### New features

Development should be done in a feature branch, branched off of master.
Send a PR (steps below) to get it into master (at least 2x LGTM applies).
PR will only be merged when master is open, will be held otherwise until master is open again.
No back porting / cherry-picking features to existing branches!

## Forking

In your browser, navigate to: [https://github.com/shapeblue/primate](https://github.com/shapeblue/primate)

Fork the repository by clicking on the 'Fork' button on the top right hand side.
The fork will happen and you will be taken to your own fork of the repository.
Copy the Git repository URL by clicking on the clipboard next to the URL on the right hand side of the page under '**HTTPS** clone URL'.  You will paste this URL when doing the following `git clone` command.

On your workstation, follow these steps to setup a local repository for working on Primate:

``` bash
$ git clone https://github.com/YOUR_ACCOUNT/primate.git
$ cd primate
$ git remote add upstream https://github.com/shapeblue/primate.git
$ git checkout master
$ git fetch upstream
$ git rebase upstream/master
```

## Making changes


It is important that you create a new branch to make changes on and that you do not change the `master` branch (other than to rebase in changes from `upstream/master`). In this example I will assume you will be making your changes to a branch called `feature_x`.
This `feature_x` branch will be created on your local repository and will be pushed to your forked repository on GitHub. Once this branch is on your fork you will create a Pull Request for the changes to be added to the Primate project.

It is best practice to create a new branch each time you want to contribute to the project and only track the changes for that pull request in this branch.

``` bash
$ git checkout -b feature_x
   (make your changes)
$ git status
$ git add .
$ git commit -a -m "descriptive commit message for your changes"
```

> The `-b` specifies that you want to create a new branch called `feature_x`.  You only specify `-b` the first time you checkout because you are creating a new branch.  Once the `feature_x` branch exists, you can later switch to it with only `git checkout feature_x`.


### Updating your branch

It is important that you maintain an up-to-date `master` branch in your local repository. You may do this by either rebasing against the upstream repository or merging the upstream branch.
For example:

1. Checkout your local `master` branch
2. Synchronize your local `master` branch with the `upstream/master` so you have all the latest changes from the project
3. Merge or Rebase the latest project code into your `feature_x` branch so it is up-to-date with the upstream code

``` bash
$ git checkout master
$ git fetch upstream
$ git rebase upstream/master
$ git checkout feature_x
$ git merge master
```

> Now your `feature_x` branch is up-to-date with all the code in `upstream/master`.


## Sending a Pull Request

When you are happy with your changes and you are ready to contribute them, you will create a Pull Request on GitHub to do so.
This is done by pushing your local changes to your forked repository (default remote name is `origin`) and then initiating a pull request on GitHub.

Please include relevant issue ids, links, detailed information about the bug/feature, what all tests are executed, how the reviewer can test this feature etc. A screenshot is preferred.

> **IMPORTANT:** Make sure you have rebased your `feature_x` branch to include the latest code from `upstream/master` _before_ you do this.

``` bash
$ git push origin master
$ git push origin feature_x
```

Now that the `feature_x` branch has been pushed to your GitHub repository, you can initiate the pull request.

To initiate the pull request, do the following:

1. In your browser, navigate to your forked repository: [https://github.com/YOUR_ACCOUNT/primate](https://github.com/YOUR_ACCOUNT/primate)
2. Click the new button called '**Compare & pull request**' that showed up just above the main area in your forked repository
3. Validate the pull request will be into the upstream `master` and will be from your `feature_x` branch
4. Enter a detailed description of the work you have done and then click '**Send pull request**'

If you are requested to make modifications to your proposed changes, make the changes locally on your `feature_x` branch, re-push the `feature_x` branch to your fork.  The existing pull request should automatically pick up the change and update accordingly.


Cleaning up after a successful pull request
-------------------------------------------

Once the `feature_x` branch has been committed into the `upstream/master` branch, your local `feature_x` branch and the `origin/feature_x` branch are no longer needed.  If you want to make additional changes, restart the process with a new branch.

> **IMPORTANT:** Make sure that your changes are in `upstream/master` before you delete your `feature_x` and `origin/feature_x` branches!

You can delete these deprecated branches with the following:

``` bash
$ git checkout master
$ git branch -D feature_x
$ git push origin :feature_x
``
