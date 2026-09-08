# Repair Desk: keep the learner in the loop

A host upgrade to the existing Counterstep Relay project. Original work, MIT licensed, with substantial AI assistance. The exact checker, learned ranker, MCP SDK and replay rules are unchanged.

## What a learner can now do

Fix the first faulty line without retyping the whole solution. The original problem remains locked in this line editor. The complete revised chain is sent to the real `repair_turn` tool; later lines are preserved, so correcting one mistake may expose a different mistake next. Equivalent unfinished work can append a next equation. The full workboard remains available for changing several lines.

The next-step guide responds to the checked state: repair, unsupported expression, unfinished work, fresh-number practice, or an exported study note. It does not supply a correct equation or reveal a counterexample automatically. Existing explicit hint and reveal controls remain.

The readable Markdown study note comes from `repair_report`, which replays the supplied session. It lists the actual work, recorded attempts, and separate independent, assisted and revised completions. A repair is not counted as a new independent practice answer. A Markdown note is not an importable session; save the handoff JSON to resume.

## Useful evidence, not inflated promises

The previous release is the comparison: changing one line formerly required using the entire workboard; review export was machine-readable JSON. This update adds a targeted edit and a readable handoff. It does not establish measured time savings, improved learning, teacher approval or production accessibility. Such outcomes require authorized user evaluation.

The implementation contains sixteen new Node test methods and an actual browser workflow suite. Read `evidence/desk-release.json` for which gates actually passed, and `evidence/desk-public-browser.json` for any later public-origin verification. A written test is not an executed result.

## Scope and deployment

This is still an explicit-command Alexa+ experience simulator calling a real self-hosted MCP service, not live Alexa or a natural-language agent. No AWS runtime integration is claimed. Synthetic equations only in public examples. The same 2-to-12-line linear-equation limit applies. The line editor checks one equation at a time, with a 300-character limit.

Imported fingerprints identify bytes, not authorship or a certified grade. Anyone controlling a session can construct another internally valid history. Every supported equation transition is checked, but unwritten reasoning and understanding are not inferred.

Keep the submitted original immutable until this candidate passes release and public-origin gates. Do not merge a portfolio preview branch into the unrelated portfolio production branch. The dedicated Counterstep-Relay repository is the source of record.
