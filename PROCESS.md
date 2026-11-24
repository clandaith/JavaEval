## The plan you started with, and whether you asked the AI to plan first.
I started with the AI reading the PRD.  I gave a bunch of context to start with.  Then I had the AI prompt me with questions before it was allowed to create any code.

## Your workflow step by step: each main prompt, what the AI produced, and what you accepted, changed, or rejected.
This can be found in the AI-Interactions.txt file.  I had the AI write out all our interactions.

## At least two places where the AI was wrong or went off track, and how you noticed.
I noticed the AI hadn't created enough unit tests.  This was based on me looking at the /test directory.  

I also noticed that the AI wasn't logging all of our interactions.

## What you wrote or fixed by hand.
Other than this file, PROCESS.md, I wrote 0% of the code.  It was all created by AI.

## How you checked that it works.
I had the AI run all the unit tests and test the app backend via cURL commands.  I then launched the app myself and went through all the PRD requirements.

## Roughly how long you spent, and what you'd do with another hour.
I spent maybe 15 minutes with the AI.  I then spent another 10 minutes testing the app myself without AI help.  

If I had another hour, I would work with the AI to create boilerplate for this type of application.  I build out guardrails for the missed parts.  I would also start to train the AI on the requirements and PRD doc to know what to expect.

