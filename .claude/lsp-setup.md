# jdtls Setup

## Status

jdtls is installed via Mason (Neovim) at:
`~/.local/share/nvim/mason/packages/jdtls/`

Lombok jar is present — symbol navigation should work correctly.

## Troubleshooting

If symbols are incomplete or Lombok-generated methods are missing:
1. Verify lombok.jar exists: `ls ~/.local/share/nvim/mason/packages/jdtls/lombok.jar`
2. Ensure jdtls JVM args include: `-javaagent:/path/to/lombok.jar`
3. Mason's jdtls package bundles lombok.jar — check mason-receipt.json for config

## Why this matters

With jdtls active, Claude navigates by symbol instead of grep.
Cuts exploration token usage significantly on large codebases.
