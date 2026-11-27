<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 -->

# Writing

How text is written in the tree, in commits, in pull requests and in review
replies. Many contributors and reviewers read English as a second language, so
plain text is faster to review and harder to misread.

## Contents

- [Voice](#voice)
- [Text by surface](#text-by-surface)
- [Code comments](#code-comments)
- [Commit messages](#commit-messages)
- [Pull request descriptions](#pull-request-descriptions)
- [Review replies](#review-replies)
- [Words to avoid](#words-to-avoid)

## Voice

| Rule | Detail |
| --- | --- |
| Use plain, spoken English | Short sentences, one point each. If a sentence has to be read twice, rewrite it |
| Start with the point | Cut openers such as "It is worth noting that" or "The key insight is" |
| Cut what adds nothing | If the reader would act the same without a sentence, delete it |
| Prefer tables, lists and examples | They read faster than paragraphs for options, steps, cases and before/after output |
| Keep measured numbers | "Startup went from 41 s to 6 s" is evidence |
| Say each fact once | Link to the document that owns it; two copies drift apart |

## Text by surface

- **Code comments, Javadoc and documents in the tree** describe the code as it is now. They don't say how it used to work.
- **API descriptions and error messages** say what a parameter or call does. An error says what was refused and what to do instead.
- **Log messages** say what happened and to which object.
- **Commit messages and PR descriptions** say why the change was made. They are the only place for history.

## Code comments

Add a comment only for a reason the code cannot show: an invariant, a
constraint, a workaround for a hypervisor or library quirk, or evidence such as
a log line or a spec excerpt.

| Rule | Example of what to cut |
| --- | --- |
| Don't restate the code | `// increment the counter` |
| Don't narrate control flow | `// first check the cache, then the DB, then fall back to...` |
| Don't describe the edit | `// changed to use the new helper`, `// no longer needed` |
| No issue, PR or ticket numbers | `// fix for #1234` |
| No timeline words | `// for now`, `// temporary`, `// will be removed later` |
| Prefer a method over a comment | `// check the offering is usable` above ten lines: move them into `validateOffering()` |

One or two lines is the normal length. A comment longer than its code needs a
reason a reviewer would agree with.

## Commit messages

- The subject is one line that starts with a verb: "Refuse to resize the volume of a running system VM".
- A body, when there is one, says why; the diff shows what.
- PRs are squash-merged with the PR title as the subject, so the title matters more than the branch's commits.

## Pull request descriptions

Reviewers use the description to decide whether to review now. Months later,
someone reads it to find out why the code changed. Write for both.

1. **Fill in the template.** Use [PULL_REQUEST_TEMPLATE.md](../../PULL_REQUEST_TEMPLATE.md) and keep all its sections.
2. **Make the title say what the change does.** Start with a verb, and use the words someone would search for: "Fix VM migration with attached ISOs".
3. **Start with the problem.** Say what went wrong and what happens now. Put details after that.
4. **Show before and after.** Paste the command output, API response or log line that changed. This convinces a reviewer faster than an explanation.
5. **Say how you tested it.** Name the environment, the steps and what you saw. CI passing is not enough on its own.
6. **Say what is not done yet,** and whether a follow-up PR will do it.
7. **Don't walk through the diff file by file.** The reviewer can read the diff.
8. **Don't number PRs in a series** ("2/5"). The numbers stop matching as PRs merge or close. Write "Depends on #N" instead.

## Review replies

- Answer each point: what changed (with the commit), or why not.
- Decline a suggestion that is wrong, with the reason from the code.
- Keep replies short; a reviewer has many PRs.

## Words to avoid

| Avoid | Write |
| --- | --- |
| "for now", "currently", "temporarily", "at this stage" | Say what the code does. If something is missing, say it is not supported |
| "robust", "comprehensive", "seamless", "leverage", "utilize" | Say what it does: "retries three times", "uses" |
| "This PR...", "This change..." as an opener | Start with what was wrong |
| "Note that", "Importantly", "It should be noted" | Delete them |
