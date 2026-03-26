# 2025.3+ Compatibility Design

## Summary

Upgrade the plugin from the legacy IntelliJ Gradle plugin and 2023.2-era platform baseline to the current JetBrains-supported 2025.3+ model. The new minimum supported IDE version will be IntelliJ Platform build `253` and the target compatibility window will explicitly include `2026.1` build line `261.*`.

This change is a compatibility and build-chain migration, not a feature redesign. The plugin's existing user-facing behavior should remain intact: it still adds a commit-message generation action to the commit workflow, analyzes selected changes, and writes the generated message back into the commit UI.

## Goals

- Support IntelliJ Platform `2025.3+`.
- Explicitly support IntelliJ IDEA `2026.1`.
- Align the build with the current JetBrains plugin toolchain requirements.
- Keep current plugin behavior and configuration semantics unchanged unless an API migration makes a small adjustment necessary.

## Non-Goals

- Preserving compatibility with `2025.2` or earlier IDEs.
- Refactoring unrelated business logic or UI.
- Adding new AI providers, settings, or commit-message features.
- Reworking the plugin architecture beyond what is required for compatibility.

## Current State

The repository currently targets IntelliJ Platform `2023.2.5` and uses the legacy `org.jetbrains.intellij` Gradle plugin `1.17.4`. Compatibility is capped at `untilBuild=253.*`, which prevents installation on `2026.1` (`261.*`). The project also compiles against Java `17`, which is below the preferred baseline for current IntelliJ Platform targets.

The highest-risk integration points are:

- Build configuration and plugin packaging.
- Commit dialog integration via `CheckinProjectPanel` and related VCS APIs.
- Diff generation via IntelliJ patch APIs.
- Startup activity registration across old and new extension models.

## Selected Approach

Adopt a single upgraded baseline for `2025.3+` rather than maintaining dual build tracks.

This means:

- Migrate from `org.jetbrains.intellij` `1.x` to IntelliJ Platform Gradle Plugin `2.x`.
- Move the Java and Kotlin compilation target to `21`.
- Upgrade the Gradle wrapper to a current `8.14.x` release compatible with the new plugin.
- Switch the target IDE product type from legacy `IC` to `IU`, matching the JetBrains 2025.3+ product model.
- Set plugin compatibility to start at build `253` and extend through `261.*`.

This approach minimizes long-term maintenance cost and matches JetBrains guidance for modern plugin builds. A dual-track build would add complexity without clear benefit for this repository.

## Planned File Changes

### Build Configuration

Files:

- `build.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/gradle-wrapper.properties`

Planned changes:

- Replace legacy IntelliJ Gradle plugin usage with IntelliJ Platform Gradle Plugin `2.x`.
- Update the plugin DSL and dependency declarations to the new format.
- Raise the platform baseline to a `2025.3` IntelliJ IDEA Ultimate target.
- Raise Java and Kotlin JVM target levels to `21`.
- Update Gradle wrapper from `8.5` to `8.14.x`.
- Unify version and compatibility metadata so the project does not maintain conflicting values in both Gradle build logic and project properties.
- Ensure signing and publishing tasks still read credentials from environment variables.

### Plugin Metadata

File:

- `src/main/resources/META-INF/plugin.xml`

Planned changes:

- Keep static plugin metadata and extension declarations that remain valid.
- Let Gradle inject plugin version, `since-build`, and `until-build` values so `plugin.xml` does not duplicate compatibility metadata.
- Re-check extension declarations that interact with startup and VCS commit integration for 2025.3+ validity.

### Source Compatibility Adjustments

Files most likely to change:

- `src/main/java/com/github/jdami/aicommit/startup/PluginUpdateActivity.java`
- `src/main/java/com/github/jdami/aicommit/vcs/CommitMessageGeneratorCheckinHandlerFactory.java`
- `src/main/java/com/github/jdami/aicommit/actions/GenerateCommitMessageAction.java`
- `src/main/java/com/github/jdami/aicommit/util/UnifiedDiffGenerator.java`

Planned changes:

- Resolve any compile or verifier failures caused by API changes between the current baseline and `2025.3+`.
- Prefer the smallest viable adaptation when an API has changed.
- Preserve existing runtime behavior unless a JetBrains API migration requires a minor integration change.

## Compatibility Rules

- Minimum supported build: `253`
- Maximum supported build for this change: `261.*`
- Minimum runtime/toolchain: Java `21`
- Supported branch after migration: IntelliJ IDEA `2025.3`, `2026.1`

## Validation Plan

### Build Validation

- Run `./gradlew buildPlugin` to verify dependency resolution, code compilation, plugin XML patching, and ZIP packaging.

### Compatibility Validation

- Run the IntelliJ Platform verifier task provided by the new Gradle plugin against `2025.3` and `2026.1` targets.
- Treat any missing class, removed method, invalid extension, or binary compatibility issue as a blocker.

### Runtime Smoke Validation

- If local environment permits, run `./gradlew runIde`.
- Open the commit dialog and verify:
  - the action is present,
  - generation can be triggered,
  - generated text is written back into the commit message field.

If runtime smoke testing cannot be completed locally, the work may still ship with successful build and verifier results, but that limitation must be reported explicitly.

## Risks and Mitigations

### Commit Workflow API Drift

Risk:
`CheckinProjectPanel` and surrounding commit UI APIs have changed across platform releases.

Mitigation:
Compile first against the new platform, then apply the minimum integration update needed to keep the action visible and able to write the message back.

### Patch/Diff API Instability

Risk:
`IdeaTextPatchBuilder` and related patch-writing APIs may have changed behavior or signatures.

Mitigation:
Keep the existing implementation if it compiles and verifies. If not, replace it with the smallest equivalent patch-generation path supported by the target platform.

### Build DSL Migration Errors

Risk:
The migration from the old Gradle plugin DSL to the new IntelliJ Platform plugin DSL may break packaging or verifier configuration.

Mitigation:
Treat `buildPlugin` and verifier execution as mandatory completion gates.

## Constraints

- Do not touch unrelated untracked workspace files.
- Do not change plugin behavior unless required for compatibility.
- Do not preserve old-platform compatibility.

## Success Criteria

The migration is complete when all of the following are true:

1. The project builds successfully with the new toolchain.
2. The plugin declares compatibility starting at `2025.3`.
3. The plugin verifier reports no blocking compatibility issues for `2025.3` and `2026.1`.
4. No intentional user-facing feature changes were introduced as part of the migration.
