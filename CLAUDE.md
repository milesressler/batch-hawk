# Claude Code Instructions

Read `AGENTS.md` first — it is the primary source of truth for project context, conventions, architecture decisions, and key commands.

## Claude-specific notes

- When changing a response DTO or adding an endpoint, remind the user to run `./gradlew :api:generateClientTypes` and commit the updated `web/src/api-types.ts`.
- `web/src/api-types.ts` is generated — never edit it directly.
- Java style: use `final` on locals and parameters, prefer streams and `Optional` chaining.
