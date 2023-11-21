#!/usr/bin/env bash

# Helper function to read absolute file location of symlink
rreadlink() ( # execute function in a *subshell* to localize the effect of `cd`, ...

  local target=$1 fname targetDir readlinkexe=$(command -v readlink) CDPATH=

  # Since we'll be using `command` below for a predictable execution
  # environment, we make sure that it has its original meaning.
  { \unalias command; \unset -f command; } &>/dev/null

  while :; do # Resolve potential symlinks until the ultimate target is found.
      [[ -L $target || -e $target ]] || { command printf '%s\n' "$FUNCNAME: ERROR: '$target' does not exist." >&2; return 1; }
      command cd "$(command dirname -- "$target")" # Change to target dir; necessary for correct resolution of target path.
      fname=$(command basename -- "$target") # Extract filename.
      [[ $fname == '/' ]] && fname='' # !! curiously, `basename /` returns '/'
      if [[ -L $fname ]]; then
        # Extract [next] target path, which is defined
        # relative to the symlink's own directory.
        if [[ -n $readlinkexe ]]; then # Use `readlink`.
          target=$("$readlinkexe" -- "$fname")
        else # `readlink` utility not available.
          # Parse `ls -l` output, which, unfortunately, is the only POSIX-compliant
          # way to determine a symlink's target. Hypothetically, this can break with
          # filenames containing literal ' -> ' and embedded newlines.
          target=$(command ls -l -- "$fname")
          target=${target#* -> }
        fi
        continue # Resolve [next] symlink target.
      fi
      break # Ultimate target reached.
  done
  targetDir=$(command pwd -P) # Get canonical dir. path
  # Output the ultimate target's canonical path.
  # Note that we manually resolve paths ending in /. and /.. to make sure we
  # have a normalized path.
  if [[ $fname == '.' ]]; then
    command printf '%s\n' "${targetDir%/}"
  elif  [[ $fname == '..' ]]; then
    # Caveat: something like /var/.. will resolve to /private (assuming
    # /var@ -> /private/var), i.e. the '..' is applied AFTER canonicalization.
    command printf '%s\n' "$(command dirname -- "${targetDir}")"
  else
    command printf '%s\n' "${targetDir%/}/$fname"
  fi
)

scriptDir=$(dirname -- "$(rreadlink "$BASH_SOURCE")")
projectDir="$scriptDir"/..
libsDir="$projectDir"/build/libs

# Build rust
cargo build --release --manifest-path="$projectDir"/worker/Cargo.toml

# Copy executable
cp "$projectDir"/worker/target/release/worker "$libsDir"
# Copy launch script
cp "$scriptDir"/launch-script.sh "$libsDir"/TiM
# Copy fonts
cp -r "$projectDir"/fonts "$libsDir"
# Mark both executable
chmod +x "$libsDir"/worker
chmod +x "$libsDir"/TiM

# Copy config if exists
if [ -e "$projectDir"/config.toml ]
 then cp "$projectDir"/config.toml "$libsDir"/config.toml
fi
