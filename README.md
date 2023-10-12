# dipix-ledger-police
It's basically copypasted /ledger inspect with age limit.

Needs patched Ledger v1.2.8, with full kotlin metadata

To compile, publish ledger to mavenLocal.

Most of the code is part of Ledger licensed under LGPL-3.0

Config:
```toml
[dipix-police]
fingerprintMaxAge = 259200 # max age in seconds. no default value, this one is 3 days
```