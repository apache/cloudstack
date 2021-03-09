Contributing to Apache CloudStack (ACS)
=======================================

Summary
-------
This document covers how to contribute to the ACS project. ACS uses github PRs to manage code contributions. 
These instructions assume you have a GitHub.com account, so if you don't have one you will have to create one. Your proposed code changes will be published to your own fork of the ACS project and you will submit a Pull Request for your changes to be added.

_Lets get started!!!_

Bug fixes
---------

It's very important that we can easily track bug fix commits, so their hashes should remain the same in all branches. 
Therefore, a pull request (PR) that fixes a bug, should be sent against a release branch. 
This can be either the "current release" or the "previous release", depending on which ones are maintained. 
Since the goal is a stable main, bug fixes should be "merged forward" to the next branch in order: "previous release" -> "current release" -> main (in other words: old to new)

Developing new features
-----------------------

Development should be done in a feature branch, branched off of main. 
Send a PR(steps below) to get it into main (2x LGTM applies). 
PR will only be merged when main is open, will be held otherwise until main is open again. 
No back porting / cherry-picking features to existing branches!

PendingReleaseNotes file
------------------------
When developing a new feature or making a (major) change to a existing feature you are encouraged to append this to the PendingReleaseNotes file so that the Release Manager can
use this file as a source of information when compiling the Release Notes for a new release.

When adding information to the PendingReleaseNotes file make sure that you write a good and understandable description of the new feature or change which you have developed.

Updating the PendingReleaseNotes file is preferably a part of the original Pull Request, but that is up to the developers' discretion.

Fork the code 
-------------

In your browser, navigate to: [https://github.com/apache/cloudstack](https://github.com/apache/cloudstack)

Fork the repository by clicking on the 'Fork' button on the top right hand side.  The fork will happen and you will be taken to your own fork of the repository.  Copy the Git repository URL by clicking on the clipboard next to the URL on the right hand side of the page under '**HTTPS** clone URL'.  You will paste this URL when doing the following `git clone` command.

On your computer, follow these steps to setup a local repository for working on ACS:

``` bash
$ git clone https://github.com/YOUR_ACCOUNT/cloudstack.git
$ cd cloudstack
$ git remote add upstream https://github.com/apache/cloudstack.git
$ git checkout main
$ git fetch upstream
$ git rebase upstream/main
```


Making changes
--------------


It is important that you create a new branch to make changes on and that you do not change the `main` branch (other than to rebase in changes from `upstream/main`).  In this example I will assume you will be making your changes to a branch called `feature_x`.  This `feature_x` branch will be created on your local repository and will be pushed to your forked repository on GitHub.  Once this branch is on your fork you will create a Pull Request for the changes to be added to the ACS project.

It is best practice to create a new branch each time you want to contribute to the project and only track the changes for that pull request in this branch.

``` bash
$ git checkout -b feature_x
   (make your changes)
$ git status
$ git add .
$ git commit -a -m "descriptive commit message for your changes"
```

> The `-b` specifies that you want to create a new branch called `feature_x`.  You only specify `-b` the first time you checkout because you are creating a new branch.  Once the `feature_x` branch exists, you can later switch to it with only `git checkout feature_x`.


Rebase `feature_x` to include updates from `upstream/main`
------------------------------------------------------------

It is important that you maintain an up-to-date `main` branch in your local repository.  This is done by rebasing in the code changes from `upstream/main` (the official ACS project repository) into your local repository.  You will want to do this before you start working on a feature as well as right before you submit your changes as a pull request.  I recommend you do this process periodically while you work to make sure you are working off the most recent project code.

This process will do the following:

1. Checkout your local `main` branch
2. Synchronize your local `main` branch with the `upstream/main` so you have all the latest changes from the project
3. Rebase the latest project code into your `feature_x` branch so it is up-to-date with the upstream code

``` bash
$ git checkout main
$ git fetch upstream
$ git rebase upstream/main
$ git checkout feature_x
$ git rebase main
```

> Now your `feature_x` branch is up-to-date with all the code in `upstream/main`.


Make a GitHub Pull Request to contribute your changes
-----------------------------------------------------

When you are happy with your changes and you are ready to contribute them, you will create a Pull Request on GitHub to do so.  This is done by pushing your local changes to your forked repository (default remote name is `origin`) and then initiating a pull request on GitHub.

Please include JIRA id, detailed information about the bug/feature, what all tests are executed, how the reviewer can test this feature etc. Incase of UI PRs, a screenshot is preferred.

> **IMPORTANT:** Make sure you have rebased your `feature_x` branch to include the latest code from `upstream/main` _before_ you do this.

``` bash
$ git push origin main
$ git push origin feature_x
```

Now that the `feature_x` branch has been pushed to your GitHub repository, you can initiate the pull request.  

To initiate the pull request, do the following:

1. In your browser, navigate to your forked repository: [https://github.com/YOUR_ACCOUNT/cloudstack](https://github.com/YOUR_ACCOUNT/cloudstack)
2. Click the new button called '**Compare & pull request**' that showed up just above the main area in your forked repository
3. Validate the pull request will be into the upstream `main` and will be from your `feature_x` branch
4. Enter a detailed description of the work you have done and then click '**Send pull request**'

If you are requested to make modifications to your proposed changes, make the changes locally on your `feature_x` branch, re-push the `feature_x` branch to your fork.  The existing pull request should automatically pick up the change and update accordingly.


Cleaning up after a successful pull request
-------------------------------------------

Once the `feature_x` branch has been committed into the `upstream/main` branch, your local `feature_x` branch and the `origin/feature_x` branch are no longer needed.  If you want to make additional changes, restart the process with a new branch.

> **IMPORTANT:** Make sure that your changes are in `upstream/main` before you delete your `feature_x` and `origin/feature_x` branches!

You can delete these deprecated branches with the following:

``` bash
$ git checkout main
$ git branch -D feature_x
$ git push origin :feature_x
```

Release Principles
------------------
Detailed information about ACS release principles is available at https://cwiki.apache.org/confluence/display/CLOUDSTACK/Release+principles+for+Apache+CloudStack+4.6+and+up 
